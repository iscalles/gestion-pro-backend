package cl.duoc.gestionpro.recalculo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.duoc.gestionpro.alerta.AlertaService;
import cl.duoc.gestionpro.comun.RecursoNoEncontradoException;
import cl.duoc.gestionpro.kpi.KpiService;
import cl.duoc.gestionpro.kpi.OrigenMedicion;
import cl.duoc.gestionpro.planificacion.Actividad;
import cl.duoc.gestionpro.planificacion.ActividadRepository;
import cl.duoc.gestionpro.planificacion.ArbolActividades;
import cl.duoc.gestionpro.planificacion.CalendarioFactory;
import cl.duoc.gestionpro.planificacion.CalendarioLaboral;
import cl.duoc.gestionpro.planificacion.DependenciaRepository;
import cl.duoc.gestionpro.planificacion.MotorProgramacion;
import cl.duoc.gestionpro.proyecto.Proyecto;
import cl.duoc.gestionpro.proyecto.ProyectoRepository;
import cl.duoc.gestionpro.recurso.AsignacionProfesional;
import cl.duoc.gestionpro.recurso.AsignacionProfesionalRepository;

/**
 * Cadena de recálculo de un proyecto, en una sola transacción (la de quien llama):
 * <ol>
 * <li>fechas planificadas (MotorProgramacion, reglas tipo MS Project) y avance ponderado;</li>
 * <li>semanas de cada asignación de profesional = duración / días hábiles por semana, así que una duración nueva
 * cambia el costo planificado;</li>
 * <li>KPI automáticos: se guarda una medición nueva cuando cambia el valor o el semáforo, anotando la solicitud de
 * cambio que la gatilló;</li>
 * <li>alertas: se abren, actualizan o resuelven según el estado resultante.</li>
 * </ol>
 * Si algo falla, no queda nada a medias.
 */
@Service
public class RecalculoService {

    private final ProyectoRepository proyectos;
    private final ActividadRepository actividades;
    private final DependenciaRepository dependencias;
    private final AsignacionProfesionalRepository asignaciones;
    private final CalendarioFactory calendarios;
    private final MotorProgramacion motor;
    private final KpiService kpi;
    private final AlertaService alertas;

    public RecalculoService(ProyectoRepository proyectos, ActividadRepository actividades, DependenciaRepository dependencias,
            AsignacionProfesionalRepository asignaciones, CalendarioFactory calendarios, MotorProgramacion motor,
            KpiService kpi, AlertaService alertas) {
        this.proyectos = proyectos;
        this.actividades = actividades;
        this.dependencias = dependencias;
        this.asignaciones = asignaciones;
        this.calendarios = calendarios;
        this.motor = motor;
        this.kpi = kpi;
        this.alertas = alertas;
    }

    /** Reprograma el proyecto y devuelve las actividades cuyas fechas planificadas cambiaron. */
    @Transactional
    public List<CambioFecha> recalcularProyecto(Long proyectoId) {
        return recalcularProyecto(proyectoId, null);
    }

    /** Igual, anotando en las mediciones de KPI la solicitud de cambio aprobada que originó el recálculo. */
    @Transactional
    public List<CambioFecha> recalcularProyecto(Long proyectoId, Long solicitudId) {
        List<CambioFecha> cambios = reprogramar(proyectoId);
        actualizarIndicadores(proyectoId, solicitudId);
        return cambios;
    }

    /** Solo KPI y alertas (cuando cambian datos que no mueven fechas ni costos: etapas de un cambio, acciones…). */
    @Transactional
    public void actualizarIndicadores(Long proyectoId, Long solicitudId) {
        kpi.medirProyecto(proyectoId, solicitudId, OrigenMedicion.RECALCULO);
        alertas.sincronizar(proyectoId);
    }

    /** Fechas, avance y semanas de las asignaciones, sin medir KPI ni sincronizar alertas. */
    @Transactional
    public List<CambioFecha> reprogramar(Long proyectoId) {
        Proyecto proyecto = proyectos.findById(proyectoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Proyecto", proyectoId));
        List<Actividad> lista = actividades.findByProyectoIdOrderByOrdenAscIdAsc(proyectoId);
        List<CambioFecha> cambios = new ArrayList<>();
        if (!lista.isEmpty()) {
            Map<Long, LocalDate[]> antes = new HashMap<>();
            lista.forEach(a -> antes.put(a.getId(), new LocalDate[] { a.getFechaInicioPlan(), a.getFechaTerminoPlan() }));

            CalendarioLaboral cal = calendarios.deCalendario(proyecto.getCalendarioId());
            motor.programar(proyecto.getFechaInicioPlan(), lista, dependencias.findByProyectoIdOrderByIdAsc(proyectoId), cal);

            ArbolActividades arbol = ArbolActividades.de(lista);
            proyecto.setAvance(MotorProgramacion.avancePonderado(arbol.hojas()));

            for (Actividad a : lista) {
                LocalDate[] previo = antes.get(a.getId());
                if (!Objects.equals(previo[0], a.getFechaInicioPlan()) || !Objects.equals(previo[1], a.getFechaTerminoPlan())) {
                    cambios.add(new CambioFecha(a.getId(), a.getNombre(), previo[0], previo[1],
                            a.getFechaInicioPlan(), a.getFechaTerminoPlan()));
                }
            }
            cambios.sort(Comparator.comparing(CambioFecha::inicio).thenComparing(CambioFecha::nombre));

            // Costos: las semanas de trabajo siguen a la duración de la actividad.
            int diasSemana = calendarios.diasPorSemana(proyecto.getCalendarioId());
            for (AsignacionProfesional asig : asignaciones.deProyecto(proyectoId)) {
                Actividad a = arbol.get(asig.getActividadId());
                if (a != null) {
                    BigDecimal semanas = semanas(a.getDuracionPlan(), diasSemana);
                    if (asig.getSemanas() == null || asig.getSemanas().compareTo(semanas) != 0) {
                        asig.setSemanas(semanas);
                    }
                }
            }
        }
        actividades.flush();
        return cambios;
    }

    /** Reprograma todos los proyectos que usan un calendario. */
    @Transactional
    public void recalcularCalendario(Long calendarioId) {
        proyectos.findByCalendarioId(calendarioId).forEach(p -> recalcularProyecto(p.getId()));
    }

    @Transactional(readOnly = true)
    public CalendarioLaboral calendarioDe(Long calendarioId) {
        return calendarios.deCalendario(calendarioId);
    }

    /** Semanas de trabajo de una actividad: duración en días hábiles / días hábiles por semana (mínimo un día). */
    public static BigDecimal semanas(Integer duracion, int diasPorSemana) {
        int dias = Math.max(duracion == null ? 0 : duracion, 1);
        return BigDecimal.valueOf(dias).divide(BigDecimal.valueOf(Math.max(diasPorSemana, 1)), 2, RoundingMode.HALF_UP)
                .max(new BigDecimal("0.01"));
    }
}
