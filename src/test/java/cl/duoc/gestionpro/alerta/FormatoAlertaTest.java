package cl.duoc.gestionpro.alerta;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import cl.duoc.gestionpro.cambio.EstadoSolicitud;

/** Los textos de las alertas usan coma decimal y estados en español con tilde. */
class FormatoAlertaTest {

    @Test
    @DisplayName("Números con coma decimal y sin ceros sobrantes")
    void numeros() {
        assertThat(AlertaService.num(new BigDecimal("33.3333"))).isEqualTo("33,3");
        assertThat(AlertaService.num(new BigDecimal("6.4600"))).isEqualTo("6,46");
        assertThat(AlertaService.num(new BigDecimal("5.00"))).isEqualTo("5");
        assertThat(AlertaService.num(new BigDecimal("120"))).isEqualTo("120");
    }

    @Test
    @DisplayName("Estados de la solicitud en texto legible")
    void estados() {
        assertThat(EstadoSolicitud.EN_IMPLEMENTACION.texto()).isEqualTo("en implementación");
        assertThat(EstadoSolicitud.EN_EVALUACION.texto()).isEqualTo("en evaluación");
    }
}
