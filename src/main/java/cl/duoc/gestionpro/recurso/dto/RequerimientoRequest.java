package cl.duoc.gestionpro.recurso.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RequerimientoRequest(
        @NotNull(message = "Elige el tipo de profesional.") Long tipoProfesionalId,
        @NotNull(message = "Indica la cantidad.") @Min(value = 1, message = "Se requiere al menos 1.") Integer cantidadRequerida,
        @NotNull(message = "Indica la tarifa.") @Positive(message = "La tarifa debe ser mayor a cero.") BigDecimal tarifaHora) {
}
