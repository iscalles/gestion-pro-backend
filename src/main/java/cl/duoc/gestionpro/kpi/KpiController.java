package cl.duoc.gestionpro.kpi;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.gestionpro.kpi.dto.KpiDto;
import cl.duoc.gestionpro.kpi.dto.KpiRequest;
import cl.duoc.gestionpro.kpi.dto.MedicionRequest;
import cl.duoc.gestionpro.kpi.dto.PruebaFormulaDto;
import cl.duoc.gestionpro.kpi.dto.PruebaFormulaRequest;
import cl.duoc.gestionpro.kpi.dto.VariableDto;
import cl.duoc.gestionpro.proyecto.AccesoProyecto;
import jakarta.validation.Valid;

/** KPI de un proyecto. Todos los miembros los consultan y registran mediciones manuales; el administrador los define. */
@RestController
public class KpiController {

    private final KpiService servicio;
    private final AccesoProyecto acceso;

    public KpiController(KpiService servicio, AccesoProyecto acceso) {
        this.servicio = servicio;
        this.acceso = acceso;
    }

    @GetMapping("/api/kpi/variables")
    public List<VariableDto> variables() {
        return servicio.variables();
    }

    @GetMapping("/api/proyectos/{proyectoId}/kpi")
    public List<KpiDto> listar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long proyectoId,
            @RequestParam(required = false) CategoriaKpi categoria) {
        acceso.verificar(jwt, proyectoId);
        return servicio.listar(proyectoId, categoria);
    }

    @GetMapping("/api/proyectos/{proyectoId}/kpi/{id}")
    public KpiDto obtener(@AuthenticationPrincipal Jwt jwt, @PathVariable Long proyectoId, @PathVariable Long id) {
        acceso.verificar(jwt, proyectoId);
        return servicio.obtener(proyectoId, id);
    }

    @PostMapping("/api/proyectos/{proyectoId}/kpi")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public KpiDto crear(@PathVariable Long proyectoId, @Valid @RequestBody KpiRequest req) {
        return servicio.crear(proyectoId, req);
    }

    @PutMapping("/api/proyectos/{proyectoId}/kpi/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public KpiDto actualizar(@PathVariable Long proyectoId, @PathVariable Long id, @Valid @RequestBody KpiRequest req) {
        return servicio.actualizar(proyectoId, id, req);
    }

    @DeleteMapping("/api/proyectos/{proyectoId}/kpi/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public void eliminar(@PathVariable Long proyectoId, @PathVariable Long id) {
        servicio.eliminar(proyectoId, id);
    }

    @PostMapping("/api/proyectos/{proyectoId}/kpi/{id}/mediciones")
    @ResponseStatus(HttpStatus.CREATED)
    public KpiDto registrarMedicion(@AuthenticationPrincipal Jwt jwt, @PathVariable Long proyectoId, @PathVariable Long id,
            @Valid @RequestBody MedicionRequest req) {
        acceso.verificar(jwt, proyectoId);
        return servicio.registrarMedicion(proyectoId, id, req, Long.valueOf(jwt.getSubject()));
    }

    /** Evalúa una fórmula con los datos de hoy sin guardar nada (constructor de fórmulas del frontend). */
    @PostMapping("/api/proyectos/{proyectoId}/kpi/probar")
    public PruebaFormulaDto probar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long proyectoId,
            @Valid @RequestBody PruebaFormulaRequest req) {
        acceso.verificar(jwt, proyectoId);
        return servicio.probar(proyectoId, req);
    }
}
