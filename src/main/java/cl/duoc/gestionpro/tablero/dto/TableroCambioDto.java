package cl.duoc.gestionpro.tablero.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import cl.duoc.gestionpro.alerta.AlertaService;
import cl.duoc.gestionpro.cambio.dto.AccionDto;
import cl.duoc.gestionpro.cambio.dto.SolicitudResumenDto;
import cl.duoc.gestionpro.kpi.dto.KpiDto;

/** Tablero 2: gestión del cambio. Filtros: rango de fechas de solicitud, tipo y prioridad. */
public record TableroCambioDto(
        Long proyectoId,
        String proyecto,
        Filtros filtros,
        Resumen resumen,
        List<Conteo> porEstado,
        List<Conteo> porTipo,
        List<Conteo> porPrioridad,
        List<Conteo> porEtapa,
        List<CeldaRiesgo> matrizRiesgo,
        List<Tiempos> tiempos,
        List<Costos> costos,
        List<CumplimientoEtapa> cumplimientoEtapas,
        List<SolicitudResumenDto> solicitudes,
        List<AccionDto> acciones,
        List<KpiDto> kpis,
        List<AlertaService.AlertaDto> alertas) {

    public record Filtros(LocalDate desde, LocalDate hasta, String tipo, String prioridad) {
    }

    public record Resumen(int total, int enCurso, int aprobadas, int rechazadas, int cerradas, int atrasadas,
            int riesgosCriticos, BigDecimal tasaAprobacion, BigDecimal diasEvaluacionPromedio,
            BigDecimal diasImplementacionPromedio, BigDecimal costoEstimado, BigDecimal costoReal, int diasDesviacion,
            BigDecimal impactoCostoPlan, int accionesAbiertas, int accionesCerradas, int accionesVencidas,
            BigDecimal efectividadPromedio, BigDecimal adopcionPromedio) {
    }

    public record Conteo(String clave, String nombre, int cantidad) {
    }

    public record CeldaRiesgo(String probabilidad, String impacto, int cantidad, List<String> solicitudes) {
    }

    public record Tiempos(String codigo, Integer diasEvaluacion, Integer diasImplementacion) {
    }

    public record Costos(String codigo, BigDecimal estimado, BigDecimal real) {
    }

    public record CumplimientoEtapa(int numero, String nombre, int aTiempo, int tarde, int vencidas, int enCurso) {
    }
}
