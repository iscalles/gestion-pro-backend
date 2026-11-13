package cl.duoc.gestionpro.cambio.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CierreRequest(
        @NotBlank(message = "Describe el resultado.") @Size(max = 2000) String resultado,
        @NotNull(message = "Indica la efectividad.") @DecimalMin(value = "0", message = "Entre 0 y 100.")
        @DecimalMax(value = "100", message = "Entre 0 y 100.") BigDecimal efectividad,
        @DecimalMin(value = "0", message = "Entre 0 y 100.") @DecimalMax(value = "100", message = "Entre 0 y 100.") BigDecimal adopcion,
        @NotNull(message = "Indica el costo real.") @PositiveOrZero(message = "El costo no puede ser negativo.") BigDecimal costoReal,
        LocalDate fechaReal) {
}
