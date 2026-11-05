package cl.duoc.gestionpro.planificacion.calendario.dto;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.Set;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CalendarioRequest(
        @NotBlank(message = "Escribe el nombre.") @Size(max = 80) String nombre,
        @NotEmpty(message = "Marca al menos un día laborable.") Set<DayOfWeek> diasLaborables,
        @NotNull(message = "Indica las horas por día.")
        @DecimalMin(value = "0.5", message = "Las horas por día deben ser mayores que cero.")
        @DecimalMax(value = "24", message = "Un día tiene como máximo 24 horas.") BigDecimal horasDia) {
}
