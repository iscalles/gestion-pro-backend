package cl.duoc.gestionpro.cambio.dto;

import cl.duoc.gestionpro.cambio.EntidadModificada;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ModificacionRequest(
        @NotNull(message = "Elige qué se modifica.") EntidadModificada entidad,
        @NotNull(message = "Elige el elemento.") Long entidadId,
        @NotBlank(message = "Elige el campo.") String campo,
        @NotBlank(message = "Indica el valor nuevo.") @Size(max = 200) String valorNuevo) {
}
