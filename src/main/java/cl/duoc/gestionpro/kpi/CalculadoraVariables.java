package cl.duoc.gestionpro.kpi;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import cl.duoc.gestionpro.cambio.AccionCorrectiva;
import cl.duoc.gestionpro.cambio.AccionCorrectivaRepository;
import cl.duoc.gestionpro.cambio.SolicitudCambio;
import cl.duoc.gestionpro.cambio.SolicitudCambioRepository;
import cl.duoc.gestionpro.cambio.SolicitudEtapa;
import cl.duoc.gestionpro.cambio.SolicitudEtapaRepository;
import cl.duoc.gestionpro.comun.RecursoNoEncontradoException;
import cl.duoc.gestionpro.evidencia.EvidenciaRepository;
import cl.duoc.gestionpro.hito.EstadoHito;
import cl.duoc.gestionpro.hito.Hito;
import cl.duoc.gestionpro.hito.HitoCriterio;
import cl.duoc.gestionpro.hito.HitoCriterioRepository;
import cl.duoc.gestionpro.hito.HitoRepository;
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

/**
 * Calcula las variables del catálogo para un ámbito. Primero se cargan los datos del proyecto una sola vez
 * ({@link #datos}); luego cada KPI toma el subconjunto que le corresponde:
 * <ul>
 * <li>PROYECTO: todo el proyecto.</li>
 * <li>ETAPA / ACTIVIDAD: el elemento y sus descendientes, y los hitos ligados a ellos.</li>
 * <li>HITO: el hito indicado y las actividades que lo componen (todos los hitos si no se indica uno).</li>
 * <li>CAMBIO: la solicitud indicada (todas si no se indica una).</li>
 * </ul>
 * Una variable sin datos (por ejemplo un promedio sin casos) queda nula y el KPI no se mide.
 */
@Component
public class CalculadoraVariables {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    /** Todo lo necesario para medir los KPI de un proyecto. */
    public record Datos(Proyecto proyecto, LocalDate hoy, CalendarioLaboral cal, ArbolActividades arbol,
            CostoService.Costos costos, List<AsignacionProfesional> asignaciones, List<Hito> hitos,
            Map<Long, List<Long>> actividadesPorHito, List<HitoCriterio> criterios, Set<Long> hitosConEvidencia,
            List<SolicitudCambio> solicitudes, List<SolicitudEtapa> etapas, List<AccionCorrectiva> acciones) {
    }

    private final ProyectoRepository proyectos;
    private final ActividadRepository actividades;
    private final AsignacionProfesionalRepository asignacionesProfesional;
    private final AsignacionRecursoRepository asignacionesRecurso;
    private final HitoRepository hitos;
    private final HitoCriterioRepository criterios;
    private final EvidenciaRepository evidencias;
    private final SolicitudCambioRepository solicitudes;
    private final SolicitudEtapaRepository etapas;
    private final AccionCorrectivaRepository acciones;
    private final CalendarioFactory calendarios;
    private final CargaService carga;

    public CalculadoraVariables(ProyectoRepository proyectos, ActividadRepository actividades,
            AsignacionProfesionalRepository asignacionesProfesional, AsignacionRecursoRepository asignacionesRecurso,
            HitoRepository hitos, HitoCriterioRepository criterios, EvidenciaRepository evidencias,
            SolicitudCambioRepository solicitudes, SolicitudEtapaRepository etapas, AccionCorrectivaRepository acciones,
            CalendarioFactory calendarios, CargaService carga) {
        this.proyectos = proyectos;
        this.actividades = actividades;
        this.asignacionesProfesional = asignacionesProfesional;
        this.asignacionesRecurso = asignacionesRecurso;
        this.hitos = hitos;
        this.criterios = criterios;
        this.evidencias = evidencias;
        this.solicitudes = solicitudes;
        this.etapas = etapas;
        this.acciones = acciones;
        this.calendarios = calendarios;
        this.carga = carga;
    }

