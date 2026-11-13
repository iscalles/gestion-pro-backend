package cl.duoc.gestionpro.tablero;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.duoc.gestionpro.alerta.AlertaService;
import cl.duoc.gestionpro.cambio.EtapaCambio;
import cl.duoc.gestionpro.cambio.EtapaCambioRepository;
import cl.duoc.gestionpro.cambio.CambioService;
import cl.duoc.gestionpro.cambio.EstadoEtapa;
import cl.duoc.gestionpro.cambio.EstadoSolicitud;
import cl.duoc.gestionpro.cambio.Impacto;
import cl.duoc.gestionpro.cambio.Prioridad;
import cl.duoc.gestionpro.cambio.Probabilidad;
import cl.duoc.gestionpro.cambio.SolicitudCambio;
import cl.duoc.gestionpro.cambio.SolicitudCambioRepository;
import cl.duoc.gestionpro.cambio.SolicitudEtapa;
import cl.duoc.gestionpro.cambio.SolicitudEtapaRepository;
import cl.duoc.gestionpro.cambio.TipoCambio;
import cl.duoc.gestionpro.cambio.dto.AccionDto;
import cl.duoc.gestionpro.cambio.dto.SolicitudResumenDto;
import cl.duoc.gestionpro.comun.RecursoNoEncontradoException;
import cl.duoc.gestionpro.hito.HitoService;
import cl.duoc.gestionpro.hito.dto.HitoDto;
import cl.duoc.gestionpro.kpi.AmbitoKpi;
import cl.duoc.gestionpro.kpi.CategoriaKpi;
import cl.duoc.gestionpro.kpi.KpiService;
import cl.duoc.gestionpro.kpi.Semaforo;
import cl.duoc.gestionpro.kpi.dto.KpiDto;
import cl.duoc.gestionpro.planificacion.Actividad;
import cl.duoc.gestionpro.planificacion.ActividadRepository;
import cl.duoc.gestionpro.planificacion.ArbolActividades;
import cl.duoc.gestionpro.planificacion.CalendarioFactory;
import cl.duoc.gestionpro.planificacion.CalendarioLaboral;
import cl.duoc.gestionpro.proyecto.Proyecto;
import cl.duoc.gestionpro.proyecto.ProyectoRepository;
import cl.duoc.gestionpro.recurso.AsignacionProfesional;
import cl.duoc.gestionpro.recurso.AsignacionProfesionalRepository;
import cl.duoc.gestionpro.recurso.AsignacionRecursoRepository;
import cl.duoc.gestionpro.recurso.CargaService;
import cl.duoc.gestionpro.recurso.CostoService;
import cl.duoc.gestionpro.recurso.RecursoService;
import cl.duoc.gestionpro.tablero.dto.TableroCambioDto;
import cl.duoc.gestionpro.tablero.dto.TableroProyectoDto;

/** Arma los dos tableros de control a partir de los mismos servicios que usan las pantallas de cada módulo. */
@Service
public class TableroService {

    private static final ZoneId ZONA = ZoneId.of("America/Santiago");
    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    private final ProyectoRepository proyectos;
    private final ActividadRepository actividades;
    private final AsignacionProfesionalRepository asignacionesProfesional;
    private final AsignacionRecursoRepository asignacionesRecurso;
    private final CalendarioFactory calendarios;
    private final KpiService kpis;
    private final HitoService hitos;
    private final AlertaService alertas;
    private final CargaService carga;
    private final RecursoService recursos;
    private final CambioService cambios;
    private final SolicitudCambioRepository solicitudes;
    private final SolicitudEtapaRepository etapasSolicitud;
    private final EtapaCambioRepository catalogoEtapas;

    public TableroService(ProyectoRepository proyectos, ActividadRepository actividades,
            AsignacionProfesionalRepository asignacionesProfesional, AsignacionRecursoRepository asignacionesRecurso,
            CalendarioFactory calendarios, KpiService kpis, HitoService hitos, AlertaService alertas, CargaService carga,
            RecursoService recursos, CambioService cambios, SolicitudCambioRepository solicitudes,
            SolicitudEtapaRepository etapasSolicitud, EtapaCambioRepository catalogoEtapas) {
        this.proyectos = proyectos;
        this.actividades = actividades;
        this.asignacionesProfesional = asignacionesProfesional;
        this.asignacionesRecurso = asignacionesRecurso;
        this.calendarios = calendarios;
        this.kpis = kpis;
        this.hitos = hitos;
        this.alertas = alertas;
        this.carga = carga;
        this.recursos = recursos;
        this.cambios = cambios;
        this.solicitudes = solicitudes;
        this.etapasSolicitud = etapasSolicitud;
        this.catalogoEtapas = catalogoEtapas;
    }

