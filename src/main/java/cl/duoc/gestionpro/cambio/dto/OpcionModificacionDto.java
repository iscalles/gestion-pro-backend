package cl.duoc.gestionpro.cambio.dto;

import java.util.List;

import cl.duoc.gestionpro.cambio.EntidadModificada;

/** Elemento del proyecto que una solicitud puede modificar, con sus campos editables y valor vigente. */
public record OpcionModificacionDto(EntidadModificada entidad, Long entidadId, String nombre, List<Campo> campos) {

    /** tipo: ENTERO, DECIMAL, FECHA o LISTA (opciones en "opciones"). */
    public record Campo(String campo, String etiqueta, String tipo, String valorActual, List<String> opciones) {
    }
}
