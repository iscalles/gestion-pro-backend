package cl.duoc.gestionpro.recurso;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.duoc.gestionpro.planificacion.Actividad;
import cl.duoc.gestionpro.planificacion.ActividadRepository;
import cl.duoc.gestionpro.planificacion.CalendarioFactory;
import cl.duoc.gestionpro.planificacion.CalendarioLaboral;
import cl.duoc.gestionpro.proyecto.Proyecto;
import cl.duoc.gestionpro.proyecto.ProyectoRepository;

/**
 * Carga semanal de cada profesional sumando todos los proyectos. Una asignación aporta a cada semana sus horas
 * semanales prorrateadas por los días hábiles que la actividad ocupa en esa semana:
 * horas = horas semanales × días de la actividad en la semana / días hábiles de la semana.
 * Hay sobreasignación cuando la suma supera las horas semanales disponibles del profesional.
 */
@Service
public class CargaService {

    public record Aporte(Long proyectoId, String proyecto, Long actividadId, String actividad, BigDecimal horas) {}

    public record CargaSemana(LocalDate semana, BigDecimal horas, BigDecimal disponibles, boolean sobreasignada,
            List<Aporte> aportes) {}

    public record CargaProfesional(Long profesionalId, String nombre, String especialidad, BigDecimal horasSemanaDisp,
            boolean activo, List<CargaSemana> semanas, boolean sobreasignado) {}

    private final AsignacionProfesionalRepository asignaciones;
    private final ProfesionalRepository profesionales;
    private final ActividadRepository actividades;
    private final ProyectoRepository proyectos;
    private final CalendarioFactory calendarios;

    public CargaService(AsignacionProfesionalRepository asignaciones, ProfesionalRepository profesionales,
            ActividadRepository actividades, ProyectoRepository proyectos, CalendarioFactory calendarios) {
        this.asignaciones = asignaciones;
        this.profesionales = profesionales;
        this.actividades = actividades;
        this.proyectos = proyectos;
        this.calendarios = calendarios;
    }

