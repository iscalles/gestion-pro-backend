package cl.duoc.gestionpro.cambio.dto;

import java.time.LocalDate;

public record AccionDto(Long id, Long solicitudId, String solicitud, String descripcion, Long responsableId,
        String responsable, LocalDate fechaCompromiso, LocalDate fechaCierre, String estado, boolean vencida) {
}