    // ================================================================ Tablero 1: proyecto

    @Transactional(readOnly = true)
    public TableroProyectoDto proyecto(Long proyectoId, Long etapaId, LocalDate desde, LocalDate hasta, Long responsableId) {
        Proyecto p = proyectos.findById(proyectoId).orElseThrow(() -> new RecursoNoEncontradoException("Proyecto", proyectoId));
        LocalDate hoy = LocalDate.now(ZONA);
        CalendarioLaboral cal = calendarios.deCalendario(p.getCalendarioId());
        List<Actividad> lista = actividades.findByProyectoIdOrderByOrdenAscIdAsc(proyectoId);
        ArbolActividades arbol = ArbolActividades.de(lista);
        List<AsignacionProfesional> aps = asignacionesProfesional.deProyecto(proyectoId);
        CostoService.Costos costos = CostoService.calcular(lista, aps, asignacionesRecurso.deProyecto(proyectoId), cal, hoy);

        // hojas dentro de los filtros de etapa y responsable
        List<Actividad> hojas = (etapaId != null && arbol.contiene(etapaId) ? arbol.hojasDe(etapaId) : arbol.hojas()).stream()
                .filter(a -> responsableId == null || a.getResponsable() != null && responsableId.equals(a.getResponsable().getId()))
                .toList();
        Set<Long> idsHojas = hojas.stream().map(Actividad::getId).collect(Collectors.toSet());

        // resumen
        BigDecimal durTotal = BigDecimal.ZERO, durHecha = BigDecimal.ZERO, durPlan = BigDecimal.ZERO;
        BigDecimal plan = BigDecimal.ZERO, real = BigDecimal.ZERO, ev = BigDecimal.ZERO, pv = BigDecimal.ZERO;
        int terminadas = 0, enCurso = 0, atrasadas = 0;
        for (Actividad a : hojas) {
            BigDecimal dur = BigDecimal.valueOf(Math.max(a.getDuracionPlan(), 0));
            BigDecimal fr = CostoService.fraccionPlanificada(a, hoy, cal);
            durTotal = durTotal.add(dur);
            durHecha = durHecha.add(dur.multiply(a.getAvance()).divide(CIEN, 4, RoundingMode.HALF_UP));
            durPlan = durPlan.add(dur.multiply(fr));
            var c = costos.de(a.getId());
            plan = plan.add(c.plan());
            real = real.add(c.real());
            ev = ev.add(c.plan().multiply(a.getAvance()).divide(CIEN, 2, RoundingMode.HALF_UP));
            pv = pv.add(c.plan().multiply(fr).setScale(2, RoundingMode.HALF_UP));
            String estado = estado(a, hoy);
            switch (estado) {
                case "TERMINADA" -> terminadas++;
                case "ATRASADA" -> atrasadas++;
                case "EN_CURSO" -> enCurso++;
                default -> { }
            }
        }
        BigDecimal avanceReal = porcentaje(durHecha, durTotal);
        BigDecimal avancePlan = porcentaje(durPlan, durTotal);

        // hitos filtrados
        Set<Long> subEtapa = etapaId != null && arbol.contiene(etapaId) ? new HashSet<>(arbol.subarbol(etapaId)) : null;
        List<HitoDto> hs = hitos.listar(proyectoId).stream()
                .filter(h -> subEtapa == null || h.actividadIds().stream().anyMatch(subEtapa::contains))
                .filter(h -> responsableId == null || responsableId.equals(h.responsableId()))
                .filter(h -> dentro(h.fechaComprometida(), desde, hasta))
                .toList();

        // KPI de proyecto (los de etapa/actividad se muestran si caen en la etapa filtrada)
        List<KpiDto> ks = kpis.listar(proyectoId, CategoriaKpi.PROYECTO).stream()
                .filter(k -> subEtapa == null || k.ambito() == AmbitoKpi.PROYECTO || k.ambito() == AmbitoKpi.HITO
                        || k.ambitoRefId() != null && subEtapa.contains(k.ambitoRefId()))
                .map(k -> conHistorial(k, desde, hasta)).toList();

        var abiertas = alertas.listar(proyectoId, false);
        var sobre = carga.sobreasignaciones(proyectoId);

        TableroProyectoDto.Resumen resumen = new TableroProyectoDto.Resumen(avanceReal, avancePlan, plan, real, ev, pv,
                razon(ev, pv), razon(ev, real), p.getPresupuesto(), hojas.size(), terminadas, enCurso, atrasadas, hs.size(),
                (int) hs.stream().filter(h -> "CUMPLIDO".equals(h.estadoCalculado())).count(),
                (int) hs.stream().filter(h -> "ATRASADO".equals(h.estadoCalculado())).count(),
                (int) hs.stream().filter(HitoDto::enRiesgo).count(), abiertas.size(),
                (int) abiertas.stream().filter(a -> "ALTA".equals(a.severidad())).count(),
                (int) sobre.stream().map(CargaService.Sobreasignacion::profesionalId).distinct().count(),
                (int) ks.stream().filter(k -> k.semaforo() == Semaforo.VERDE).count(),
                (int) ks.stream().filter(k -> k.semaforo() == Semaforo.AMBAR).count(),
                (int) ks.stream().filter(k -> k.semaforo() == Semaforo.ROJO).count());

        // etapas
        List<TableroProyectoDto.Etapa> ets = new ArrayList<>();
        for (Actividad e : arbol.enOrden()) {
            if (e.getPadreId() != null) {
                continue;
            }
            List<Actividad> hh = arbol.hojasDe(e.getId());
            BigDecimal t = BigDecimal.ZERO, pl = BigDecimal.ZERO;
            int atr = 0;
            for (Actividad a : hh) {
                BigDecimal dur = BigDecimal.valueOf(Math.max(a.getDuracionPlan(), 0));
                t = t.add(dur);
                pl = pl.add(dur.multiply(CostoService.fraccionPlanificada(a, hoy, cal)));
                if ("ATRASADA".equals(estado(a, hoy))) {
                    atr++;
                }
            }
            var c = costos.de(e.getId());
            ets.add(new TableroProyectoDto.Etapa(e.getId(), arbol.codigo(e.getId()), e.getNombre(), e.getFechaInicioPlan(),
                    e.getFechaTerminoPlan(), e.getAvance(), porcentaje(pl, t), c.plan(), c.real(), atr));
        }

        // curva semanal (avance y costo planificados acumulados) para las hojas del filtro
        LocalDate ini = hojas.stream().map(Actividad::getFechaInicioPlan).filter(Objects::nonNull).min(LocalDate::compareTo)
                .orElse(p.getFechaInicioPlan());
        LocalDate fin = hojas.stream().map(Actividad::getFechaTerminoPlan).filter(Objects::nonNull).max(LocalDate::compareTo)
                .orElse(p.getFechaTerminoPlan());
        LocalDate curvaDesde = desde != null && desde.isAfter(ini) ? desde : ini;
        LocalDate curvaHasta = hasta != null && hasta.isBefore(fin) ? hasta : fin;
        List<TableroProyectoDto.PuntoCurva> curva = new ArrayList<>();
        for (LocalDate s = CargaService.lunesDe(curvaDesde); !s.isAfter(curvaHasta.plusDays(6)); s = s.plusWeeks(1)) {
            LocalDate corte = s.plusDays(4).isAfter(fin) ? fin : s.plusDays(4); // viernes de la semana
            BigDecimal d = BigDecimal.ZERO, cp = BigDecimal.ZERO, dr = BigDecimal.ZERO;
            boolean pasada = !s.isAfter(hoy);
            for (Actividad a : hojas) {
                BigDecimal fr = CostoService.fraccionPlanificada(a, corte, cal);
                BigDecimal dur = BigDecimal.valueOf(Math.max(a.getDuracionPlan(), 0));
                d = d.add(dur.multiply(fr));
                cp = cp.add(costos.de(a.getId()).plan().multiply(fr));
                if (pasada) {
                    dr = dr.add(dur.multiply(fraccionReal(a, corte.isAfter(hoy) ? hoy : corte, hoy)));
                }
            }
            curva.add(new TableroProyectoDto.PuntoCurva(s, porcentaje(d, durTotal), cp.setScale(0, RoundingMode.HALF_UP),
                    pasada ? porcentaje(dr, durTotal) : null));
            if (curva.size() > 120) {
                break;
            }
        }

        // actividades filtradas por fechas
        List<TableroProyectoDto.Actividad> acts = hojas.stream()
                .filter(a -> solapa(a.getFechaInicioPlan(), a.getFechaTerminoPlan(), desde, hasta))
                .map(a -> {
                    String est = estado(a, hoy);
                    int atraso = "ATRASADA".equals(est) ? Math.max(cal.contar(a.getFechaTerminoPlan().plusDays(1), hoy), 1) : 0;
                    var c = costos.de(a.getId());
                    Actividad etapa = arbol.etapaDe(a.getId());
                    return new TableroProyectoDto.Actividad(a.getId(), arbol.codigo(a.getId()), a.getNombre(),
                            etapa == null ? null : etapa.getNombre(), a.getResponsable() == null ? null : a.getResponsable().getNombre(),
                            a.getFechaInicioPlan(), a.getFechaTerminoPlan(), a.getAvance(), est, atraso, c.plan(), c.real());
                }).toList();

        // carga de los profesionales del filtro (8 semanas desde la semana anterior a hoy, o el rango pedido)
        LocalDate cargaDesde = desde != null ? desde : hoy.minusWeeks(2);
        LocalDate cargaHasta = hasta != null ? hasta : cargaDesde.plusWeeks(7);
        if (ChronoUnit.WEEKS.between(cargaDesde, cargaHasta) > 26) {
            cargaHasta = cargaDesde.plusWeeks(26);
        }
        List<Long> profs = aps.stream().filter(a -> idsHojas.contains(a.getActividadId()))
                .map(AsignacionProfesional::getProfesionalId).distinct().toList();
        List<CargaService.CargaProfesional> cargas = profs.isEmpty() ? List.of() : carga.carga(cargaDesde, cargaHasta, profs);

        List<TableroProyectoDto.Opcion> opcionesEtapa = arbol.enOrden().stream().filter(a -> a.getPadreId() == null)
                .map(a -> new TableroProyectoDto.Opcion(a.getId(), arbol.etiqueta(a.getId()))).toList();
        Map<Long, String> responsables = new LinkedHashMap<>();
        arbol.enOrden().stream().map(Actividad::getResponsable).filter(Objects::nonNull)
                .forEach(r -> responsables.putIfAbsent(r.getId(), r.getNombre()));
        List<TableroProyectoDto.Opcion> opcionesResp = responsables.entrySet().stream()
                .sorted(Map.Entry.comparingByValue()).map(e -> new TableroProyectoDto.Opcion(e.getKey(), e.getValue())).toList();

        LocalDate terminoPrevisto = lista.stream().map(Actividad::getFechaTerminoPlan).filter(Objects::nonNull)
                .max(LocalDate::compareTo).orElse(null);
        return new TableroProyectoDto(
                new TableroProyectoDto.Proyecto(p.getId(), p.getCodigo(), p.getNombre(), p.getEstado().name(),
                        p.getResponsable().getNombre(), p.getFechaInicioPlan(), p.getFechaTerminoPlan(), terminoPrevisto,
                        p.getPresupuesto()),
                new TableroProyectoDto.Filtros(etapaId, desde, hasta, responsableId), resumen, ets, curva, ks,
                hs.stream().map(h -> new TableroProyectoDto.Hito(h.id(), h.codigo(), h.descripcion(), h.responsable(),
                        h.fechaComprometida(), h.fechaPrevista(), h.fechaReal(), h.estadoCalculado(), h.enRiesgo(),
                        h.criteriosCumplidos(), h.criterios().size())).toList(),
                acts, cargas, recursos.costos(proyectoId).porTipo(), abiertas, opcionesEtapa, opcionesResp);
    }

