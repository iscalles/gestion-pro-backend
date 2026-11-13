package cl.duoc.gestionpro.cambio.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.PositiveOrZero;

/** Datos que se registran durante la implementación (antes del cierre). */
public record SeguimientoRequest(
        @PositiveOrZero(message = "El costo no puede ser negativo.") BigDecimal costoReal,
        @DecimalMin(value = "0", message = "Entre 0 y 100.") @DecimalMax(value = "100", message = "Entre 0 y 100.") BigDecimal adopcion,
        @DecimalMin(value = "0", message = "Entre 0 y 100.") @DecimalMax(value = "100", message = "Entre 0 y 100.") BigDecimal efectividad) {
}
