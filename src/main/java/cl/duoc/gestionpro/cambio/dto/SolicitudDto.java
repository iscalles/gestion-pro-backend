package cl.duoc.gestionpro.cambio.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import cl.duoc.gestionpro.cambio.EstadoSolicitud;
import cl.duoc.gestionpro.cambio.Impacto;
import cl.duoc.gestionpro.cambio.Prioridad;
import cl.duoc.gestionpro.cambio.Probabilidad;
import cl.duoc.gestionpro.cambio.TipoCambio;
import cl.duoc.gestionpro.evidencia.dto.EvidenciaDto;

/** Solicitud completa: datos, avance por etapa, modificaciones, acciones, evidencias y su efecto en los KPI. */
public record SolicitudDto(
        Long id,
        String codigo,
        String titulo,
        String solicitante,
        LocalDate fechaSolicitud,
        String descripcion,
        String justificacion,
        TipoCambio tipo,
        Prioridad prioridad,
        String riesgo,
        Probabilidad probabilidad,
        Impacto impacto,
        boolean riesgoCritico,
        int impactoPlazoDias,
        BigDecimal impactoCosto,
        String impactoRecursos,
        String impactoAlcance,
        Long responsableId,
        String responsable,
        EstadoSolicitud estado,
        int etapaActual,
        LocalDate fechaComprometida,
        LocalDate fechaReal,
        boolean atrasada,
        BigDecimal costoEstimado,
        BigDecimal costoReal,
        Long aprobadoPorId,
        String aprobadoPor,
        LocalDateTime fechaDecision,
        String resultado,
        BigDecimal efectividad,
        BigDecimal adopcion,
        Integer impactoPlazoReal,
        BigDecimal impactoCostoReal,
        String lineaBase,
        Long version,
        List<EtapaDto> etapas,
        List<ModificacionDto> modificaciones,
        List<AccionDto> acciones,
        List<EvidenciaDto> evidencias,
        List<MedicionGatilladaDto> kpiAfectados) {
}
