package cl.duoc.gestionpro.planificacion.calendario;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.gestionpro.planificacion.calendario.dto.CalendarioDto;
import cl.duoc.gestionpro.planificacion.calendario.dto.CalendarioRequest;
import cl.duoc.gestionpro.planificacion.calendario.dto.DiaNoLaborableRequest;
import jakarta.validation.Valid;

/**
 * Calendarios laborales. Todos los usuarios los consultan; solo el administrador los modifica,
 * porque un calendario puede estar compartido por varios proyectos.
 */
@RestController
@RequestMapping("/api/calendarios")
public class CalendarioController {

    private final CalendarioService servicio;

    public CalendarioController(CalendarioService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<CalendarioDto> listar() {
        return servicio.listar();
    }

    @GetMapping("/{id}")
    public CalendarioDto obtener(@PathVariable Long id) {
        return servicio.obtener(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public CalendarioDto actualizar(@PathVariable Long id, @Valid @RequestBody CalendarioRequest req) {
        return servicio.actualizar(id, req);
    }

    @PostMapping("/{id}/dias-no-laborables")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public CalendarioDto agregarDia(@PathVariable Long id, @Valid @RequestBody DiaNoLaborableRequest req) {
        return servicio.agregarDia(id, req);
    }

    @DeleteMapping("/{id}/dias-no-laborables/{diaId}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public CalendarioDto quitarDia(@PathVariable Long id, @PathVariable Long diaId) {
        return servicio.quitarDia(id, diaId);
    }
}
