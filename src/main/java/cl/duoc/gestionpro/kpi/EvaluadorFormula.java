package cl.duoc.gestionpro.kpi;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cl.duoc.gestionpro.comun.ReglaNegocioException;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import net.objecthunter.exp4j.function.Function;
import net.objecthunter.exp4j.ValidationResult;

/**
 * Fórmulas de KPI: aritmética (+ − × ÷, paréntesis, potencias y funciones como abs, sqrt, min o max) sobre las variables
 * del catálogo. Una división por cero o una variable sin datos deja el KPI "sin datos" en vez de inventar un valor.
 */
public final class EvaluadorFormula {

    private static final Pattern IDENTIFICADOR = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*(\\s*\\()?");

    /** exp4j no trae min ni max; son útiles para acotar (por ejemplo, min(100, avance / plan * 100)). */
    private static final Function MIN = new Function("min", 2) {
        @Override
        public double apply(double... a) {
            return Math.min(a[0], a[1]);
        }
    };
    private static final Function MAX = new Function("max", 2) {
        @Override
        public double apply(double... a) {
            return Math.max(a[0], a[1]);
        }
    };

    private EvaluadorFormula() {
    }

    private static ExpressionBuilder builder(String formula) {
        return new ExpressionBuilder(formula).functions(MIN, MAX);
    }

    /** Variables que usa la fórmula. Falla con FORMULA_INVALIDA si hay nombres desconocidos o errores de sintaxis. */
    public static Set<String> variables(String formula, Collection<String> catalogo) {
        if (formula == null || formula.isBlank()) {
            throw new ReglaNegocioException("FORMULA_INVALIDA", "Escribe la fórmula.");
        }
        Set<String> usadas = new LinkedHashSet<>();
        Matcher m = IDENTIFICADOR.matcher(formula);
        while (m.find()) {
            if (m.group(1) != null) {
                continue; // es una función: abs(…), sqrt(…)
            }
            String nombre = m.group().trim();
            if (!catalogo.contains(nombre)) {
                throw new ReglaNegocioException("FORMULA_INVALIDA", "«" + nombre + "» no es una variable del catálogo.");
            }
            usadas.add(nombre);
        }
        try {
            Expression e = builder(formula).variables(usadas).build();
            ValidationResult v = e.validate(false);
            if (!v.isValid()) {
                throw new ReglaNegocioException("FORMULA_INVALIDA", "La fórmula no es válida: " + String.join("; ", v.getErrors()));
            }
        } catch (IllegalArgumentException ex) {
            throw new ReglaNegocioException("FORMULA_INVALIDA", "La fórmula no es válida: " + ex.getMessage());
        }
        return usadas;
    }

    /** Valor de la fórmula con 4 decimales; vacío si falta una variable o el resultado no es un número. */
    public static Optional<BigDecimal> evaluar(String formula, Set<String> usadas, Map<String, BigDecimal> valores) {
        for (String v : usadas) {
            if (valores.get(v) == null) {
                return Optional.empty();
            }
        }
        try {
            Expression e = builder(formula).variables(usadas).build();
            for (String v : usadas) {
                e.setVariable(v, valores.get(v).doubleValue());
            }
            double r = e.evaluate();
            if (Double.isNaN(r) || Double.isInfinite(r)) {
                return Optional.empty();
            }
            return Optional.of(BigDecimal.valueOf(r).setScale(4, RoundingMode.HALF_UP));
        } catch (ArithmeticException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
