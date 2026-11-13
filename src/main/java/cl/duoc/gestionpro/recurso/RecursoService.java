package cl.duoc.gestionpro.recurso;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.duoc.gestionpro.comun.RecursoNoEncontradoException;
import cl.duoc.gestionpro.comun.ReglaNegocioException;
import cl.duoc.gestionpro.planificacion.Actividad;
import cl.duoc.gestionpro.planificacion.ActividadRepository;
import cl.duoc.gestionpro.planificacion.ArbolActividades;
import cl.duoc.gestionpro.planificacion.CalendarioFactory;
import cl.duoc.gestionpro.proyecto.Proyecto;
import cl.duoc.gestionpro.proyecto.ProyectoRepository;
import cl.duoc.gestionpro.recalculo.RecalculoService;
import cl.duoc.gestionpro.recurso.dto.AsignacionProfesionalDto;
import cl.duoc.gestionpro.recurso.dto.AsignacionProfesionalRequest;
import cl.duoc.gestionpro.recurso.dto.AsignacionRecursoDto;
import cl.duoc.gestionpro.recurso.dto.AsignacionRecursoRequest;
import cl.duoc.gestionpro.recurso.dto.CostoActividadDto;
import cl.duoc.gestionpro.recurso.dto.CostoTipoDto;
import cl.duoc.gestionpro.recurso.dto.CostosDto;
import cl.duoc.gestionpro.recurso.dto.RecursoDto;
import cl.duoc.gestionpro.recurso.dto.RecursoRequest;
import cl.duoc.gestionpro.recurso.dto.RequerimientoDto;
import cl.duoc.gestionpro.recurso.dto.RequerimientoRequest;
import cl.duoc.gestionpro.recurso.dto.SobreasignacionDto;

/**
 * Módulo 5: dotación requerida, recursos del proyecto y asignaciones a actividades hoja. Cada modificación
 * pasa por el recálculo (semanas → costos → KPI → alertas) en la misma transacción y responde los costos al día.
 */
@Service
public class RecursoService {

    private static final ZoneId ZONA = ZoneId.of("America/Santiago");

    private final ProyectoRepository proyectos;
    private final ActividadRepository actividades;
    private final ProfesionalRepository profesionales;
    private final TipoProfesionalRepository tipos;
    private final RequerimientoProfesionalRepository requerimientos;
    private final RecursoRepository recursos;
    private final AsignacionProfesionalRepository asignacionesProfesional;
    private final AsignacionRecursoRepository asignacionesRecurso;
    private final CargaService carga;
    private final CalendarioFactory calendarios;
    private final RecalculoService recalculo;

    public RecursoService(ProyectoRepository proyectos, ActividadRepository actividades, ProfesionalRepository profesionales,
            TipoProfesionalRepository tipos, RequerimientoProfesionalRepository requerimientos, RecursoRepository recursos,
            AsignacionProfesionalRepository asignacionesProfesional, AsignacionRecursoRepository asignacionesRecurso,
            CargaService carga, CalendarioFactory calendarios, RecalculoService recalculo) {
        this.proyectos = proyectos;
        this.actividades = actividades;
        this.profesionales = profesionales;
        this.tipos = tipos;
        this.requerimientos = requerimientos;
        this.recursos = recursos;
        this.asignacionesProfesional = asignacionesProfesional;
        this.asignacionesRecurso = asignacionesRecurso;
        this.carga = carga;
        this.calendarios = calendarios;
        this.recalculo = recalculo;
    }

    // ---------------------------------------------------------------- consulta

