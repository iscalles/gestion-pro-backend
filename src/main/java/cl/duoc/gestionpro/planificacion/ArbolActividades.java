package cl.duoc.gestionpro.planificacion;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Vista de árbol de las actividades de un proyecto: orden de recorrido (etapa, sus actividades,
 * sus subactividades…), nivel, hojas, subárboles y código EDT derivado de la posición (1, 1.2, 1.2.3).
 * El código no se guarda: se recalcula desde el orden, así que nunca queda desfasado.
 */
public final class ArbolActividades {

    private static final Comparator<Actividad> POR_ORDEN =
            Comparator.comparing(Actividad::getOrden).thenComparing(Actividad::getId);

    private final Map<Long, Actividad> porId = new HashMap<>();
    private final Map<Long, List<Actividad>> hijos = new HashMap<>();
    private final List<Actividad> enOrden = new ArrayList<>();
    private final Map<Long, String> codigos = new HashMap<>();
    private final Map<Long, Integer> niveles = new HashMap<>();

    private ArbolActividades(List<Actividad> lista) {
        List<Actividad> raices = new ArrayList<>();
        for (Actividad a : lista) {
            porId.put(a.getId(), a);
        }
        for (Actividad a : lista) {
            if (a.getPadreId() == null || !porId.containsKey(a.getPadreId())) {
                raices.add(a);
            } else {
                hijos.computeIfAbsent(a.getPadreId(), k -> new ArrayList<>()).add(a);
            }
        }
        hijos.values().forEach(l -> l.sort(POR_ORDEN));
        raices.sort(POR_ORDEN);
        for (int i = 0; i < raices.size(); i++) {
            recorrer(raices.get(i), String.valueOf(i + 1), 0);
        }
    }

    public static ArbolActividades de(List<Actividad> lista) {
        return new ArbolActividades(lista);
    }

    private void recorrer(Actividad raiz, String codigoRaiz, int nivelRaiz) {
        Deque<Object[]> pila = new ArrayDeque<>();
        pila.push(new Object[] { raiz, codigoRaiz, nivelRaiz });
        while (!pila.isEmpty()) {
            Object[] item = pila.pop();
            Actividad a = (Actividad) item[0];
            String codigo = (String) item[1];
            int nivel = (int) item[2];
            if (codigos.containsKey(a.getId())) {
                continue;
            }
            enOrden.add(a);
            codigos.put(a.getId(), codigo);
            niveles.put(a.getId(), nivel);
            List<Actividad> h = hijos.getOrDefault(a.getId(), List.of());
            for (int i = h.size() - 1; i >= 0; i--) {
                pila.push(new Object[] { h.get(i), codigo + "." + (i + 1), nivel + 1 });
            }
        }
    }

    public List<Actividad> enOrden() {
        return enOrden;
    }

    public Actividad get(Long id) {
        return porId.get(id);
    }

    public boolean contiene(Long id) {
        return porId.containsKey(id);
    }

    public String codigo(Long id) {
        return codigos.getOrDefault(id, "");
    }

    public int nivel(Long id) {
        return niveles.getOrDefault(id, 0);
    }

    public boolean esHoja(Long id) {
        return !hijos.containsKey(id);
    }

    public List<Actividad> hijos(Long id) {
        return hijos.getOrDefault(id, List.of());
    }

    public List<Actividad> hojas() {
        return enOrden.stream().filter(a -> esHoja(a.getId())).toList();
    }

    /** El elemento y todos sus descendientes. */
    public Set<Long> subarbol(Long id) {
        Set<Long> ids = new LinkedHashSet<>();
        Deque<Long> pila = new ArrayDeque<>(List.of(id));
        while (!pila.isEmpty()) {
            Long actual = pila.pop();
            if (ids.add(actual)) {
                hijos.getOrDefault(actual, List.of()).forEach(h -> pila.push(h.getId()));
            }
        }
        return ids;
    }

    /** Hojas del subárbol (el propio elemento si no tiene hijos). */
    public List<Actividad> hojasDe(Long id) {
        Set<Long> sub = subarbol(id);
        return enOrden.stream().filter(a -> sub.contains(a.getId()) && esHoja(a.getId())).toList();
    }

    /** Etapa (raíz) a la que pertenece el elemento. */
    public Actividad etapaDe(Long id) {
        Actividad a = porId.get(id);
        while (a != null && a.getPadreId() != null && porId.containsKey(a.getPadreId())) {
            a = porId.get(a.getPadreId());
        }
        return a;
    }

    /** Nombre con su código EDT: "3.1.2 Backend API inventario". */
    public String etiqueta(Long id) {
        Actividad a = porId.get(id);
        return a == null ? "" : codigo(id) + " " + a.getNombre();
    }
}
