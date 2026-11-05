package cl.duoc.gestionpro.planificacion.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de POST y PUT de actividades. El tipo se deduce del padre (sin padre: etapa).
 * padreId solo se usa al crear. En elementos con hijos se ignoran duración, fechas reales y avance:
 * los calcula el motor desde los hijos.
 */
public record ActividadRequest(
        Long padreId,
        @NotBlank(message = "Escribe el nombre.") @Size(max = 150, message = "Máximo 150 caracteres.") String nombre,
        @Size(max = 2000, message = "Máximo 2000 caracteres.") String descripcion,
        Long responsableId,
        @NotNull(message = "Indica la duración.") @PositiveOrZero(message = "La duración no puede ser negativa.")
        @Max(value = 9999, message = "La duración es demasiado larga.") Integer duracionPlan,
        @PositiveOrZero(message = "La duración real no puede ser negativa.") Integer duracionReal,
        LocalDate fechaInicioReal,
        LocalDate fechaTerminoReal,
        @DecimalMin(value = "0", message = "El avance va de 0 a 100.")
        @DecimalMax(value = "100", message = "El avance va de 0 a 100.") BigDecimal avance) {

    @AssertTrue(message = "El término real debe ser igual o posterior al inicio real.")
    public boolean isFechasRealesValidas() {
        return fechaInicioReal == null || fechaTerminoReal == null || !fechaTerminoReal.isBefore(fechaInicioReal);
    }

    @AssertTrue(message = "Indica el inicio real antes del término real.")
    public boolean isInicioRealPresente() {
        return fechaTerminoReal == null || fechaInicioReal != null;
    }
}
