package cl.duoc.gestionpro.planificacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.duoc.gestionpro.comun.RecursoNoEncontradoException;
import cl.duoc.gestionpro.comun.ReglaNegocioException;
import cl.duoc.gestionpro.planificacion.dto.ActividadDto;
import cl.duoc.gestionpro.planificacion.dto.ActividadRequest;
import cl.duoc.gestionpro.planificacion.dto.DependenciaDto;
import cl.duoc.gestionpro.planificacion.dto.DependenciaRequest;
import cl.duoc.gestionpro.planificacion.dto.PlanCambioDto;
import cl.duoc.gestionpro.planificacion.dto.PlanDto;
import cl.duoc.gestionpro.proyecto.Proyecto;
import cl.duoc.gestionpro.proyecto.ProyectoRepository;
import cl.duoc.gestionpro.recalculo.CambioFecha;
import cl.duoc.gestionpro.recalculo.RecalculoService;
import cl.duoc.gestionpro.recurso.ProfesionalRepository;

/**
 * Módulo 3: CRUD de actividades y dependencias. Toda modificación termina en el recálculo del proyecto,
 * dentro de la misma transacción, y responde el plan actualizado con las fechas que se movieron.
 */
@Service
public class PlanificacionService {

    private static final ZoneId ZONA = ZoneId.of("America/Santiago");
    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    private final ProyectoRepository proyectos;
    private final ActividadRepository actividades;
    private final DependenciaRepository dependencias;
    private final ProfesionalRepository profesionales;
    private final RecalculoService recalculo;

    public PlanificacionService(ProyectoRepository proyectos, ActividadRepository actividades,
            DependenciaRepository dependencias, ProfesionalRepository profesionales, RecalculoService recalculo) {
        this.proyectos = proyectos;
        this.actividades = actividades;
        this.dependencias = dependencias;
        this.profesionales = profesionales;
        this.recalculo = recalculo;
    }

    @Transactional(readOnly = true)
    public PlanDto plan(Long proyectoId) {
        Proyecto p = proyectos.findById(proyectoId).orElseThrow(() -> new RecursoNoEncontradoException("Proyecto", proyectoId));
        List<Actividad> lista = actividades.findByProyectoIdOrderByOrdenAscIdAsc(proyectoId);
        List<DependenciaDto> deps = dependencias.findByProyectoIdOrderByIdAsc(proyectoId).stream().map(DependenciaDto::de).toList();
        return new PlanDto(p.getId(), p.getCalendarioId(), p.getFechaInicioPlan(), enOrdenDeArbol(lista), deps);
    }

    // ---------------------------------------------------------------- actividades

    @Transactional
    public PlanCambioDto crearActividad(Long proyectoId, ActividadRequest req) {
        Actividad padre = req.padreId() == null ? null : buscarActividad(proyectoId, req.padreId());
        TipoActividad tipo = tipoHijo(padre);
        if (padre != null && esHoja(padre) && dependencias.existsByPredecesoraIdOrSucesoraId(padre.getId(), padre.getId())) {
            throw new ReglaNegocioException("PADRE_CON_DEPENDENCIAS", "«" + padre.getNombre()
                    + "» tiene dependencias. Quítalas antes de agregarle subactividades: los vínculos se definen entre elementos sin hijos.");
        }
        var a = new Actividad();
        a.setProyectoId(proyectoId);
        a.setPadreId(padre == null ? null : padre.getId());
        a.setTipo(tipo);
        a.setOrden(actividades.maxOrden(proyectoId, a.getPadreId()) + 1);
        aplicar(a, req, true);
        actividades.saveAndFlush(a);
        return recalcular(proyectoId);
    }

    @Transactional
    public PlanCambioDto actualizarActividad(Long proyectoId, Long id, ActividadRequest req) {
        var a = buscarActividad(proyectoId, id);
        aplicar(a, req, !actividades.existsByPadreId(id));
        actividades.flush();
        return recalcular(proyectoId);
    }

