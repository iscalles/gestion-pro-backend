package cl.duoc.gestionpro.recurso.dto;

import java.math.BigDecimal;

import cl.duoc.gestionpro.recurso.TipoRecurso;

/** capacidad = cantidad × periodo; asignado = suma de cantidad × periodo de sus asignaciones. */
public record RecursoDto(Long id, TipoRecurso tipo, String descripcion, String detalle, String unidad,
        BigDecimal cantidad, BigDecimal costoUnitario, int periodo, BigDecimal costoTotal,
        BigDecimal capacidad, BigDecimal asignado, BigDecimal disponible) {
}
