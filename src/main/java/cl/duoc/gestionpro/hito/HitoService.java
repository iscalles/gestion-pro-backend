package cl.duoc.gestionpro.hito;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.duoc.gestionpro.comun.RecursoNoEncontradoException;
import cl.duoc.gestionpro.comun.ReglaNegocioException;
import cl.duoc.gestionpro.evidencia.EvidenciaService;
import cl.duoc.gestionpro.evidencia.dto.EvidenciaDto;
import cl.duoc.gestionpro.hito.dto.HitoDto;
import cl.duoc.gestionpro.hito.dto.HitoRequest;
import cl.duoc.gestionpro.kpi.Kpi;
import cl.duoc.gestionpro.kpi.KpiRepository;
import cl.duoc.gestionpro.kpi.MedicionKpiRepository;
import cl.duoc.gestionpro.kpi.AmbitoKpi;
import cl.duoc.gestionpro.planificacion.Actividad;
import cl.duoc.gestionpro.planificacion.ActividadRepository;
import cl.duoc.gestionpro.planificacion.ArbolActividades;
import cl.duoc.gestionpro.recalculo.RecalculoService;
import cl.duoc.gestionpro.recurso.ProfesionalRepository;

/**
 * Módulo 6. Un hito es un punto de control de duración cero ligado a actividades: su fecha prevista es el término
 * más tardío de ellas. Solo se marca cumplido con fecha real y todos sus criterios de aceptación cumplidos.
 * Cada cambio recalcula KPI y alertas.
 */
@Service
public class HitoService {

    private static final ZoneId ZONA = ZoneId.of("America/Santiago");

    private final HitoRepository hitos;
    private final HitoCriterioRepository criterios;
    private final ActividadRepository actividades;
    private final ProfesionalRepository profesionales;
    private final KpiRepository kpis;
    private final MedicionKpiRepository mediciones;
    private final EvidenciaService evidencias;
    private final RecalculoService recalculo;
    private final JdbcTemplate jdbc;

