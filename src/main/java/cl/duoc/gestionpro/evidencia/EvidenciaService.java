package cl.duoc.gestionpro.evidencia;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import cl.duoc.gestionpro.cambio.SolicitudCambioRepository;
import cl.duoc.gestionpro.comun.RecursoNoEncontradoException;
import cl.duoc.gestionpro.comun.ReglaNegocioException;
import cl.duoc.gestionpro.evidencia.dto.EvidenciaDto;
import cl.duoc.gestionpro.hito.HitoRepository;
import cl.duoc.gestionpro.seguridad.Usuario;
import cl.duoc.gestionpro.seguridad.UsuarioRepository;

/** Evidencias (archivos de hasta 10 MB) de hitos y solicitudes de cambio, guardadas como BLOB en Oracle. */
@Service
public class EvidenciaService {

    public static final long MAXIMO = 10L * 1024 * 1024;
    private static final ZoneId ZONA = ZoneId.of("America/Santiago");

    private final EvidenciaRepository evidencias;
    private final HitoRepository hitos;
    private final SolicitudCambioRepository solicitudes;
    private final UsuarioRepository usuarios;

    public EvidenciaService(EvidenciaRepository evidencias, HitoRepository hitos, SolicitudCambioRepository solicitudes,
            UsuarioRepository usuarios) {
        this.evidencias = evidencias;
        this.hitos = hitos;
        this.solicitudes = solicitudes;
        this.usuarios = usuarios;
    }

    @Transactional(readOnly = true)
    public Map<Long, List<EvidenciaDto>> deProyectoPorHito(Long proyectoId) {
        Map<Long, String> nombres = nombres();
        Map<Long, List<EvidenciaDto>> m = new HashMap<>();
        for (var r : evidencias.deProyecto(proyectoId)) {
            if (r.hitoId() != null) {
                m.computeIfAbsent(r.hitoId(), k -> new ArrayList<>()).add(dto(r, nombres));
            }
        }
        return m;
    }

    @Transactional(readOnly = true)
    public Map<Long, List<EvidenciaDto>> deProyectoPorSolicitud(Long proyectoId) {
        Map<Long, String> nombres = nombres();
        Map<Long, List<EvidenciaDto>> m = new HashMap<>();
        for (var r : evidencias.deProyecto(proyectoId)) {
            if (r.solicitudId() != null) {
                m.computeIfAbsent(r.solicitudId(), k -> new ArrayList<>()).add(dto(r, nombres));
            }
        }
        return m;
    }

    @Transactional
    public EvidenciaDto subirAHito(Long proyectoId, Long hitoId, MultipartFile archivo, Long usuarioId) {
        hitos.findById(hitoId).filter(h -> h.getProyectoId().equals(proyectoId))
                .orElseThrow(() -> new RecursoNoEncontradoException("Hito", hitoId));
        var e = nueva(archivo, usuarioId);
        e.setHitoId(hitoId);
        evidencias.saveAndFlush(e);
        return dto(e);
    }

    @Transactional
    public EvidenciaDto subirASolicitud(Long proyectoId, Long solicitudId, MultipartFile archivo, Long usuarioId) {
        solicitudes.findById(solicitudId).filter(s -> s.getProyectoId().equals(proyectoId))
                .orElseThrow(() -> new RecursoNoEncontradoException("Solicitud de cambio", solicitudId));
        var e = nueva(archivo, usuarioId);
        e.setSolicitudId(solicitudId);
        evidencias.saveAndFlush(e);
        return dto(e);
    }

    /** Proyecto al que pertenece la evidencia (para verificar el acceso antes de descargar o borrar). */
    @Transactional(readOnly = true)
    public Long proyectoDe(Long evidenciaId) {
        var e = evidencias.findById(evidenciaId).orElseThrow(() -> new RecursoNoEncontradoException("Evidencia", evidenciaId));
        if (e.getHitoId() != null) {
            return hitos.findById(e.getHitoId()).orElseThrow().getProyectoId();
        }
        return solicitudes.findById(e.getSolicitudId()).orElseThrow().getProyectoId();
    }

    @Transactional(readOnly = true)
    public Evidencia archivo(Long id) {
        var e = evidencias.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Evidencia", id));
        e.getContenido(); // carga el BLOB dentro de la transacción
        return e;
    }

    /** Puede borrar quien la subió o un administrador. Devuelve el hito al que pertenecía (o nulo). */
    @Transactional
    public Long eliminar(Long id, Long usuarioId, boolean esAdmin) {
        var e = evidencias.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Evidencia", id));
        if (!esAdmin && !e.getSubidoPorId().equals(usuarioId)) {
            throw new ReglaNegocioException("SIN_PERMISO_EVIDENCIA", "Solo quien subió la evidencia o un administrador puede eliminarla.");
        }
        Long hito = e.getHitoId();
        evidencias.delete(e);
        evidencias.flush();
        return hito;
    }

    private Evidencia nueva(MultipartFile archivo, Long usuarioId) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ReglaNegocioException("ARCHIVO_VACIO", "Elige un archivo con contenido.");
        }
        if (archivo.getSize() > MAXIMO) {
            throw new ReglaNegocioException("ARCHIVO_GRANDE", "El archivo supera los 10 MB.");
        }
        String nombre = archivo.getOriginalFilename() == null ? "archivo" : archivo.getOriginalFilename();
        nombre = nombre.replace('\\', '/');
        nombre = nombre.substring(nombre.lastIndexOf('/') + 1);
        if (nombre.isBlank()) {
            nombre = "archivo";
        }
        if (nombre.length() > 200) {
            nombre = nombre.substring(nombre.length() - 200);
        }
        var e = new Evidencia();
        e.setNombreArchivo(nombre);
        e.setTipoMime(archivo.getContentType() == null || archivo.getContentType().isBlank()
                ? "application/octet-stream" : archivo.getContentType());
        e.setTamanoBytes(archivo.getSize());
        try {
            e.setContenido(archivo.getBytes());
        } catch (IOException ex) {
            throw new ReglaNegocioException("ARCHIVO_ILEGIBLE", "No se pudo leer el archivo.");
        }
        e.setSubidoPorId(usuarioId);
        e.setSubidoEn(LocalDateTime.now(ZONA));
        return e;
    }

    private Map<Long, String> nombres() {
        return usuarios.findAll().stream().collect(Collectors.toMap(Usuario::getId, Usuario::getNombre));
    }

    private EvidenciaDto dto(Evidencia e) {
        return new EvidenciaDto(e.getId(), e.getNombreArchivo(), e.getTipoMime(), e.getTamanoBytes(), e.getSubidoPorId(),
                usuarios.findById(e.getSubidoPorId()).map(Usuario::getNombre).orElse(null), e.getSubidoEn());
    }

    private static EvidenciaDto dto(EvidenciaRepository.Resumen r, Map<Long, String> nombres) {
        return new EvidenciaDto(r.id(), r.nombreArchivo(), r.tipoMime(), r.tamanoBytes(), r.subidoPorId(),
                nombres.get(r.subidoPorId()), r.subidoEn());
    }
}
