package cl.duoc.gestionpro.recurso.dto;

import java.math.BigDecimal;

public record AsignacionProfesionalDto(Long id, Long actividadId, String actividad, Long profesionalId, String profesional,
        String especialidad, BigDecimal horasSemana, BigDecimal semanas, BigDecimal valorHora, BigDecimal horasPlan,
        BigDecimal costoPlan, BigDecimal horasReales, BigDecimal costoReal) {
}
