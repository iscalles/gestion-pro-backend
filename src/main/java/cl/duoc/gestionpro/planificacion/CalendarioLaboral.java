package cl.duoc.gestionpro.planificacion;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

/**
 * Aritmética de días hábiles: días de la semana laborables menos los días no laborables del calendario.
 * Objeto de valor sin dependencias de Spring, para usarlo directo en el motor y en los tests.
 */
public final class CalendarioLaboral {

    /** Tope de seguridad: ningún cálculo recorre más de diez años de calendario. */
    private static final int MAX_DIAS = 3650;

    private final Set<DayOfWeek> laborables;
    private final Set<LocalDate> noLaborables;

    public CalendarioLaboral(Collection<DayOfWeek> laborables, Collection<LocalDate> noLaborables) {
        if (laborables.isEmpty()) {
            throw new IllegalArgumentException("El calendario necesita al menos un día laborable.");
        }
        this.laborables = EnumSet.copyOf(laborables);
        this.noLaborables = new HashSet<>(noLaborables);
    }

    /** Lunes a viernes, sin feriados. */
    public static CalendarioLaboral lunesAViernes(Collection<LocalDate> noLaborables) {
        return new CalendarioLaboral(EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.FRIDAY), noLaborables);
    }

    public boolean esLaborable(LocalDate fecha) {
        return laborables.contains(fecha.getDayOfWeek()) && !noLaborables.contains(fecha);
    }

    /** La misma fecha si es hábil; si no, el siguiente día hábil. */
    public LocalDate alinear(LocalDate fecha) {
        LocalDate f = fecha;
        for (int i = 0; i < MAX_DIAS && !esLaborable(f); i++) {
            f = f.plusDays(1);
        }
        return f;
    }

    /**
     * Avanza (n &gt; 0) o retrocede (n &lt; 0) n días hábiles desde una fecha.
     * Con n = 0 devuelve la fecha alineada al siguiente día hábil.
     */
    public LocalDate sumar(LocalDate fecha, int n) {
        LocalDate f = alinear(fecha);
        int paso = n >= 0 ? 1 : -1;
        int restantes = Math.abs(n);
        for (int i = 0; restantes > 0 && i < MAX_DIAS; i++) {
            f = f.plusDays(paso);
            if (esLaborable(f)) {
                restantes--;
            }
        }
        return f;
    }

    /** Días hábiles entre dos fechas, ambas incluidas. Cero si término es anterior a inicio. */
    public int contar(LocalDate inicio, LocalDate termino) {
        int dias = 0;
        for (LocalDate f = inicio; !f.isAfter(termino); f = f.plusDays(1)) {
            if (esLaborable(f)) {
                dias++;
            }
        }
        return dias;
    }

    /** Término de una tarea que empieza en un día hábil y dura n días hábiles (0 o 1: el mismo día). */
    public LocalDate termino(LocalDate inicio, int duracion) {
        return sumar(inicio, Math.max(duracion, 1) - 1);
    }

    /** Inicio de una tarea que debe terminar en una fecha y dura n días hábiles. */
    public LocalDate inicioParaTerminar(LocalDate termino, int duracion) {
        return sumar(termino, -(Math.max(duracion, 1) - 1));
    }
}
