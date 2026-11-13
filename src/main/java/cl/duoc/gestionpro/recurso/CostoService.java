package cl.duoc.gestionpro.recurso;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.duoc.gestionpro.planificacion.Actividad;
import cl.duoc.gestionpro.planificacion.ActividadRepository;
import cl.duoc.gestionpro.planificacion.ArbolActividades;
import cl.duoc.gestionpro.planificacion.CalendarioFactory;
import cl.duoc.gestionpro.planificacion.CalendarioLaboral;

/**
 * Costos planificados y reales por actividad. Los recursos solo se asignan a hojas; los elementos con hijos
 * suman los de sus descendientes, así que nada se cuenta dos veces. Calcula también el valor ganado (EV =
 * costo planificado × avance) y el valor planificado a la fecha (PV = costo planificado × fracción del plazo
 * planificado transcurrida).
 */
@Service
public class CostoService {

    public record CostoActividad(BigDecimal planProfesionales, BigDecimal planRecursos,
            BigDecimal realProfesionales, BigDecimal realRecursos, int asignaciones) {

        static final CostoActividad CERO = new CostoActividad(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0);

        public BigDecimal plan() {
            return planProfesionales.add(planRecursos);
        }

        public BigDecimal real() {
            return realProfesionales.add(realRecursos);
        }

        CostoActividad mas(CostoActividad o) {
            return new CostoActividad(planProfesionales.add(o.planProfesionales), planRecursos.add(o.planRecursos),
                    realProfesionales.add(o.realProfesionales), realRecursos.add(o.realRecursos), asignaciones + o.asignaciones);
        }
    }

    /** Costos del proyecto: por actividad (hojas y consolidados) y totales. */
    public record Costos(Map<Long, CostoActividad> porActividad, BigDecimal plan, BigDecimal real,
            BigDecimal valorGanado, BigDecimal valorPlanificado) {

        public CostoActividad de(Long actividadId) {
            return porActividad.getOrDefault(actividadId, CostoActividad.CERO);
        }
    }

    private final ActividadRepository actividades;
    private final AsignacionProfesionalRepository asignacionesProfesional;
    private final AsignacionRecursoRepository asignacionesRecurso;
    private final CalendarioFactory calendarios;

    public CostoService(ActividadRepository actividades, AsignacionProfesionalRepository asignacionesProfesional,
            AsignacionRecursoRepository asignacionesRecurso, CalendarioFactory calendarios) {
        this.actividades = actividades;
        this.asignacionesProfesional = asignacionesProfesional;
        this.asignacionesRecurso = asignacionesRecurso;
        this.calendarios = calendarios;
    }

    @Transactional(readOnly = true)
    public Costos calcular(Long proyectoId, LocalDate hoy) {
        List<Actividad> lista = actividades.findByProyectoIdOrderByOrdenAscIdAsc(proyectoId);
        return calcular(lista, asignacionesProfesional.deProyecto(proyectoId), asignacionesRecurso.deProyecto(proyectoId),
                calendarios.deProyecto(proyectoId), hoy);
    }

    /** Versión pura (sin base de datos) para reutilizar datos ya cargados. */
    public static Costos calcular(List<Actividad> lista, List<AsignacionProfesional> profesionales,
            List<AsignacionRecurso> recursos, CalendarioLaboral cal, LocalDate hoy) {
        Map<Long, CostoActividad> hojas = new HashMap<>();
        for (AsignacionProfesional a : profesionales) {
            hojas.merge(a.getActividadId(),
                    new CostoActividad(a.costoPlan(), BigDecimal.ZERO, a.getCostoReal(), BigDecimal.ZERO, 1), CostoActividad::mas);
        }
        for (AsignacionRecurso a : recursos) {
            hojas.merge(a.getActividadId(),
                    new CostoActividad(BigDecimal.ZERO, a.costoPlan(), BigDecimal.ZERO, a.getCostoReal(), 1), CostoActividad::mas);
        }
        ArbolActividades arbol = ArbolActividades.de(lista);
        Map<Long, CostoActividad> porActividad = new HashMap<>();
        BigDecimal plan = BigDecimal.ZERO;
        BigDecimal real = BigDecimal.ZERO;
        BigDecimal ev = BigDecimal.ZERO;
        BigDecimal pv = BigDecimal.ZERO;
        for (Actividad hoja : arbol.hojas()) {
            CostoActividad c = hojas.getOrDefault(hoja.getId(), CostoActividad.CERO);
            // la hoja y todos sus ancestros acumulan su costo
            for (Actividad x = hoja; x != null; x = x.getPadreId() == null ? null : arbol.get(x.getPadreId())) {
                porActividad.merge(x.getId(), c, CostoActividad::mas);
            }
            plan = plan.add(c.plan());
            real = real.add(c.real());
            ev = ev.add(c.plan().multiply(hoja.getAvance()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
            pv = pv.add(c.plan().multiply(fraccionPlanificada(hoja, hoy, cal)).setScale(2, RoundingMode.HALF_UP));
        }
        return new Costos(porActividad, plan, real, ev, pv);
    }

    /** Parte del plazo planificado de la hoja que ya transcurrió a la fecha (0 a 1). */
    public static BigDecimal fraccionPlanificada(Actividad a, LocalDate hoy, CalendarioLaboral cal) {
        if (a.getFechaInicioPlan() == null || a.getFechaTerminoPlan() == null || hoy.isBefore(a.getFechaInicioPlan())) {
            return BigDecimal.ZERO;
        }
        if (!hoy.isBefore(a.getFechaTerminoPlan())) {
            return BigDecimal.ONE;
        }
        int total = cal.contar(a.getFechaInicioPlan(), a.getFechaTerminoPlan());
        if (total <= 0) {
            return BigDecimal.ONE;
        }
        int transcurridos = cal.contar(a.getFechaInicioPlan(), hoy);
        return BigDecimal.valueOf(transcurridos).divide(BigDecimal.valueOf(total), 6, RoundingMode.HALF_UP);
    }
}
