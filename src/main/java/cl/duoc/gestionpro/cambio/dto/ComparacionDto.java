package cl.duoc.gestionpro.cambio.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Plan vigente contra una línea base, actividad por actividad. variacion = días hábiles de diferencia en el término. */
public record ComparacionDto(LineaBaseDto lineaBase, LocalDate terminoBase, LocalDate terminoActual, int variacionTermino,
        BigDecimal costoBase, BigDecimal costoActual, List<Fila> actividades) {

    public record Fila(Long actividadId, String codigoEdt, String nombre, int nivel, boolean esHoja, LocalDate inicioBase,
            LocalDate terminoBase, Integer duracionBase, BigDecimal costoBase, LocalDate inicioActual, LocalDate terminoActual,
            int duracionActual, BigDecimal costoActual, Integer variacionTermino, boolean nueva, boolean cambio) {
    }
}
