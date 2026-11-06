package cl.duoc.gestionpro.hito;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.gestionpro.hito.dto.HitoDto;
import cl.duoc.gestionpro.proyecto.AccesoProyecto;

@RestController
@RequestMapping("/api/proyectos/{proyectoId}/hitos")
public class HitoController {

    private final HitoService servicio;
    private final AccesoProyecto acceso;

    public HitoController(HitoService servicio, AccesoProyecto acceso) {
        this.servicio = servicio;
        this.acceso = acceso;
    }

    @GetMapping
    public List<HitoDto> listar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long proyectoId) {
        acceso.verificar(jwt, proyectoId);
        return servicio.listar(proyectoId);
    }
}
