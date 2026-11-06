package cl.duoc.gestionpro.hito;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.duoc.gestionpro.hito.dto.HitoDto;
import cl.duoc.gestionpro.planificacion.Actividad;
import cl.duoc.gestionpro.planificacion.ActividadRepository;

/** Lectura de hitos para la Carta Gantt y los tableros. El CRUD llega con el Módulo 6. */
@Service
public class HitoService {

    private static final ZoneId ZONA = ZoneId.of("America/Santiago");

    private final HitoRepository hitos;
    private final ActividadRepository actividades;

    public HitoService(HitoRepository hitos, ActividadRepository actividades) {
        this.hitos = hitos;
        this.actividades = actividades;
    }

    @Transactional(readOnly = true)
    public List<HitoDto> listar(Long proyectoId) {
        Map<Long, List<Long>> porHito = new HashMap<>();
        for (Object[] fila : hitos.actividadesDeHitos(proyectoId)) {
            porHito.computeIfAbsent(((Number) fila[0]).longValue(), k -> new ArrayList<>()).add(((Number) fila[1]).longValue());
        }
        Map<Long, LocalDate> terminos = new HashMap<>();
        for (Actividad a : actividades.findByProyectoIdOrderByOrdenAscIdAsc(proyectoId)) {
            if (a.getFechaTerminoPlan() != null) {
                terminos.put(a.getId(), a.getFechaTerminoPlan());
            }
        }
        LocalDate hoy = LocalDate.now(ZONA);
        return hitos.findByProyectoIdOrderByFechaComprometidaAscCodigoAsc(proyectoId).stream().map(h -> {
            List<Long> ids = porHito.getOrDefault(h.getId(), List.of());
            LocalDate prevista = ids.stream().map(terminos::get).filter(Objects::nonNull).max(LocalDate::compareTo).orElse(null);
            boolean pendiente = h.getEstado() == EstadoHito.PENDIENTE;
            String calculado = pendiente && h.getFechaComprometida().isBefore(hoy) ? "ATRASADO" : h.getEstado().name();
            boolean enPlazo = h.getEstado() == EstadoHito.CUMPLIDO && !h.getFechaReal().isAfter(h.getFechaComprometida());
            boolean enRiesgo = pendiente && prevista != null && prevista.isAfter(h.getFechaComprometida());
            var r = h.getResponsable();
            return new HitoDto(h.getId(), h.getCodigo(), h.getDescripcion(), r.getId(), r.getNombre(),
                    h.getFechaComprometida(), h.getFechaReal(), h.getEstado(), calculado, enPlazo, prevista, enRiesgo, ids);
        }).toList();
    }
}
