package cl.duoc.gestionpro.recurso.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/** valorHora opcional: si no viene se copia el del profesional. Las semanas las calcula el recálculo. */
public record AsignacionProfesionalRequest(
        @NotNull(message = "Elige la actividad.") Long actividadId,
        @NotNull(message = "Elige el profesional.") Long profesionalId,
        @NotNull(message = "Indica las horas semanales.") @Positive(message = "Las horas deben ser mayores a cero.")
        @DecimalMax(value = "168", message = "Una semana tiene 168 horas.") BigDecimal horasSemana,
        @Positive(message = "El valor hora debe ser mayor a cero.") BigDecimal valorHora,
        @PositiveOrZero(message = "Las horas reales no pueden ser negativas.") BigDecimal horasReales) {
}