    public static LocalDate lunesDe(LocalDate fecha) {
        return fecha.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    /** Carga semanal entre dos fechas de los profesionales indicados (todos si es nulo). */
    @Transactional(readOnly = true)
    public List<CargaProfesional> carga(LocalDate desde, LocalDate hasta, Collection<Long> profesionalIds) {
        LocalDate inicio = lunesDe(desde);
        LocalDate fin = lunesDe(hasta);
        List<AsignacionProfesional> todas = asignaciones.findAll();
        Map<Long, Actividad> acts = new HashMap<>();
        actividades.findAllById(todas.stream().map(AsignacionProfesional::getActividadId).distinct().toList())
                .forEach(a -> acts.put(a.getId(), a));
        Map<Long, Proyecto> pry = new HashMap<>();
        proyectos.findAllById(acts.values().stream().map(Actividad::getProyectoId).distinct().toList())
                .forEach(p -> pry.put(p.getId(), p));
        Map<Long, CalendarioLaboral> cals = new HashMap<>();

        List<Profesional> lista = profesionalIds == null ? profesionales.findAllByOrderByNombreAsc()
                : profesionales.findAllById(profesionalIds).stream().sorted(Comparator.comparing(Profesional::getNombre)).toList();
        Set<Long> ids = lista.stream().map(Profesional::getId).collect(java.util.stream.Collectors.toSet());

        // profesional → semana → aportes
        Map<Long, Map<LocalDate, List<Aporte>>> mapa = new HashMap<>();
        for (AsignacionProfesional asig : todas) {
            if (!ids.contains(asig.getProfesionalId())) {
                continue;
            }
            Actividad a = acts.get(asig.getActividadId());
            if (a == null || a.getFechaInicioPlan() == null || a.getFechaTerminoPlan() == null) {
                continue;
            }
            Proyecto p = pry.get(a.getProyectoId());
            CalendarioLaboral cal = cals.computeIfAbsent(p.getCalendarioId(), calendarios::deCalendario);
            LocalDate desdeAct = lunesDe(a.getFechaInicioPlan()).isBefore(inicio) ? inicio : lunesDe(a.getFechaInicioPlan());
            LocalDate hastaAct = lunesDe(a.getFechaTerminoPlan()).isAfter(fin) ? fin : lunesDe(a.getFechaTerminoPlan());
            for (LocalDate lunes = desdeAct; !lunes.isAfter(hastaAct); lunes = lunes.plusWeeks(1)) {
                BigDecimal horas = horasEnSemana(asig, a, lunes, cal);
                if (horas.signum() > 0) {
                    mapa.computeIfAbsent(asig.getProfesionalId(), k -> new TreeMap<>())
                            .computeIfAbsent(lunes, k -> new ArrayList<>())
                            .add(new Aporte(p.getId(), p.getCodigo(), a.getId(), a.getNombre(), horas));
                }
            }
        }

        List<CargaProfesional> salida = new ArrayList<>();
        for (Profesional prof : lista) {
            Map<LocalDate, List<Aporte>> porSemana = mapa.getOrDefault(prof.getId(), Map.of());
            List<CargaSemana> semanas = new ArrayList<>();
            boolean alguna = false;
            for (LocalDate lunes = inicio; !lunes.isAfter(fin); lunes = lunes.plusWeeks(1)) {
                List<Aporte> aportes = porSemana.getOrDefault(lunes, List.of());
                BigDecimal total = aportes.stream().map(Aporte::horas).reduce(BigDecimal.ZERO, BigDecimal::add)
                        .setScale(2, RoundingMode.HALF_UP);
                boolean sobre = total.compareTo(prof.getHorasSemanaDisp()) > 0;
                alguna |= sobre;
                semanas.add(new CargaSemana(lunes, total, prof.getHorasSemanaDisp(), sobre, aportes));
            }
            salida.add(new CargaProfesional(prof.getId(), prof.getNombre(), prof.getEspecialidad(), prof.getHorasSemanaDisp(),
                    prof.isActivo(), semanas, alguna));
        }
        return salida;
    }

    /** Semanas sobreasignadas de profesionales del proyecto en las que el proyecto aporta horas. */
    @Transactional(readOnly = true)
    public List<Sobreasignacion> sobreasignaciones(Long proyectoId) {
        Proyecto p = proyectos.findById(proyectoId).orElseThrow();
        List<AsignacionProfesional> delProyecto = asignaciones.deProyecto(proyectoId);
        if (delProyecto.isEmpty()) {
            return List.of();
        }
        List<Actividad> acts = actividades.findByProyectoIdOrderByOrdenAscIdAsc(proyectoId);
        LocalDate desde = acts.stream().map(Actividad::getFechaInicioPlan).filter(java.util.Objects::nonNull)
                .min(LocalDate::compareTo).orElse(p.getFechaInicioPlan());
        LocalDate hasta = acts.stream().map(Actividad::getFechaTerminoPlan).filter(java.util.Objects::nonNull)
                .max(LocalDate::compareTo).orElse(p.getFechaTerminoPlan());
        List<Long> ids = delProyecto.stream().map(AsignacionProfesional::getProfesionalId).distinct().toList();
        Predicate<Aporte> deEste = ap -> ap.proyectoId().equals(proyectoId);
        List<Sobreasignacion> salida = new ArrayList<>();
        for (CargaProfesional c : carga(desde, hasta, ids)) {
            for (CargaSemana s : c.semanas()) {
                if (s.sobreasignada() && s.aportes().stream().anyMatch(deEste)) {
                    salida.add(new Sobreasignacion(c.profesionalId(), c.nombre(), s.semana(), s.horas(), s.disponibles(),
                            s.horas().subtract(s.disponibles()), s.aportes()));
                }
            }
        }
        salida.sort(Comparator.comparing(Sobreasignacion::semana).thenComparing(Sobreasignacion::profesional));
        return salida;
    }

    public record Sobreasignacion(Long profesionalId, String profesional, LocalDate semana, BigDecimal horas,
            BigDecimal disponibles, BigDecimal exceso, List<Aporte> aportes) {}

    /**
     * Horas asignadas en la semana de la fecha (todos los proyectos) y horas disponibles de los profesionales
     * asignados al proyecto. Base del KPI de utilización.
     */
    @Transactional(readOnly = true)
    public BigDecimal[] utilizacion(Long proyectoId, LocalDate fecha) {
        List<Long> ids = asignaciones.deProyecto(proyectoId).stream().map(AsignacionProfesional::getProfesionalId).distinct().toList();
        if (ids.isEmpty()) {
            return new BigDecimal[] { BigDecimal.ZERO, BigDecimal.ZERO };
        }
        BigDecimal asignadas = BigDecimal.ZERO;
        BigDecimal disponibles = BigDecimal.ZERO;
        for (CargaProfesional c : carga(fecha, fecha, ids)) {
            asignadas = asignadas.add(c.semanas().get(0).horas());
            disponibles = disponibles.add(c.horasSemanaDisp());
        }
        return new BigDecimal[] { asignadas, disponibles };
    }

    static BigDecimal horasEnSemana(AsignacionProfesional asig, Actividad a, LocalDate lunes, CalendarioLaboral cal) {
        LocalDate domingo = lunes.plusDays(6);
        int habilesSemana = cal.contar(lunes, domingo);
        if (habilesSemana == 0) {
            return BigDecimal.ZERO;
        }
        LocalDate desde = a.getFechaInicioPlan().isAfter(lunes) ? a.getFechaInicioPlan() : lunes;
        LocalDate hasta = a.getFechaTerminoPlan().isBefore(domingo) ? a.getFechaTerminoPlan() : domingo;
        int dias = cal.contar(desde, hasta);
        return asig.getHorasSemana().multiply(BigDecimal.valueOf(dias))
                .divide(BigDecimal.valueOf(habilesSemana), 2, RoundingMode.HALF_UP);
    }
}
