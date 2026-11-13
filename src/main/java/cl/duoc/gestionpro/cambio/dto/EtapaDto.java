package cl.duoc.gestionpro.cambio.dto;

import java.time.LocalDate;

import cl.duoc.gestionpro.cambio.EstadoEtapa;

public record EtapaDto(Long id, int numero, String nombre, String descripcion, EstadoEtapa estado, Long responsableId,
        String responsable, LocalDate fechaInicio, LocalDate fechaFin, LocalDate fechaLimite, String observacion,
        boolean vencida, boolean aTiempo) {
}
