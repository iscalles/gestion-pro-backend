package cl.duoc.gestionpro.evidencia.dto;

import java.time.LocalDateTime;

public record EvidenciaDto(Long id, String nombreArchivo, String tipoMime, long tamanoBytes, Long subidoPorId,
        String subidoPor, LocalDateTime subidoEn) {
}
