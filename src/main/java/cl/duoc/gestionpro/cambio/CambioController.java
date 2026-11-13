package cl.duoc.gestionpro.cambio;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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

import cl.duoc.gestionpro.cambio.dto.AccionDto;
import cl.duoc.gestionpro.cambio.dto.AccionRequest;
import cl.duoc.gestionpro.cambio.dto.AprobacionDto;
import cl.duoc.gestionpro.cambio.dto.CierreRequest;
import cl.duoc.gestionpro.cambio.dto.ComparacionDto;
import cl.duoc.gestionpro.cambio.dto.DecisionRequest;
import cl.duoc.gestionpro.cambio.dto.EtapaRequest;
import cl.duoc.gestionpro.cambio.dto.LineaBaseDto;
import cl.duoc.gestionpro.cambio.dto.ModificacionRequest;
import cl.duoc.gestionpro.cambio.dto.OpcionModificacionDto;
import cl.duoc.gestionpro.cambio.dto.SeguimientoRequest;
import cl.duoc.gestionpro.cambio.dto.SolicitudDto;
import cl.duoc.gestionpro.cambio.dto.SolicitudRequest;
import cl.duoc.gestionpro.cambio.dto.SolicitudResumenDto;
import cl.duoc.gestionpro.cambio.dto.TrazabilidadDto;
import cl.duoc.gestionpro.proyecto.AccesoProyecto;
import jakarta.validation.Valid;

/**
 * Solicitudes de cambio de un proyecto. Los miembros registran, avanzan etapas, proponen modificaciones y
 * acciones; solo el administrador aprueba o rechaza.
 */
@RestController
@RequestMapping("/api/proyectos/{proyectoId}")
public class CambioController {

    private final CambioService servicio;
    private final LineaBaseService lineasBase;
    private final AccesoProyecto acceso;

    public CambioController(CambioService servicio, LineaBaseService lineasBase, AccesoProyecto acceso) {
        this.servicio = servicio;
        this.lineasBase = lineasBase;
        this.acceso = acceso;
    }

