package cl.duoc.gestionpro.recalculo;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.duoc.gestionpro.comun.RecursoNoEncontradoException;
import cl.duoc.gestionpro.planificacion.Actividad;
import cl.duoc.gestionpro.planificacion.ActividadRepository;
import cl.duoc.gestionpro.planificacion.CalendarioLaboral;
import cl.duoc.gestionpro.planificacion.DependenciaRepository;
import cl.duoc.gestionpro.planificacion.MotorProgramacion;
import cl.duoc.gestionpro.planificacion.calendario.CalendarioRepository;
import cl.duoc.gestionpro.planificacion.calendario.DiaNoLaborable;
import cl.duoc.gestionpro.planificacion.calendario.DiaNoLaborableRepository;
import cl.duoc.gestionpro.proyecto.Proyecto;
import cl.duoc.gestionpro.proyecto.ProyectoRepository;

/**
 * Cadena de recálculo. Hoy: fechas (MotorProgramacion) → avance del proyecto.
 * Los módulos de costos, KPI y alertas se enganchan aquí, después de las fechas.
 * Corre en la transacción de quien llama: si algo falla, no queda nada a medias.
 */
@Service
public class RecalculoService {

    private final ProyectoRepository proyectos;
    private final ActividadRepository actividades;
    private final DependenciaRepository dependencias;
    private final CalendarioRepository calendarios;
    private final DiaNoLaborableRepository diasNoLaborables;
    private final MotorProgramacion motor;

    public RecalculoService(ProyectoRepository proyectos, ActividadRepository actividades, DependenciaRepository dependencias,
            CalendarioRepository calendarios, DiaNoLaborableRepository diasNoLaborables, MotorProgramacion motor) {
        this.proyectos = proyectos;
        this.actividades = actividades;
        this.dependencias = dependencias;
        this.calendarios = calendarios;
        this.diasNoLaborables = diasNoLaborables;
        this.motor = motor;
    }

    /** Reprograma el proyecto y devuelve las actividades cuyas fechas planificadas cambiaron. */
    @Transactional
    public List<CambioFecha> recalcularProyecto(Long proyectoId) {
        Proyecto proyecto = proyectos.findById(proyectoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Proyecto", proyectoId));
        List<Actividad> lista = actividades.findByProyectoIdOrderByOrdenAscIdAsc(proyectoId);
        if (lista.isEmpty()) {
            return List.of();
        }
        Map<Long, LocalDate[]> antes = new HashMap<>();
        lista.forEach(a -> antes.put(a.getId(), new LocalDate[] { a.getFechaInicioPlan(), a.getFechaTerminoPlan() }));

        CalendarioLaboral cal = calendarioDe(proyecto.getCalendarioId());
        motor.programar(proyecto.getFechaInicioPlan(), lista, dependencias.findByProyectoIdOrderByIdAsc(proyectoId), cal);

        List<Actividad> hojas = lista.stream()
                .filter(a -> lista.stream().noneMatch(h -> a.getId().equals(h.getPadreId())))
                .toList();
        proyecto.setAvance(MotorProgramacion.avancePonderado(hojas));

        List<CambioFecha> cambios = new ArrayList<>();
        for (Actividad a : lista) {
            LocalDate[] previo = antes.get(a.getId());
            if (!Objects.equals(previo[0], a.getFechaInicioPlan()) || !Objects.equals(previo[1], a.getFechaTerminoPlan())) {
                cambios.add(new CambioFecha(a.getId(), a.getNombre(), previo[0], previo[1],
                        a.getFechaInicioPlan(), a.getFechaTerminoPlan()));
            }
        }
        cambios.sort(java.util.Comparator.comparing(CambioFecha::inicio).thenComparing(CambioFecha::nombre));
        // Aquí se agregarán: costos → KPI (nuevas mediciones) → alertas.
        return cambios;
    }

    /** Reprograma todos los proyectos que usan un calendario. */
    @Transactional
    public void recalcularCalendario(Long calendarioId) {
        proyectos.findByCalendarioId(calendarioId).forEach(p -> recalcularProyecto(p.getId()));
    }

    @Transactional(readOnly = true)
    public CalendarioLaboral calendarioDe(Long calendarioId) {
        var c = calendarios.findById(calendarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Calendario", calendarioId));
        var feriados = diasNoLaborables.findByCalendarioIdOrderByFechaAsc(calendarioId).stream()
                .map(DiaNoLaborable::getFecha).toList();
        return new CalendarioLaboral(c.diasLaborables(), feriados);
    }
}
