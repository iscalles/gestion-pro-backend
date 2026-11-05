package cl.duoc.gestionpro.planificacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CalendarioLaboralTest {

    // 18-09-2026 es viernes y feriado; 19 y 20 son fin de semana.
    private final CalendarioLaboral cal = CalendarioLaboral.lunesAViernes(List.of(LocalDate.of(2026, 9, 18)));

    @Test
    @DisplayName("Alinear salta fin de semana y feriados")
    void alinear() {
        assertThat(cal.alinear(LocalDate.of(2026, 9, 5))).isEqualTo(LocalDate.of(2026, 9, 7)); // sábado → lunes
        assertThat(cal.alinear(LocalDate.of(2026, 9, 18))).isEqualTo(LocalDate.of(2026, 9, 21)); // feriado → lunes
        assertThat(cal.alinear(LocalDate.of(2026, 9, 1))).isEqualTo(LocalDate.of(2026, 9, 1));
    }

    @Test
    @DisplayName("Sumar avanza y retrocede en días hábiles")
    void sumar() {
        assertThat(cal.sumar(LocalDate.of(2026, 9, 17), 1)).isEqualTo(LocalDate.of(2026, 9, 21));
        assertThat(cal.sumar(LocalDate.of(2026, 9, 21), -1)).isEqualTo(LocalDate.of(2026, 9, 17));
        assertThat(cal.sumar(LocalDate.of(2026, 9, 1), 5)).isEqualTo(LocalDate.of(2026, 9, 8));
        assertThat(cal.sumar(LocalDate.of(2026, 9, 1), 0)).isEqualTo(LocalDate.of(2026, 9, 1));
    }

    @Test
    @DisplayName("Contar incluye ambos extremos y omite días no laborables")
    void contar() {
        assertThat(cal.contar(LocalDate.of(2026, 9, 14), LocalDate.of(2026, 9, 21))).isEqualTo(5);
        assertThat(cal.contar(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 1))).isEqualTo(1);
        assertThat(cal.contar(LocalDate.of(2026, 9, 19), LocalDate.of(2026, 9, 20))).isZero();
    }

    @Test
    @DisplayName("Término de una tarea: duración 1 y 0 terminan el mismo día")
    void termino() {
        assertThat(cal.termino(LocalDate.of(2026, 9, 1), 5)).isEqualTo(LocalDate.of(2026, 9, 7));
        assertThat(cal.termino(LocalDate.of(2026, 9, 1), 1)).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(cal.termino(LocalDate.of(2026, 9, 1), 0)).isEqualTo(LocalDate.of(2026, 9, 1));
    }
}
