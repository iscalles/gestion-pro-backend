package cl.duoc.gestionpro.cambio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.duoc.gestionpro.cambio.dto.AccionDto;
import cl.duoc.gestionpro.cambio.dto.AccionRequest;
import cl.duoc.gestionpro.cambio.dto.AprobacionDto;
import cl.duoc.gestionpro.cambio.dto.CierreRequest;
import cl.duoc.gestionpro.cambio.dto.DecisionRequest;
import cl.duoc.gestionpro.cambio.dto.EtapaDto;
import cl.duoc.gestionpro.cambio.dto.EtapaRequest;
import cl.duoc.gestionpro.cambio.dto.MedicionGatilladaDto;
import cl.duoc.gestionpro.cambio.dto.ModificacionDto;
import cl.duoc.gestionpro.cambio.dto.ModificacionRequest;
import cl.duoc.gestionpro.cambio.dto.OpcionModificacionDto;
import cl.duoc.gestionpro.cambio.dto.SeguimientoRequest;
import cl.duoc.gestionpro.cambio.dto.SolicitudDto;
import cl.duoc.gestionpro.cambio.dto.SolicitudRequest;
import cl.duoc.gestionpro.cambio.dto.SolicitudResumenDto;
import cl.duoc.gestionpro.cambio.dto.TrazabilidadDto;
import cl.duoc.gestionpro.comun.RecursoNoEncontradoException;
import cl.duoc.gestionpro.comun.ReglaNegocioException;
import cl.duoc.gestionpro.evidencia.EvidenciaService;
import cl.duoc.gestionpro.kpi.Kpi;
import cl.duoc.gestionpro.kpi.KpiRepository;
import cl.duoc.gestionpro.kpi.MedicionKpi;
import cl.duoc.gestionpro.kpi.MedicionKpiRepository;
import cl.duoc.gestionpro.planificacion.Actividad;
import cl.duoc.gestionpro.planificacion.ActividadRepository;
import cl.duoc.gestionpro.planificacion.ArbolActividades;
import cl.duoc.gestionpro.planificacion.CalendarioFactory;
import cl.duoc.gestionpro.proyecto.ProyectoRepository;
import cl.duoc.gestionpro.recalculo.CambioFecha;
import cl.duoc.gestionpro.recalculo.RecalculoService;
import cl.duoc.gestionpro.recurso.CostoService;
import cl.duoc.gestionpro.seguridad.Usuario;
import cl.duoc.gestionpro.seguridad.UsuarioRepository;

/**
 * Módulo 8: solicitudes de cambio que recorren 10 etapas.
 * <pre>
 * 1 Registro → 2 Análisis de impacto → 3 Evaluación de riesgo → 4 Aprobación (aprobar / rechazar)
 *   → 5 Planificación (se guarda la línea base y se aplican las modificaciones) → 6 Comunicación
 *   → 7 Implementación → 8 Verificación → 9 Adopción → 10 Cierre
 * </pre>
 * Estados: ABIERTO (etapa 1), EN_EVALUACION (2–4), APROBADO (5–6), EN_IMPLEMENTACION (7–10), CERRADO o RECHAZADO.
 * Aprobar aplica las modificaciones y recalcula fechas → costos → KPI → alertas en una sola transacción; cada
 * modificación queda con su valor anterior y la fecha en que se aplicó (trazabilidad).
 */
@Service
public class CambioService {

    private static final ZoneId ZONA = ZoneId.of("America/Santiago");

    private final SolicitudCambioRepository solicitudes;
    private final SolicitudEtapaRepository etapas;
    private final EtapaCambioRepository catalogoEtapas;
    private final CambioModificacionRepository modificaciones;
    private final AccionCorrectivaRepository acciones;
    private final LineaBaseRepository lineas;
    private final LineaBaseService lineaBase;
    private final AplicadorModificaciones aplicador;
    private final ProyectoRepository proyectos;
    private final ActividadRepository actividades;
    private final UsuarioRepository usuarios;
    private final EvidenciaService evidencias;
    private final KpiRepository kpis;
    private final MedicionKpiRepository mediciones;
    private final RecalculoService recalculo;
    private final CostoService costos;
    private final CalendarioFactory calendarios;

