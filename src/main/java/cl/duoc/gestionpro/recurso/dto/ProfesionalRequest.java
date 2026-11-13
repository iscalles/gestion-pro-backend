package cl.duoc.gestionpro.recurso.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ProfesionalRequest(
        @NotBlank(message = "Escribe el nombre.") @Size(max = 120) String nombre,
        @NotBlank(message = "Escribe la especialidad.") @Size(max = 80) String especialidad,
        @NotNull(message = "Elige el tipo de profesional.") Long tipoProfesionalId,
        @NotNull(message = "Indica el valor hora.") @Positive(message = "El valor hora debe ser mayor a cero.") BigDecimal valorHora,
        @NotNull(message = "Indica las horas disponibles.") @Positive(message = "Las horas deben ser mayores a cero.")
        @DecimalMax(value = "168", message = "Una semana tiene 168 horas.") BigDecimal horasSemanaDisp,
        Long usuarioId,
        Boolean activo) {
}
