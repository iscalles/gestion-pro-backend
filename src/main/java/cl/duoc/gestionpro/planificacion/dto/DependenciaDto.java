package cl.duoc.gestionpro.planificacion.dto;

import cl.duoc.gestionpro.planificacion.Dependencia;
import cl.duoc.gestionpro.planificacion.TipoDependencia;

public record DependenciaDto(
        Long id,
        Long predecesoraId,
        Long sucesoraId,
        TipoDependencia tipo,
        int desfaseDias) {

    public static DependenciaDto de(Dependencia d) {
        return new DependenciaDto(d.getId(), d.getPredecesoraId(), d.getSucesoraId(), d.getTipo(), d.getDesfaseDias());
    }
}
