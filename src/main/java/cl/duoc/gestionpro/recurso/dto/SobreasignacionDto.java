package cl.duoc.gestionpro.recurso.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import cl.duoc.gestionpro.recurso.CargaService;

public record SobreasignacionDto(Long profesionalId, String profesional, LocalDate semana, BigDecimal horas,
        BigDecimal disponibles, BigDecimal exceso, List<CargaService.Aporte> aportes) {

    public static SobreasignacionDto de(CargaService.Sobreasignacion s) {
        return new SobreasignacionDto(s.profesionalId(), s.profesional(), s.semana(), s.horas(), s.disponibles(),
                s.exceso(), s.aportes());
    }
}
