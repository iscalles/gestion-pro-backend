package cl.duoc.gestionpro.cambio.dto;

import jakarta.validation.constraints.Size;

/** Observación al avanzar o aprobar; motivo obligatorio al rechazar (lo valida el servicio). */
public record DecisionRequest(@Size(max = 1000) String observacion) {
}
