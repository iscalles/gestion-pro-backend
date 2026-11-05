package cl.duoc.gestionpro.planificacion.calendario;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.duoc.gestionpro.comun.RecursoNoEncontradoException;
import cl.duoc.gestionpro.comun.ReglaNegocioException;
import cl.duoc.gestionpro.planificacion.calendario.dto.CalendarioDto;
import cl.duoc.gestionpro.planificacion.calendario.dto.CalendarioRequest;
import cl.duoc.gestionpro.planificacion.calendario.dto.DiaNoLaborableRequest;
import cl.duoc.gestionpro.recalculo.RecalculoService;

/**
 * Calendarios laborales. Cualquier cambio (días hábiles o no laborables) reprograma
 * todos los proyectos que usan el calendario, en la misma transacción.
 */
@Service
public class CalendarioService {

    private final CalendarioRepository calendarios;
    private final DiaNoLaborableRepository dias;
    private final RecalculoService recalculo;

    public CalendarioService(CalendarioRepository calendarios, DiaNoLaborableRepository dias, RecalculoService recalculo) {
        this.calendarios = calendarios;
        this.dias = dias;
        this.recalculo = recalculo;
    }

    @Transactional(readOnly = true)
    public List<CalendarioDto> listar() {
        return calendarios.findAllByOrderByNombreAsc().stream().map(this::dto).toList();
    }

    @Transactional(readOnly = true)
    public CalendarioDto obtener(Long id) {
        return dto(buscar(id));
    }

    @Transactional
    public CalendarioDto actualizar(Long id, CalendarioRequest req) {
        var c = buscar(id);
        c.setNombre(req.nombre().trim());
        c.setDiasLaborables(req.diasLaborables());
        c.setHorasDia(req.horasDia());
        calendarios.flush();
        recalculo.recalcularCalendario(id);
        return dto(c);
    }

    @Transactional
    public CalendarioDto agregarDia(Long calendarioId, DiaNoLaborableRequest req) {
        var c = buscar(calendarioId);
        if (dias.existsByCalendarioIdAndFecha(calendarioId, req.fecha())) {
            throw new ReglaNegocioException("FECHA_DUPLICADA", "Esa fecha ya está marcada como no laborable.");
        }
        var d = new DiaNoLaborable();
        d.setCalendarioId(calendarioId);
        d.setFecha(req.fecha());
        d.setMotivo(req.motivo().trim());
        dias.saveAndFlush(d);
        recalculo.recalcularCalendario(calendarioId);
        return dto(c);
    }

    @Transactional
    public CalendarioDto quitarDia(Long calendarioId, Long diaId) {
        var c = buscar(calendarioId);
        var d = dias.findById(diaId)
                .filter(x -> x.getCalendarioId().equals(calendarioId))
                .orElseThrow(() -> new RecursoNoEncontradoException("Día no laborable", diaId));
        dias.delete(d);
        dias.flush();
        recalculo.recalcularCalendario(calendarioId);
        return dto(c);
    }

    private CalendarioDto dto(Calendario c) {
        return CalendarioDto.de(c, dias.findByCalendarioIdOrderByFechaAsc(c.getId()));
    }

    private Calendario buscar(Long id) {
        return calendarios.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Calendario", id));
    }
}
