package cl.duoc.gestionpro.tablero.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import cl.duoc.gestionpro.alerta.AlertaService;
import cl.duoc.gestionpro.kpi.dto.KpiDto;
import cl.duoc.gestionpro.recurso.CargaService;
import cl.duoc.gestionpro.recurso.dto.CostoTipoDto;

/**
 * Tablero 1: estado del proyecto. Filtros: etapa, rango de fechas y responsable (profesional).
 * El resumen se calcula sobre las actividades que cumplen etapa y responsable; las fechas acotan listas, curva y
 * el historial de los KPI.
 */
public record TableroProyectoDto(
        Proyecto proyecto,
        Filtros filtros,
        Resumen resumen,
        List<Etapa> etapas,
        List<PuntoCurva> curva,
        List<KpiDto> kpis,
        List<Hito> hitos,
        List<Actividad> actividades,
        List<CargaService.CargaProfesional> carga,
        List<CostoTipoDto> costosPorTipo,
        List<AlertaService.AlertaDto> alertas,
        List<Opcion> opcionesEtapa,
        List<Opcion> opcionesResponsable) {

    public record Proyecto(Long id, String codigo, String nombre, String estado, String responsable, LocalDate fechaInicio,
            LocalDate fechaTermino, LocalDate terminoPrevisto, BigDecimal presupuesto) {
    }

    public record Filtros(Long etapaId, LocalDate desde, LocalDate hasta, Long responsableId) {
    }

    public record Resumen(BigDecimal avanceReal, BigDecimal avancePlanificado, BigDecimal costoPlan, BigDecimal costoReal,
            BigDecimal valorGanado, BigDecimal valorPlanificado, BigDecimal spi, BigDecimal cpi, BigDecimal presupuesto,
            int actividades, int terminadas, int enCurso, int atrasadas, int hitos, int hitosCumplidos, int hitosAtrasados,
            int hitosEnRiesgo, int alertasAbiertas, int alertasAltas, int sobreasignaciones, int kpiVerdes, int kpiAmbar,
            int kpiRojos) {
    }

    public record Etapa(Long id, String codigoEdt, String nombre, LocalDate inicio, LocalDate termino, BigDecimal avance,
            BigDecimal avancePlanificado, BigDecimal costoPlan, BigDecimal costoReal, int atrasadas) {
    }

    /** Semana a semana: avance planificado acumulado (%) y costo planificado acumulado. */
    /** avanceReal: solo semanas ya transcurridas, reconstruido desde las fechas reales. */
    public record PuntoCurva(LocalDate semana, BigDecimal avancePlanificado, BigDecimal costoPlanificado, BigDecimal avanceReal) {
    }

    public record Hito(Long id, String codigo, String descripcion, String responsable, LocalDate fechaComprometida,
            LocalDate fechaPrevista, LocalDate fechaReal, String estado, boolean enRiesgo, int criteriosCumplidos,
            int criteriosTotal) {
    }

    /** estado: TERMINADA, ATRASADA, EN_CURSO o PENDIENTE. */
    public record Actividad(Long id, String codigoEdt, String nombre, String etapa, String responsable, LocalDate inicio,
            LocalDate termino, BigDecimal avance, String estado, int diasAtraso, BigDecimal costoPlan, BigDecimal costoReal) {
    }

    public record Opcion(Long id, String nombre) {
    }
}
