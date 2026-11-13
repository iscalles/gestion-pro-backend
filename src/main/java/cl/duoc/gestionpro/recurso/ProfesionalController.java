package cl.duoc.gestionpro.recurso;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.gestionpro.comun.RecursoNoEncontradoException;
import cl.duoc.gestionpro.comun.ReglaNegocioException;
import cl.duoc.gestionpro.recurso.dto.ProfesionalDto;
import cl.duoc.gestionpro.recurso.dto.ProfesionalRequest;
import cl.duoc.gestionpro.recurso.dto.TipoProfesionalDto;
import cl.duoc.gestionpro.recurso.dto.TipoProfesionalRequest;
import jakarta.validation.Valid;

/**
 * Catálogo compartido de profesionales y tipos de profesional. Todos leen; solo el administrador modifica.
 * Un profesional con asignaciones o responsabilidades no se borra: se desactiva.
 */
@RestController
public class ProfesionalController {

    private final ProfesionalRepository profesionales;
    private final TipoProfesionalRepository tipos;
    private final AsignacionProfesionalRepository asignaciones;
    private final CargaService carga;
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;

    public ProfesionalController(ProfesionalRepository profesionales, TipoProfesionalRepository tipos,
            AsignacionProfesionalRepository asignaciones, CargaService carga, org.springframework.jdbc.core.JdbcTemplate jdbc) {
        this.profesionales = profesionales;
        this.tipos = tipos;
        this.asignaciones = asignaciones;
        this.carga = carga;
        this.jdbc = jdbc;
    }

    /** Activos por defecto (para elegir responsables); con todos=true, el catálogo completo. */
    @GetMapping("/api/profesionales")
    @Transactional(readOnly = true)
    public List<ProfesionalDto> listar(@RequestParam(defaultValue = "false") boolean todos) {
        Map<Long, String> nombresTipo = nombresTipo();
        var lista = todos ? profesionales.findAllByOrderByNombreAsc() : profesionales.findByActivoTrueOrderByNombreAsc();
        return lista.stream().map(p -> ProfesionalDto.de(p, nombresTipo.get(p.getTipoProfesionalId()))).toList();
    }

    @PostMapping("/api/profesionales")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Transactional
    public ProfesionalDto crear(@Valid @RequestBody ProfesionalRequest req) {
        var p = new Profesional();
        aplicar(p, req);
        profesionales.saveAndFlush(p);
        return ProfesionalDto.de(p, nombresTipo().get(p.getTipoProfesionalId()));
    }

