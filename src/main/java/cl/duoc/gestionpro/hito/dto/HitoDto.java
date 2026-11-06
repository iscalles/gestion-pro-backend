package cl.duoc.gestionpro.hito.dto;

import java.time.LocalDate;
import java.util.List;

import cl.duoc.gestionpro.hito.EstadoHito;

/**
 * Hito con su estado calculado (PENDIENTE, ATRASADO, CUMPLIDO, CANCELADO) y la fecha prevista:
 * el término planificado más tardío de sus actividades. enRiesgo = pendiente y la fecha prevista
 * supera la comprometida.
 */
public record HitoDto(
        Long id,
        String codigo,
        String descripcion,
        Long responsableId,
        String responsable,
        LocalDate fechaComprometida,
        LocalDate fechaReal,
        EstadoHito estado,
        String estadoCalculado,
        boolean cumplidoEnPlazo,
        LocalDate fechaPrevista,
        boolean enRiesgo,
        List<Long> actividadIds) {
}