    @Transactional(readOnly = true)
    public Datos datos(Long proyectoId, LocalDate hoy) {
        Proyecto p = proyectos.findById(proyectoId).orElseThrow(() -> new RecursoNoEncontradoException("Proyecto", proyectoId));
        CalendarioLaboral cal = calendarios.deCalendario(p.getCalendarioId());
        List<Actividad> lista = actividades.findByProyectoIdOrderByOrdenAscIdAsc(proyectoId);
        List<AsignacionProfesional> aps = asignacionesProfesional.deProyecto(proyectoId);
        CostoService.Costos costos = CostoService.calcular(lista, aps, asignacionesRecurso.deProyecto(proyectoId), cal, hoy);
        Map<Long, List<Long>> porHito = new HashMap<>();
        for (Object[] fila : hitos.actividadesDeHitos(proyectoId)) {
            porHito.computeIfAbsent(((Number) fila[0]).longValue(), k -> new ArrayList<>()).add(((Number) fila[1]).longValue());
        }
        Set<Long> conEvidencia = evidencias.deProyecto(proyectoId).stream().map(EvidenciaRepository.Resumen::hitoId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        return new Datos(p, hoy, cal, ArbolActividades.de(lista), costos, aps,
                hitos.findByProyectoIdOrderByFechaComprometidaAscCodigoAsc(proyectoId), porHito, criterios.deProyecto(proyectoId),
                conEvidencia, solicitudes.findByProyectoIdOrderByCodigoAsc(proyectoId), etapas.deProyecto(proyectoId),
                acciones.deProyecto(proyectoId));
    }

    /** Valores de todas las variables del catálogo para el ámbito (las que no tienen datos quedan nulas). */
    public Map<String, BigDecimal> calcular(Datos d, AmbitoKpi ambito, Long refId) {
        Map<String, BigDecimal> v = new HashMap<>();
        List<Actividad> hojas = hojas(d, ambito, refId);
        List<Hito> hs = hitos(d, ambito, refId);
        List<SolicitudCambio> sols = ambito == AmbitoKpi.CAMBIO && refId != null
                ? d.solicitudes().stream().filter(s -> s.getId().equals(refId)).toList()
                : d.solicitudes();

        // ------------------------------------------------ trabajo y actividades
        BigDecimal completado = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal planificadoFecha = BigDecimal.ZERO;
        int atrasadas = 0, evaluadas = 0, terminadas = 0, enPlazo = 0;
        BigDecimal real = BigDecimal.ZERO, plan = BigDecimal.ZERO, ev = BigDecimal.ZERO, pv = BigDecimal.ZERO;
        for (Actividad a : hojas) {
            BigDecimal dur = BigDecimal.valueOf(a.getDuracionPlan());
            BigDecimal fraccion = CostoService.fraccionPlanificada(a, d.hoy(), d.cal());
            completado = completado.add(dur.multiply(a.getAvance()).divide(CIEN, 4, RoundingMode.HALF_UP));
            total = total.add(dur);
            planificadoFecha = planificadoFecha.add(dur.multiply(fraccion));
            boolean termino = a.getAvance().compareTo(CIEN) >= 0;
            if (!termino && a.getFechaTerminoPlan() != null && a.getFechaTerminoPlan().isBefore(d.hoy())) {
                atrasadas++;
            }
            if (a.getFechaInicioPlan() != null && !a.getFechaInicioPlan().isAfter(d.hoy())) {
                evaluadas++;
            }
            if (termino) {
                terminadas++;
                if (a.getFechaTerminoReal() != null && a.getFechaTerminoPlan() != null
                        && !a.getFechaTerminoReal().isAfter(a.getFechaTerminoPlan())) {
                    enPlazo++;
                }
            }
            var c = d.costos().de(a.getId());
            real = real.add(c.real());
            plan = plan.add(c.plan());
            ev = ev.add(c.plan().multiply(a.getAvance()).divide(CIEN, 2, RoundingMode.HALF_UP));
            pv = pv.add(c.plan().multiply(fraccion).setScale(2, RoundingMode.HALF_UP));
        }
        v.put("trabajo_completado", completado);
        v.put("trabajo_total", total);
        v.put("trabajo_planificado_fecha", planificadoFecha.setScale(4, RoundingMode.HALF_UP));
        v.put("actividades_atrasadas", BigDecimal.valueOf(atrasadas));
        v.put("actividades_evaluadas", BigDecimal.valueOf(evaluadas));
        v.put("actividades_terminadas", BigDecimal.valueOf(terminadas));
        v.put("actividades_terminadas_en_plazo", BigDecimal.valueOf(enPlazo));
        v.put("costo_real", real);
        v.put("costo_planificado_fecha", pv);
        v.put("costo_planificado_total", plan);
        v.put("valor_ganado", ev);
        v.put("valor_planificado", pv);
        v.put("presupuesto", d.proyecto().getPresupuesto());

        // ------------------------------------------------ utilización de profesionales (semana de hoy)
        Set<Long> ids = hojas.stream().map(Actividad::getId).collect(Collectors.toSet());
        List<Long> profesionales = d.asignaciones().stream().filter(a -> ids.contains(a.getActividadId()))
                .map(AsignacionProfesional::getProfesionalId).distinct().toList();
        BigDecimal asignadas = BigDecimal.ZERO, disponibles = BigDecimal.ZERO;
        if (!profesionales.isEmpty()) {
            for (CargaService.CargaProfesional c : carga.carga(d.hoy(), d.hoy(), profesionales)) {
                asignadas = asignadas.add(c.semanas().get(0).horas());
                disponibles = disponibles.add(c.horasSemanaDisp());
            }
        }
        v.put("horas_asignadas", asignadas);
        v.put("horas_disponibles", disponibles);

        // ------------------------------------------------ hitos y criterios
        int cumplidos = 0, cumplidosEnPlazo = 0, vencidosOCompletados = 0, conEvidencia = 0;
        for (Hito h : hs) {
            if (h.getEstado() == EstadoHito.CUMPLIDO) {
                cumplidos++;
                vencidosOCompletados++;
                if (!h.getFechaReal().isAfter(h.getFechaComprometida())) {
                    cumplidosEnPlazo++;
                }
                if (d.hitosConEvidencia().contains(h.getId())) {
                    conEvidencia++;
                }
            } else if (h.getEstado() == EstadoHito.PENDIENTE && h.getFechaComprometida().isBefore(d.hoy())) {
                vencidosOCompletados++;
            }
        }
        Set<Long> hitoIds = hs.stream().map(Hito::getId).collect(Collectors.toSet());
        List<HitoCriterio> crit = d.criterios().stream().filter(c -> hitoIds.contains(c.getHitoId())).toList();
        v.put("hitos_cumplidos", BigDecimal.valueOf(cumplidos));
        v.put("hitos_cumplidos_en_plazo", BigDecimal.valueOf(cumplidosEnPlazo));
        v.put("hitos_vencidos_o_completados", BigDecimal.valueOf(vencidosOCompletados));
        v.put("hitos_cumplidos_con_evidencia", BigDecimal.valueOf(conEvidencia));
        v.put("criterios_total", BigDecimal.valueOf(crit.size()));
        v.put("criterios_cumplidos", BigDecimal.valueOf(crit.stream().filter(HitoCriterio::isCumplido).count()));

        // ------------------------------------------------ cambios
        Set<Long> solIds = sols.stream().map(SolicitudCambio::getId).collect(Collectors.toSet());
        List<SolicitudCambio> aprobadas = sols.stream().filter(s -> s.getEstado().aprobada()).toList();
        List<SolicitudCambio> enCurso = sols.stream().filter(s -> s.getEstado().enCurso()).toList();
        v.put("cambios_registrados", BigDecimal.valueOf(sols.size()));
        v.put("cambios_aprobados", BigDecimal.valueOf(aprobadas.size()));
        v.put("cambios_decididos", BigDecimal.valueOf(sols.stream().filter(s -> s.getFechaDecision() != null
                && (s.getEstado().aprobada() || s.getEstado() == cl.duoc.gestionpro.cambio.EstadoSolicitud.RECHAZADO)).count()));
        v.put("cambios_en_curso", BigDecimal.valueOf(enCurso.size()));
        v.put("cambios_atrasados", BigDecimal.valueOf(enCurso.stream().filter(s -> s.getFechaComprometida().isBefore(d.hoy())).count()));
        v.put("dias_evaluacion_promedio", promedio(sols.stream().filter(s -> s.getFechaDecision() != null)
                .map(s -> BigDecimal.valueOf(ChronoUnit.DAYS.between(s.getFechaSolicitud(), s.getFechaDecision().toLocalDate())))
                .toList()));
        v.put("dias_implementacion_promedio", promedio(aprobadas.stream()
                .filter(s -> s.getFechaReal() != null && s.getFechaDecision() != null)
                .map(s -> BigDecimal.valueOf(ChronoUnit.DAYS.between(s.getFechaDecision().toLocalDate(), s.getFechaReal())))
                .toList()));
        v.put("costo_cambios", aprobadas.stream().map(SolicitudCambio::costoVigente).reduce(BigDecimal.ZERO, BigDecimal::add));
        List<SolicitudEtapa> ets = d.etapas().stream().filter(e -> solIds.contains(e.getSolicitudId())).toList();
        v.put("etapas_completadas_a_tiempo", BigDecimal.valueOf(ets.stream().filter(SolicitudEtapa::aTiempo).count()));
        v.put("etapas_vencidas_o_completadas", BigDecimal.valueOf(ets.stream()
                .filter(e -> e.getEstado() == cl.duoc.gestionpro.cambio.EstadoEtapa.COMPLETADA || e.vencida(d.hoy())).count()));
        v.put("efectividad_promedio", promedio(sols.stream().map(SolicitudCambio::getEfectividad).filter(Objects::nonNull).toList()));
        v.put("adopcion_promedio", promedio(sols.stream().map(SolicitudCambio::getAdopcion).filter(Objects::nonNull).toList()));
        v.put("riesgos_criticos_abiertos", BigDecimal.valueOf(enCurso.stream().filter(SolicitudCambio::riesgoCritico).count()));
        List<AccionCorrectiva> accs = d.acciones().stream().filter(a -> solIds.contains(a.getSolicitudId())).toList();
        v.put("acciones_correctivas", BigDecimal.valueOf(accs.size()));
        v.put("acciones_correctivas_cerradas", BigDecimal.valueOf(accs.stream().filter(AccionCorrectiva::cerrada).count()));
        v.put("dias_desviacion_por_cambios", BigDecimal.valueOf(aprobadas.stream()
                .map(SolicitudCambio::getImpactoPlazoReal).filter(Objects::nonNull).mapToInt(Integer::intValue).sum()));
        return v;
    }

    private static List<Actividad> hojas(Datos d, AmbitoKpi ambito, Long refId) {
        ArbolActividades arbol = d.arbol();
        return switch (ambito) {
            case ETAPA, ACTIVIDAD -> refId != null && arbol.contiene(refId) ? arbol.hojasDe(refId) : arbol.hojas();
            case HITO -> {
                if (refId == null) {
                    yield arbol.hojas();
                }
                Set<Long> ids = new LinkedHashSet<>();
                for (Long a : d.actividadesPorHito().getOrDefault(refId, List.of())) {
                    if (arbol.contiene(a)) {
                        arbol.hojasDe(a).forEach(h -> ids.add(h.getId()));
                    }
                }
                yield ids.stream().map(arbol::get).toList();
            }
            default -> arbol.hojas();
        };
    }

    private static List<Hito> hitos(Datos d, AmbitoKpi ambito, Long refId) {
        return switch (ambito) {
            case HITO -> refId == null ? d.hitos() : d.hitos().stream().filter(h -> h.getId().equals(refId)).toList();
            case ETAPA, ACTIVIDAD -> {
                if (refId == null || !d.arbol().contiene(refId)) {
                    yield d.hitos();
                }
                Set<Long> sub = new HashSet<>(d.arbol().subarbol(refId));
                yield d.hitos().stream()
                        .filter(h -> d.actividadesPorHito().getOrDefault(h.getId(), List.of()).stream().anyMatch(sub::contains))
                        .toList();
            }
            default -> d.hitos();
        };
    }

    private static BigDecimal promedio(Collection<BigDecimal> valores) {
        if (valores.isEmpty()) {
            return null;
        }
        return valores.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(valores.size()), 4, RoundingMode.HALF_UP);
    }
}