    /** Elimina la actividad, sus descendientes y todas las dependencias que los tocan. */
    @Transactional
    public PlanCambioDto eliminarActividad(Long proyectoId, Long id) {
        buscarActividad(proyectoId, id);
        List<Actividad> todas = actividades.findByProyectoIdOrderByOrdenAscIdAsc(proyectoId);
        Map<Long, List<Actividad>> hijos = todas.stream().filter(x -> x.getPadreId() != null)
                .collect(Collectors.groupingBy(Actividad::getPadreId));
        List<Actividad> subarbol = new ArrayList<>();
        Deque<Long> pila = new ArrayDeque<>(List.of(id));
        Map<Long, Actividad> porId = todas.stream().collect(Collectors.toMap(Actividad::getId, x -> x));
        while (!pila.isEmpty()) {
            Long actual = pila.pop();
            subarbol.add(porId.get(actual));
            hijos.getOrDefault(actual, List.of()).forEach(h -> pila.push(h.getId()));
        }
        Set<Long> ids = subarbol.stream().map(Actividad::getId).collect(Collectors.toSet());
        dependencias.deleteAll(dependencias.findByProyectoIdOrderByIdAsc(proyectoId).stream()
                .filter(d -> ids.contains(d.getPredecesoraId()) || ids.contains(d.getSucesoraId())).toList());
        dependencias.flush();
        // Hijos antes que padres.
        for (int i = subarbol.size() - 1; i >= 0; i--) {
            actividades.delete(subarbol.get(i));
        }
        actividades.flush();
        return recalcular(proyectoId);
    }

    // ---------------------------------------------------------------- dependencias

    @Transactional
    public PlanCambioDto crearDependencia(Long proyectoId, DependenciaRequest req) {
        var d = new Dependencia();
        d.setProyectoId(proyectoId);
        aplicar(d, req, proyectoId, null);
        dependencias.saveAndFlush(d);
        return recalcular(proyectoId);
    }

    @Transactional
    public PlanCambioDto actualizarDependencia(Long proyectoId, Long id, DependenciaRequest req) {
        var d = buscarDependencia(proyectoId, id);
        aplicar(d, req, proyectoId, id);
        dependencias.flush();
        return recalcular(proyectoId);
    }

    @Transactional
    public PlanCambioDto eliminarDependencia(Long proyectoId, Long id) {
        dependencias.delete(buscarDependencia(proyectoId, id));
        dependencias.flush();
        return recalcular(proyectoId);
    }

    // ---------------------------------------------------------------- reglas

