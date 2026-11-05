package cl.duoc.gestionpro.planificacion.dto;

import java.util.List;

import cl.duoc.gestionpro.recalculo.CambioFecha;

/** Respuesta de toda operación que modifica la planificación: el plan nuevo y qué fechas se movieron. */
public record PlanCambioDto(PlanDto plan, List<CambioFecha> recalculadas) {
}
