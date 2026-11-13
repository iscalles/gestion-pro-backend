package cl.duoc.gestionpro.recurso.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record AsignacionRecursoRequest(
        @NotNull(message = "Elige la actividad.") Long actividadId,
        @NotNull(message = "Elige el recurso.") Long recursoId,
        @NotNull(message = "Indica la cantidad.") @Positive(message = "La cantidad debe ser mayor a cero.") BigDecimal cantidad,
        @Min(value = 1, message = "El periodo mínimo es 1.") Integer periodo,
        @PositiveOrZero(message = "El costo real no puede ser negativo.") BigDecimal costoReal) {
}
