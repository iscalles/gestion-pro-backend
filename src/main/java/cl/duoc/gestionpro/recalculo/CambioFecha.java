package cl.duoc.gestionpro.recalculo;

import java.time.LocalDate;

/** Actividad cuyas fechas planificadas cambiaron en un recálculo. */
public record CambioFecha(
        Long id,
        String nombre,
        LocalDate inicioAntes,
        LocalDate terminoAntes,
        LocalDate inicio,
        LocalDate termino) {
}
