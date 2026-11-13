package cl.duoc.gestionpro.recurso.dto;

import java.math.BigDecimal;
import java.util.List;

/** Dotación requerida de un tipo de profesional y cuántos profesionales de ese tipo trabajan en el proyecto. */
public record RequerimientoDto(Long id, Long tipoProfesionalId, String categoria, int cantidadRequerida,
        BigDecimal tarifaHora, int asignados, boolean cubierto, List<String> profesionales) {
}
