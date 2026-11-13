package cl.duoc.gestionpro.alerta;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.duoc.gestionpro.cambio.EstadoEtapa;
import cl.duoc.gestionpro.cambio.EtapaCambio;
import cl.duoc.gestionpro.cambio.EtapaCambioRepository;
import cl.duoc.gestionpro.cambio.SolicitudCambio;
import cl.duoc.gestionpro.cambio.SolicitudEtapa;
import cl.duoc.gestionpro.hito.EstadoHito;
import cl.duoc.gestionpro.hito.Hito;
import cl.duoc.gestionpro.kpi.CalculadoraVariables;
import cl.duoc.gestionpro.kpi.Kpi;
import cl.duoc.gestionpro.kpi.KpiRepository;
import cl.duoc.gestionpro.kpi.MedicionKpiRepository;
import cl.duoc.gestionpro.kpi.Semaforo;
import cl.duoc.gestionpro.planificacion.Actividad;
import cl.duoc.gestionpro.recurso.CargaService;

/**
 * Alertas del proyecto. No se escriben a mano: cada recálculo evalúa las condiciones vigentes, abre las nuevas,
 * actualiza el texto de las que siguen y marca resueltas las que ya no se cumplen (quedan en el historial).
 */
@Service
public class AlertaService {

    private static final ZoneId ZONA = ZoneId.of("America/Santiago");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    public record AlertaDto(Long id, TipoAlerta tipo, String severidad, String mensaje, String entidad, Long entidadId,
            LocalDateTime creadaEn, boolean resuelta, LocalDateTime resueltaEn) {

        static AlertaDto de(Alerta a) {
            return new AlertaDto(a.getId(), a.getTipo(), a.getSeveridad(), a.getMensaje(), a.getEntidad(), a.getEntidadId(),
                    a.getCreadaEn(), a.isResuelta(), a.getResueltaEn());
        }
    }

    private record Condicion(TipoAlerta tipo, String severidad, String mensaje, String entidad, Long entidadId) {
        String clave() {
            return tipo + "|" + entidad + "|" + entidadId;
        }
    }

    private final AlertaRepository alertas;
    private final CalculadoraVariables calculadora;
    private final CargaService carga;
    private final KpiRepository kpis;
    private final MedicionKpiRepository mediciones;
    private final EtapaCambioRepository etapasCambio;

    public AlertaService(AlertaRepository alertas, CalculadoraVariables calculadora, CargaService carga, KpiRepository kpis,
            MedicionKpiRepository mediciones, EtapaCambioRepository etapasCambio) {
        this.alertas = alertas;
        this.calculadora = calculadora;
        this.carga = carga;
        this.kpis = kpis;
        this.mediciones = mediciones;
        this.etapasCambio = etapasCambio;
    }

    @Transactional(readOnly = true)
    public List<AlertaDto> listar(Long proyectoId, boolean todas) {
        var lista = todas ? alertas.findByProyectoIdOrderByCreadaEnDescIdDesc(proyectoId)
                : alertas.findByProyectoIdAndResueltaFalseOrderByCreadaEnDescIdDesc(proyectoId);
        return lista.stream()
                .sorted((a, b) -> a.isResuelta() != b.isResuelta() ? Boolean.compare(a.isResuelta(), b.isResuelta())
                        : !a.getSeveridad().equals(b.getSeveridad()) ? a.getSeveridad().compareTo(b.getSeveridad())
                        : b.getCreadaEn().compareTo(a.getCreadaEn()))
                .map(AlertaDto::de).toList();
    }

    /** Abre, actualiza y resuelve alertas según el estado actual del proyecto. Devuelve cuántas quedan abiertas. */
    @Transactional
    public int sincronizar(Long proyectoId) {
        LocalDateTime ahora = LocalDateTime.now(ZONA);
        Map<String, Condicion> vigentes = new LinkedHashMap<>();
        condiciones(proyectoId, ahora.toLocalDate()).forEach(c -> vigentes.putIfAbsent(c.clave(), c));

        Map<String, Alerta> abiertas = new HashMap<>();
        for (Alerta a : alertas.findByProyectoIdAndResueltaFalseOrderByCreadaEnDescIdDesc(proyectoId)) {
            String clave = a.getTipo() + "|" + a.getEntidad() + "|" + a.getEntidadId();
            if (abiertas.putIfAbsent(clave, a) != null) {
                resolver(a, ahora); // duplicada
            }
        }
        for (Condicion c : vigentes.values()) {
            Alerta a = abiertas.remove(c.clave());
            if (a == null) {
                a = new Alerta();
                a.setProyectoId(proyectoId);
                a.setTipo(c.tipo());
                a.setEntidad(c.entidad());
                a.setEntidadId(c.entidadId());
                a.setCreadaEn(ahora);
            }
            a.setSeveridad(c.severidad());
            a.setMensaje(recortar(c.mensaje()));
            if (a.getId() == null) {
                alertas.save(a); // con IDENTITY el insert es inmediato: los campos obligatorios van antes
            }
        }
        abiertas.values().forEach(a -> resolver(a, ahora));
        alertas.flush();
        return vigentes.size();
    }

