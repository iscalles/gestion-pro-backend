package cl.duoc.gestionpro.planificacion.dto;

import java.time.LocalDate;
import java.util.List;

/** Planificación completa de un proyecto. Las actividades vienen en orden de árbol (etapa, sus hijos, …). */
public record PlanDto(
        Long proyectoId,
        Long calendarioId,
        LocalDate fechaInicioProyecto,
        List<ActividadDto> actividades,
        List<DependenciaDto> dependencias) {
}