    public CambioService(SolicitudCambioRepository solicitudes, SolicitudEtapaRepository etapas,
            EtapaCambioRepository catalogoEtapas, CambioModificacionRepository modificaciones, AccionCorrectivaRepository acciones,
            LineaBaseRepository lineas, LineaBaseService lineaBase, AplicadorModificaciones aplicador, ProyectoRepository proyectos,
            ActividadRepository actividades, UsuarioRepository usuarios, EvidenciaService evidencias, KpiRepository kpis,
            MedicionKpiRepository mediciones, RecalculoService recalculo, CostoService costos, CalendarioFactory calendarios) {
        this.solicitudes = solicitudes;
        this.etapas = etapas;
        this.catalogoEtapas = catalogoEtapas;
        this.modificaciones = modificaciones;
        this.acciones = acciones;
        this.lineas = lineas;
        this.lineaBase = lineaBase;
        this.aplicador = aplicador;
        this.proyectos = proyectos;
        this.actividades = actividades;
        this.usuarios = usuarios;
        this.evidencias = evidencias;
        this.kpis = kpis;
        this.mediciones = mediciones;
        this.recalculo = recalculo;
        this.costos = costos;
        this.calendarios = calendarios;
    }

    // ---------------------------------------------------------------- consulta

    @Transactional(readOnly = true)
    public List<SolicitudResumenDto> listar(Long proyectoId) {
        Map<Long, String> nombres = nombresUsuarios();
        Map<Integer, String> nombresEtapa = nombresEtapa();
        Map<Long, List<CambioModificacion>> mods = modificaciones.deProyecto(proyectoId).stream()
                .collect(Collectors.groupingBy(CambioModificacion::getSolicitudId));
        LocalDate hoy = hoy();
        return solicitudes.findByProyectoIdOrderByCodigoAsc(proyectoId).stream().map(s -> {
            List<CambioModificacion> ms = mods.getOrDefault(s.getId(), List.of());
            return new SolicitudResumenDto(s.getId(), s.getCodigo(), s.getTitulo(), s.getSolicitante(), s.getFechaSolicitud(),
                    s.getTipo(), s.getPrioridad(), s.getEstado(), s.getEtapaActual(), nombresEtapa.get(s.getEtapaActual()),
                    s.getResponsableId(), nombres.get(s.getResponsableId()), s.getFechaComprometida(), s.getFechaReal(),
                    atrasada(s, hoy), s.getProbabilidad(), s.getImpacto(), s.riesgoCritico(), s.getImpactoPlazoDias(),
                    s.getCostoEstimado(), s.getCostoReal(), ms.size(), (int) ms.stream().filter(CambioModificacion::isAplicada).count());
        }).toList();
    }

    @Transactional(readOnly = true)
    public SolicitudDto obtener(Long proyectoId, Long id) {
        SolicitudCambio s = buscar(proyectoId, id);
        Map<Long, String> nombres = nombresUsuarios();
        Map<Integer, EtapaCambio> catalogo = catalogoEtapas.findAllByOrderByNumeroAsc().stream()
                .collect(Collectors.toMap(EtapaCambio::getNumero, e -> e));
        LocalDate hoy = hoy();
        List<EtapaDto> ets = etapas.findBySolicitudIdOrderByEtapaNumeroAsc(id).stream().map(e -> {
            EtapaCambio c = catalogo.get(e.getEtapaNumero());
            return new EtapaDto(e.getId(), e.getEtapaNumero(), c.getNombre(), c.getDescripcion(), e.getEstado(),
                    e.getResponsableId(), nombres.get(e.getResponsableId()), e.getFechaInicio(), e.getFechaFin(),
                    e.getFechaLimite(), e.getObservacion(), e.vencida(hoy), e.aTiempo());
        }).toList();
        ArbolActividades arbol = arbol(proyectoId);
        List<ModificacionDto> mods = modificaciones.findBySolicitudIdOrderByIdAsc(id).stream()
                .map(m -> modificacionDto(proyectoId, m, arbol)).toList();
        List<AccionDto> accs = acciones.findBySolicitudIdOrderByFechaCompromisoAscIdAsc(id).stream()
                .map(a -> accionDto(a, s, nombres, hoy)).toList();
        String lb = lineas.findBySolicitudId(id).map(LineaBase::codigo).orElse(null);
        return new SolicitudDto(s.getId(), s.getCodigo(), s.getTitulo(), s.getSolicitante(), s.getFechaSolicitud(),
                s.getDescripcion(), s.getJustificacion(), s.getTipo(), s.getPrioridad(), s.getRiesgo(), s.getProbabilidad(),
                s.getImpacto(), s.riesgoCritico(), s.getImpactoPlazoDias(), s.getImpactoCosto(), s.getImpactoRecursos(),
                s.getImpactoAlcance(), s.getResponsableId(), nombres.get(s.getResponsableId()), s.getEstado(), s.getEtapaActual(),
                s.getFechaComprometida(), s.getFechaReal(), atrasada(s, hoy), s.getCostoEstimado(), s.getCostoReal(),
                s.getAprobadoPorId(), nombres.get(s.getAprobadoPorId()), s.getFechaDecision(), s.getResultado(),
                s.getEfectividad(), s.getAdopcion(), s.getImpactoPlazoReal(), s.getImpactoCostoReal(), lb, s.getVersion(), ets,
                mods, accs, evidencias.deProyectoPorSolicitud(proyectoId).getOrDefault(id, List.of()),
                gatilladas(proyectoId, id));
    }

