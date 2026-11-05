package cl.duoc.gestionpro.comun.web;

import java.time.OffsetDateTime;
import java.util.Map;

/** Cuerpo de error uniforme para el frontend. */
public record ApiError(
        int estado,
        String codigo,
        String mensaje,
        Map<String, String> campos,
        OffsetDateTime fecha) {

    public static ApiError de(int estado, String codigo, String mensaje) {
        return new ApiError(estado, codigo, mensaje, Map.of(), OffsetDateTime.now());
    }
}
