package cl.duoc.gestionpro.proyecto;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.gestionpro.proyecto.dto.ProyectoDto;
import cl.duoc.gestionpro.proyecto.dto.ProyectoRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/proyectos")
public class ProyectoController {

    private final ProyectoService servicio;

    public ProyectoController(ProyectoService servicio) {
        this.servicio = servicio;
    }

    /** El administrador ve todos; el usuario de proyecto, solo los suyos. */
    @GetMapping
    public List<ProyectoDto> listar(@AuthenticationPrincipal Jwt jwt) {
        boolean esAdmin = "ADMINISTRADOR".equals(jwt.getClaimAsString("rol"));
        return servicio.listar(Long.valueOf(jwt.getSubject()), esAdmin);
    }

    @GetMapping("/{id}")
    public ProyectoDto obtener(@PathVariable Long id) {
        return servicio.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ProyectoDto crear(@Valid @RequestBody ProyectoRequest req) {
        return servicio.crear(req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ProyectoDto actualizar(@PathVariable Long id, @Valid @RequestBody ProyectoRequest req) {
        return servicio.actualizar(id, req);
    }
}
