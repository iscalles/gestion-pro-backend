package cl.duoc.gestionpro.planificacion;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import cl.duoc.gestionpro.comun.ReglaNegocioException;

/**
 * Calcula las fechas planificadas de un proyecto.
 *
 * <ol>
 * <li>Ordena las actividades hoja según sus dependencias (orden topológico de Kahn). Si queda alguna sin
 * ordenar hay un ciclo: se informa el camino y se rechaza con {@code DEPENDENCIA_CIRCULAR} (422).</li>
 * <li>Recorre ese orden y fija el inicio de cada hoja en el primer día hábil que cumple todas sus
 * predecesoras y el inicio del proyecto; el término sale de la duración en días hábiles.</li>
 * <li>Deriva de abajo hacia arriba las etapas y actividades con hijos: inicio mínimo, término máximo,
 * duración en días hábiles, fechas reales y avance ponderado por duración.</li>
 * </ol>
 *
 * Reglas de cada tipo, con días inclusivos (una tarea de 1 día empieza y termina el mismo día) y
 * desfase d en días hábiles, positivo o negativo. Siguen la convención de MS Project: "después de"
 * es el instante en que la otra termina o empieza.
 * <ul>
 * <li>FS: inicio sucesora ≥ término predecesora + 1 + d (con d = 0, el día hábil siguiente).</li>
 * <li>SS: inicio sucesora ≥ inicio predecesora + d.</li>
 * <li>FF: término sucesora ≥ término predecesora + d.</li>
 * <li>SF: término sucesora ≥ inicio predecesora − 1 + d (con d = 0, termina el día hábil anterior al inicio
 * de la predecesora, es decir, justo cuando esta empieza).</li>
 * </ul>
 * Las dependencias unen solo actividades hoja. La clase no toca la base de datos: modifica las entidades
 * recibidas y quien la llama las persiste dentro de su transacción.
 */
