package cl.duoc.gestionpro.proyecto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.duoc.gestionpro.cambio.EstadoSolicitud;
import cl.duoc.gestionpro.cambio.SolicitudCambio;
import cl.duoc.gestionpro.cambio.SolicitudCambioRepository;
import cl.duoc.gestionpro.comun.RecursoNoEncontradoException;
import cl.duoc.gestionpro.comun.ReglaNegocioException;
import cl.duoc.gestionpro.planificacion.Actividad;
import cl.duoc.gestionpro.planificacion.ActividadRepository;
import cl.duoc.gestionpro.proyecto.dto.CierreProyectoRequest;
import cl.duoc.gestionpro.proyecto.dto.ProyectoDto;
import cl.duoc.gestionpro.proyecto.dto.ProyectoRequest;
import cl.duoc.gestionpro.recalculo.RecalculoService;
import cl.duoc.gestionpro.recurso.CostoService;
import cl.duoc.gestionpro.seguridad.UsuarioRepository;

/**
 * Módulo 1: crear, editar, consultar y cerrar proyectos.
 * <ul>
 * <li>Pasar a EN_EJECUCION registra el inicio real (si no lo tenía).</li>
 * <li>Solo se cierra (TERMINADO) sin actividades abiertas ni solicitudes de cambio en curso; el cierre registra el
 * término real.</li>
 * <li>Solo se elimina un proyecto PLANIFICADO.</li>
 * </ul>
 */
@Service
public class ProyectoService {

    private static final ZoneId ZONA = ZoneId.of("America/Santiago");

    private final ProyectoRepository proyectos;
    private final ProyectoMiembroRepository miembros;
    private final UsuarioRepository usuarios;
    private final ActividadRepository actividades;
    private final SolicitudCambioRepository solicitudes;
    private final CostoService costos;
    private final RecalculoService recalculo;

    public ProyectoService(ProyectoRepository proyectos, ProyectoMiembroRepository miembros, UsuarioRepository usuarios,
            ActividadRepository actividades, SolicitudCambioRepository solicitudes, CostoService costos,
            RecalculoService recalculo) {
        this.proyectos = proyectos;
        this.miembros = miembros;
        this.usuarios = usuarios;
        this.actividades = actividades;
        this.solicitudes = solicitudes;
        this.costos = costos;
        this.recalculo = recalculo;
    }

    @Transactional(readOnly = true)
    public List<ProyectoDto> listar(Long usuarioId, boolean esAdmin) {
        var lista = esAdmin ? proyectos.findAllByOrderByCodigoAsc() : proyectos.visiblesPara(usuarioId);
        return lista.stream().map(this::dto).toList();
    }

    @Transactional(readOnly = true)
    public ProyectoDto obtener(Long id) {
        return dto(buscar(id));
    }

    @Transactional
    public ProyectoDto crear(ProyectoRequest req) {
        if (proyectos.existsByCodigo(req.codigo().trim())) {
            throw new ReglaNegocioException("CODIGO_DUPLICADO", "Ya existe un proyecto con el código " + req.codigo() + ".");
        }
        if (req.estado() == EstadoProyecto.TERMINADO) {
            throw new ReglaNegocioException("ESTADO_INVALIDO", "Un proyecto nuevo no puede nacer terminado.");
        }
        var p = new Proyecto();
        aplicar(p, req);
        proyectos.saveAndFlush(p);
        guardarMiembros(p, req.miembroIds());
        return dto(p);
    }

    @Transactional
    public ProyectoDto actualizar(Long id, ProyectoRequest req) {
        var p = buscar(id);
        EstadoProyecto anterior = p.getEstado();
        if (!p.getCodigo().equals(req.codigo().trim()) && proyectos.existsByCodigo(req.codigo().trim())) {
            throw new ReglaNegocioException("CODIGO_DUPLICADO", "Ya existe un proyecto con el código " + req.codigo() + ".");
        }
        aplicar(p, req);
        if (p.getEstado() == EstadoProyecto.TERMINADO && anterior != EstadoProyecto.TERMINADO) {
            validarCierre(p);
            p.setFechaTerminoReal(terminoReal(id, null));
        } else if (p.getEstado() != EstadoProyecto.TERMINADO) {
            p.setFechaTerminoReal(null); // reabierto
        }
        proyectos.flush();
        guardarMiembros(p, req.miembroIds());
        recalculo.recalcularProyecto(id); // la fecha de inicio, el calendario o el presupuesto pueden haber cambiado
        return dto(p);
    }

    /** Cierre formal: valida actividades y solicitudes de cambio, y registra el término real. */
    @Transactional
    public ProyectoDto cerrar(Long id, CierreProyectoRequest req) {
        var p = buscar(id);
        if (p.getEstado() == EstadoProyecto.TERMINADO) {
            throw new ReglaNegocioException("PROYECTO_TERMINADO", "El proyecto ya está cerrado.");
        }
        validarCierre(p);
        LocalDate termino = terminoReal(id, req == null ? null : req.fechaTerminoReal());
        if (termino.isAfter(LocalDate.now(ZONA))) {
            throw new ReglaNegocioException("FECHA_FUTURA", "El término real no puede ser posterior a hoy.");
        }
        if (p.getFechaInicioReal() == null) {
            p.setFechaInicioReal(actividades.findByProyectoIdOrderByOrdenAscIdAsc(id).stream()
                    .map(Actividad::getFechaInicioReal).filter(Objects::nonNull).min(LocalDate::compareTo)
                    .orElse(p.getFechaInicioPlan()));
        }
        p.setEstado(EstadoProyecto.TERMINADO);
        p.setFechaTerminoReal(termino);
        proyectos.flush();
        recalculo.actualizarIndicadores(id, null);
        return dto(p);
    }

