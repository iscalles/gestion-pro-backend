package cl.duoc.gestionpro.recurso.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Recursos y costos de un proyecto. spi = EV / PV y cpi = EV / costo real (nulos si el divisor es cero).
 * desviacion = costo planificado − presupuesto (positivo: el plan supera lo autorizado).
 */
public record CostosDto(
        Long proyectoId,
        BigDecimal presupuesto,
        BigDecimal costoPlan,
        BigDecimal costoReal,
        BigDecimal valorGanado,
        BigDecimal valorPlanificado,
        BigDecimal spi,
        BigDecimal cpi,
        BigDecimal desviacion,
        List<CostoTipoDto> porTipo,
        List<CostoActividadDto> actividades,
        List<AsignacionProfesionalDto> asignacionesProfesional,
        List<AsignacionRecursoDto> asignacionesRecurso,
        List<RecursoDto> recursos,
        List<RequerimientoDto> requerimientos,
        List<SobreasignacionDto> sobreasignaciones) {
}
