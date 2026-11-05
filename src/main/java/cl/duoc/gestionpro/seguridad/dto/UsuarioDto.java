package cl.duoc.gestionpro.seguridad.dto;

import cl.duoc.gestionpro.seguridad.Rol;
import cl.duoc.gestionpro.seguridad.Usuario;

public record UsuarioDto(Long id, String nombre, String correo, Rol rol) {

    public static UsuarioDto de(Usuario u) {
        return new UsuarioDto(u.getId(), u.getNombre(), u.getCorreo(), u.getRol());
    }
}