    @Transactional(readOnly = true)
    public CostosDto costos(Long proyectoId) {
        Proyecto p = proyecto(proyectoId);
        List<Actividad> lista = actividades.findByProyectoIdOrderByOrdenAscIdAsc(proyectoId);
        ArbolActividades arbol = ArbolActividades.de(lista);
        List<AsignacionProfesional> aps = asignacionesProfesional.deProyecto(proyectoId);
        List<AsignacionRecurso> ars = asignacionesRecurso.deProyecto(proyectoId);
        List<Recurso> recs = recursos.findByProyectoIdOrderByTipoAscDescripcionAsc(proyectoId);
        Map<Long, Recurso> recPorId = recs.stream().collect(Collectors.toMap(Recurso::getId, r -> r));
        Map<Long, Profesional> profPorId = new HashMap<>();
        profesionales.findAll().forEach(x -> profPorId.put(x.getId(), x));
        Map<Long, String> tipoPorId = new HashMap<>();
        tipos.findAll().forEach(t -> tipoPorId.put(t.getId(), t.getCategoria()));

        LocalDate hoy = LocalDate.now(ZONA);
        CostoService.Costos c = CostoService.calcular(lista, aps, ars, calendarios.deCalendario(p.getCalendarioId()), hoy);

        List<CostoActividadDto> acts = arbol.enOrden().stream().map(a -> {
            var ca = c.de(a.getId());
            return new CostoActividadDto(a.getId(), a.getPadreId(), arbol.codigo(a.getId()), a.getNombre(), a.getTipo(),
                    arbol.nivel(a.getId()), arbol.esHoja(a.getId()), a.getDuracionPlan(), a.getFechaInicioPlan(),
                    a.getFechaTerminoPlan(), a.getAvance(), ca.planProfesionales(), ca.planRecursos(), ca.plan(), ca.real(),
                    ca.asignaciones());
        }).toList();

        List<AsignacionProfesionalDto> apDtos = aps.stream().map(a -> {
            Profesional prof = profPorId.get(a.getProfesionalId());
            return new AsignacionProfesionalDto(a.getId(), a.getActividadId(), arbol.etiqueta(a.getActividadId()),
                    a.getProfesionalId(), prof.getNombre(), prof.getEspecialidad(), a.getHorasSemana(), a.getSemanas(),
                    a.getValorHora(), a.horasPlan(), a.costoPlan(), a.getHorasReales(), a.getCostoReal());
        }).sorted(java.util.Comparator.comparing((AsignacionProfesionalDto d) -> ordenEn(arbol, d.actividadId()))
                .thenComparing(AsignacionProfesionalDto::profesional)).toList();

        List<AsignacionRecursoDto> arDtos = ars.stream().map(a -> {
            Recurso r = recPorId.get(a.getRecursoId());
            return new AsignacionRecursoDto(a.getId(), a.getActividadId(), arbol.etiqueta(a.getActividadId()),
                    a.getRecursoId(), r.getDescripcion(), r.getTipo(), r.getUnidad(), a.getCantidad(), a.getCostoUnitario(),
                    a.getPeriodo(), a.costoPlan(), a.getCostoReal());
        }).sorted(java.util.Comparator.comparing((AsignacionRecursoDto d) -> ordenEn(arbol, d.actividadId()))
                .thenComparing(AsignacionRecursoDto::recurso)).toList();

        Map<Long, BigDecimal> consumo = new HashMap<>();
        ars.forEach(a -> consumo.merge(a.getRecursoId(), a.consumo(), BigDecimal::add));
        List<RecursoDto> recDtos = recs.stream().map(r -> {
            BigDecimal asignado = consumo.getOrDefault(r.getId(), BigDecimal.ZERO);
            return new RecursoDto(r.getId(), r.getTipo(), r.getDescripcion(), r.getDetalle(), r.getUnidad(), r.getCantidad(),
                    r.getCostoUnitario(), r.getPeriodo(), r.costoTotal(), r.capacidad(), asignado, r.capacidad().subtract(asignado));
        }).toList();

        // profesionales distintos del proyecto agrupados por tipo
        Map<Long, List<String>> porTipo = new HashMap<>();
        aps.stream().map(AsignacionProfesional::getProfesionalId).distinct().map(profPorId::get)
                .forEach(x -> porTipo.computeIfAbsent(x.getTipoProfesionalId(), k -> new ArrayList<>()).add(x.getNombre()));
        List<RequerimientoDto> reqDtos = requerimientos.findByProyectoIdOrderByIdAsc(proyectoId).stream().map(r -> {
            List<String> nombres = porTipo.getOrDefault(r.getTipoProfesionalId(), List.of()).stream().sorted().toList();
            return new RequerimientoDto(r.getId(), r.getTipoProfesionalId(), tipoPorId.get(r.getTipoProfesionalId()),
                    r.getCantidadRequerida(), r.getTarifaHora(), nombres.size(), nombres.size() >= r.getCantidadRequerida(), nombres);
        }).toList();

        Map<String, BigDecimal[]> costoTipo = new LinkedHashMap<>();
        costoTipo.put("PROFESIONALES", new BigDecimal[] { BigDecimal.ZERO, BigDecimal.ZERO });
        for (TipoRecurso t : TipoRecurso.values()) {
            costoTipo.put(t.name(), new BigDecimal[] { BigDecimal.ZERO, BigDecimal.ZERO });
        }
        aps.forEach(a -> sumar(costoTipo.get("PROFESIONALES"), a.costoPlan(), a.getCostoReal()));
        ars.forEach(a -> sumar(costoTipo.get(recPorId.get(a.getRecursoId()).getTipo().name()), a.costoPlan(), a.getCostoReal()));
        List<CostoTipoDto> tipoDtos = costoTipo.entrySet().stream()
                .map(e -> new CostoTipoDto(e.getKey(), e.getValue()[0], e.getValue()[1])).toList();

        List<SobreasignacionDto> sobre = carga.sobreasignaciones(proyectoId).stream().map(SobreasignacionDto::de).toList();

        return new CostosDto(proyectoId, p.getPresupuesto(), c.plan(), c.real(), c.valorGanado(), c.valorPlanificado(),
                razon(c.valorGanado(), c.valorPlanificado()), razon(c.valorGanado(), c.real()),
                c.plan().subtract(p.getPresupuesto()), tipoDtos, acts, apDtos, arDtos, recDtos, reqDtos, sobre);
    }

