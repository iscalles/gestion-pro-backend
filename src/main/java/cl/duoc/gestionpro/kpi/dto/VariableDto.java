package cl.duoc.gestionpro.kpi.dto;

import cl.duoc.gestionpro.kpi.CategoriaKpi;
import cl.duoc.gestionpro.kpi.VariableKpi;

public record VariableDto(String codigo, String descripcion, String unidad, CategoriaKpi categoria) {

    public static VariableDto de(VariableKpi v) {
        return new VariableDto(v.getCodigo(), v.getDescripcion(), v.getUnidad(), v.getCategoria());
    }
}
