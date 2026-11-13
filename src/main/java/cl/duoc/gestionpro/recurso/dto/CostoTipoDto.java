package cl.duoc.gestionpro.recurso.dto;

import java.math.BigDecimal;

/** Costo por tipo de recurso: PROFESIONALES, LICENCIA, MATERIAL, EQUIPAMIENTO u OTRO. */
public record CostoTipoDto(String tipo, BigDecimal costoPlan, BigDecimal costoReal) {
}
