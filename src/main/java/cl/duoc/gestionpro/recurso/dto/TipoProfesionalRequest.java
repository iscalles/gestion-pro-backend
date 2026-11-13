package cl.duoc.gestionpro.recurso.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record TipoProfesionalRequest(
        @NotBlank(message = "Escribe la categoría.") @Size(max = 80) String categoria,
        @NotNull(message = "Indica la tarifa de referencia.") @Positive(message = "La tarifa debe ser mayor a cero.") BigDecimal tarifaHoraRef) {
}