    // ---------------------------------------------------------------- dotación requerida

    @Transactional
    public CostosDto crearRequerimiento(Long proyectoId, RequerimientoRequest req) {
        proyecto(proyectoId);
        if (requerimientos.existsByProyectoIdAndTipoProfesionalId(proyectoId, req.tipoProfesionalId())) {
            throw new ReglaNegocioException("REQUERIMIENTO_DUPLICADO", "Ese tipo de profesional ya está en la dotación del proyecto.");
        }
        var r = new RequerimientoProfesional();
        r.setProyectoId(proyectoId);
        aplicar(r, req);
        requerimientos.saveAndFlush(r);
        return costos(proyectoId);
    }

    @Transactional
    public CostosDto actualizarRequerimiento(Long proyectoId, Long id, RequerimientoRequest req) {
        var r = requerimientos.findById(id).filter(x -> x.getProyectoId().equals(proyectoId))
                .orElseThrow(() -> new RecursoNoEncontradoException("Requerimiento", id));
        if (!r.getTipoProfesionalId().equals(req.tipoProfesionalId())
                && requerimientos.existsByProyectoIdAndTipoProfesionalId(proyectoId, req.tipoProfesionalId())) {
            throw new ReglaNegocioException("REQUERIMIENTO_DUPLICADO", "Ese tipo de profesional ya está en la dotación del proyecto.");
        }
        aplicar(r, req);
        requerimientos.flush();
        return costos(proyectoId);
    }

    @Transactional
    public CostosDto eliminarRequerimiento(Long proyectoId, Long id) {
        var r = requerimientos.findById(id).filter(x -> x.getProyectoId().equals(proyectoId))
                .orElseThrow(() -> new RecursoNoEncontradoException("Requerimiento", id));
        requerimientos.delete(r);
        requerimientos.flush();
        return costos(proyectoId);
    }

