package cl.duoc.gestionpro.cambio.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import cl.duoc.gestionpro.cambio.EstadoSolicitud;
import cl.duoc.gestionpro.cambio.Impacto;
import cl.duoc.gestionpro.cambio.Prioridad;
import cl.duoc.gestionpro.cambio.Probabilidad;
import cl.duoc.gestionpro.cambio.TipoCambio;

public record SolicitudResumenDto(Long id, String codigo, String titulo, String solicitante, LocalDate fechaSolicitud,
        TipoCambio tipo, Prioridad prioridad, EstadoSolicitud estado, int etapaActual, String etapaNombre,
        Long responsableId, String responsable, LocalDate fechaComprometida, LocalDate fechaReal, boolean atrasada,
        Probabilidad probabilidad, Impacto impacto, boolean riesgoCritico, int impactoPlazoDias, BigDecimal costoEstimado,
        BigDecimal costoReal, int modificaciones, int modificacionesAplicadas) {
}
