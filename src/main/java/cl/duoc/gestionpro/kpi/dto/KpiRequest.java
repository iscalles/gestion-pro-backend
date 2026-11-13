package cl.duoc.gestionpro.kpi.dto;

import java.math.BigDecimal;
import java.util.List;

import cl.duoc.gestionpro.kpi.AmbitoKpi;
import cl.duoc.gestionpro.kpi.CategoriaKpi;
import cl.duoc.gestionpro.kpi.FuenteKpi;
import cl.duoc.gestionpro.kpi.Periodicidad;
import cl.duoc.gestionpro.kpi.SentidoKpi;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record KpiRequest(
        @NotBlank(message = "Escribe el código.") @Size(max = 10, message = "Máximo 10 caracteres.") String codigo,
        @NotBlank(message = "Escribe el nombre.") @Size(max = 120) String nombre,
        @NotBlank(message = "Escribe el objetivo.") @Size(max = 500) String objetivo,
        @NotNull(message = "Elige la categoría.") CategoriaKpi categoria,
        @NotNull(message = "Elige el ámbito.") AmbitoKpi ambito,
        Long ambitoRefId,
        @Size(max = 1000) String formula,
        @NotBlank(message = "Indica la unidad.") @Size(max = 20) String unidad,
        @NotNull(message = "Elige el sentido.") SentidoKpi sentido,
        @NotNull(message = "Indica la meta.") BigDecimal meta,
        BigDecimal verdeMin,
        BigDecimal verdeMax,
        BigDecimal ambarMin,
        BigDecimal ambarMax,
        @NotNull(message = "Elige la periodicidad.") Periodicidad periodicidad,
        @NotNull(message = "Elige la fuente.") FuenteKpi fuente,
        @NotNull(message = "Elige el responsable.") Long responsableId,
        Boolean activo,
        List<Long> hitoIds) {
}