    private void aplicar(RequerimientoProfesional r, RequerimientoRequest req) {
        if (!tipos.existsById(req.tipoProfesionalId())) {
            throw new RecursoNoEncontradoException("Tipo de profesional", req.tipoProfesionalId());
        }
        r.setTipoProfesionalId(req.tipoProfesionalId());
        r.setCantidadRequerida(req.cantidadRequerida());
        r.setTarifaHora(req.tarifaHora());
    }

    // ---------------------------------------------------------------- recursos

    @Transactional
    public CostosDto crearRecurso(Long proyectoId, RecursoRequest req) {
        proyecto(proyectoId);
        var r = new Recurso();
        r.setProyectoId(proyectoId);
        aplicar(r, req);
        recursos.saveAndFlush(r);
        return recalcular(proyectoId);
    }

    @Transactional
    public CostosDto actualizarRecurso(Long proyectoId, Long id, RecursoRequest req) {
        var r = recurso(proyectoId, id);
        aplicar(r, req);
        validarCapacidad(r);
        // el precio corregido del recurso se aplica a sus asignaciones
        asignacionesRecurso.findByRecursoId(id).forEach(a -> a.setCostoUnitario(r.getCostoUnitario()));
        recursos.flush();
        return recalcular(proyectoId);
    }

    @Transactional
    public CostosDto eliminarRecurso(Long proyectoId, Long id) {
        var r = recurso(proyectoId, id);
        if (!asignacionesRecurso.findByRecursoId(id).isEmpty()) {
            throw new ReglaNegocioException("RECURSO_ASIGNADO",
                    "«" + r.getDescripcion() + "» está asignado a actividades. Quita esas asignaciones antes de eliminarlo.");
        }
        recursos.delete(r);
        recursos.flush();
        return recalcular(proyectoId);
    }

    private static void aplicar(Recurso r, RecursoRequest req) {
        r.setTipo(req.tipo());
        r.setDescripcion(req.descripcion().trim());
        r.setDetalle(vacioANulo(req.detalle()));
        r.setUnidad(vacioANulo(req.unidad()));
        r.setCantidad(req.cantidad());
        r.setCostoUnitario(req.costoUnitario());
        r.setPeriodo(req.periodo());
    }

    /** La capacidad del recurso (cantidad × periodo) debe cubrir lo asignado. */
    public void validarCapacidad(Recurso r) {
        BigDecimal asignado = asignacionesRecurso.findByRecursoId(r.getId()).stream().map(AsignacionRecurso::consumo)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (asignado.compareTo(r.capacidad()) > 0) {
            throw new ReglaNegocioException("RECURSO_INSUFICIENTE", "«" + r.getDescripcion() + "» quedaría con capacidad "
                    + texto(r.capacidad()) + " y ya hay " + texto(asignado) + " asignados. Reduce primero las asignaciones.");
        }
    }

    // ---------------------------------------------------------------- asignación de profesionales

