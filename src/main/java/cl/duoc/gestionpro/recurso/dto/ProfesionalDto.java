package cl.duoc.gestionpro.recurso.dto;

import java.math.BigDecimal;

import cl.duoc.gestionpro.recurso.Profesional;

/** Compatible con el resumen anterior (id, nombre, especialidad) más los datos del catálogo. */
public record ProfesionalDto(Long id, String nombre, String especialidad, Long tipoProfesionalId, String tipoProfesional,
        BigDecimal valorHora, BigDecimal horasSemanaDisp, Long usuarioId, boolean activo) {

    public static ProfesionalDto de(Profesional p, String tipo) {
        return new ProfesionalDto(p.getId(), p.getNombre(), p.getEspecialidad(), p.getTipoProfesionalId(), tipo,
                p.getValorHora(), p.getHorasSemanaDisp(), p.getUsuarioId(), p.isActivo());
    }
}
