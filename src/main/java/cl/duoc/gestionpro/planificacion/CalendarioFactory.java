package cl.duoc.gestionpro.planificacion;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import cl.duoc.gestionpro.comun.RecursoNoEncontradoException;
import cl.duoc.gestionpro.planificacion.calendario.CalendarioRepository;
import cl.duoc.gestionpro.planificacion.calendario.DiaNoLaborable;
import cl.duoc.gestionpro.planificacion.calendario.DiaNoLaborableRepository;
import cl.duoc.gestionpro.proyecto.ProyectoRepository;

/**
 * Arma el {@link CalendarioLaboral} de un calendario o de un proyecto. Lo usan el motor, los costos
 * (semanas de cada asignación), la carga semanal de profesionales y los KPI.
 */
@Component
public class CalendarioFactory {

    private final CalendarioRepository calendarios;
    private final DiaNoLaborableRepository diasNoLaborables;
    private final ProyectoRepository proyectos;

    public CalendarioFactory(CalendarioRepository calendarios, DiaNoLaborableRepository diasNoLaborables,
            ProyectoRepository proyectos) {
        this.calendarios = calendarios;
        this.diasNoLaborables = diasNoLaborables;
        this.proyectos = proyectos;
    }

    @Transactional(readOnly = true)
    public CalendarioLaboral deCalendario(Long calendarioId) {
        var c = calendarios.findById(calendarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Calendario", calendarioId));
        var feriados = diasNoLaborables.findByCalendarioIdOrderByFechaAsc(calendarioId).stream()
                .map(DiaNoLaborable::getFecha).toList();
        return new CalendarioLaboral(c.diasLaborables(), feriados);
    }

    @Transactional(readOnly = true)
    public CalendarioLaboral deProyecto(Long proyectoId) {
        var p = proyectos.findById(proyectoId).orElseThrow(() -> new RecursoNoEncontradoException("Proyecto", proyectoId));
        return deCalendario(p.getCalendarioId());
    }

    /** Días laborables por semana del calendario (5 en el calendario lunes a viernes). */
    @Transactional(readOnly = true)
    public int diasPorSemana(Long calendarioId) {
        return calendarios.findById(calendarioId).map(c -> c.diasLaborables().size()).orElse(5);
    }
}
