package cl.duoc.gestionpro.recurso.dto;

import java.math.BigDecimal;

public record TipoProfesionalDto(Long id, String categoria, BigDecimal tarifaHoraRef, long profesionales) {
}
