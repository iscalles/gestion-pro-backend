package cl.duoc.gestionpro.kpi;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SemaforoTest {

    private static BigDecimal n(String v) {
        return v == null ? null : new BigDecimal(v);
    }

    @Test
    @DisplayName("Mayor es mejor: verde ≥ 90, ámbar 75–90, rojo bajo 75")
    void mayorEsMejor() {
        assertThat(Semaforo.de(n("95"), n("90"), null, n("75"), n("90"))).isEqualTo(Semaforo.VERDE);
        assertThat(Semaforo.de(n("90"), n("90"), null, n("75"), n("90"))).isEqualTo(Semaforo.VERDE);
        assertThat(Semaforo.de(n("80"), n("90"), null, n("75"), n("90"))).isEqualTo(Semaforo.AMBAR);
        assertThat(Semaforo.de(n("74.9"), n("90"), null, n("75"), n("90"))).isEqualTo(Semaforo.ROJO);
    }

    @Test
    @DisplayName("Menor es mejor: verde ≤ 5, ámbar 5–15, rojo sobre 15")
    void menorEsMejor() {
        assertThat(Semaforo.de(n("0"), null, n("5"), n("5"), n("15"))).isEqualTo(Semaforo.VERDE);
        assertThat(Semaforo.de(n("10"), null, n("5"), n("5"), n("15"))).isEqualTo(Semaforo.AMBAR);
        assertThat(Semaforo.de(n("28.57"), null, n("5"), n("5"), n("15"))).isEqualTo(Semaforo.ROJO);
    }

    @Test
    @DisplayName("Sin franja ámbar, lo que no es verde es rojo")
    void sinAmbar() {
        assertThat(Semaforo.de(n("0.95"), n("0.95"), n("1.05"), null, null)).isEqualTo(Semaforo.VERDE);
        assertThat(Semaforo.de(n("0.9"), n("0.95"), n("1.05"), null, null)).isEqualTo(Semaforo.ROJO);
    }
}