    // ================================================================ Tablero 2: cambios

    @Transactional(readOnly = true)
    public TableroCambioDto cambio(Long proyectoId, LocalDate desde, LocalDate hasta, TipoCambio tipo, Prioridad prioridad) {
        Proyecto p = proyectos.findById(proyectoId).orElseThrow(() -> new RecursoNoEncontradoException("Proyecto", proyectoId));
        LocalDate hoy = LocalDate.now(ZONA);
        List<SolicitudCambio> sols = solicitudes.findByProyectoIdOrderByCodigoAsc(proyectoId).stream()
                .filter(s -> dentro(s.getFechaSolicitud(), desde, hasta))
                .filter(s -> tipo == null || s.getTipo() == tipo)
                .filter(s -> prioridad == null || s.getPrioridad() == prioridad)
                .toList();
        Set<Long> ids = sols.stream().map(SolicitudCambio::getId).collect(Collectors.toSet());
        List<SolicitudResumenDto> resumenes = cambios.listar(proyectoId).stream().filter(s -> ids.contains(s.id())).toList();
        List<AccionDto> accs = cambios.accionesDelProyecto(proyectoId).stream().filter(a -> ids.contains(a.solicitudId())).toList();

        List<SolicitudCambio> aprobadas = sols.stream().filter(s -> s.getEstado().aprobada()).toList();
        long rechazadas = sols.stream().filter(s -> s.getEstado() == EstadoSolicitud.RECHAZADO).count();
        List<Integer> evaluacion = sols.stream().filter(s -> s.getFechaDecision() != null)
                .map(s -> (int) ChronoUnit.DAYS.between(s.getFechaSolicitud(), s.getFechaDecision().toLocalDate())).toList();
        List<Integer> implementacion = aprobadas.stream().filter(s -> s.getFechaReal() != null && s.getFechaDecision() != null)
                .map(s -> (int) ChronoUnit.DAYS.between(s.getFechaDecision().toLocalDate(), s.getFechaReal())).toList();
        TableroCambioDto.Resumen resumen = new TableroCambioDto.Resumen(sols.size(),
                (int) sols.stream().filter(s -> s.getEstado().enCurso()).count(), aprobadas.size(), (int) rechazadas,
                (int) sols.stream().filter(s -> s.getEstado() == EstadoSolicitud.CERRADO).count(),
                (int) sols.stream().filter(s -> s.getEstado().enCurso() && s.getFechaComprometida().isBefore(hoy)).count(),
                (int) sols.stream().filter(s -> s.getEstado().enCurso() && s.riesgoCritico()).count(),
                aprobadas.size() + rechazadas == 0 ? null
                        : porcentaje(BigDecimal.valueOf(aprobadas.size()), BigDecimal.valueOf(aprobadas.size() + rechazadas)),
                promedioEnteros(evaluacion), promedioEnteros(implementacion),
                sols.stream().map(SolicitudCambio::getCostoEstimado).reduce(BigDecimal.ZERO, BigDecimal::add),
                sols.stream().map(SolicitudCambio::getCostoReal).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add),
                aprobadas.stream().map(SolicitudCambio::getImpactoPlazoReal).filter(Objects::nonNull).mapToInt(Integer::intValue).sum(),
                aprobadas.stream().map(SolicitudCambio::getImpactoCostoReal).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add),
                (int) accs.stream().filter(a -> "ABIERTA".equals(a.estado())).count(),
                (int) accs.stream().filter(a -> "CERRADA".equals(a.estado())).count(),
                (int) accs.stream().filter(AccionDto::vencida).count(),
                promedio(sols.stream().map(SolicitudCambio::getEfectividad).filter(Objects::nonNull).toList()),
                promedio(sols.stream().map(SolicitudCambio::getAdopcion).filter(Objects::nonNull).toList()));

        Map<Integer, String> nombresEtapa = catalogoEtapas.findAllByOrderByNumeroAsc().stream()
                .collect(Collectors.toMap(EtapaCambio::getNumero, EtapaCambio::getNombre, (a, b) -> a, LinkedHashMap::new));

        List<TableroCambioDto.Conteo> porEstado = new ArrayList<>();
        for (EstadoSolicitud e : EstadoSolicitud.values()) {
            porEstado.add(new TableroCambioDto.Conteo(e.name(), etiqueta(e.name()), (int) sols.stream().filter(s -> s.getEstado() == e).count()));
        }
        List<TableroCambioDto.Conteo> porTipo = new ArrayList<>();
        for (TipoCambio t : TipoCambio.values()) {
            porTipo.add(new TableroCambioDto.Conteo(t.name(), etiqueta(t.name()), (int) sols.stream().filter(s -> s.getTipo() == t).count()));
        }
        List<TableroCambioDto.Conteo> porPrioridad = new ArrayList<>();
        for (Prioridad pr : Prioridad.values()) {
            porPrioridad.add(new TableroCambioDto.Conteo(pr.name(), etiqueta(pr.name()),
                    (int) sols.stream().filter(s -> s.getPrioridad() == pr).count()));
        }
        List<TableroCambioDto.Conteo> porEtapa = new ArrayList<>();
        nombresEtapa.forEach((n, nombre) -> porEtapa.add(new TableroCambioDto.Conteo(String.valueOf(n), nombre,
                (int) sols.stream().filter(s -> s.getEstado().enCurso() && s.getEtapaActual() == n).count())));

        List<TableroCambioDto.CeldaRiesgo> matriz = new ArrayList<>();
        for (Probabilidad pr : new Probabilidad[] { Probabilidad.ALTA, Probabilidad.MEDIA, Probabilidad.BAJA }) {
            for (Impacto im : Impacto.values()) {
                List<String> codigos = sols.stream().filter(s -> s.getProbabilidad() == pr && s.getImpacto() == im)
                        .map(SolicitudCambio::getCodigo).toList();
                matriz.add(new TableroCambioDto.CeldaRiesgo(pr.name(), im.name(), codigos.size(), codigos));
            }
        }

        List<TableroCambioDto.Tiempos> tiempos = sols.stream().map(s -> new TableroCambioDto.Tiempos(s.getCodigo(),
                s.getFechaDecision() == null ? null : (int) ChronoUnit.DAYS.between(s.getFechaSolicitud(), s.getFechaDecision().toLocalDate()),
                s.getFechaDecision() == null || s.getFechaReal() == null ? null
                        : (int) ChronoUnit.DAYS.between(s.getFechaDecision().toLocalDate(), s.getFechaReal()))).toList();
        List<TableroCambioDto.Costos> costos = sols.stream()
                .map(s -> new TableroCambioDto.Costos(s.getCodigo(), s.getCostoEstimado(), s.getCostoReal())).toList();

        Map<Integer, int[]> porNumero = new HashMap<>();
        for (SolicitudEtapa e : etapasSolicitud.deProyecto(proyectoId)) {
            if (!ids.contains(e.getSolicitudId())) {
                continue;
            }
            int[] c = porNumero.computeIfAbsent(e.getEtapaNumero(), k -> new int[4]);
            if (e.getEstado() == EstadoEtapa.COMPLETADA) {
                c[e.aTiempo() ? 0 : 1]++;
            } else if (e.vencida(hoy)) {
                c[2]++;
            } else if (e.getEstado() == EstadoEtapa.EN_CURSO) {
                c[3]++;
            }
        }
        List<TableroCambioDto.CumplimientoEtapa> cumplimiento = new ArrayList<>();
        nombresEtapa.forEach((n, nombre) -> {
            int[] c = porNumero.getOrDefault(n, new int[4]);
            cumplimiento.add(new TableroCambioDto.CumplimientoEtapa(n, nombre, c[0], c[1], c[2], c[3]));
        });

        List<KpiDto> ks = kpis.listar(proyectoId, CategoriaKpi.CAMBIO).stream().map(k -> conHistorial(k, desde, hasta)).toList();
        Set<Long> kpiCambio = ks.stream().map(KpiDto::id).collect(Collectors.toSet());
        List<AlertaService.AlertaDto> als = alertas.listar(proyectoId, false).stream()
                .filter(a -> ("SOLICITUD".equals(a.entidad()) && ids.contains(a.entidadId()))
                        || "SOLICITUD_ETAPA".equals(a.entidad())
                        || ("KPI".equals(a.entidad()) && kpiCambio.contains(a.entidadId())))
                .toList();

        return new TableroCambioDto(proyectoId, p.getCodigo() + " " + p.getNombre(),
                new TableroCambioDto.Filtros(desde, hasta, tipo == null ? null : tipo.name(), prioridad == null ? null : prioridad.name()),
                resumen, porEstado, porTipo, porPrioridad, porEtapa, matriz, tiempos, costos, cumplimiento, resumenes, accs, ks, als);
    }

    // ================================================================ apoyo

    private static KpiDto conHistorial(KpiDto k, LocalDate desde, LocalDate hasta) {
        if (desde == null && hasta == null) {
            return k;
        }
        return new KpiDto(k.id(), k.codigo(), k.nombre(), k.objetivo(), k.categoria(), k.ambito(), k.ambitoRefId(), k.ambitoNombre(),
                k.formula(), k.variables(), k.unidad(), k.sentido(), k.meta(), k.verdeMin(), k.verdeMax(), k.ambarMin(), k.ambarMax(),
                k.periodicidad(), k.fuente(), k.responsableId(), k.responsable(), k.activo(), k.valor(), k.semaforo(), k.medidoEn(),
                k.valorAnterior(), k.sinDatos(), k.hitoIds(),
                k.historial().stream().filter(m -> dentro(m.medidoEn().toLocalDate(), desde, hasta)).toList());
    }

    private static String estado(Actividad a, LocalDate hoy) {
        if (a.getAvance().compareTo(CIEN) >= 0) {
            return "TERMINADA";
        }
        if (a.getFechaTerminoPlan() != null && a.getFechaTerminoPlan().isBefore(hoy)) {
            return "ATRASADA";
        }
        if (a.getAvance().signum() > 0 || a.getFechaInicioPlan() != null && !a.getFechaInicioPlan().isAfter(hoy)) {
            return "EN_CURSO";
        }
        return "PENDIENTE";
    }

    private static boolean dentro(LocalDate f, LocalDate desde, LocalDate hasta) {
        return f != null && (desde == null || !f.isBefore(desde)) && (hasta == null || !f.isAfter(hasta));
    }

    private static boolean solapa(LocalDate inicio, LocalDate termino, LocalDate desde, LocalDate hasta) {
        if (inicio == null || termino == null) {
            return desde == null && hasta == null;
        }
        return (desde == null || !termino.isBefore(desde)) && (hasta == null || !inicio.isAfter(hasta));
    }

    private static BigDecimal porcentaje(BigDecimal parte, BigDecimal total) {
        return total.signum() == 0 ? BigDecimal.ZERO : parte.multiply(CIEN).divide(total, 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal razon(BigDecimal a, BigDecimal b) {
        return b.signum() == 0 ? null : a.divide(b, 3, RoundingMode.HALF_UP);
    }

    private static BigDecimal promedio(List<BigDecimal> v) {
        return v.isEmpty() ? null : v.stream().reduce(BigDecimal.ZERO, BigDecimal::add).divide(BigDecimal.valueOf(v.size()), 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal promedioEnteros(List<Integer> v) {
        return promedio(v.stream().map(BigDecimal::valueOf).toList());
    }

    private static String etiqueta(String clave) {
        String s = clave.replace('_', ' ').toLowerCase();
        s = s.replace("evaluacion", "evaluación").replace("implementacion", "implementación").replace("tecnologia", "tecnología")
                .replace("organizacion", "organización").replace("critica", "crítica");
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /**
     * Fracción ejecutada de una hoja a una fecha de corte pasada. No se guarda el historial de avance, así que se
     * reconstruye desde las fechas reales: terminada = 1; en curso = avance actual prorrateado linealmente entre el
     * inicio real y hoy.
     */
    static BigDecimal fraccionReal(Actividad a, LocalDate corte, LocalDate hoy) {
        LocalDate ini = a.getFechaInicioReal();
        if (ini == null || corte.isBefore(ini)) {
            return BigDecimal.ZERO;
        }
        if (a.getFechaTerminoReal() != null && !corte.isBefore(a.getFechaTerminoReal())) {
            return BigDecimal.ONE;
        }
        BigDecimal actual = a.getAvance() == null ? BigDecimal.ZERO : a.getAvance().movePointLeft(2);
        long total = java.time.temporal.ChronoUnit.DAYS.between(ini, hoy) + 1;
        long hasta = java.time.temporal.ChronoUnit.DAYS.between(ini, corte) + 1;
        if (total <= 0 || hasta >= total) {
            return actual.min(BigDecimal.ONE);
        }
        return actual.multiply(BigDecimal.valueOf(hasta)).divide(BigDecimal.valueOf(total), 6, RoundingMode.HALF_UP).min(BigDecimal.ONE);
    }
}
