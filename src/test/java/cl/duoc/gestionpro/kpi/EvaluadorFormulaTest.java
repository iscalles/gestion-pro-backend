package cl.duoc.gestionpro.kpi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import cl.duoc.gestionpro.comun.ReglaNegocioException;

class EvaluadorFormulaTest {

    private static final List<String> CATALOGO = List.of("hitos_cumplidos", "hitos_totales", "costo_real", "costo_plan", "ev", "pv");

    @Test
    @DisplayName("Detecta las variables del catálogo e ignora las funciones")
    void variables() {
        assertThat(EvaluadorFormula.variables("hitos_cumplidos / hitos_totales * 100", CATALOGO))
                .containsExactlyInAnyOrder("hitos_cumplidos", "hitos_totales");
        assertThat(EvaluadorFormula.variables("abs(costo_real - costo_plan) / max(costo_plan, 1) * 100", CATALOGO))
                .containsExactlyInAnyOrder("costo_real", "costo_plan");
    }

    @Test
    @DisplayName("Rechaza variables desconocidas y fórmulas mal escritas")
    void invalidas() {
        assertThatThrownBy(() -> EvaluadorFormula.variables("hitos_cumplidos / total_hitos", CATALOGO))
                .isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> EvaluadorFormula.variables("(ev / pv", CATALOGO))
                .isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    @DisplayName("Evalúa con los valores de las variables")
    void evalua() {
        var r = EvaluadorFormula.evaluar("hitos_cumplidos / hitos_totales * 100", Set.of("hitos_cumplidos", "hitos_totales"),
                Map.of("hitos_cumplidos", BigDecimal.valueOf(2), "hitos_totales", BigDecimal.valueOf(6)));
        assertThat(r).isPresent();
        assertThat(r.get().doubleValue()).isCloseTo(33.3333, org.assertj.core.data.Offset.offset(0.001));
    }

    @Test
    @DisplayName("Sin datos o división por cero no hay medición")
    void sinDatos() {
        assertThat(EvaluadorFormula.evaluar("ev / pv", Set.of("ev", "pv"), Map.of("ev", BigDecimal.TEN, "pv", BigDecimal.ZERO))).isEmpty();
        assertThat(EvaluadorFormula.evaluar("ev / pv", Set.of("ev", "pv"), Map.of("ev", BigDecimal.TEN))).isEmpty();
    }
}
