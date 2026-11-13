package cl.duoc.gestionpro.cambio.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import cl.duoc.gestionpro.kpi.Semaforo;

/** Medición de KPI registrada por el recálculo que siguió a la aprobación del cambio. */
public record MedicionGatilladaDto(Long kpiId, String kpi, String nombre, String unidad, BigDecimal valorAnterior,
        Semaforo semaforoAnterior, BigDecimal valor, Semaforo semaforo, LocalDateTime medidoEn) {
}
