package cl.duoc.gestionpro.proyecto.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import cl.duoc.gestionpro.proyecto.EstadoProyecto;
import cl.duoc.gestionpro.proyecto.Proyecto;

/**
 * Misma forma que la interfaz `Proyecto` del frontend. terminoPrevisto es la fecha de término que resulta de la
 * programación; costoPlan y costoReal salen de las asignaciones de profesionales y recursos.
 */
public record ProyectoDto(
        Long id,
        String codigo,
        String nombre,
        String descripcion,
        Long responsableId,
        String responsable,
        Long calendarioId,
        LocalDate fechaInicio,
        LocalDate fechaTermino,
        BigDecimal presupuesto,
        EstadoProyecto estado,
        BigDecimal avance,
        LocalDate fechaInicioReal,
        LocalDate fechaTerminoReal,
        LocalDate terminoPrevisto,
        BigDecimal costoPlan,
        BigDecimal costoReal,
        List<Long> miembroIds,
        int solicitudesAbiertas) {

    public static ProyectoDto de(Proyecto p, LocalDate terminoPrevisto, BigDecimal costoPlan, BigDecimal costoReal,
            List<Long> miembroIds, int solicitudesAbiertas) {
        return new ProyectoDto(p.getId(), p.getCodigo(), p.getNombre(), p.getDescripcion(),
                p.getResponsable().getId(), p.getResponsable().getNombre(), p.getCalendarioId(),
                p.getFechaInicioPlan(), p.getFechaTerminoPlan(), p.getPresupuesto(), p.getEstado(), p.getAvance(),
                p.getFechaInicioReal(), p.getFechaTerminoReal(), terminoPrevisto, costoPlan, costoReal, miembroIds,
                solicitudesAbiertas);
    }
}
