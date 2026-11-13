package cl.duoc.gestionpro.cambio.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Size;

public record EtapaRequest(Long responsableId, LocalDate fechaLimite, @Size(max = 1000) String observacion) {
}