    private static void resolver(Alerta a, LocalDateTime ahora) {
        a.setResuelta(true);
        a.setResueltaEn(ahora);
    }

    private List<Condicion> condiciones(Long proyectoId, LocalDate hoy) {
        var d = calculadora.datos(proyectoId, hoy);
        List<Condicion> salida = new ArrayList<>();
        NumberFormat pesos = NumberFormat.getIntegerInstance(Locale.forLanguageTag("es-CL"));

        // Sobreasignación: una alerta por profesional, con sus semanas excedidas
        Map<Long, List<CargaService.Sobreasignacion>> porProf = carga.sobreasignaciones(proyectoId).stream()
                .collect(Collectors.groupingBy(CargaService.Sobreasignacion::profesionalId, LinkedHashMap::new, Collectors.toList()));
        porProf.forEach((id, semanas) -> {
            var peor = semanas.stream().max((a, b) -> a.exceso().compareTo(b.exceso())).orElseThrow();
            boolean actual = semanas.stream().anyMatch(s -> !s.semana().plusDays(6).isBefore(hoy));
            salida.add(new Condicion(TipoAlerta.SOBREASIGNACION, actual ? "ALTA" : "MEDIA",
                    peor.profesional() + " supera sus horas disponibles en " + semanas.size()
                            + (semanas.size() == 1 ? " semana" : " semanas") + " (máximo " + num(peor.horas()) + " h de "
                            + num(peor.disponibles()) + " h, semana del " + FECHA.format(peor.semana()) + ").",
                    "PROFESIONAL", id));
        });

        // KPI fuera de meta: rojo = alta, ámbar = media
        for (Kpi k : kpis.findByProyectoIdOrderByCodigoAsc(proyectoId)) {
            if (!k.isActivo()) {
                continue;
            }
            mediciones.findFirstByKpiIdOrderByMedidoEnDescIdDesc(k.getId()).ifPresent(m -> {
                if (m.getSemaforo() != Semaforo.VERDE) {
                    salida.add(new Condicion(TipoAlerta.KPI_FUERA_META, m.getSemaforo() == Semaforo.ROJO ? "ALTA" : "MEDIA",
                            k.getCodigo() + " " + k.getNombre() + ": " + num(m.getValor()) + " " + k.getUnidad() + " (meta "
                                    + num(k.getMeta()) + " " + k.getUnidad() + ", semáforo " + m.getSemaforo().name().toLowerCase().replace("ambar", "ámbar") + ").",
                            "KPI", k.getId()));
                }
            });
        }

        // Actividades hoja atrasadas
        for (Actividad a : d.arbol().hojas()) {
            if (a.getAvance().compareTo(CIEN) < 0 && a.getFechaTerminoPlan() != null && a.getFechaTerminoPlan().isBefore(hoy)) {
                int dias = Math.max(d.cal().contar(a.getFechaTerminoPlan().plusDays(1), hoy), 1);
                salida.add(new Condicion(TipoAlerta.ACTIVIDAD_ATRASADA, dias > 5 ? "ALTA" : "MEDIA",
                        d.arbol().etiqueta(a.getId()) + ": debía terminar el " + FECHA.format(a.getFechaTerminoPlan()) + " y va en "
                                + num(a.getAvance()) + " % (" + dias + (dias == 1 ? " día hábil" : " días hábiles") + " de atraso).",
                        "ACTIVIDAD", a.getId()));
            }
            var c = d.costos().de(a.getId());
            if (c.plan().signum() > 0 && c.real().compareTo(c.plan()) > 0) {
                salida.add(new Condicion(TipoAlerta.DESVIACION_COSTO, "MEDIA",
                        d.arbol().etiqueta(a.getId()) + ": costo real $" + pesos.format(c.real()) + " supera lo planificado ($"
                                + pesos.format(c.plan()) + ").", "ACTIVIDAD", a.getId()));
            }
        }

        // Hitos atrasados o en riesgo
        for (Hito h : d.hitos()) {
            if (h.getEstado() != EstadoHito.PENDIENTE) {
                continue;
            }
            if (h.getFechaComprometida().isBefore(hoy)) {
                salida.add(new Condicion(TipoAlerta.HITO_ATRASADO, "ALTA", h.getCodigo() + " " + h.getDescripcion()
                        + ": comprometido para el " + FECHA.format(h.getFechaComprometida()) + " y sigue pendiente.", "HITO", h.getId()));
                continue;
            }
            LocalDate prevista = d.actividadesPorHito().getOrDefault(h.getId(), List.of()).stream()
                    .map(id -> d.arbol().get(id)).filter(Objects::nonNull).map(Actividad::getFechaTerminoPlan)
                    .filter(Objects::nonNull).max(LocalDate::compareTo).orElse(null);
            if (prevista != null && prevista.isAfter(h.getFechaComprometida())) {
                salida.add(new Condicion(TipoAlerta.HITO_ATRASADO, "MEDIA", h.getCodigo() + " " + h.getDescripcion()
                        + ": sus actividades terminan el " + FECHA.format(prevista) + ", después de la fecha comprometida ("
                        + FECHA.format(h.getFechaComprometida()) + ").", "HITO", h.getId()));
            }
        }

        // Cambios atrasados, etapas vencidas, riesgos críticos y sobrecostos
        Map<Integer, String> nombresEtapa = etapasCambio.findAllByOrderByNumeroAsc().stream()
                .collect(Collectors.toMap(EtapaCambio::getNumero, EtapaCambio::getNombre));
        Map<Long, List<SolicitudEtapa>> etapasPorSolicitud = d.etapas().stream()
                .collect(Collectors.groupingBy(SolicitudEtapa::getSolicitudId));
        for (SolicitudCambio s : d.solicitudes()) {
            String nombre = s.getCodigo() + (s.getTitulo() == null ? "" : " " + s.getTitulo());
            if (s.getEstado().enCurso()) {
                if (s.getFechaComprometida().isBefore(hoy)) {
                    salida.add(new Condicion(TipoAlerta.CAMBIO_ATRASADO, "ALTA", nombre + ": comprometida para el "
                            + FECHA.format(s.getFechaComprometida()) + " y sigue " + s.getEstado().texto() + ".",
                            "SOLICITUD", s.getId()));
                }
                for (SolicitudEtapa e : etapasPorSolicitud.getOrDefault(s.getId(), List.of())) {
                    if (e.getEstado() == EstadoEtapa.EN_CURSO && e.vencida(hoy)) {
                        salida.add(new Condicion(TipoAlerta.CAMBIO_ATRASADO, "MEDIA", nombre + ": la etapa «"
                                + nombresEtapa.get(e.getEtapaNumero()) + "» venció el " + FECHA.format(e.getFechaLimite()) + ".",
                                "SOLICITUD_ETAPA", e.getId()));
                    }
                }
                if (s.riesgoCritico()) {
                    salida.add(new Condicion(TipoAlerta.RIESGO_CRITICO, "ALTA", nombre + ": probabilidad alta e impacto alto. "
                            + (s.getRiesgo() == null ? "" : s.getRiesgo()), "SOLICITUD", s.getId()));
                }
            }
            if (s.getCostoReal() != null && s.getCostoReal().compareTo(s.getCostoEstimado()) > 0) {
                salida.add(new Condicion(TipoAlerta.DESVIACION_COSTO, "MEDIA", nombre + ": costo real $" + pesos.format(s.getCostoReal())
                        + " supera el estimado ($" + pesos.format(s.getCostoEstimado()) + ").", "SOLICITUD", s.getId()));
            }
        }

        // Costo planificado sobre el presupuesto
        BigDecimal presupuesto = d.proyecto().getPresupuesto();
        if (d.costos().plan().compareTo(presupuesto) > 0) {
            salida.add(new Condicion(TipoAlerta.DESVIACION_COSTO, "ALTA", "El costo planificado ($" + pesos.format(d.costos().plan())
                    + ") supera el presupuesto ($" + pesos.format(presupuesto) + ").", "PROYECTO", proyectoId));
        }
        return salida;
    }

    /** Número con coma decimal (formato chileno), sin ceros sobrantes. */
    static String num(BigDecimal n) {
        String t = n.stripTrailingZeros().scale() <= 0 ? n.stripTrailingZeros().toPlainString()
                : n.setScale(n.abs().compareTo(BigDecimal.TEN) >= 0 ? 1 : 2, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
        return t.replace('.', ',');
    }

    private static String recortar(String s) {
        return s.length() <= 500 ? s : s.substring(0, 497) + "…";
    }
}
