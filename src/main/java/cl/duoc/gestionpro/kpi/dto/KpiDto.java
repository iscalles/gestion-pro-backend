package cl.duoc.gestionpro.kpi.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import cl.duoc.gestionpro.kpi.AmbitoKpi;
import cl.duoc.gestionpro.kpi.CategoriaKpi;
import cl.duoc.gestionpro.kpi.FuenteKpi;
import cl.duoc.gestionpro.kpi.Periodicidad;
import cl.duoc.gestionpro.kpi.Semaforo;
import cl.duoc.gestionpro.kpi.SentidoKpi;

/**
 * KPI con su último valor. valor y semaforo son nulos si aún no hay mediciones; sinDatos indica que la fórmula
 * no se pudo evaluar hoy (por ejemplo, división por cero). valorAnterior permite mostrar la tendencia.
 */
public record KpiDto(
        Long id,
        String codigo,
        String nombre,
        String objetivo,
        CategoriaKpi categoria,
        AmbitoKpi ambito,
        Long ambitoRefId,
        String ambitoNombre,
        String formula,
        Set<String> variables,
        String unidad,
        SentidoKpi sentido,
        BigDecimal meta,
        BigDecimal verdeMin,
        BigDecimal verdeMax,
        BigDecimal ambarMin,
        BigDecimal ambarMax,
        Periodicidad periodicidad,
        FuenteKpi fuente,
        Long responsableId,
        String responsable,
        boolean activo,
        BigDecimal valor,
        Semaforo semaforo,
        LocalDateTime medidoEn,
        BigDecimal valorAnterior,
        boolean sinDatos,
        List<Long> hitoIds,
        List<MedicionDto> historial) {
}
