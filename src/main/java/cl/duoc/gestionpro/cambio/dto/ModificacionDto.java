package cl.duoc.gestionpro.cambio.dto;

import java.time.LocalDateTime;

import cl.duoc.gestionpro.cambio.EntidadModificada;

/** valorActual es el valor vigente hoy en el proyecto (para comparar antes de aprobar). */
public record ModificacionDto(Long id, EntidadModificada entidad, Long entidadId, String entidadNombre, String campo,
        String campoNombre, String valorAnterior, String valorActual, String valorNuevo, boolean aplicada,
        LocalDateTime fechaAplicacion) {
}