    @PutMapping("/api/profesionales/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Transactional
    public ProfesionalDto actualizar(@PathVariable Long id, @Valid @RequestBody ProfesionalRequest req) {
        var p = profesionales.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Profesional", id));
        aplicar(p, req);
        profesionales.flush();
        return ProfesionalDto.de(p, nombresTipo().get(p.getTipoProfesionalId()));
    }

    /** Borra si no tiene historia; si la tiene, lo desactiva y lo informa. */
    @DeleteMapping("/api/profesionales/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Transactional
    public Map<String, Object> eliminar(@PathVariable Long id) {
        var p = profesionales.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Profesional", id));
        Integer referencias = jdbc.queryForObject("""
                select (select count(*) from actividad where responsable_id = ?)
                     + (select count(*) from hito where responsable_id = ?) from dual""", Integer.class, id, id);
        if (asignaciones.existsByProfesionalId(id) || (referencias != null && referencias > 0)) {
            p.setActivo(false);
            return Map.of("eliminado", false, "desactivado", true,
                    "mensaje", p.getNombre() + " tiene asignaciones o responsabilidades: quedó inactivo para conservar la historia.");
        }
        profesionales.delete(p);
        return Map.of("eliminado", true, "desactivado", false, "mensaje", p.getNombre() + " se eliminó del catálogo.");
    }

    /** Carga semanal de todos los profesionales (todos los proyectos). */
    @GetMapping("/api/profesionales/carga")
    public List<CargaService.CargaProfesional> carga(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        if (hasta.isBefore(desde) || desde.plusWeeks(60).isBefore(hasta)) {
            throw new ReglaNegocioException("RANGO_INVALIDO", "Elige un rango de hasta 60 semanas con término posterior al inicio.");
        }
        return carga.carga(desde, hasta, null);
    }

    private void aplicar(Profesional p, ProfesionalRequest req) {
        if (!tipos.existsById(req.tipoProfesionalId())) {
            throw new RecursoNoEncontradoException("Tipo de profesional", req.tipoProfesionalId());
        }
        p.setNombre(req.nombre().trim());
        p.setEspecialidad(req.especialidad().trim());
        p.setTipoProfesionalId(req.tipoProfesionalId());
        p.setValorHora(req.valorHora());
        p.setHorasSemanaDisp(req.horasSemanaDisp());
        p.setUsuarioId(req.usuarioId());
        if (req.activo() != null) {
            p.setActivo(req.activo());
        }
    }

    // ---------------------------------------------------------------- tipos de profesional

    @GetMapping("/api/tipos-profesional")
    @Transactional(readOnly = true)
    public List<TipoProfesionalDto> tipos() {
        Map<Long, Long> cuenta = new HashMap<>();
        profesionales.findAll().forEach(p -> cuenta.merge(p.getTipoProfesionalId(), 1L, Long::sum));
        return tipos.findAllByOrderByCategoriaAsc().stream()
                .map(t -> new TipoProfesionalDto(t.getId(), t.getCategoria(), t.getTarifaHoraRef(), cuenta.getOrDefault(t.getId(), 0L)))
                .toList();
    }

    @PostMapping("/api/tipos-profesional")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Transactional
    public TipoProfesionalDto crearTipo(@Valid @RequestBody TipoProfesionalRequest req) {
        if (tipos.existsByCategoriaIgnoreCase(req.categoria().trim())) {
            throw new ReglaNegocioException("CATEGORIA_DUPLICADA", "Ya existe la categoría «" + req.categoria().trim() + "».");
        }
        var t = new TipoProfesional();
        t.setCategoria(req.categoria().trim());
        t.setTarifaHoraRef(req.tarifaHoraRef());
        tipos.saveAndFlush(t);
        return new TipoProfesionalDto(t.getId(), t.getCategoria(), t.getTarifaHoraRef(), 0);
    }

    @PutMapping("/api/tipos-profesional/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Transactional
    public TipoProfesionalDto actualizarTipo(@PathVariable Long id, @Valid @RequestBody TipoProfesionalRequest req) {
        var t = tipos.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Tipo de profesional", id));
        if (!t.getCategoria().equalsIgnoreCase(req.categoria().trim()) && tipos.existsByCategoriaIgnoreCase(req.categoria().trim())) {
            throw new ReglaNegocioException("CATEGORIA_DUPLICADA", "Ya existe la categoría «" + req.categoria().trim() + "».");
        }
        t.setCategoria(req.categoria().trim());
        t.setTarifaHoraRef(req.tarifaHoraRef());
        tipos.flush();
        long n = profesionales.findAll().stream().filter(p -> p.getTipoProfesionalId().equals(id)).count();
        return new TipoProfesionalDto(t.getId(), t.getCategoria(), t.getTarifaHoraRef(), n);
    }

    @DeleteMapping("/api/tipos-profesional/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Transactional
    public void eliminarTipo(@PathVariable Long id) {
        var t = tipos.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Tipo de profesional", id));
        Integer usos = jdbc.queryForObject("select count(*) from requerimiento_profesional where tipo_profesional_id = ?",
                Integer.class, id);
        if (profesionales.existsByTipoProfesionalId(id) || (usos != null && usos > 0)) {
            throw new ReglaNegocioException("TIPO_EN_USO",
                    "«" + t.getCategoria() + "» tiene profesionales o está en la dotación de un proyecto; no se puede eliminar.");
        }
        tipos.delete(t);
    }

    private Map<Long, String> nombresTipo() {
        Map<Long, String> m = new HashMap<>();
        tipos.findAll().forEach(t -> m.put(t.getId(), t.getCategoria()));
        return m;
    }
}
