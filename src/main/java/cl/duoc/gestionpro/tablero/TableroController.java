package cl.duoc.gestionpro.tablero;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.gestionpro.cambio.Prioridad;
import cl.duoc.gestionpro.cambio.TipoCambio;
import cl.duoc.gestionpro.proyecto.AccesoProyecto;
import cl.duoc.gestionpro.tablero.dto.TableroCambioDto;
import cl.duoc.gestionpro.tablero.dto.TableroProyectoDto;

@RestController
@RequestMapping("/api/tableros")
public class TableroController {

    private final TableroService servicio;
    private final AccesoProyecto acceso;

    public TableroController(TableroService servicio, AccesoProyecto acceso) {
        this.servicio = servicio;
        this.acceso = acceso;
    }

    /** Tablero 1. Filtros opcionales: etapa, rango de fechas y responsable (profesional). */
    @GetMapping("/proyecto")
    public TableroProyectoDto proyecto(@AuthenticationPrincipal Jwt jwt, @RequestParam Long proyectoId,
            @RequestParam(required = false) Long etapaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Long responsableId) {
        acceso.verificar(jwt, proyectoId);
        return servicio.proyecto(proyectoId, etapaId, desde, hasta, responsableId);
    }

    /** Tablero 2. Filtros opcionales: rango de fechas de solicitud, tipo y prioridad. */
    @GetMapping("/cambio")
    public TableroCambioDto cambio(@AuthenticationPrincipal Jwt jwt, @RequestParam Long proyectoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) TipoCambio tipo, @RequestParam(required = false) Prioridad prioridad) {
        acceso.verificar(jwt, proyectoId);
        return servicio.cambio(proyectoId, desde, hasta, tipo, prioridad);
    }
}