    public HitoService(HitoRepository hitos, HitoCriterioRepository criterios, ActividadRepository actividades,
            ProfesionalRepository profesionales, KpiRepository kpis, MedicionKpiRepository mediciones,
            EvidenciaService evidencias, RecalculoService recalculo, JdbcTemplate jdbc) {
        this.hitos = hitos;
        this.criterios = criterios;
        this.actividades = actividades;
        this.profesionales = profesionales;
        this.kpis = kpis;
        this.mediciones = mediciones;
        this.evidencias = evidencias;
        this.recalculo = recalculo;
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public List<HitoDto> listar(Long proyectoId) {
        Map<Long, List<Long>> porHito = new HashMap<>();
        for (Object[] fila : hitos.actividadesDeHitos(proyectoId)) {
            porHito.computeIfAbsent(((Number) fila[0]).longValue(), k -> new ArrayList<>()).add(((Number) fila[1]).longValue());
        }
        ArbolActividades arbol = ArbolActividades.de(actividades.findByProyectoIdOrderByOrdenAscIdAsc(proyectoId));
        Map<Long, List<HitoCriterio>> crit = criterios.deProyecto(proyectoId).stream()
                .collect(Collectors.groupingBy(HitoCriterio::getHitoId));
        Map<Long, List<EvidenciaDto>> evs = evidencias.deProyectoPorHito(proyectoId);
        Map<Long, Kpi> kpiPorId = kpis.findByProyectoIdOrderByCodigoAsc(proyectoId).stream().collect(Collectors.toMap(Kpi::getId, k -> k));
        Map<Long, List<Long>> kpisPorHito = new HashMap<>();
        for (Object[] f : kpis.hitosDeKpis(proyectoId)) {
            kpisPorHito.computeIfAbsent(((Number) f[1]).longValue(), x -> new ArrayList<>()).add(((Number) f[0]).longValue());
        }
        LocalDate hoy = LocalDate.now(ZONA);
        return hitos.findByProyectoIdOrderByFechaComprometidaAscCodigoAsc(proyectoId).stream().map(h -> {
            List<Long> ids = porHito.getOrDefault(h.getId(), List.of()).stream()
                    .sorted((a, b) -> Integer.compare(arbol.enOrden().indexOf(arbol.get(a)), arbol.enOrden().indexOf(arbol.get(b))))
                    .toList();
            LocalDate prevista = ids.stream().map(arbol::get).filter(Objects::nonNull).map(Actividad::getFechaTerminoPlan)
                    .filter(Objects::nonNull).max(LocalDate::compareTo).orElse(null);
            boolean pendiente = h.getEstado() == EstadoHito.PENDIENTE;
            String calculado = pendiente && h.getFechaComprometida().isBefore(hoy) ? "ATRASADO" : h.getEstado().name();
            boolean enPlazo = h.getEstado() == EstadoHito.CUMPLIDO && !h.getFechaReal().isAfter(h.getFechaComprometida());
            boolean enRiesgo = pendiente && prevista != null && prevista.isAfter(h.getFechaComprometida());
            var r = h.getResponsable();
            List<HitoDto.CriterioDto> cs = crit.getOrDefault(h.getId(), List.of()).stream()
                    .map(c -> new HitoDto.CriterioDto(c.getId(), c.getOrden(), c.getDescripcion(), c.isCumplido())).toList();
            List<HitoDto.KpiVinculado> ks = kpisPorHito.getOrDefault(h.getId(), List.of()).stream().map(kpiPorId::get)
                    .filter(Objects::nonNull).map(k -> {
                        var m = mediciones.findFirstByKpiIdOrderByMedidoEnDescIdDesc(k.getId()).orElse(null);
                        return new HitoDto.KpiVinculado(k.getId(), k.getCodigo(), k.getNombre(), m == null ? null : m.getValor(),
                                k.getUnidad(), m == null ? null : m.getSemaforo());
                    }).sorted((a, b) -> a.codigo().compareTo(b.codigo())).toList();
            return new HitoDto(h.getId(), h.getCodigo(), h.getDescripcion(), r.getId(), r.getNombre(),
                    h.getFechaComprometida(), h.getFechaReal(), h.getEstado(), calculado, enPlazo, prevista, enRiesgo, ids,
                    ids.stream().map(arbol::etiqueta).toList(), cs, (int) cs.stream().filter(HitoDto.CriterioDto::cumplido).count(),
                    evs.getOrDefault(h.getId(), List.of()), ks);
        }).toList();
    }

    @Transactional(readOnly = true)
    public HitoDto obtener(Long proyectoId, Long id) {
        buscar(proyectoId, id);
        return listar(proyectoId).stream().filter(h -> h.id().equals(id)).findFirst().orElseThrow();
    }

    @Transactional
    public HitoDto crear(Long proyectoId, HitoRequest req) {
        String codigo = req.codigo().trim().toUpperCase();
        if (hitos.existsByProyectoIdAndCodigo(proyectoId, codigo)) {
            throw new ReglaNegocioException("CODIGO_DUPLICADO", "El proyecto ya tiene un hito " + codigo + ".");
        }
        var h = new Hito();
        h.setProyectoId(proyectoId);
        aplicar(h, req, codigo);
        hitos.saveAndFlush(h);
        guardarRelaciones(h, req);
        return terminar(proyectoId, h.getId());
    }

    @Transactional
    public HitoDto actualizar(Long proyectoId, Long id, HitoRequest req) {
        Hito h = buscar(proyectoId, id);
        String codigo = req.codigo().trim().toUpperCase();
        if (!h.getCodigo().equals(codigo) && hitos.existsByProyectoIdAndCodigo(proyectoId, codigo)) {
            throw new ReglaNegocioException("CODIGO_DUPLICADO", "El proyecto ya tiene un hito " + codigo + ".");
        }
        guardarRelaciones(h, req); // primero los criterios: la regla de cumplido los mira
        aplicar(h, req, codigo);
        hitos.flush();
        return terminar(proyectoId, id);
    }

    @Transactional
    public HitoDto marcarCriterio(Long proyectoId, Long id, Long criterioId, boolean cumplido) {
        Hito h = buscar(proyectoId, id);
        HitoCriterio c = criterios.findById(criterioId).filter(x -> x.getHitoId().equals(h.getId()))
                .orElseThrow(() -> new RecursoNoEncontradoException("Criterio", criterioId));
        if (!cumplido && h.getEstado() == EstadoHito.CUMPLIDO) {
            throw new ReglaNegocioException("HITO_CUMPLIDO", "El hito ya está cumplido; vuelve a dejarlo pendiente antes de desmarcar criterios.");
        }
        c.setCumplido(cumplido);
        criterios.flush();
        return terminar(proyectoId, id);
    }

    @Transactional
    public void eliminar(Long proyectoId, Long id) {
        Hito h = buscar(proyectoId, id);
        var usado = kpis.findByProyectoIdOrderByCodigoAsc(proyectoId).stream()
                .filter(k -> k.getAmbito() == AmbitoKpi.HITO && id.equals(k.getAmbitoRefId())).findFirst();
        if (usado.isPresent()) {
            throw new ReglaNegocioException("HITO_CON_KPI", "El KPI " + usado.get().getCodigo()
                    + " mide este hito. Cambia su ámbito o elimínalo antes de borrar el hito.");
        }
        hitos.delete(h);
        hitos.flush();
        recalculo.recalcularProyecto(proyectoId);
    }

    // ---------------------------------------------------------------- reglas

    private void aplicar(Hito h, HitoRequest req, String codigo) {
        h.setCodigo(codigo);
        h.setDescripcion(req.descripcion().trim());
        h.setResponsable(profesionales.findById(req.responsableId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Profesional", req.responsableId())));
        h.setFechaComprometida(req.fechaComprometida());
        EstadoHito estado = req.estado() == null ? EstadoHito.PENDIENTE : req.estado();
        if (estado == EstadoHito.CUMPLIDO) {
            if (req.fechaReal() == null) {
                throw new ReglaNegocioException("HITO_SIN_FECHA_REAL", "Indica la fecha real en que se cumplió el hito.");
            }
            if (req.fechaReal().isAfter(LocalDate.now(ZONA))) {
                throw new ReglaNegocioException("FECHA_FUTURA", "La fecha real no puede ser futura.");
            }
            long pendientes = h.getId() == null ? 0 : criterios.findByHitoIdOrderByOrdenAscIdAsc(h.getId()).stream()
                    .filter(c -> !c.isCumplido()).count();
            if (h.getId() == null && req.criterios() != null) {
                pendientes = req.criterios().stream().filter(c -> !c.cumplido()).count();
            }
            if (pendientes > 0) {
                throw new ReglaNegocioException("CRITERIOS_PENDIENTES", "Faltan " + pendientes
                        + (pendientes == 1 ? " criterio" : " criterios") + " de aceptación por cumplir: el hito no puede marcarse cumplido.");
            }
            h.setFechaReal(req.fechaReal());
        } else {
            h.setFechaReal(null);
        }
        h.setEstado(estado);
    }

    private void guardarRelaciones(Hito h, HitoRequest req) {
        if (req.actividadIds() != null) {
            Set<Long> validas = actividades.findByProyectoIdOrderByOrdenAscIdAsc(h.getProyectoId()).stream()
                    .map(Actividad::getId).collect(Collectors.toSet());
            jdbc.update("delete from hito_actividad where hito_id = ?", h.getId());
            for (Long a : new LinkedHashSet<>(req.actividadIds())) {
                if (!validas.contains(a)) {
                    throw new RecursoNoEncontradoException("Actividad", a);
                }
                jdbc.update("insert into hito_actividad (hito_id, actividad_id, proyecto_id) values (?, ?, ?)", h.getId(), a, h.getProyectoId());
            }
        }
        if (req.criterios() != null) {
            List<HitoCriterio> actuales = criterios.findByHitoIdOrderByOrdenAscIdAsc(h.getId());
            Map<Long, HitoCriterio> porId = actuales.stream().collect(Collectors.toMap(HitoCriterio::getId, c -> c));
            Set<Long> conservados = new HashSet<>();
            int orden = 1;
            for (HitoRequest.CriterioRequest cr : req.criterios()) {
                HitoCriterio c = cr.id() == null ? null : porId.get(cr.id());
                if (c == null) {
                    c = new HitoCriterio();
                    c.setHitoId(h.getId());
                }
                c.setOrden(orden++);
                c.setDescripcion(cr.descripcion().trim());
                c.setCumplido(cr.cumplido());
                criterios.save(c);
                if (c.getId() != null) {
                    conservados.add(c.getId());
                }
            }
            actuales.stream().filter(c -> !conservados.contains(c.getId())).forEach(criterios::delete);
            criterios.flush();
        }
        if (req.kpiIds() != null) {
            Set<Long> validos = kpis.findByProyectoIdOrderByCodigoAsc(h.getProyectoId()).stream().map(Kpi::getId).collect(Collectors.toSet());
            jdbc.update("delete from kpi_hito where hito_id = ?", h.getId());
            for (Long k : new LinkedHashSet<>(req.kpiIds())) {
                if (!validos.contains(k)) {
                    throw new RecursoNoEncontradoException("KPI", k);
                }
                jdbc.update("insert into kpi_hito (kpi_id, hito_id) values (?, ?)", k, h.getId());
            }
        }
    }

    private HitoDto terminar(Long proyectoId, Long id) {
        recalculo.recalcularProyecto(proyectoId);
        return obtener(proyectoId, id);
    }

    private Hito buscar(Long proyectoId, Long id) {
        return hitos.findById(id).filter(h -> h.getProyectoId().equals(proyectoId))
                .orElseThrow(() -> new RecursoNoEncontradoException("Hito", id));
    }
}
