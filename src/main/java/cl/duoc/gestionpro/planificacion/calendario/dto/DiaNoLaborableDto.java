package cl.duoc.gestionpro.planificacion.calendario.dto;

import java.time.LocalDate;

import cl.duoc.gestionpro.planificacion.calendario.DiaNoLaborable;

public record DiaNoLaborableDto(Long id, LocalDate fecha, String motivo) {

    public static DiaNoLaborableDto de(DiaNoLaborable d) {
        return new DiaNoLaborableDto(d.getId(), d.getFecha(), d.getMotivo());
    }
}
