package cl.duoc.gestionpro.recurso.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import cl.duoc.gestionpro.planificacion.TipoActividad;

/** Costo de una actividad; en las que tienen hijos es la suma de sus descendientes. */
public record CostoActividadDto(Long id, Long padreId, String codigoEdt, String nombre, TipoActividad tipo, int nivel,
        boolean esHoja, int duracionPlan, LocalDate fechaInicioPlan, LocalDate fechaTerminoPlan, BigDecimal avance,
        BigDecimal planProfesionales, BigDecimal planRecursos, BigDecimal costoPlan, BigDecimal costoReal,
        int asignaciones) {
}
