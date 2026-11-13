package cl.duoc.gestionpro.cambio.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import cl.duoc.gestionpro.recalculo.CambioFecha;

/** Resultado de aprobar: la solicitud, la línea base guardada y el efecto en fechas, costo y KPI. */
public record AprobacionDto(SolicitudDto solicitud, String lineaBase, LocalDate terminoAntes, LocalDate terminoDespues,
        int diasHabilesDesplazados, BigDecimal costoAntes, BigDecimal costoDespues, List<CambioFecha> recalculadas,
        List<MedicionGatilladaDto> kpiAfectados) {
}
