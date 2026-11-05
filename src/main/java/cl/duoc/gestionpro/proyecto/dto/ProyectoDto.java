package cl.duoc.gestionpro.proyecto.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import cl.duoc.gestionpro.proyecto.EstadoProyecto;
import cl.duoc.gestionpro.proyecto.Proyecto;

/** Misma forma que la interfaz `Proyecto` del frontend. */
public record ProyectoDto(
        Long id,
        String codigo,
        String nombre,
        String descripcion,
        Long responsableId,
        String responsable,
        LocalDate fechaInicio,
        LocalDate fechaTermino,
        BigDecimal presupuesto,
        EstadoProyecto estado,
        BigDecimal avance) {

    public static ProyectoDto de(Proyecto p) {
        return new ProyectoDto(p.getId(), p.getCodigo(), p.getNombre(), p.getDescripcion(),
                p.getResponsable().getId(), p.getResponsable().getNombre(),
                p.getFechaInicioPlan(), p.getFechaTerminoPlan(), p.getPresupuesto(), p.getEstado(), p.getAvance());
    }
}
