package cl.duoc.gestionpro.tablero;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import cl.duoc.gestionpro.planificacion.Actividad;

/** Avance real acumulado de una actividad en un corte semanal de la curva S. */
class CurvaRealTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 12);

    private static Actividad act(LocalDate inicioReal, LocalDate terminoReal, String avance) {
        var a = new Actividad();
        a.setFechaInicioReal(inicioReal);
        a.setFechaTerminoReal(terminoReal);
        a.setAvance(avance == null ? null : new BigDecimal(avance));
        return a;
    }

    @Test
    @DisplayName("Sin inicio real no aporta avance")
    void sinInicio() {
        assertThat(TableroService.fraccionReal(act(null, null, "0"), HOY, HOY)).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("Antes del inicio real el aporte es cero")
    void antesDelInicio() {
        var a = act(LocalDate.of(2026, 11, 2), null, "50");
        assertThat(TableroService.fraccionReal(a, LocalDate.of(2026, 10, 30), HOY)).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("Terminada: desde su término real aporta el 100 %")
    void terminada() {
        var a = act(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 14), "100");
        assertThat(TableroService.fraccionReal(a, LocalDate.of(2026, 9, 14), HOY)).isEqualByComparingTo("1");
        assertThat(TableroService.fraccionReal(a, LocalDate.of(2026, 10, 5), HOY)).isEqualByComparingTo("1");
    }

    @Test
    @DisplayName("En curso: el avance de hoy se reparte en forma lineal desde el inicio real")
    void enCurso() {
        // Inicio 03-11, hoy 12-11: 10 días corridos con 40 % de avance.
        var a = act(LocalDate.of(2026, 11, 3), null, "40");
        assertThat(TableroService.fraccionReal(a, HOY, HOY)).isEqualByComparingTo("0.4");
        // Al 07-11 (5 de 10 días) se estima la mitad: 20 %.
        assertThat(TableroService.fraccionReal(a, LocalDate.of(2026, 11, 7), HOY)).isEqualByComparingTo("0.2");
    }
}
