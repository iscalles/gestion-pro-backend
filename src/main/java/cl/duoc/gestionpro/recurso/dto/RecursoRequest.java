package cl.duoc.gestionpro.recurso.dto;

import java.math.BigDecimal;

import cl.duoc.gestionpro.recurso.TipoRecurso;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record RecursoRequest(
        @NotNull(message = "Elige el tipo de recurso.") TipoRecurso tipo,
        @NotBlank(message = "Escribe la descripción.") @Size(max = 150) String descripcion,
        @Size(max = 150) String detalle,
        @Size(max = 30) String unidad,
        @NotNull(message = "Indica la cantidad.") @Positive(message = "La cantidad debe ser mayor a cero.") BigDecimal cantidad,
        @NotNull(message = "Indica el costo unitario.") @PositiveOrZero(message = "El costo no puede ser negativo.") BigDecimal costoUnitario,
        @NotNull(message = "Indica el periodo.") @Min(value = 1, message = "El periodo mínimo es 1.") Integer periodo) {
}
