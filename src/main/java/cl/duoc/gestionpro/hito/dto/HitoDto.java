package cl.duoc.gestionpro.hito.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import cl.duoc.gestionpro.evidencia.dto.EvidenciaDto;
import cl.duoc.gestionpro.hito.EstadoHito;
import cl.duoc.gestionpro.kpi.Semaforo;

/**
 * Hito con su estado calculado (PENDIENTE, ATRASADO, CUMPLIDO, CANCELADO) y la fecha prevista:
 * el término planificado más tardío de sus actividades. enRiesgo = pendiente y la fecha prevista
 * supera la comprometida. Incluye criterios de aceptación, evidencias y KPI vinculados.
 */
public record HitoDto(
        Long id,
        String codigo,
        String descripcion,
        Long responsableId,
        String responsable,
        LocalDate fechaComprometida,
        LocalDate fechaReal,
        EstadoHito estado,
        String estadoCalculado,
        boolean cumplidoEnPlazo,
        LocalDate fechaPrevista,
        boolean enRiesgo,
        List<Long> actividadIds,
        List<String> actividades,
        List<CriterioDto> criterios,
        int criteriosCumplidos,
        List<EvidenciaDto> evidencias,
        List<KpiVinculado> kpis) {

    public record CriterioDto(Long id, int orden, String descripcion, boolean cumplido) {
    }

    public record KpiVinculado(Long id, String codigo, String nombre, BigDecimal valor, String unidad, Semaforo semaforo) {
    }
}
