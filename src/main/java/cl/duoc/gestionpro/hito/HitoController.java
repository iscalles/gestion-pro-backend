package cl.duoc.gestionpro.hito;

import java.util.List;
import java.util.Map;

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

import cl.duoc.gestionpro.hito.dto.HitoDto;
import cl.duoc.gestionpro.hito.dto.HitoRequest;
import cl.duoc.gestionpro.proyecto.AccesoProyecto;
import jakarta.validation.Valid;

/** Módulo 6: hitos del proyecto con criterios de aceptación, actividades asociadas, KPI y evidencias. */
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

    @GetMapping("/{id}")
    public HitoDto obtener(@AuthenticationPrincipal Jwt jwt, @PathVariable Long proyectoId, @PathVariable Long id) {
        acceso.verificar(jwt, proyectoId);
        return servicio.obtener(proyectoId, id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public HitoDto crear(@AuthenticationPrincipal Jwt jwt, @PathVariable Long proyectoId, @Valid @RequestBody HitoRequest req) {
        acceso.verificar(jwt, proyectoId);
        return servicio.crear(proyectoId, req);
    }

    @PutMapping("/{id}")
    public HitoDto actualizar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long proyectoId, @PathVariable Long id,
            @Valid @RequestBody HitoRequest req) {
        acceso.verificar(jwt, proyectoId);
        return servicio.actualizar(proyectoId, id, req);
    }

    /** Marca o desmarca un criterio de aceptación sin reenviar el hito completo. */
    @PutMapping("/{id}/criterios/{criterioId}")
    public HitoDto marcarCriterio(@AuthenticationPrincipal Jwt jwt, @PathVariable Long proyectoId, @PathVariable Long id,
            @PathVariable Long criterioId, @RequestBody Map<String, Boolean> cuerpo) {
        acceso.verificar(jwt, proyectoId);
        return servicio.marcarCriterio(proyectoId, id, criterioId, Boolean.TRUE.equals(cuerpo.get("cumplido")));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long proyectoId, @PathVariable Long id) {
        acceso.verificar(jwt, proyectoId);
        servicio.eliminar(proyectoId, id);
    }
}