    @Transactional
    public void eliminar(Long id) {
        var p = buscar(id);
        if (p.getEstado() != EstadoProyecto.PLANIFICADO) {
            throw new ReglaNegocioException("PROYECTO_INICIADO",
                    "Solo se puede eliminar un proyecto planificado. Uno en ejecución se suspende o se cierra.");
        }
        proyectos.delete(p);
        proyectos.flush();
    }

    // ---------------------------------------------------------------- apoyo

    private void validarCierre(Proyecto p) {
        if (actividades.existsAbiertasEnProyecto(p.getId())) {
            throw new ReglaNegocioException("ACTIVIDADES_ABIERTAS",
                    "No puedes cerrar el proyecto: tiene actividades sin terminar (avance menor a 100 %).");
        }
        if (solicitudes.existenAbiertas(p.getId())) {
            String codigos = solicitudes.findByProyectoIdOrderByCodigoAsc(p.getId()).stream()
                    .filter(s -> s.getEstado() != EstadoSolicitud.CERRADO && s.getEstado() != EstadoSolicitud.RECHAZADO)
                    .map(SolicitudCambio::getCodigo).reduce((a, b) -> a + ", " + b).orElse("");
            throw new ReglaNegocioException("CAMBIOS_ABIERTOS",
                    "No puedes cerrar el proyecto: hay solicitudes de cambio sin cerrar (" + codigos + ").");
        }
    }

    private LocalDate terminoReal(Long proyectoId, LocalDate pedida) {
        if (pedida != null) {
            return pedida;
        }
        return actividades.findByProyectoIdOrderByOrdenAscIdAsc(proyectoId).stream().map(Actividad::getFechaTerminoReal)
                .filter(Objects::nonNull).max(LocalDate::compareTo).orElse(LocalDate.now(ZONA));
    }

    private void aplicar(Proyecto p, ProyectoRequest req) {
        p.setCodigo(req.codigo().trim());
        p.setNombre(req.nombre().trim());
        p.setDescripcion(req.descripcion() == null || req.descripcion().isBlank() ? null : req.descripcion().trim());
        p.setResponsable(usuarios.findById(req.responsableId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario", req.responsableId())));
        p.setCalendarioId(req.calendarioId());
        p.setFechaInicioPlan(req.fechaInicio());
        p.setFechaTerminoPlan(req.fechaTermino());
        p.setPresupuesto(req.presupuesto());
        if (req.estado() != null) {
            p.setEstado(req.estado());
        }
        if (p.getEstado() == EstadoProyecto.EN_EJECUCION && p.getFechaInicioReal() == null) {
            LocalDate hoy = LocalDate.now(ZONA);
            p.setFechaInicioReal(p.getFechaInicioPlan().isAfter(hoy) ? hoy : p.getFechaInicioPlan());
        }
    }

    private void guardarMiembros(Proyecto p, List<Long> ids) {
        if (ids == null) {
            return;
        }
        Set<Long> nuevos = new LinkedHashSet<>(ids);
        nuevos.remove(p.getResponsable().getId());
        for (Long u : nuevos) {
            if (!usuarios.existsById(u)) {
                throw new RecursoNoEncontradoException("Usuario", u);
            }
        }
        miembros.deleteByProyectoId(p.getId());
        miembros.flush();
        for (Long u : nuevos) {
            var m = new ProyectoMiembro();
            m.setProyectoId(p.getId());
            m.setUsuarioId(u);
            miembros.save(m);
        }
        miembros.flush();
    }

    private ProyectoDto dto(Proyecto p) {
        LocalDate hoy = LocalDate.now(ZONA);
        var c = costos.calcular(p.getId(), hoy);
        LocalDate termino = actividades.findByProyectoIdOrderByOrdenAscIdAsc(p.getId()).stream()
                .map(Actividad::getFechaTerminoPlan).filter(Objects::nonNull).max(LocalDate::compareTo).orElse(null);
        int abiertas = (int) solicitudes.findByProyectoIdOrderByCodigoAsc(p.getId()).stream()
                .filter(s -> s.getEstado() != EstadoSolicitud.CERRADO && s.getEstado() != EstadoSolicitud.RECHAZADO).count();
        List<Long> ms = miembros.findByProyectoId(p.getId()).stream().map(ProyectoMiembro::getUsuarioId).sorted().toList();
        BigDecimal plan = c.plan(), real = c.real();
        return ProyectoDto.de(p, termino, plan, real, ms, abiertas);
    }

    private Proyecto buscar(Long id) {
        return proyectos.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Proyecto", id));
    }
}
