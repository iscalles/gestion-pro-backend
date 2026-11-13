package cl.duoc.gestionpro.recurso;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.gestionpro.proyecto.AccesoProyecto;
import cl.duoc.gestionpro.recurso.dto.AsignacionProfesionalRequest;
import cl.duoc.gestionpro.recurso.dto.AsignacionRecursoRequest;
import cl.duoc.gestionpro.recurso.dto.CostosDto;
import cl.duoc.gestionpro.recurso.dto.RecursoRequest;
import cl.duoc.gestionpro.recurso.dto.RequerimientoRequest;
import jakarta.validation.Valid;

/** Recursos, asignaciones y costos de un proyecto. Cada modificación responde los costos recalculados. */
@RestController
@RequestMapping("/api/proyectos/{proyectoId}")
public class RecursoController {

    private final RecursoService servicio;
    private final CargaService carga;
    private final AccesoProyecto acceso;

    public RecursoController(RecursoService servicio, CargaService carga, AccesoProyecto acceso) {
        this.servicio = servicio;
        this.carga = carga;
        this.acceso = acceso;
    }

    @GetMapping("/costos")
    public CostosDto costos(@AuthenticationPrincipal Jwt jwt, @PathVariable Long proyectoId) {
        acceso.verificar(jwt, proyectoId);
        return servicio.costos(proyectoId);
    }

    /** Carga semanal (todos los proyectos) de los profesionales asignados a este proyecto. */
    @GetMapping("/carga")
    public List<CargaService.CargaProfesional> carga(@AuthenticationPrincipal Jwt jwt, @PathVariable Long proyectoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        acceso.verificar(jwt, proyectoId);
        var ids = servicio.costos(proyectoId).asignacionesProfesional().stream().map(a -> a.profesionalId()).distinct().toList();
        return ids.isEmpty() ? List.of() : carga.carga(desde, hasta, ids);
    }

    // requerimientos
    @PostMapping("/requerimientos")
    @ResponseStatus(HttpStatus.CREATED)
    public CostosDto crearRequerimiento(@AuthenticationPrincipal Jwt jwt, @PathVariable Long proyectoId,
            @Valid @RequestBody RequerimientoRequest req) {
        acceso.verificar(jwt, proyectoId);
        return servicio.crearRequerimiento(proyectoId, req);
    }

    @PutMapping("/requerimientos/{id}")
    public CostosDto actualizarRequerimiento(@AuthenticationPrincipal Jwt jwt, @PathVariable Long proyectoId, @PathVariable Long id,
            @Valid @RequestBody RequerimientoRequest req) {
        acceso.verificar(jwt, proyectoId);
        return servicio.actualizarRequerimiento(proyectoId, id, req);
    }

    @DeleteMapping("/requerimientos/{id}")
    public CostosDto eliminarRequerimiento(@AuthenticationPrincipal Jwt jwt, @PathVariable Long proyectoId, @PathVariable Long id) {
        acceso.verificar(jwt, proyectoId);
        return servicio.eliminarRequerimiento(proyectoId, id);
    }

    // recursos
    @PostMapping("/recursos")
    @ResponseStatus(HttpStatus.CREATED)
    public CostosDto crearRecurso(@AuthenticationPrincipal Jwt jwt, @PathVariable Long proyectoId, @Valid @RequestBody RecursoRequest req) {
        acceso.verificar(jwt, proyectoId);
        return servicio.crearRecurso(proyectoId, req);
    }

    @PutMapping("/recursos/{id}")
    public CostosDto actualizarRecurso(@AuthenticationPrincipal Jwt jwt, @PathVariable Long proyectoId, @PathVariable Long id,
            @Valid @RequestBody RecursoRequest req) {
        acceso.verificar(jwt, proyectoId);
        return servicio.actualizarRecurso(proyectoId, id, req);
    }

    @DeleteMapping("/recursos/{id}")
    public CostosDto eliminarRecurso(@AuthenticationPrincipal Jwt jwt, @PathVariable Long proyectoId, @PathVariable Long id) {
        acceso.verificar(jwt, proyectoId);
        return servicio.eliminarRecurso(proyectoId, id);
    }

    // asignaciones de profesionales
    @PostMapping("/asignaciones-profesional")
    @ResponseStatus(HttpStatus.CREATED)
    public CostosDto crearAsignacionProfesional(@AuthenticationPrincipal Jwt jwt, @PathVariable Long proyectoId,
            @Valid @RequestBody AsignacionProfesionalRequest req) {
        acceso.verificar(jwt, proyectoId);
        return servicio.crearAsignacionProfesional(proyectoId, req);
    }

    @PutMapping("/asignaciones-profesional/{id}")
    public CostosDto actualizarAsignacionProfesional(@AuthenticationPrincipal Jwt jwt, @PathVariable Long proyectoId,
            @PathVariable Long id, @Valid @RequestBody AsignacionProfesionalRequest req) {
        acceso.verificar(jwt, proyectoId);
        return servicio.actualizarAsignacionProfesional(proyectoId, id, req);
    }

    @DeleteMapping("/asignaciones-profesional/{id}")
    public CostosDto eliminarAsignacionProfesional(@AuthenticationPrincipal Jwt jwt, @PathVariable Long proyectoId, @PathVariable Long id) {
        acceso.verificar(jwt, proyectoId);
        return servicio.eliminarAsignacionProfesional(proyectoId, id);
    }

    // asignaciones de recursos
    @PostMapping("/asignaciones-recurso")
    @ResponseStatus(HttpStatus.CREATED)
    public CostosDto crearAsignacionRecurso(@AuthenticationPrincipal Jwt jwt, @PathVariable Long proyectoId,
            @Valid @RequestBody AsignacionRecursoRequest req) {
        acceso.verificar(jwt, proyectoId);
        return servicio.crearAsignacionRecurso(proyectoId, req);
    }

    @PutMapping("/asignaciones-recurso/{id}")
    public CostosDto actualizarAsignacionRecurso(@AuthenticationPrincipal Jwt jwt, @PathVariable Long proyectoId,
            @PathVariable Long id, @Valid @RequestBody AsignacionRecursoRequest req) {
        acceso.verificar(jwt, proyectoId);
        return servicio.actualizarAsignacionRecurso(proyectoId, id, req);
    }

    @DeleteMapping("/asignaciones-recurso/{id}")
    public CostosDto eliminarAsignacionRecurso(@AuthenticationPrincipal Jwt jwt, @PathVariable Long proyectoId, @PathVariable Long id) {
        acceso.verificar(jwt, proyectoId);
        return servicio.eliminarAsignacionRecurso(proyectoId, id);
    }
}