    @GetMapping("/cambios")
    public List<SolicitudResumenDto> listar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long proyectoId) {
        acceso.verificar(jwt, proyectoId);
        return servicio.listar(proyectoId);
    }

    @GetMapping("/cambios/{id}")
    public SolicitudDto obtener(@AuthenticationPrincipal Jwt jwt, @PathVariable Long proyectoId, @PathVariable Long id) {
        acceso.verificar(jwt, proyectoId);
        return servicio.obtener(proyectoId, id);
    }

    @PostMapping("/cambios")
    @ResponseStatus(HttpStatus.CREATED)
    public SolicitudDto crear(@AuthenticationPrincipal Jwt jwt, @PathVariable Long proyectoId, @Valid @RequestBody SolicitudRequest req) {
        acceso.verificar(jwt, proyectoId);
        return servicio.crear(proyectoId, req, usuario(jwt));
    }

    @PutMapping("/cambios/{id}")
    public SolicitudDto actualizar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long proyectoId, @PathVariable Long id,
            @Valid @RequestBody SolicitudRequest req) {
        acceso.verificar(jwt, proyectoId);
        return servicio.actualizar(proyectoId, id, req);
    }

    @PutMapping("/cambios/{id}/seguimiento")
    public SolicitudDto seguimiento(@AuthenticationPrincipal Jwt jwt, @PathVariable Long proyectoId, @PathVariable Long id,
            @Valid @RequestBody SeguimientoRequest req) {
        acceso.verificar(jwt, proyectoId);
        return servicio.seguimiento(proyectoId, id, req);
    }

    @DeleteMapping("/cambios/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public void eliminar(@PathVariable Long proyectoId, @PathVariable Long id) {
        servicio.eliminar(proyectoId, id);
    }

    @PostMapping("/cambios/{id}/avanzar")
    public SolicitudDto avanzar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long proyectoId, @PathVariable Long id,
            @Valid @RequestBody(required = false) DecisionRequest req) {
        acceso.verificar(jwt, proyectoId);
        return servicio.avanzar(proyectoId, id, req);
    }

    @PostMapping("/cambios/{id}/aprobar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public AprobacionDto aprobar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long proyectoId, @PathVariable Long id,
            @Valid @RequestBody(required = false) DecisionRequest req) {
        return servicio.aprobar(proyectoId, id, req, usuario(jwt));
    }

    @PostMapping("/cambios/{id}/rechazar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public SolicitudDto rechazar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long proyectoId, @PathVariable Long id,
            @Valid @RequestBody DecisionRequest req) {
        return servicio.rechazar(proyectoId, id, req, usuario(jwt));
    }

    @PostMapping("/cambios/{id}/cerrar")
    public SolicitudDto cerrar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long proyectoId, @PathVariable Long id,
            @Valid @RequestBody CierreRequest req) {
        acceso.verificar(jwt, proyectoId);
        return servicio.cerrar(proyectoId, id, req);
    }

    @PutMapping("/cambios/{id}/etapas/{numero}")
    public SolicitudDto actualizarEtapa(@AuthenticationPrincipal Jwt jwt, @PathVariable Long proyectoId, @PathVariable Long id,
            @PathVariable int numero, @Valid @RequestBody EtapaRequest req) {
        acceso.verificar(jwt, proyectoId);
        return servicio.actualizarEtapa(proyectoId, id, numero, req);
    }

    @GetMapping("/opciones-modificacion")
    public List<OpcionModificacionDto> opciones(@AuthenticationPrincipal Jwt jwt, @PathVariable Long proyectoId) {
        acceso.verificar(jwt, proyectoId);
        return servicio.opciones(proyectoId);
    }

    @PostMapping("/cambios/{id}/modificaciones")
    @ResponseStatus(HttpStatus.CREATED)
    public SolicitudDto agregarModificacion(@AuthenticationPrincipal Jwt jwt, @PathVariable Long proyectoId, @PathVariable Long id,
            @Valid @RequestBody ModificacionRequest req) {
        acceso.verificar(jwt, proyectoId);
        return servicio.agregarModificacion(proyectoId, id, req);
    }

    @DeleteMapping("/cambios/{id}/modificaciones/{modificacionId}")
    public SolicitudDto eliminarModificacion(@AuthenticationPrincipal Jwt jwt, @PathVariable Long proyectoId, @PathVariable Long id,
            @PathVariable Long modificacionId) {
        acceso.verificar(jwt, proyectoId);
        return servicio.eliminarModificacion(proyectoId, id, modificacionId);
    }

    @GetMapping("/acciones-correctivas")
    public List<AccionDto> acciones(@AuthenticationPrincipal Jwt jwt, @PathVariable Long proyectoId) {
        acceso.verificar(jwt, proyectoId);
        return servicio.accionesDelProyecto(proyectoId);
    }

    @PostMapping("/cambios/{id}/acciones")
    @ResponseStatus(HttpStatus.CREATED)
    public SolicitudDto crearAccion(@AuthenticationPrincipal Jwt jwt, @PathVariable Long proyectoId, @PathVariable Long id,
            @Valid @RequestBody AccionRequest req) {
        acceso.verificar(jwt, proyectoId);
        return servicio.crearAccion(proyectoId, id, req);
    }

    @PutMapping("/cambios/{id}/acciones/{accionId}")
    public SolicitudDto actualizarAccion(@AuthenticationPrincipal Jwt jwt, @PathVariable Long proyectoId, @PathVariable Long id,
            @PathVariable Long accionId, @Valid @RequestBody AccionRequest req) {
        acceso.verificar(jwt, proyectoId);
        return servicio.actualizarAccion(proyectoId, id, accionId, req);
    }

    @DeleteMapping("/cambios/{id}/acciones/{accionId}")
    public SolicitudDto eliminarAccion(@AuthenticationPrincipal Jwt jwt, @PathVariable Long proyectoId, @PathVariable Long id,
            @PathVariable Long accionId) {
        acceso.verificar(jwt, proyectoId);
        return servicio.eliminarAccion(proyectoId, id, accionId);
    }

    @GetMapping("/trazabilidad")
    public List<TrazabilidadDto> trazabilidad(@AuthenticationPrincipal Jwt jwt, @PathVariable Long proyectoId) {
        acceso.verificar(jwt, proyectoId);
        return servicio.trazabilidad(proyectoId);
    }

    @GetMapping("/lineas-base")
    public List<LineaBaseDto> lineasBase(@AuthenticationPrincipal Jwt jwt, @PathVariable Long proyectoId) {
        acceso.verificar(jwt, proyectoId);
        return lineasBase.listar(proyectoId);
    }

    @GetMapping("/lineas-base/{id}/comparacion")
    public ComparacionDto comparar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long proyectoId, @PathVariable Long id) {
        acceso.verificar(jwt, proyectoId);
        return lineasBase.comparar(proyectoId, id);
    }

    private static Long usuario(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
