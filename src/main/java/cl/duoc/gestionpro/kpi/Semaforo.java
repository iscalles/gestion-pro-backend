package cl.duoc.gestionpro.kpi;

import java.math.BigDecimal;

/**
 * Verde si el valor está dentro de [verdeMin, verdeMax]; si no, ámbar si está dentro de [ambarMin, ambarMax];
 * si no, rojo. Un límite nulo significa "sin límite" por ese lado.
 */
public enum Semaforo {
    VERDE,
    AMBAR,
    ROJO;

    public static Semaforo de(BigDecimal valor, BigDecimal verdeMin, BigDecimal verdeMax, BigDecimal ambarMin, BigDecimal ambarMax) {
        if (dentro(valor, verdeMin, verdeMax)) {
            return VERDE;
        }
        if ((ambarMin != null || ambarMax != null) && dentro(valor, ambarMin, ambarMax)) {
            return AMBAR;
        }
        return ROJO;
    }

    private static boolean dentro(BigDecimal v, BigDecimal min, BigDecimal max) {
        return (min == null || v.compareTo(min) >= 0) && (max == null || v.compareTo(max) <= 0);
    }
}