    private void aplicar(Actividad a, ActividadRequest req, boolean esHoja) {
        a.setNombre(req.nombre().trim());
        a.setDescripcion(req.descripcion() == null || req.descripcion().isBlank() ? null : req.descripcion().trim());
        a.setResponsable(req.responsableId() == null ? null : profesionales.findById(req.responsableId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Profesional", req.responsableId())));
        if (!esHoja) {
            return; // duración, fechas reales y avance se derivan de los hijos
        }
        BigDecimal avance = req.avance() == null ? BigDecimal.ZERO : req.avance();
        if (avance.compareTo(CIEN) == 0 && req.fechaTerminoReal() == null) {
            throw new ReglaNegocioException("AVANCE_SIN_TERMINO", "Para marcar 100 % de avance indica el término real.");
        }
        if (req.fechaTerminoReal() != null && avance.compareTo(CIEN) < 0) {
            throw new ReglaNegocioException("TERMINO_SIN_AVANCE", "Si la actividad tiene término real, su avance debe ser 100 %.");
        }
        if (avance.signum() > 0 && req.fechaInicioReal() == null) {
            throw new ReglaNegocioException("AVANCE_SIN_INICIO", "Indica el inicio real para registrar avance.");
        }
        a.setDuracionPlan(req.duracionPlan());
        a.setFechaInicioReal(req.fechaInicioReal());
        a.setFechaTerminoReal(req.fechaTerminoReal());
        a.setAvance(avance);
        Integer duracionReal = req.duracionReal();
        if (duracionReal == null && req.fechaInicioReal() != null && req.fechaTerminoReal() != null) {
            Proyecto p = proyectos.findById(a.getProyectoId()).orElseThrow();
            duracionReal = recalculo.calendarioDe(p.getCalendarioId()).contar(req.fechaInicioReal(), req.fechaTerminoReal());
        }
        a.setDuracionReal(duracionReal);
    }

    private void aplicar(Dependencia d, DependenciaRequest req, Long proyectoId, Long idActual) {
        Actividad pred = buscarActividad(proyectoId, req.predecesoraId());
        Actividad suc = buscarActividad(proyectoId, req.sucesoraId());
        if (pred.getId().equals(suc.getId())) {
            throw new ReglaNegocioException("DEPENDENCIA_PROPIA", "Una actividad no puede depender de sí misma.");
        }
        for (Actividad a : List.of(pred, suc)) {
            if (!esHoja(a)) {
                throw new ReglaNegocioException("DEPENDENCIA_SOLO_HOJAS", "«" + a.getNombre()
                        + "» tiene subactividades: las dependencias se definen entre elementos sin hijos.");
            }
        }
        List<Dependencia> otras = dependencias.findByProyectoIdOrderByIdAsc(proyectoId).stream()
                .filter(x -> !Objects.equals(x.getId(), idActual)).toList();
        boolean repetida = otras.stream().anyMatch(x -> x.getPredecesoraId().equals(pred.getId()) && x.getSucesoraId().equals(suc.getId()));
        if (repetida) {
            throw new ReglaNegocioException("DEPENDENCIA_DUPLICADA",
                    "«" + suc.getNombre() + "» ya tiene a «" + pred.getNombre() + "» como predecesora.");
        }
        // Si la predecesora ya depende (directa o indirectamente) de la sucesora, el vínculo cierra un ciclo.
        List<Long> camino = MotorProgramacion.camino(otras, suc.getId(), pred.getId());
        if (!camino.isEmpty()) {
            Map<Long, String> nombres = new HashMap<>();
            actividades.findAllById(camino).forEach(a -> nombres.put(a.getId(), a.getNombre()));
            String ruta = camino.stream().map(id -> "«" + nombres.get(id) + "»").collect(Collectors.joining(" → "));
            throw new ReglaNegocioException("DEPENDENCIA_CIRCULAR", "Dependencia circular. «" + pred.getNombre()
                    + "» ya depende de «" + suc.getNombre() + "» (" + ruta + "). Elige otra actividad para guardar el vínculo.");
        }
        d.setPredecesoraId(pred.getId());
        d.setSucesoraId(suc.getId());
        d.setTipo(req.tipo());
        d.setDesfaseDias(req.desfaseDias());
    }

    private static TipoActividad tipoHijo(Actividad padre) {
        if (padre == null) {
            return TipoActividad.ETAPA;
        }
        return switch (padre.getTipo()) {
            case ETAPA -> TipoActividad.ACTIVIDAD;
            case ACTIVIDAD -> TipoActividad.SUBACTIVIDAD;
            case SUBACTIVIDAD -> throw new ReglaNegocioException("NIVEL_MAXIMO",
                    "Las subactividades no admiten hijos: la jerarquía es etapa → actividad → subactividad.");
        };
    }

    private PlanCambioDto recalcular(Long proyectoId) {
        List<CambioFecha> cambios = recalculo.recalcularProyecto(proyectoId);
        actividades.flush();
        return new PlanCambioDto(plan(proyectoId), cambios);
    }

    private boolean esHoja(Actividad a) {
        return !actividades.existsByPadreId(a.getId());
    }

    private Actividad buscarActividad(Long proyectoId, Long id) {
        return actividades.findById(id).filter(a -> a.getProyectoId().equals(proyectoId))
                .orElseThrow(() -> new RecursoNoEncontradoException("Actividad", id));
    }

    private Dependencia buscarDependencia(Long proyectoId, Long id) {
        return dependencias.findById(id).filter(d -> d.getProyectoId().equals(proyectoId))
                .orElseThrow(() -> new RecursoNoEncontradoException("Dependencia", id));
    }

    /** Recorrido en profundidad: cada etapa seguida de sus actividades y subactividades. */
    private static List<ActividadDto> enOrdenDeArbol(List<Actividad> lista) {
        Map<Long, List<Actividad>> hijos = new HashMap<>();
        List<Actividad> raices = new ArrayList<>();
        Comparator<Actividad> porOrden = Comparator.comparing(Actividad::getOrden).thenComparing(Actividad::getId);
        for (Actividad a : lista) {
            if (a.getPadreId() == null) {
                raices.add(a);
            } else {
                hijos.computeIfAbsent(a.getPadreId(), k -> new ArrayList<>()).add(a);
            }
        }
        LocalDate hoy = LocalDate.now(ZONA);
        List<ActividadDto> salida = new ArrayList<>(lista.size());
        Set<Long> visitados = new HashSet<>();
        raices.sort(porOrden);
        Deque<Object[]> pila = new ArrayDeque<>();
        for (int i = raices.size() - 1; i >= 0; i--) {
            pila.push(new Object[] { raices.get(i), 0 });
        }
        while (!pila.isEmpty()) {
            Object[] item = pila.pop();
            Actividad a = (Actividad) item[0];
            int nivel = (int) item[1];
            if (!visitados.add(a.getId())) {
                continue;
            }
            List<Actividad> h = hijos.getOrDefault(a.getId(), List.of());
            salida.add(ActividadDto.de(a, nivel, h.isEmpty(), hoy));
            List<Actividad> ordenados = new ArrayList<>(h);
            ordenados.sort(porOrden);
            for (int i = ordenados.size() - 1; i >= 0; i--) {
                pila.push(new Object[] { ordenados.get(i), nivel + 1 });
            }
        }
        return salida;
    }
}