@Component
public class MotorProgramacion {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);
    private static final BigDecimal CASI_CIEN = new BigDecimal("99.99");

    /**
     * Programa todas las actividades del proyecto.
     *
     * @param inicioProyecto fecha de inicio planificada del proyecto; ninguna actividad empieza antes
     * @return las hojas en el orden topológico usado
     */
    public List<Actividad> programar(LocalDate inicioProyecto, List<Actividad> actividades,
            List<Dependencia> dependencias, CalendarioLaboral calendario) {
        Map<Long, Actividad> porId = actividades.stream()
                .collect(Collectors.toMap(Actividad::getId, a -> a, (a, b) -> a, LinkedHashMap::new));
        Map<Long, List<Actividad>> hijos = hijosPorPadre(actividades);
        validarExtremos(dependencias, porId, hijos);

        List<Actividad> hojas = actividades.stream().filter(a -> !hijos.containsKey(a.getId())).toList();
        List<Actividad> orden = ordenTopologico(hojas, dependencias, porId);

        Map<Long, List<Dependencia>> entrantes = dependencias.stream()
                .collect(Collectors.groupingBy(Dependencia::getSucesoraId));
        LocalDate base = calendario.alinear(inicioProyecto);

        for (Actividad hoja : orden) {
            int duracion = hoja.getDuracionPlan() == null ? 0 : hoja.getDuracionPlan();
            LocalDate inicio = base;
            for (Dependencia dep : entrantes.getOrDefault(hoja.getId(), List.of())) {
                LocalDate candidato = inicioMinimo(dep, porId.get(dep.getPredecesoraId()), duracion, calendario);
                if (candidato.isAfter(inicio)) {
                    inicio = candidato;
                }
            }
            inicio = calendario.alinear(inicio);
            hoja.setFechaInicioPlan(inicio);
            hoja.setFechaTerminoPlan(calendario.termino(inicio, duracion));
        }

        // Etapas y actividades padre: se derivan desde sus hijos, del nivel más profundo hacia arriba.
        actividades.stream()
                .filter(a -> hijos.containsKey(a.getId()))
                .sorted(Comparator.comparingInt((Actividad a) -> profundidad(a, porId)).reversed())
                .forEach(padre -> derivar(padre, hijos.get(padre.getId()), hojasDe(padre, hijos), calendario));
        return orden;
    }

    /** Fecha más temprana en que puede empezar la sucesora por esta dependencia. */
    static LocalDate inicioMinimo(Dependencia dep, Actividad pred, int duracionSucesora, CalendarioLaboral cal) {
        int d = dep.getDesfaseDias() == null ? 0 : dep.getDesfaseDias();
        return switch (dep.getTipo()) {
            case FS -> cal.sumar(pred.getFechaTerminoPlan(), 1 + d);
            case SS -> cal.sumar(pred.getFechaInicioPlan(), d);
            case FF -> cal.inicioParaTerminar(cal.sumar(pred.getFechaTerminoPlan(), d), duracionSucesora);
            case SF -> cal.inicioParaTerminar(cal.sumar(pred.getFechaInicioPlan(), d - 1), duracionSucesora);
        };
    }

    /**
     * Lanza DEPENDENCIA_CIRCULAR si las dependencias forman un ciclo. No calcula fechas.
     */
    public void validarSinCiclos(List<Actividad> actividades, List<Dependencia> dependencias) {
        Map<Long, Actividad> porId = actividades.stream().collect(Collectors.toMap(Actividad::getId, a -> a));
        Set<Long> nodos = new HashSet<>();
        dependencias.forEach(d -> {
            nodos.add(d.getPredecesoraId());
            nodos.add(d.getSucesoraId());
        });
        ordenTopologico(nodos.stream().map(porId::get).filter(Objects::nonNull).toList(), dependencias, porId);
    }

    /**
     * Camino de dependencias desde {@code desde} hasta {@code hasta} (ambos incluidos), o lista vacía si no
     * existe. Sirve para explicar por qué un vínculo nuevo cerraría un ciclo.
     */
    public static List<Long> camino(Collection<Dependencia> dependencias, Long desde, Long hasta) {
        Map<Long, List<Long>> salientes = new HashMap<>();
        dependencias.forEach(d -> salientes.computeIfAbsent(d.getPredecesoraId(), k -> new ArrayList<>()).add(d.getSucesoraId()));
        Map<Long, Long> previo = new HashMap<>();
        Deque<Long> cola = new ArrayDeque<>(List.of(desde));
        Set<Long> vistos = new HashSet<>(List.of(desde));
        while (!cola.isEmpty()) {
            Long actual = cola.poll();
            if (actual.equals(hasta)) {
                List<Long> ruta = new ArrayList<>();
                for (Long n = hasta; n != null; n = previo.get(n)) {
                    ruta.addFirst(n);
                }
                return ruta;
            }
            for (Long sig : salientes.getOrDefault(actual, List.of())) {
                if (vistos.add(sig)) {
                    previo.put(sig, actual);
                    cola.add(sig);
                }
            }
        }
        return List.of();
    }

    /** Avance ponderado por duración planificada; si todas duran 0, promedio simple. */
    public static BigDecimal avancePonderado(Collection<Actividad> hojas) {
        if (hojas.isEmpty()) {
            return BigDecimal.ZERO;
        }
        boolean todasTerminadas = hojas.stream().allMatch(h -> h.getAvance().compareTo(CIEN) >= 0);
        if (todasTerminadas) {
            return CIEN.setScale(2);
        }
        long pesoTotal = hojas.stream().mapToLong(h -> Math.max(0, h.getDuracionPlan())).sum();
        BigDecimal suma = BigDecimal.ZERO;
        for (Actividad h : hojas) {
            BigDecimal peso = BigDecimal.valueOf(pesoTotal == 0 ? 1 : Math.max(0, h.getDuracionPlan()));
            suma = suma.add(h.getAvance().multiply(peso));
        }
        BigDecimal divisor = BigDecimal.valueOf(pesoTotal == 0 ? hojas.size() : pesoTotal);
        BigDecimal avance = suma.divide(divisor, 2, RoundingMode.HALF_UP);
        // Solo llega a 100 % cuando todas las hojas terminaron.
        return avance.compareTo(CASI_CIEN) > 0 ? CASI_CIEN : avance;
    }

    // ---------------------------------------------------------------------------------------------

    private static Map<Long, List<Actividad>> hijosPorPadre(List<Actividad> actividades) {
        Map<Long, List<Actividad>> hijos = new HashMap<>();
        actividades.stream().filter(a -> a.getPadreId() != null)
                .forEach(a -> hijos.computeIfAbsent(a.getPadreId(), k -> new ArrayList<>()).add(a));
        return hijos;
    }

    private static void validarExtremos(List<Dependencia> deps, Map<Long, Actividad> porId, Map<Long, List<Actividad>> hijos) {
        for (Dependencia d : deps) {
            for (Long id : List.of(d.getPredecesoraId(), d.getSucesoraId())) {
                if (!porId.containsKey(id)) {
                    throw new IllegalStateException("La dependencia " + d.getId() + " apunta a una actividad de otro proyecto.");
                }
                if (hijos.containsKey(id)) {
                    throw new ReglaNegocioException("DEPENDENCIA_SOLO_HOJAS", "«" + porId.get(id).getNombre()
                            + "» tiene subactividades: las dependencias se definen entre elementos sin hijos.");
                }
            }
        }
    }

    /** Kahn sobre las hojas. Desempata por el orden recibido para que el resultado sea estable. */
    private static List<Actividad> ordenTopologico(List<Actividad> nodos, List<Dependencia> deps, Map<Long, Actividad> porId) {
        Map<Long, Integer> gradoEntrada = new LinkedHashMap<>();
        nodos.forEach(n -> gradoEntrada.put(n.getId(), 0));
        Map<Long, List<Long>> salientes = new HashMap<>();
        for (Dependencia d : deps) {
            if (gradoEntrada.containsKey(d.getPredecesoraId()) && gradoEntrada.containsKey(d.getSucesoraId())) {
                salientes.computeIfAbsent(d.getPredecesoraId(), k -> new ArrayList<>()).add(d.getSucesoraId());
                gradoEntrada.merge(d.getSucesoraId(), 1, Integer::sum);
            }
        }
        Deque<Long> listos = new ArrayDeque<>();
        gradoEntrada.forEach((id, g) -> {
            if (g == 0) {
                listos.add(id);
            }
        });
        List<Actividad> orden = new ArrayList<>(nodos.size());
        while (!listos.isEmpty()) {
            Long id = listos.poll();
            orden.add(porId.get(id));
            for (Long suc : salientes.getOrDefault(id, List.of())) {
                if (gradoEntrada.merge(suc, -1, Integer::sum) == 0) {
                    listos.add(suc);
                }
            }
        }
        if (orden.size() < nodos.size()) {
            Set<Long> pendientes = new HashSet<>(gradoEntrada.keySet());
            orden.forEach(a -> pendientes.remove(a.getId()));
            List<Long> ciclo = buscarCiclo(pendientes, salientes);
            String texto = ciclo.stream().map(id -> "«" + porId.get(id).getNombre() + "»").collect(Collectors.joining(" → "));
            throw new ReglaNegocioException("DEPENDENCIA_CIRCULAR",
                    "Dependencia circular: " + texto + ". Quita uno de esos vínculos.");
        }
        return orden;
    }

    /** Recorre los nodos que quedaron sin ordenar hasta repetir uno: ese tramo es el ciclo. */
    private static List<Long> buscarCiclo(Set<Long> pendientes, Map<Long, List<Long>> salientes) {
        Long actual = pendientes.iterator().next();
        List<Long> recorrido = new ArrayList<>();
        Map<Long, Integer> posicion = new HashMap<>();
        while (!posicion.containsKey(actual)) {
            posicion.put(actual, recorrido.size());
            recorrido.add(actual);
            actual = salientes.getOrDefault(actual, List.of()).stream()
                    .filter(pendientes::contains).findFirst().orElseThrow();
        }
        List<Long> ciclo = new ArrayList<>(recorrido.subList(posicion.get(actual), recorrido.size()));
        ciclo.add(actual);
        return ciclo;
    }

    private static int profundidad(Actividad a, Map<Long, Actividad> porId) {
        int p = 0;
        for (Long padre = a.getPadreId(); padre != null && porId.containsKey(padre); padre = porId.get(padre).getPadreId()) {
            p++;
        }
        return p;
    }

    private static List<Actividad> hojasDe(Actividad padre, Map<Long, List<Actividad>> hijos) {
        List<Actividad> hojas = new ArrayList<>();
        Deque<Actividad> pila = new ArrayDeque<>(hijos.get(padre.getId()));
        while (!pila.isEmpty()) {
            Actividad a = pila.pop();
            List<Actividad> h = hijos.get(a.getId());
            if (h == null) {
                hojas.add(a);
            } else {
                pila.addAll(h);
            }
        }
        return hojas;
    }

    private static void derivar(Actividad padre, List<Actividad> hijosDirectos, List<Actividad> hojas, CalendarioLaboral cal) {
        LocalDate inicio = hijosDirectos.stream().map(Actividad::getFechaInicioPlan).min(LocalDate::compareTo).orElseThrow();
        LocalDate termino = hijosDirectos.stream().map(Actividad::getFechaTerminoPlan).max(LocalDate::compareTo).orElseThrow();
        padre.setFechaInicioPlan(inicio);
        padre.setFechaTerminoPlan(termino);
        padre.setDuracionPlan(cal.contar(inicio, termino));

        LocalDate inicioReal = hojas.stream().map(Actividad::getFechaInicioReal).filter(Objects::nonNull)
                .min(LocalDate::compareTo).orElse(null);
        boolean todasCerradas = hojas.stream().allMatch(h -> h.getFechaTerminoReal() != null);
        LocalDate terminoReal = todasCerradas
                ? hojas.stream().map(Actividad::getFechaTerminoReal).max(LocalDate::compareTo).orElseThrow()
                : null;
        padre.setFechaInicioReal(inicioReal);
        padre.setFechaTerminoReal(terminoReal);
        padre.setDuracionReal(inicioReal != null && terminoReal != null ? cal.contar(inicioReal, terminoReal) : null);
        padre.setAvance(avancePonderado(hojas));
    }
}
