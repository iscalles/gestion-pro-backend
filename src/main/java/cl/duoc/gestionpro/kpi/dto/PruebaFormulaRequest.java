package cl.duoc.gestionpro.kpi.dto;

import cl.duoc.gestionpro.kpi.AmbitoKpi;
import jakarta.validation.constraints.NotBlank;

public record PruebaFormulaRequest(@NotBlank(message = "Escribe la fórmula.") String formula, AmbitoKpi ambito, Long ambitoRefId) {
}
