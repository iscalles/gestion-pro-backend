package cl.duoc.gestionpro.kpi.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import cl.duoc.gestionpro.kpi.OrigenMedicion;
import cl.duoc.gestionpro.kpi.Semaforo;

public record MedicionDto(Long id, LocalDateTime medidoEn, BigDecimal valor, Semaforo semaforo, OrigenMedicion origen,
        Long solicitudId, String solicitud, String registradoPor, String observacion) {
}
