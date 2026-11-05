package cl.duoc.gestionpro.seguridad;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.gestionpro.seguridad.dto.LoginRequest;
import cl.duoc.gestionpro.seguridad.dto.SesionResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** Devuelve el JWT y el usuario, con la misma forma que la interfaz `Sesion` del frontend. */
    @PostMapping("/login")
    public SesionResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req.correo(), req.clave());
    }
}
