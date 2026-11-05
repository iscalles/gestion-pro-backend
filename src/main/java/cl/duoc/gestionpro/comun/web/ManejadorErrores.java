package cl.duoc.gestionpro.comun.web;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import cl.duoc.gestionpro.comun.ReglaNegocioException;
import cl.duoc.gestionpro.comun.RecursoNoEncontradoException;

/**
 * Traduce las excepciones a respuestas JSON legibles.
 * Las violaciones de restricciones de Oracle llegan con el nombre de la constraint
 * (por ejemplo CK_ACT_PADRE) para que el frontend muestre un mensaje preciso.
 */
@RestControllerAdvice
public class ManejadorErrores {

    private static final Pattern CONSTRAINT = Pattern.compile("\\(([A-Z0-9_]+)\\.([A-Z0-9_]+)\\)");
    private static final Pattern ERROR_APP = Pattern.compile("ORA-20\\d{3}: ([^\\n]+)");

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validacion(MethodArgumentNotValidException ex) {
        Map<String, String> campos = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(e -> campos.putIfAbsent(e.getField(), e.getDefaultMessage()));
        var error = new ApiError(400, "VALIDACION", "Revisa los campos marcados.", campos, OffsetDateTime.now());
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(ReglaNegocioException.class)
    ResponseEntity<ApiError> reglaNegocio(ReglaNegocioException ex) {
        return ResponseEntity.unprocessableEntity().body(ApiError.de(422, ex.getCodigo(), ex.getMessage()));
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    ResponseEntity<ApiError> noEncontrado(RecursoNoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiError.de(404, "NO_ENCONTRADO", ex.getMessage()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> integridad(DataIntegrityViolationException ex) {
        String detalle = ex.getMostSpecificCause().getMessage();
        Matcher app = ERROR_APP.matcher(detalle);
        if (app.find()) {
            return ResponseEntity.unprocessableEntity().body(ApiError.de(422, "REGLA_BD", app.group(1).trim()));
        }
        Matcher m = CONSTRAINT.matcher(detalle);
        String constraint = m.find() ? m.group(2) : "DESCONOCIDA";
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.de(409, constraint, "Los datos no cumplen una regla de integridad (" + constraint + ")."));
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiError> prohibido(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiError.de(403, "SIN_PERMISO", "Tu rol no permite esta acción."));
    }
}
