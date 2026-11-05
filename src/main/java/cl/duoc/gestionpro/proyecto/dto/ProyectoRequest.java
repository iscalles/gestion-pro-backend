package cl.duoc.gestionpro.proyecto.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import cl.duoc.gestionpro.proyecto.EstadoProyecto;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ProyectoRequest(
        @NotBlank(message = "Escribe el código.") @Size(max = 20) String codigo,
        @NotBlank(message = "Escribe el nombre.") @Size(max = 150) String nombre,
        @Size(max = 2000) String descripcion,
        @NotNull(message = "Elige un responsable.") Long responsableId,
        @NotNull(message = "Elige un calendario.") Long calendarioId,
        @NotNull(message = "Indica la fecha de inicio.") LocalDate fechaInicio,
        @NotNull(message = "Indica la fecha de término.") LocalDate fechaTermino,
        @NotNull(message = "Indica el presupuesto.") @PositiveOrZero(message = "El presupuesto no puede ser negativo.") BigDecimal presupuesto,
        EstadoProyecto estado) {

    @AssertTrue(message = "La fecha de término debe ser igual o posterior al inicio.")
    public boolean isFechasValidas() {
        return fechaInicio == null || fechaTermino == null || !fechaTermino.isBefore(fechaInicio);
    }
}
