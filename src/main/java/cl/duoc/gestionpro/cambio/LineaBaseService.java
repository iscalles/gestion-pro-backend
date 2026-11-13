package cl.duoc.gestionpro.cambio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.duoc.gestionpro.cambio.dto.ComparacionDto;
import cl.duoc.gestionpro.cambio.dto.LineaBaseDto;
import cl.duoc.gestionpro.comun.RecursoNoEncontradoException;
import cl.duoc.gestionpro.planificacion.Actividad;
import cl.duoc.gestionpro.planificacion.ActividadRepository;
import cl.duoc.gestionpro.planificacion.ArbolActividades;
import cl.duoc.gestionpro.planificacion.CalendarioFactory;
import cl.duoc.gestionpro.planificacion.CalendarioLaboral;
import cl.duoc.gestionpro.recurso.CostoService;
import cl.duoc.gestionpro.seguridad.Usuario;
import cl.duoc.gestionpro.seguridad.UsuarioRepository;

/**
 * Líneas base: foto del plan (fechas, duraciones y costos por actividad). LB-01 es la línea base inicial del
 * proyecto; cada cambio aprobado guarda una nueva con el plan ya modificado y reprogramado, así la comparación
 * contra la anterior muestra el efecto del cambio y la comparación contra la vigente, la desviación de la ejecución.
 */
@Service
public class LineaBaseService {

    private static final ZoneId ZONA = ZoneId.of("America/Santiago");

    private final LineaBaseRepository lineas;
    private final LineaBaseActividadRepository filas;
    private final ActividadRepository actividades;
    private final SolicitudCambioRepository solicitudes;
    private final UsuarioRepository usuarios;
    private final CostoService costos;
    private final CalendarioFactory calendarios;

    public LineaBaseService(LineaBaseRepository lineas, LineaBaseActividadRepository filas, ActividadRepository actividades,
            SolicitudCambioRepository solicitudes, UsuarioRepository usuarios, CostoService costos, CalendarioFactory calendarios) {
        this.lineas = lineas;
        this.filas = filas;
        this.actividades = actividades;
        this.solicitudes = solicitudes;
        this.usuarios = usuarios;
        this.costos = costos;
        this.calendarios = calendarios;
    }

    /** Guarda el plan vigente como nueva línea base. Nula si el proyecto aún no tiene actividades programadas. */
    @Transactional
    public LineaBase guardar(Long proyectoId, Long solicitudId, Long usuarioId) {
        List<Actividad> lista = actividades.findByProyectoIdOrderByOrdenAscIdAsc(proyectoId).stream()
                .filter(a -> a.getFechaInicioPlan() != null && a.getFechaTerminoPlan() != null).toList();
        if (lista.isEmpty()) {
            return null;
        }
        var c = costos.calcular(proyectoId, LocalDate.now(ZONA));
        var lb = new LineaBase();
        lb.setProyectoId(proyectoId);
        lb.setNumero(lineas.findFirstByProyectoIdOrderByNumeroDesc(proyectoId).map(LineaBase::getNumero).orElse(0) + 1);
        lb.setSolicitudId(solicitudId);
        lb.setCreadaEn(LocalDateTime.now(ZONA));
        lb.setCreadaPorId(usuarioId);
        lb.setFechaTermino(lista.stream().map(Actividad::getFechaTerminoPlan).max(LocalDate::compareTo).orElseThrow());
        lb.setCostoTotal(c.plan());
        lineas.saveAndFlush(lb);
        for (Actividad a : lista) {
            var f = new LineaBaseActividad();
            f.setLineaBaseId(lb.getId());
            f.setActividadId(a.getId());
            f.setFechaInicioPlan(a.getFechaInicioPlan());
            f.setFechaTerminoPlan(a.getFechaTerminoPlan());
            f.setDuracionPlan(a.getDuracionPlan());
            f.setCostoPlan(c.de(a.getId()).plan());
            filas.save(f);
        }
        filas.flush();
        return lb;
    }

    @Transactional(readOnly = true)
    public List<LineaBaseDto> listar(Long proyectoId) {
        Map<Long, String> nombres = usuarios.findAll().stream().collect(Collectors.toMap(Usuario::getId, Usuario::getNombre));
        Map<Long, SolicitudCambio> sols = solicitudes.findByProyectoIdOrderByCodigoAsc(proyectoId).stream()
                .collect(Collectors.toMap(SolicitudCambio::getId, s -> s));
        return lineas.findByProyectoIdOrderByNumeroAsc(proyectoId).stream().map(l -> dto(l, nombres, sols)).toList();
    }

