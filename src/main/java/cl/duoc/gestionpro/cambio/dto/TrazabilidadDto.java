package cl.duoc.gestionpro.cambio.dto;

import java.time.LocalDateTime;
import java.util.List;

/** Una solicitud aprobada con lo que cambió en el proyecto y los KPI que se movieron por ella. */
public record TrazabilidadDto(Long solicitudId, String codigo, String titulo, String estado, String aprobadoPor,
        LocalDateTime fechaDecision, String lineaBase, Integer impactoPlazoReal, java.math.BigDecimal impactoCostoReal,
        List<ModificacionDto> modificaciones, List<MedicionGatilladaDto> kpiAfectados) {
}