    @Transactional
    public CostosDto crearAsignacionProfesional(Long proyectoId, AsignacionProfesionalRequest req) {
        Actividad a = hoja(proyectoId, req.actividadId());
        Profesional prof = profesionales.findById(req.profesionalId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Profesional", req.profesionalId()));
        if (!prof.isActivo()) {
            throw new ReglaNegocioException("PROFESIONAL_INACTIVO", prof.getNombre() + " está inactivo.");
        }
        if (asignacionesProfesional.existsByActividadIdAndProfesionalId(a.getId(), prof.getId())) {
            throw new ReglaNegocioException("ASIGNACION_DUPLICADA", prof.getNombre() + " ya está asignado a «" + a.getNombre() + "».");
        }
        var asig = new AsignacionProfesional();
        asig.setActividadId(a.getId());
        asig.setProfesionalId(prof.getId());
        asig.setHorasSemana(req.horasSemana());
        asig.setValorHora(req.valorHora() == null ? prof.getValorHora() : req.valorHora());
        asig.setSemanas(semanas(a, proyecto(proyectoId)));
        aplicarReal(asig, req.horasReales());
        asignacionesProfesional.saveAndFlush(asig);
        return recalcular(proyectoId);
    }

    @Transactional
    public CostosDto actualizarAsignacionProfesional(Long proyectoId, Long id, AsignacionProfesionalRequest req) {
        var asig = asignacionProfesional(proyectoId, id);
        if (!asig.getActividadId().equals(req.actividadId()) || !asig.getProfesionalId().equals(req.profesionalId())) {
            Actividad a = hoja(proyectoId, req.actividadId());
            if (asignacionesProfesional.existsByActividadIdAndProfesionalId(a.getId(), req.profesionalId())) {
                throw new ReglaNegocioException("ASIGNACION_DUPLICADA", "Ese profesional ya está asignado a «" + a.getNombre() + "».");
            }
            Profesional prof = profesionales.findById(req.profesionalId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Profesional", req.profesionalId()));
            if (!asig.getProfesionalId().equals(prof.getId())) {
                asig.setValorHora(prof.getValorHora());
            }
            asig.setActividadId(a.getId());
            asig.setProfesionalId(prof.getId());
            asig.setSemanas(semanas(a, proyecto(proyectoId)));
        }
        asig.setHorasSemana(req.horasSemana());
        if (req.valorHora() != null) {
            asig.setValorHora(req.valorHora());
        }
        aplicarReal(asig, req.horasReales());
        asignacionesProfesional.flush();
        return recalcular(proyectoId);
    }

    @Transactional
    public CostosDto eliminarAsignacionProfesional(Long proyectoId, Long id) {
        asignacionesProfesional.delete(asignacionProfesional(proyectoId, id));
        asignacionesProfesional.flush();
        return recalcular(proyectoId);
    }

    /** costo real = horas reales × valor hora. */
    private static void aplicarReal(AsignacionProfesional asig, BigDecimal horasReales) {
        BigDecimal h = horasReales == null ? asig.getHorasReales() : horasReales;
        asig.setHorasReales(h);
        asig.setCostoReal(h.multiply(asig.getValorHora()).setScale(2, RoundingMode.HALF_UP));
    }

    private BigDecimal semanas(Actividad a, Proyecto p) {
        return RecalculoService.semanas(a.getDuracionPlan(), calendarios.diasPorSemana(p.getCalendarioId()));
    }

    // ---------------------------------------------------------------- asignación de recursos

    @Transactional
    public CostosDto crearAsignacionRecurso(Long proyectoId, AsignacionRecursoRequest req) {
        Actividad a = hoja(proyectoId, req.actividadId());
        Recurso r = recurso(proyectoId, req.recursoId());
        if (asignacionesRecurso.existsByActividadIdAndRecursoId(a.getId(), r.getId())) {
            throw new ReglaNegocioException("ASIGNACION_DUPLICADA", "«" + r.getDescripcion() + "» ya está asignado a «" + a.getNombre() + "».");
        }
        var asig = new AsignacionRecurso();
        asig.setActividadId(a.getId());
        asig.setRecursoId(r.getId());
        aplicar(asig, req, r);
        validarDisponible(r, asig, null);
        asignacionesRecurso.saveAndFlush(asig);
        return recalcular(proyectoId);
    }

    @Transactional
    public CostosDto actualizarAsignacionRecurso(Long proyectoId, Long id, AsignacionRecursoRequest req) {
        var asig = asignacionRecurso(proyectoId, id);
        Recurso r = recurso(proyectoId, req.recursoId());
        if (!asig.getActividadId().equals(req.actividadId()) || !asig.getRecursoId().equals(req.recursoId())) {
            Actividad a = hoja(proyectoId, req.actividadId());
            if (asignacionesRecurso.existsByActividadIdAndRecursoId(a.getId(), r.getId())) {
                throw new ReglaNegocioException("ASIGNACION_DUPLICADA", "«" + r.getDescripcion() + "» ya está asignado a «" + a.getNombre() + "».");
            }
            asig.setActividadId(a.getId());
            asig.setRecursoId(r.getId());
        }
        aplicar(asig, req, r);
        validarDisponible(r, asig, id);
        asignacionesRecurso.flush();
        return recalcular(proyectoId);
    }

    @Transactional
    public CostosDto eliminarAsignacionRecurso(Long proyectoId, Long id) {
        asignacionesRecurso.delete(asignacionRecurso(proyectoId, id));
        asignacionesRecurso.flush();
        return recalcular(proyectoId);
    }

    private static void aplicar(AsignacionRecurso asig, AsignacionRecursoRequest req, Recurso r) {
        asig.setCantidad(req.cantidad());
        asig.setPeriodo(req.periodo() == null ? 1 : req.periodo());
        asig.setCostoUnitario(r.getCostoUnitario());
        asig.setCostoReal(req.costoReal() == null ? asig.getCostoReal() : req.costoReal());
    }

    /** Lo asignado del recurso (sin contar la asignación que se edita) más la nueva no puede superar su capacidad. */
    public void validarDisponible(Recurso r, AsignacionRecurso nueva, Long excluirId) {
        BigDecimal otras = asignacionesRecurso.findByRecursoId(r.getId()).stream()
                .filter(x -> !Objects.equals(x.getId(), excluirId)).map(AsignacionRecurso::consumo)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal total = otras.add(nueva.consumo());
        if (total.compareTo(r.capacidad()) > 0) {
            throw new ReglaNegocioException("RECURSO_INSUFICIENTE", "«" + r.getDescripcion() + "» tiene capacidad "
                    + texto(r.capacidad()) + " (cantidad × periodo) y quedarían " + texto(total) + " asignados. Disponible: "
                    + texto(r.capacidad().subtract(otras)) + ".");
        }
    }

    // ---------------------------------------------------------------- apoyo

    private CostosDto recalcular(Long proyectoId) {
        recalculo.recalcularProyecto(proyectoId);
        return costos(proyectoId);
    }

    private Proyecto proyecto(Long id) {
        return proyectos.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Proyecto", id));
    }

    private Recurso recurso(Long proyectoId, Long id) {
        return recursos.findById(id).filter(r -> r.getProyectoId().equals(proyectoId))
                .orElseThrow(() -> new RecursoNoEncontradoException("Recurso", id));
    }

    private Actividad hoja(Long proyectoId, Long actividadId) {
        Actividad a = actividades.findById(actividadId).filter(x -> x.getProyectoId().equals(proyectoId))
                .orElseThrow(() -> new RecursoNoEncontradoException("Actividad", actividadId));
        if (actividades.existsByPadreId(a.getId())) {
            throw new ReglaNegocioException("SOLO_HOJAS", "«" + a.getNombre()
                    + "» tiene subactividades: los recursos se asignan a elementos sin hijos, y el padre suma sus costos.");
        }
        return a;
    }

    private AsignacionProfesional asignacionProfesional(Long proyectoId, Long id) {
        var asig = asignacionesProfesional.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Asignación", id));
        actividades.findById(asig.getActividadId()).filter(a -> a.getProyectoId().equals(proyectoId))
                .orElseThrow(() -> new RecursoNoEncontradoException("Asignación", id));
        return asig;
    }

    private AsignacionRecurso asignacionRecurso(Long proyectoId, Long id) {
        var asig = asignacionesRecurso.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Asignación", id));
        actividades.findById(asig.getActividadId()).filter(a -> a.getProyectoId().equals(proyectoId))
                .orElseThrow(() -> new RecursoNoEncontradoException("Asignación", id));
        return asig;
    }

    private static int ordenEn(ArbolActividades arbol, Long actividadId) {
        return arbol.enOrden().indexOf(arbol.get(actividadId));
    }

    private static void sumar(BigDecimal[] acc, BigDecimal plan, BigDecimal real) {
        acc[0] = acc[0].add(plan);
        acc[1] = acc[1].add(real);
    }

    static BigDecimal razon(BigDecimal a, BigDecimal b) {
        return b == null || b.signum() == 0 ? null : a.divide(b, 3, RoundingMode.HALF_UP);
    }

    private static String vacioANulo(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static String texto(BigDecimal n) {
        return n.stripTrailingZeros().toPlainString();
    }
}
