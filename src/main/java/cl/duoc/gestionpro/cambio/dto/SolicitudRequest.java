package cl.duoc.gestionpro.cambio.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import cl.duoc.gestionpro.cambio.Impacto;
import cl.duoc.gestionpro.cambio.Prioridad;
import cl.duoc.gestionpro.cambio.Probabilidad;
import cl.duoc.gestionpro.cambio.TipoCambio;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record SolicitudRequest(
        @NotBlank(message = "Escribe un título.") @Size(max = 150) String titulo,
        @NotBlank(message = "Indica quién solicita.") @Size(max = 120) String solicitante,
        LocalDate fechaSolicitud,
        @NotBlank(message = "Describe el cambio.") @Size(max = 2000) String descripcion,
        @NotBlank(message = "Justifica el cambio.") @Size(max = 2000) String justificacion,
        @NotNull(message = "Elige el tipo.") TipoCambio tipo,
        @NotNull(message = "Elige la prioridad.") Prioridad prioridad,
        @Size(max = 1000) String riesgo,
        Probabilidad probabilidad,
        Impacto impacto,
        Integer impactoPlazoDias,
        @PositiveOrZero(message = "El impacto en costo no puede ser negativo.") BigDecimal impactoCosto,
        @Size(max = 300) String impactoRecursos,
        @Size(max = 300) String impactoAlcance,
        @NotNull(message = "Elige el responsable.") Long responsableId,
        @NotNull(message = "Indica la fecha comprometida.") LocalDate fechaComprometida,
        @PositiveOrZero(message = "El costo no puede ser negativo.") BigDecimal costoEstimado) {
}
