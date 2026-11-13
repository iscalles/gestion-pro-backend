package cl.duoc.gestionpro.kpi.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Medición manual. Sin fecha se registra ahora. */
public record MedicionRequest(
        @NotNull(message = "Indica el valor.") BigDecimal valor,
        LocalDate fecha,
        @Size(max = 500) String observacion) {
}
