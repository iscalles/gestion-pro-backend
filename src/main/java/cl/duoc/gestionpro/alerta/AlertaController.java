package cl.duoc.gestionpro.alerta;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.gestionpro.proyecto.AccesoProyecto;

@RestController
public class AlertaController {

    private final AlertaService servicio;
    private final AccesoProyecto acceso;

    public AlertaController(AlertaService servicio, AccesoProyecto acceso) {
        this.servicio = servicio;
        this.acceso = acceso;
    }

    /** Abiertas por defecto; con todas=true incluye el historial de resueltas. */
    @GetMapping("/api/proyectos/{proyectoId}/alertas")
    public List<AlertaService.AlertaDto> listar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long proyectoId,
            @RequestParam(defaultValue = "false") boolean todas) {
        acceso.verificar(jwt, proyectoId);
        return servicio.listar(proyectoId, todas);
    }
}
