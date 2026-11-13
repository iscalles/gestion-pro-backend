package cl.duoc.gestionpro.kpi.dto;

import java.math.BigDecimal;
import java.util.List;

/** Resultado de probar una fórmula con los datos de hoy: el valor (nulo si no se puede evaluar) y cada variable. */
public record PruebaFormulaDto(BigDecimal valor, List<VariableValor> variables, String mensaje) {

    public record VariableValor(String codigo, String descripcion, String unidad, BigDecimal valor) {
    }
}
