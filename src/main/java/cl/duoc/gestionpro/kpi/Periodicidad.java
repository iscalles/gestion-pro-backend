package cl.duoc.gestionpro.kpi;

import java.time.Duration;

public enum Periodicidad {
    DIARIA(Duration.ofDays(1)),
    SEMANAL(Duration.ofDays(7)),
    QUINCENAL(Duration.ofDays(14)),
    MENSUAL(Duration.ofDays(30)),
    /** Solo se mide cuando algo cambia (recálculo). */
    AL_EVENTO(null);

    private final Duration intervalo;

    Periodicidad(Duration intervalo) {
        this.intervalo = intervalo;
    }

    public Duration intervalo() {
        return intervalo;
    }
}
