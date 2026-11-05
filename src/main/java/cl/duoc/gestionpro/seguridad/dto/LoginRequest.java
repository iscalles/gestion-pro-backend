package cl.duoc.gestionpro.seguridad.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Escribe tu correo.") @Email(message = "Escribe un correo válido.") String correo,
        @NotBlank(message = "Escribe tu contraseña.") String clave) {
}
