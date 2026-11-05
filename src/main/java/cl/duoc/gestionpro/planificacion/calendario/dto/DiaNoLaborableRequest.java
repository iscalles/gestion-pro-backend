package cl.duoc.gestionpro.planificacion.calendario.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DiaNoLaborableRequest(
        @NotNull(message = "Indica la fecha.") LocalDate fecha,
        @NotBlank(message = "Escribe el motivo.") @Size(max = 120) String motivo) {
}
