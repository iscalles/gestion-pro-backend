package cl.duoc.gestionpro.planificacion.dto;

import cl.duoc.gestionpro.planificacion.TipoDependencia;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record DependenciaRequest(
        @NotNull(message = "Elige la actividad predecesora.") Long predecesoraId,
        @NotNull(message = "Elige la actividad sucesora.") Long sucesoraId,
        @NotNull(message = "Elige el tipo de dependencia.") TipoDependencia tipo,
        @NotNull(message = "Indica el desfase (0 si no hay).")
        @Min(value = -999, message = "El desfase va de -999 a 999 días.")
        @Max(value = 999, message = "El desfase va de -999 a 999 días.") Integer desfaseDias) {
}
