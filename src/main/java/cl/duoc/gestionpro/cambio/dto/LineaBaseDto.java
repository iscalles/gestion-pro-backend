package cl.duoc.gestionpro.cambio.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record LineaBaseDto(Long id, String codigo, Long solicitudId, String solicitud, LocalDateTime creadaEn, String creadaPor,
        LocalDate fechaTermino, BigDecimal costoTotal) {
}
