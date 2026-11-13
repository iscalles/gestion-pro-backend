package cl.duoc.gestionpro.cambio;

public enum EstadoSolicitud {
    ABIERTO,
    EN_EVALUACION,
    APROBADO,
    RECHAZADO,
    EN_IMPLEMENTACION,
    CERRADO;

    /** Sigue en el flujo: no está cerrada ni rechazada. */
    public boolean enCurso() {
        return this != CERRADO && this != RECHAZADO;
    }

    /** Texto para mensajes ("en evaluación", "en implementación"...). */
    public String texto() {
        return switch (this) {
            case ABIERTO -> "abierta";
            case EN_EVALUACION -> "en evaluación";
            case APROBADO -> "aprobada";
            case RECHAZADO -> "rechazada";
            case EN_IMPLEMENTACION -> "en implementación";
            case CERRADO -> "cerrada";
        };
    }

    /** Fue aprobada (incluye las que ya se implementan o se cerraron). */
    public boolean aprobada() {
        return this == APROBADO || this == EN_IMPLEMENTACION || this == CERRADO;
    }
}
