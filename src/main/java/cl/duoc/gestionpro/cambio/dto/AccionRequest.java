package cl.duoc.gestionpro.cambio.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AccionRequest(
        @NotBlank(message = "Describe la acción.") @Size(max = 500) String descripcion,
        @NotNull(message = "Elige el responsable.") Long responsableId,
        @NotNull(message = "Indica la fecha de compromiso.") LocalDate fechaCompromiso,
        LocalDate fechaCierre) {
}
