package cl.duoc.gestionpro.seguridad;

import java.util.Comparator;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.gestionpro.seguridad.dto.UsuarioDto;

/** Usuarios activos, para elegir responsables y miembros de proyecto. */
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioRepository usuarios;

    public UsuarioController(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    @GetMapping
    public List<UsuarioDto> listar() {
        return usuarios.findAll().stream().filter(Usuario::isActivo).sorted(Comparator.comparing(Usuario::getNombre))
                .map(UsuarioDto::de).toList();
    }
}
