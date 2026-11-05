package cl.duoc.gestionpro.planificacion.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import cl.duoc.gestionpro.planificacion.Actividad;
import cl.duoc.gestionpro.planificacion.TipoActividad;

/**
 * Misma forma que la interfaz `Actividad` del frontend. En los elementos con hijos (esHoja = false)
 * la duración, las fechas y el avance son derivados.
 */
public record ActividadDto(
        Long id,
        Long padreId,
        TipoActividad tipo,
        int nivel,
        int orden,
        String nombre,
        String descripcion,
        Long responsableId,
        String responsable,
        int duracionPlan,
        Integer duracionReal,
        LocalDate fechaInicioPlan,
        LocalDate fechaTerminoPlan,
        LocalDate fechaInicioReal,
        LocalDate fechaTerminoReal,
        BigDecimal avance,
        boolean esHoja,
        boolean atrasada) {

    public static ActividadDto de(Actividad a, int nivel, boolean esHoja, LocalDate hoy) {
        var r = a.getResponsable();
        boolean atrasada = a.getAvance().compareTo(BigDecimal.valueOf(100)) < 0
                && a.getFechaTerminoPlan() != null && a.getFechaTerminoPlan().isBefore(hoy);
        return new ActividadDto(a.getId(), a.getPadreId(), a.getTipo(), nivel, a.getOrden(), a.getNombre(),
                a.getDescripcion(), r == null ? null : r.getId(), r == null ? null : r.getNombre(),
                a.getDuracionPlan(), a.getDuracionReal(), a.getFechaInicioPlan(), a.getFechaTerminoPlan(),
                a.getFechaInicioReal(), a.getFechaTerminoReal(), a.getAvance(), esHoja, atrasada);
    }
}
