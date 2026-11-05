package cl.duoc.gestionpro.planificacion;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.gestionpro.planificacion.dto.ActividadRequest;
import cl.duoc.gestionpro.planificacion.dto.DependenciaRequest;
import cl.duoc.gestionpro.planificacion.dto.PlanCambioDto;
import cl.duoc.gestionpro.planificacion.dto.PlanDto;
import cl.duoc.gestionpro.proyecto.AccesoProyecto;
import jakarta.validation.Valid;

/**
 * Planificación de un proyecto. El administrador trabaja en todos; el usuario de proyecto,
 * en los que dirige o integra. Cada modificación responde el plan recalculado.
 */
@RestController
@RequestMapping("/api/proyectos/{proyectoId}")
public class PlanificacionController {

    private final PlanificacionService servicio;
    private final AccesoProyecto acceso;

    public PlanificacionController(PlanificacionService servicio, AccesoProyecto acceso) {
        this.servicio = servicio;
        this.acceso = acceso;
    }

    @GetMapping("/plan")
    public PlanDto plan(@AuthenticationPrincipal Jwt jwt, @PathVariable Long proyectoId) {
        acceso.verificar(jwt, proyectoId);
        return servicio.plan(proyectoId);
    }

    @PostMapping("/actividades")
    @ResponseStatus(HttpStatus.CREATED)
    public PlanCambioDto crearActividad(@AuthenticationPrincipal Jwt jwt, @PathVariable Long proyectoId,
            @Valid @RequestBody ActividadRequest req) {
        acceso.verificar(jwt, proyectoId);
        return servicio.crearActividad(proyectoId, req);
    }

    @PutMapping("/actividades/{id}")
    public PlanCambioDto actualizarActividad(@AuthenticationPrincipal Jwt jwt, @PathVariable Long proyectoId,
            @PathVariable Long id, @Valid @RequestBody ActividadRequest req) {
        acceso.verificar(jwt, proyectoId);
        return servicio.actualizarActividad(proyectoId, id, req);
    }

    @DeleteMapping("/actividades/{id}")
    public PlanCambioDto eliminarActividad(@AuthenticationPrincipal Jwt jwt, @PathVariable Long proyectoId, @PathVariable Long id) {
        acceso.verificar(jwt, proyectoId);
        return servicio.eliminarActividad(proyectoId, id);
    }

    @PostMapping("/dependencias")
    @ResponseStatus(HttpStatus.CREATED)
    public PlanCambioDto crearDependencia(@AuthenticationPrincipal Jwt jwt, @PathVariable Long proyectoId,
            @Valid @RequestBody DependenciaRequest req) {
        acceso.verificar(jwt, proyectoId);
        return servicio.crearDependencia(proyectoId, req);
    }

    @PutMapping("/dependencias/{id}")
    public PlanCambioDto actualizarDependencia(@AuthenticationPrincipal Jwt jwt, @PathVariable Long proyectoId,
            @PathVariable Long id, @Valid @RequestBody DependenciaRequest req) {
        acceso.verificar(jwt, proyectoId);
        return servicio.actualizarDependencia(proyectoId, id, req);
    }

    @DeleteMapping("/dependencias/{id}")
    public PlanCambioDto eliminarDependencia(@AuthenticationPrincipal Jwt jwt, @PathVariable Long proyectoId, @PathVariable Long id) {
        acceso.verificar(jwt, proyectoId);
        return servicio.eliminarDependencia(proyectoId, id);
    }
}