    @Transactional(readOnly = true)
    public List<OpcionModificacionDto> opciones(Long proyectoId) {
        return aplicador.opciones(proyectoId);
    }

    @Transactional(readOnly = true)
    public List<AccionDto> accionesDelProyecto(Long proyectoId) {
        Map<Long, String> nombres = nombresUsuarios();
        Map<Long, SolicitudCambio> sols = solicitudes.findByProyectoIdOrderByCodigoAsc(proyectoId).stream()
                .collect(Collectors.toMap(SolicitudCambio::getId, s -> s));
        LocalDate hoy = hoy();
        return acciones.deProyecto(proyectoId).stream().map(a -> accionDto(a, sols.get(a.getSolicitudId()), nombres, hoy)).toList();
    }

    /** Cambios aprobados con lo que modificaron en el proyecto y los KPI que movieron. */
    @Transactional(readOnly = true)
    public List<TrazabilidadDto> trazabilidad(Long proyectoId) {
        Map<Long, String> nombres = nombresUsuarios();
        ArbolActividades arbol = arbol(proyectoId);
        Map<Long, List<CambioModificacion>> mods = modificaciones.deProyecto(proyectoId).stream()
                .collect(Collectors.groupingBy(CambioModificacion::getSolicitudId));
        return solicitudes.findByProyectoIdOrderByCodigoAsc(proyectoId).stream()
                .filter(s -> s.getEstado().aprobada())
                .sorted(Comparator.comparing(SolicitudCambio::getFechaDecision, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(s -> new TrazabilidadDto(s.getId(), s.getCodigo(), s.getTitulo(), s.getEstado().name(),
                        nombres.get(s.getAprobadoPorId()), s.getFechaDecision(),
                        lineas.findBySolicitudId(s.getId()).map(LineaBase::codigo).orElse(null), s.getImpactoPlazoReal(),
                        s.getImpactoCostoReal(),
                        mods.getOrDefault(s.getId(), List.of()).stream().map(m -> modificacionDto(proyectoId, m, arbol)).toList(),
                        gatilladas(proyectoId, s.getId())))
                .toList();
    }

    // ---------------------------------------------------------------- registro y edición

    @Transactional
    public SolicitudDto crear(Long proyectoId, SolicitudRequest req, Long usuarioId) {
        proyectos.findById(proyectoId).orElseThrow(() -> new RecursoNoEncontradoException("Proyecto", proyectoId));
        var s = new SolicitudCambio();
        s.setProyectoId(proyectoId);
        s.setCodigo(siguienteCodigo(proyectoId));
        s.setFechaSolicitud(req.fechaSolicitud() == null ? hoy() : req.fechaSolicitud());
        aplicar(s, req);
        solicitudes.saveAndFlush(s);
        // 10 etapas con fechas límite repartidas entre la solicitud y la fecha comprometida
        long total = ChronoUnit.DAYS.between(s.getFechaSolicitud(), s.getFechaComprometida());
        for (EtapaCambio c : catalogoEtapas.findAllByOrderByNumeroAsc()) {
            var e = new SolicitudEtapa();
            e.setSolicitudId(s.getId());
            e.setEtapaNumero(c.getNumero());
            e.setResponsableId(c.getNumero() == 4 ? usuarioId : s.getResponsableId());
            e.setFechaLimite(s.getFechaSolicitud().plusDays(Math.round(Math.ceil(total * c.getNumero() / 10.0))));
            if (c.getNumero() == 1) {
                e.setEstado(EstadoEtapa.EN_CURSO);
                e.setFechaInicio(s.getFechaSolicitud());
            }
            etapas.save(e);
        }
        etapas.flush();
        recalculo.actualizarIndicadores(proyectoId, null);
        return obtener(proyectoId, s.getId());
    }

    @Transactional
    public SolicitudDto actualizar(Long proyectoId, Long id, SolicitudRequest req) {
        SolicitudCambio s = buscar(proyectoId, id);
        if (s.getEstado() != EstadoSolicitud.ABIERTO && s.getEstado() != EstadoSolicitud.EN_EVALUACION) {
            throw new ReglaNegocioException("SOLICITUD_DECIDIDA",
                    "La solicitud ya fue decidida: solo se puede registrar su seguimiento (costo real, adopción, efectividad).");
        }
        if (req.fechaSolicitud() != null) {
            s.setFechaSolicitud(req.fechaSolicitud());
        }
        aplicar(s, req);
        solicitudes.flush();
        recalculo.actualizarIndicadores(proyectoId, null);
        return obtener(proyectoId, id);
    }

    @Transactional
    public SolicitudDto seguimiento(Long proyectoId, Long id, SeguimientoRequest req) {
        SolicitudCambio s = buscar(proyectoId, id);
        if (s.getEstado() != EstadoSolicitud.APROBADO && s.getEstado() != EstadoSolicitud.EN_IMPLEMENTACION) {
            throw new ReglaNegocioException("ESTADO_INCORRECTO", "El seguimiento se registra en solicitudes aprobadas o en implementación.");
        }
        if (req.costoReal() != null) {
            s.setCostoReal(req.costoReal());
        }
        if (req.adopcion() != null) {
            s.setAdopcion(req.adopcion());
        }
        if (req.efectividad() != null) {
            s.setEfectividad(req.efectividad());
        }
        solicitudes.flush();
        recalculo.actualizarIndicadores(proyectoId, null);
        return obtener(proyectoId, id);
    }

    @Transactional
    public void eliminar(Long proyectoId, Long id) {
        SolicitudCambio s = buscar(proyectoId, id);
        if (s.getEstado() != EstadoSolicitud.ABIERTO) {
            throw new ReglaNegocioException("SOLICITUD_EN_CURSO", "Solo se eliminan solicitudes recién registradas; las demás se rechazan o cierran.");
        }
        solicitudes.delete(s);
        solicitudes.flush();
        recalculo.actualizarIndicadores(proyectoId, null);
    }

    private void aplicar(SolicitudCambio s, SolicitudRequest req) {
        if (req.fechaComprometida().isBefore(s.getFechaSolicitud())) {
            throw new ReglaNegocioException("FECHAS_INVALIDAS", "La fecha comprometida no puede ser anterior a la fecha de la solicitud.");
        }
        if (!usuarios.existsById(req.responsableId())) {
            throw new RecursoNoEncontradoException("Usuario", req.responsableId());
        }
        s.setTitulo(req.titulo().trim());
        s.setSolicitante(req.solicitante().trim());
        s.setDescripcion(req.descripcion().trim());
        s.setJustificacion(req.justificacion().trim());
        s.setTipo(req.tipo());
        s.setPrioridad(req.prioridad());
        s.setRiesgo(vacioANulo(req.riesgo()));
        s.setProbabilidad(req.probabilidad());
        s.setImpacto(req.impacto());
        s.setImpactoPlazoDias(req.impactoPlazoDias() == null ? 0 : req.impactoPlazoDias());
        s.setImpactoCosto(req.impactoCosto() == null ? BigDecimal.ZERO : req.impactoCosto());
        s.setImpactoRecursos(vacioANulo(req.impactoRecursos()));
        s.setImpactoAlcance(vacioANulo(req.impactoAlcance()));
        s.setResponsableId(req.responsableId());
        s.setFechaComprometida(req.fechaComprometida());
        s.setCostoEstimado(req.costoEstimado() == null ? BigDecimal.ZERO : req.costoEstimado());
    }

    // ---------------------------------------------------------------- flujo

    /** Completa la etapa en curso y abre la siguiente. La 4 se resuelve aprobando o rechazando; la 10, cerrando. */
    @Transactional
    public SolicitudDto avanzar(Long proyectoId, Long id, DecisionRequest req) {
        SolicitudCambio s = buscar(proyectoId, id);
        if (!s.getEstado().enCurso()) {
            throw new ReglaNegocioException("SOLICITUD_TERMINADA", "La solicitud está " + s.getEstado().name().toLowerCase() + ".");
        }
        int actual = s.getEtapaActual();
        if (actual == 4) {
            throw new ReglaNegocioException("DECISION_PENDIENTE", "La etapa de aprobación se resuelve aprobando o rechazando la solicitud.");
        }
        if (actual == 10) {
            throw new ReglaNegocioException("USAR_CIERRE", "Para terminar la etapa de cierre registra el resultado con «Cerrar solicitud».");
        }
        if (actual == 3 && (s.getProbabilidad() == null || s.getImpacto() == null)) {
            throw new ReglaNegocioException("RIESGO_SIN_EVALUAR", "Califica la probabilidad y el impacto del riesgo antes de pasar a la aprobación.");
        }
        Map<Integer, SolicitudEtapa> porNumero = etapasDe(id);
        completar(porNumero.get(actual), observacion(req));
        abrir(porNumero.get(actual + 1));
        s.setEtapaActual(actual + 1);
        if (actual + 1 <= 4) {
            s.setEstado(EstadoSolicitud.EN_EVALUACION);
        } else if (actual + 1 >= 7) {
            s.setEstado(EstadoSolicitud.EN_IMPLEMENTACION);
        }
        etapas.flush();
        recalculo.actualizarIndicadores(proyectoId, null);
        return obtener(proyectoId, id);
    }

    /**
     * Aprueba la solicitud (etapa 4): guarda la línea base, aplica las modificaciones (etapa 5), recalcula el
     * proyecto y registra el impacto real en plazo y costo. Si una modificación no es válida no se aplica ninguna.
     */
    @Transactional
    public AprobacionDto aprobar(Long proyectoId, Long id, DecisionRequest req, Long usuarioId) {
        SolicitudCambio s = buscar(proyectoId, id);
        if (s.getEtapaActual() != 4 || !(s.getEstado() == EstadoSolicitud.ABIERTO || s.getEstado() == EstadoSolicitud.EN_EVALUACION)) {
            throw new ReglaNegocioException("ETAPA_INCORRECTA", "Avanza la solicitud hasta la etapa de aprobación (4) antes de decidir.");
        }
        if (s.getProbabilidad() == null || s.getImpacto() == null) {
            throw new ReglaNegocioException("RIESGO_SIN_EVALUAR", "Califica la probabilidad y el impacto del riesgo antes de aprobar.");
        }
        LocalDate terminoAntes = termino(proyectoId);
        BigDecimal costoAntes = costos.calcular(proyectoId, hoy()).plan();
        Map<Long, MedicionKpi> kpiAntes = ultimas(proyectoId);

        LocalDateTime ahora = LocalDateTime.now(ZONA);
        s.setEstado(EstadoSolicitud.APROBADO);
        s.setAprobadoPorId(usuarioId);
        s.setFechaDecision(ahora);

        Map<Integer, SolicitudEtapa> porNumero = etapasDe(id);
        completar(porNumero.get(4), observacion(req) == null ? "Aprobada." : observacion(req));
        abrir(porNumero.get(5));
        List<CambioModificacion> pendientes = modificaciones.findBySolicitudIdOrderByIdAsc(id).stream()
                .filter(m -> !m.isAplicada()).toList();
        for (CambioModificacion m : pendientes) {
            m.setValorAnterior(aplicador.aplicar(proyectoId, m));
            m.setAplicada(true);
            m.setFechaAplicacion(ahora);
        }
        modificaciones.flush();
        aplicador.validarRecursos(proyectoId, pendientes);

        // El plan aprobado (con el cambio ya aplicado y reprogramado) pasa a ser la nueva línea base.
        List<CambioFecha> recalculadas = recalculo.reprogramar(proyectoId);
        LineaBase lb = lineaBase.guardar(proyectoId, id, usuarioId);
        completar(porNumero.get(5), pendientes.size() + (pendientes.size() == 1 ? " modificación aplicada" : " modificaciones aplicadas")
                + (lb == null ? "." : "; línea base " + lb.codigo() + " guardada."));
        abrir(porNumero.get(6));
        s.setEtapaActual(6);
        solicitudes.flush();

        LocalDate terminoDespues = termino(proyectoId);
        BigDecimal costoDespues = costos.calcular(proyectoId, hoy()).plan();
        int dias = terminoAntes == null || terminoDespues == null ? 0
                : LineaBaseService.diferencia(calendarios.deProyecto(proyectoId), terminoAntes, terminoDespues);
        s.setImpactoPlazoReal(dias);
        s.setImpactoCostoReal(costoDespues.subtract(costoAntes));
        solicitudes.flush();
        recalculo.actualizarIndicadores(proyectoId, id);

        List<MedicionGatilladaDto> afectados = new ArrayList<>();
        Map<Long, Kpi> kpiPorId = kpis.findByProyectoIdOrderByCodigoAsc(proyectoId).stream().collect(Collectors.toMap(Kpi::getId, k -> k));
        for (Kpi k : kpiPorId.values()) {
            MedicionKpi despues = mediciones.findFirstByKpiIdOrderByMedidoEnDescIdDesc(k.getId()).orElse(null);
            MedicionKpi antes = kpiAntes.get(k.getId());
            if (despues != null && id.equals(despues.getSolicitudId())) {
                afectados.add(new MedicionGatilladaDto(k.getId(), k.getCodigo(), k.getNombre(), k.getUnidad(),
                        antes == null ? null : antes.getValor(), antes == null ? null : antes.getSemaforo(), despues.getValor(),
                        despues.getSemaforo(), despues.getMedidoEn()));
            }
        }
        afectados.sort(Comparator.comparing(MedicionGatilladaDto::kpi));
        return new AprobacionDto(obtener(proyectoId, id), lb == null ? null : lb.codigo(), terminoAntes, terminoDespues, dias,
                costoAntes, costoDespues, recalculadas, afectados);
    }

    @Transactional
    public SolicitudDto rechazar(Long proyectoId, Long id, DecisionRequest req, Long usuarioId) {
        SolicitudCambio s = buscar(proyectoId, id);
        if (s.getEstado() != EstadoSolicitud.ABIERTO && s.getEstado() != EstadoSolicitud.EN_EVALUACION) {
            throw new ReglaNegocioException("SOLICITUD_DECIDIDA", "Solo se rechazan solicitudes que aún no se deciden.");
        }
        String motivo = observacion(req);
        if (motivo == null) {
            throw new ReglaNegocioException("MOTIVO_REQUERIDO", "Indica el motivo del rechazo.");
        }
        s.setEstado(EstadoSolicitud.RECHAZADO);
        s.setAprobadoPorId(usuarioId);
        s.setFechaDecision(LocalDateTime.now(ZONA));
        s.setResultado("Rechazada: " + motivo);
        for (SolicitudEtapa e : etapasDe(id).values()) {
            if (e.getEtapaNumero() == s.getEtapaActual()) {
                completar(e, "Rechazada: " + motivo);
            } else if (e.getEstado() == EstadoEtapa.PENDIENTE || e.getEstado() == EstadoEtapa.EN_CURSO) {
                e.setEstado(EstadoEtapa.OMITIDA);
            }
        }
        etapas.flush();
        solicitudes.flush();
        recalculo.actualizarIndicadores(proyectoId, null);
        return obtener(proyectoId, id);
    }

    @Transactional
    public SolicitudDto cerrar(Long proyectoId, Long id, CierreRequest req) {
        SolicitudCambio s = buscar(proyectoId, id);
        if (s.getEtapaActual() != 10 || s.getEstado() != EstadoSolicitud.EN_IMPLEMENTACION) {
            throw new ReglaNegocioException("ETAPA_INCORRECTA", "Avanza la solicitud hasta la etapa de cierre (10) antes de cerrarla.");
        }
        long abiertas = acciones.findBySolicitudIdOrderByFechaCompromisoAscIdAsc(id).stream().filter(a -> !a.cerrada()).count();
        if (abiertas > 0) {
            throw new ReglaNegocioException("ACCIONES_ABIERTAS", "La solicitud tiene " + abiertas
                    + (abiertas == 1 ? " acción correctiva abierta" : " acciones correctivas abiertas") + ". Ciérralas antes del cierre.");
        }
        LocalDate fecha = req.fechaReal() == null ? hoy() : req.fechaReal();
        if (fecha.isBefore(s.getFechaSolicitud())) {
            throw new ReglaNegocioException("FECHAS_INVALIDAS", "La fecha de cierre no puede ser anterior a la solicitud.");
        }
        SolicitudEtapa e10 = etapasDe(id).get(10);
        e10.setEstado(EstadoEtapa.COMPLETADA);
        if (e10.getFechaInicio() == null || e10.getFechaInicio().isAfter(fecha)) {
            e10.setFechaInicio(fecha);
        }
        e10.setFechaFin(fecha);
        e10.setObservacion(req.resultado().trim());
        s.setEstado(EstadoSolicitud.CERRADO);
        s.setFechaReal(fecha);
        s.setResultado(req.resultado().trim());
        s.setEfectividad(req.efectividad());
        if (req.adopcion() != null) {
            s.setAdopcion(req.adopcion());
        }
        s.setCostoReal(req.costoReal());
        etapas.flush();
        solicitudes.flush();
        recalculo.actualizarIndicadores(proyectoId, null);
        return obtener(proyectoId, id);
    }

    @Transactional
    public SolicitudDto actualizarEtapa(Long proyectoId, Long id, int numero, EtapaRequest req) {
        buscar(proyectoId, id);
        SolicitudEtapa e = etapasDe(id).get(numero);
        if (e == null) {
            throw new RecursoNoEncontradoException("Etapa", (long) numero);
        }
        if (req.responsableId() != null) {
            if (!usuarios.existsById(req.responsableId())) {
                throw new RecursoNoEncontradoException("Usuario", req.responsableId());
            }
            e.setResponsableId(req.responsableId());
        }
        e.setFechaLimite(req.fechaLimite());
        e.setObservacion(vacioANulo(req.observacion()));
        etapas.flush();
        recalculo.actualizarIndicadores(proyectoId, null);
        return obtener(proyectoId, id);
    }

    // ---------------------------------------------------------------- modificaciones propuestas

    @Transactional
    public SolicitudDto agregarModificacion(Long proyectoId, Long id, ModificacionRequest req) {
        SolicitudCambio s = buscar(proyectoId, id);
        exigirSinDecidir(s);
        String campo = req.campo().trim();
        aplicador.validar(proyectoId, req.entidad(), req.entidadId(), campo, req.valorNuevo());
        boolean repetida = modificaciones.findBySolicitudIdOrderByIdAsc(id).stream().anyMatch(m -> m.getEntidad() == req.entidad()
                && m.getEntidadId().equals(req.entidadId()) && m.getCampo().equals(campo));
        if (repetida) {
            throw new ReglaNegocioException("MODIFICACION_DUPLICADA", "La solicitud ya modifica ese campo; elimina la anterior para cambiar el valor.");
        }
        var m = new CambioModificacion();
        m.setSolicitudId(id);
        m.setEntidad(req.entidad());
        m.setEntidadId(req.entidadId());
        m.setCampo(campo);
        m.setValorNuevo(req.valorNuevo().trim());
        modificaciones.saveAndFlush(m);
        return obtener(proyectoId, id);
    }

    @Transactional
    public SolicitudDto eliminarModificacion(Long proyectoId, Long id, Long modificacionId) {
        SolicitudCambio s = buscar(proyectoId, id);
        exigirSinDecidir(s);
        CambioModificacion m = modificaciones.findById(modificacionId).filter(x -> x.getSolicitudId().equals(id))
                .orElseThrow(() -> new RecursoNoEncontradoException("Modificación", modificacionId));
        modificaciones.delete(m);
        modificaciones.flush();
        return obtener(proyectoId, id);
    }

    private static void exigirSinDecidir(SolicitudCambio s) {
        if (s.getEstado() != EstadoSolicitud.ABIERTO && s.getEstado() != EstadoSolicitud.EN_EVALUACION) {
            throw new ReglaNegocioException("SOLICITUD_DECIDIDA", "Las modificaciones se definen antes de la aprobación.");
        }
    }

    // ---------------------------------------------------------------- acciones correctivas

    @Transactional
    public SolicitudDto crearAccion(Long proyectoId, Long id, AccionRequest req) {
        buscar(proyectoId, id);
        var a = new AccionCorrectiva();
        a.setSolicitudId(id);
        aplicar(a, req);
        acciones.saveAndFlush(a);
        recalculo.actualizarIndicadores(proyectoId, null);
        return obtener(proyectoId, id);
    }

    @Transactional
    public SolicitudDto actualizarAccion(Long proyectoId, Long id, Long accionId, AccionRequest req) {
        buscar(proyectoId, id);
        AccionCorrectiva a = accion(id, accionId);
        aplicar(a, req);
        acciones.flush();
        recalculo.actualizarIndicadores(proyectoId, null);
        return obtener(proyectoId, id);
    }

    @Transactional
    public SolicitudDto eliminarAccion(Long proyectoId, Long id, Long accionId) {
        buscar(proyectoId, id);
        acciones.delete(accion(id, accionId));
        acciones.flush();
        recalculo.actualizarIndicadores(proyectoId, null);
        return obtener(proyectoId, id);
    }

    private void aplicar(AccionCorrectiva a, AccionRequest req) {
        if (!usuarios.existsById(req.responsableId())) {
            throw new RecursoNoEncontradoException("Usuario", req.responsableId());
        }
        if (req.fechaCierre() != null && req.fechaCierre().isAfter(hoy())) {
            throw new ReglaNegocioException("FECHA_FUTURA", "La fecha de cierre no puede ser futura.");
        }
        a.setDescripcion(req.descripcion().trim());
        a.setResponsableId(req.responsableId());
        a.setFechaCompromiso(req.fechaCompromiso());
        a.setFechaCierre(req.fechaCierre());
        a.setEstado(req.fechaCierre() == null ? "ABIERTA" : "CERRADA");
    }

    private AccionCorrectiva accion(Long solicitudId, Long accionId) {
        return acciones.findById(accionId).filter(a -> a.getSolicitudId().equals(solicitudId))
                .orElseThrow(() -> new RecursoNoEncontradoException("Acción correctiva", accionId));
    }

    // ---------------------------------------------------------------- apoyo

    private void completar(SolicitudEtapa e, String observacion) {
        LocalDate hoy = hoy();
        if (e.getFechaInicio() == null || e.getFechaInicio().isAfter(hoy)) {
            e.setFechaInicio(hoy);
        }
        e.setEstado(EstadoEtapa.COMPLETADA);
        e.setFechaFin(hoy);
        if (observacion != null) {
            e.setObservacion(observacion);
        }
    }

    private void abrir(SolicitudEtapa e) {
        e.setEstado(EstadoEtapa.EN_CURSO);
        e.setFechaInicio(hoy());
        e.setFechaFin(null);
    }

    private Map<Integer, SolicitudEtapa> etapasDe(Long solicitudId) {
        return etapas.findBySolicitudIdOrderByEtapaNumeroAsc(solicitudId).stream()
                .collect(Collectors.toMap(SolicitudEtapa::getEtapaNumero, e -> e));
    }

    private String siguienteCodigo(Long proyectoId) {
        int max = solicitudes.codigos(proyectoId).stream().map(c -> c.replaceAll("\\D", "")).filter(c -> !c.isEmpty())
                .mapToInt(Integer::parseInt).max().orElse(0);
        return String.format("SC-%03d", max + 1);
    }

    private LocalDate termino(Long proyectoId) {
        return actividades.findByProyectoIdOrderByOrdenAscIdAsc(proyectoId).stream().map(Actividad::getFechaTerminoPlan)
                .filter(Objects::nonNull).max(LocalDate::compareTo).orElse(null);
    }

    private Map<Long, MedicionKpi> ultimas(Long proyectoId) {
        Map<Long, MedicionKpi> m = new HashMap<>();
        for (Kpi k : kpis.findByProyectoIdOrderByCodigoAsc(proyectoId)) {
            mediciones.findFirstByKpiIdOrderByMedidoEnDescIdDesc(k.getId()).ifPresent(x -> m.put(k.getId(), x));
        }
        return m;
    }

    /** Mediciones de KPI que registró el recálculo de una solicitud, con el valor previo de cada KPI. */
    private List<MedicionGatilladaDto> gatilladas(Long proyectoId, Long solicitudId) {
        Map<Long, Kpi> kpiPorId = kpis.findByProyectoIdOrderByCodigoAsc(proyectoId).stream().collect(Collectors.toMap(Kpi::getId, k -> k));
        Map<Long, List<MedicionKpi>> porKpi = mediciones.deProyecto(proyectoId).stream()
                .collect(Collectors.groupingBy(MedicionKpi::getKpiId));
        List<MedicionGatilladaDto> salida = new ArrayList<>();
        porKpi.forEach((kpiId, lista) -> {
            Kpi k = kpiPorId.get(kpiId);
            MedicionKpi previa = null;
            MedicionKpi ultimaDelCambio = null;
            MedicionKpi previaDelCambio = null;
            for (MedicionKpi m : lista) {
                if (solicitudId.equals(m.getSolicitudId())) {
                    if (ultimaDelCambio == null) {
                        previaDelCambio = previa;
                    }
                    ultimaDelCambio = m;
                }
                previa = m;
            }
            if (ultimaDelCambio != null) {
                salida.add(new MedicionGatilladaDto(k.getId(), k.getCodigo(), k.getNombre(), k.getUnidad(),
                        previaDelCambio == null ? null : previaDelCambio.getValor(),
                        previaDelCambio == null ? null : previaDelCambio.getSemaforo(), ultimaDelCambio.getValor(),
                        ultimaDelCambio.getSemaforo(), ultimaDelCambio.getMedidoEn()));
            }
        });
        salida.sort(Comparator.comparing(MedicionGatilladaDto::kpi));
        return salida;
    }

    private ModificacionDto modificacionDto(Long proyectoId, CambioModificacion m, ArbolActividades arbol) {
        return new ModificacionDto(m.getId(), m.getEntidad(), m.getEntidadId(),
                aplicador.nombre(proyectoId, m.getEntidad(), m.getEntidadId(), arbol), m.getCampo(),
                AplicadorModificaciones.nombreCampo(m.getEntidad(), m.getCampo()), m.getValorAnterior(),
                aplicador.valorActual(proyectoId, m.getEntidad(), m.getEntidadId(), m.getCampo()), m.getValorNuevo(),
                m.isAplicada(), m.getFechaAplicacion());
    }

    private static AccionDto accionDto(AccionCorrectiva a, SolicitudCambio s, Map<Long, String> nombres, LocalDate hoy) {
        return new AccionDto(a.getId(), a.getSolicitudId(), s == null ? null : s.getCodigo(), a.getDescripcion(), a.getResponsableId(),
                nombres.get(a.getResponsableId()), a.getFechaCompromiso(), a.getFechaCierre(), a.getEstado(),
                !a.cerrada() && a.getFechaCompromiso().isBefore(hoy));
    }

    private ArbolActividades arbol(Long proyectoId) {
        return ArbolActividades.de(actividades.findByProyectoIdOrderByOrdenAscIdAsc(proyectoId));
    }

    private static boolean atrasada(SolicitudCambio s, LocalDate hoy) {
        return s.getEstado().enCurso() && s.getFechaComprometida().isBefore(hoy);
    }

    private Map<Long, String> nombresUsuarios() {
        return usuarios.findAll().stream().collect(Collectors.toMap(Usuario::getId, Usuario::getNombre));
    }

    private Map<Integer, String> nombresEtapa() {
        return catalogoEtapas.findAllByOrderByNumeroAsc().stream().collect(Collectors.toMap(EtapaCambio::getNumero, EtapaCambio::getNombre));
    }

    private SolicitudCambio buscar(Long proyectoId, Long id) {
        return solicitudes.findById(id).filter(s -> s.getProyectoId().equals(proyectoId))
                .orElseThrow(() -> new RecursoNoEncontradoException("Solicitud de cambio", id));
    }

    private static String observacion(DecisionRequest req) {
        return req == null ? null : vacioANulo(req.observacion());
    }

    private static String vacioANulo(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static LocalDate hoy() {
        return LocalDate.now(ZONA);
    }
}