    @Transactional(readOnly = true)
    public ComparacionDto comparar(Long proyectoId, Long lineaBaseId) {
        LineaBase lb = lineas.findById(lineaBaseId).filter(l -> l.getProyectoId().equals(proyectoId))
                .orElseThrow(() -> new RecursoNoEncontradoException("Línea base", lineaBaseId));
        Map<Long, LineaBaseActividad> base = filas.findByLineaBaseId(lb.getId()).stream()
                .collect(Collectors.toMap(LineaBaseActividad::getActividadId, f -> f));
        List<Actividad> lista = actividades.findByProyectoIdOrderByOrdenAscIdAsc(proyectoId);
        ArbolActividades arbol = ArbolActividades.de(lista);
        CalendarioLaboral cal = calendarios.deProyecto(proyectoId);
        var c = costos.calcular(proyectoId, LocalDate.now(ZONA));
        List<ComparacionDto.Fila> filasDto = arbol.enOrden().stream().map(a -> {
            LineaBaseActividad b = base.get(a.getId());
            BigDecimal costoActual = c.de(a.getId()).plan();
            Integer variacion = b == null || a.getFechaTerminoPlan() == null ? null
                    : diferencia(cal, b.getFechaTerminoPlan(), a.getFechaTerminoPlan());
            boolean cambio = b != null && (!Objects.equals(b.getFechaInicioPlan(), a.getFechaInicioPlan())
                    || !Objects.equals(b.getFechaTerminoPlan(), a.getFechaTerminoPlan())
                    || !Objects.equals(b.getDuracionPlan(), a.getDuracionPlan()) || b.getCostoPlan().compareTo(costoActual) != 0);
            return new ComparacionDto.Fila(a.getId(), arbol.codigo(a.getId()), a.getNombre(), arbol.nivel(a.getId()),
                    arbol.esHoja(a.getId()), b == null ? null : b.getFechaInicioPlan(), b == null ? null : b.getFechaTerminoPlan(),
                    b == null ? null : b.getDuracionPlan(), b == null ? null : b.getCostoPlan(), a.getFechaInicioPlan(),
                    a.getFechaTerminoPlan(), a.getDuracionPlan(), costoActual, variacion, b == null, cambio);
        }).toList();
        LocalDate terminoActual = lista.stream().map(Actividad::getFechaTerminoPlan).filter(Objects::nonNull)
                .max(LocalDate::compareTo).orElse(null);
        Map<Long, String> nombres = usuarios.findAll().stream().collect(Collectors.toMap(Usuario::getId, Usuario::getNombre));
        Map<Long, SolicitudCambio> sols = solicitudes.findByProyectoIdOrderByCodigoAsc(proyectoId).stream()
                .collect(Collectors.toMap(SolicitudCambio::getId, s -> s));
        return new ComparacionDto(dto(lb, nombres, sols), lb.getFechaTermino(), terminoActual,
                terminoActual == null ? 0 : diferencia(cal, lb.getFechaTermino(), terminoActual), lb.getCostoTotal(), c.plan(), filasDto);
    }

    /** Días hábiles de diferencia entre dos fechas (positivo si "despues" es posterior). */
    public static int diferencia(CalendarioLaboral cal, LocalDate antes, LocalDate despues) {
        if (antes.equals(despues)) {
            return 0;
        }
        return despues.isAfter(antes) ? cal.contar(antes.plusDays(1), despues) : -cal.contar(despues.plusDays(1), antes);
    }

    private static LineaBaseDto dto(LineaBase l, Map<Long, String> nombres, Map<Long, SolicitudCambio> sols) {
        SolicitudCambio s = l.getSolicitudId() == null ? null : sols.get(l.getSolicitudId());
        return new LineaBaseDto(l.getId(), l.codigo(), l.getSolicitudId(), s == null ? null : s.getCodigo(), l.getCreadaEn(),
                nombres.get(l.getCreadaPorId()), l.getFechaTermino(), l.getCostoTotal());
    }
}
