package cl.duoc.gestionpro.planificacion.calendario.dto;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.List;

import cl.duoc.gestionpro.planificacion.calendario.Calendario;
import cl.duoc.gestionpro.planificacion.calendario.DiaNoLaborable;

/** Calendario con sus días no laborables. Los días van como MONDAY…SUNDAY. */
public record CalendarioDto(
        Long id,
        String nombre,
        List<DayOfWeek> diasLaborables,
        BigDecimal horasDia,
        BigDecimal horasSemana,
        List<DiaNoLaborableDto> diasNoLaborables) {

    public static CalendarioDto de(Calendario c, List<DiaNoLaborable> dias) {
        var laborables = c.diasLaborables().stream().sorted().toList();
        return new CalendarioDto(c.getId(), c.getNombre(), laborables, c.getHorasDia(),
                c.getHorasDia().multiply(BigDecimal.valueOf(laborables.size())),
                dias.stream().map(DiaNoLaborableDto::de).toList());
    }
}
