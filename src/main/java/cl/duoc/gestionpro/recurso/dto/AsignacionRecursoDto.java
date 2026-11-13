package cl.duoc.gestionpro.recurso.dto;

import java.math.BigDecimal;

import cl.duoc.gestionpro.recurso.TipoRecurso;

public record AsignacionRecursoDto(Long id, Long actividadId, String actividad, Long recursoId, String recurso,
        TipoRecurso tipo, String unidad, BigDecimal cantidad, BigDecimal costoUnitario, int periodo,
        BigDecimal costoPlan, BigDecimal costoReal) {
}
