package cl.duoc.gestionpro.kpi;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.duoc.gestionpro.cambio.SolicitudCambio;
import cl.duoc.gestionpro.cambio.SolicitudCambioRepository;
import cl.duoc.gestionpro.comun.RecursoNoEncontradoException;
import cl.duoc.gestionpro.comun.ReglaNegocioException;
import cl.duoc.gestionpro.hito.Hito;
import cl.duoc.gestionpro.hito.HitoRepository;
import cl.duoc.gestionpro.kpi.dto.KpiDto;
import cl.duoc.gestionpro.kpi.dto.KpiRequest;
import cl.duoc.gestionpro.kpi.dto.MedicionDto;
import cl.duoc.gestionpro.kpi.dto.MedicionRequest;
import cl.duoc.gestionpro.kpi.dto.PruebaFormulaDto;
import cl.duoc.gestionpro.kpi.dto.PruebaFormulaRequest;
import cl.duoc.gestionpro.kpi.dto.VariableDto;
import cl.duoc.gestionpro.planificacion.Actividad;
import cl.duoc.gestionpro.planificacion.ActividadRepository;
import cl.duoc.gestionpro.planificacion.ArbolActividades;
import cl.duoc.gestionpro.planificacion.TipoActividad;
import cl.duoc.gestionpro.seguridad.Usuario;
import cl.duoc.gestionpro.seguridad.UsuarioRepository;

/**
 * Módulos 7 y 9: KPI configurables. El administrador define fórmula, meta, sentido y límites del semáforo;
 * el sistema mide en cada recálculo (solo guarda si el valor o el semáforo cambian), según la periodicidad
 * (mediciones programadas) o a mano (fuente MANUAL o ajuste puntual).
 */
@Service
public class KpiService {

    private static final ZoneId ZONA = ZoneId.of("America/Santiago");

    private final KpiRepository kpis;
    private final MedicionKpiRepository mediciones;
    private final VariableKpiRepository variables;
    private final CalculadoraVariables calculadora;
    private final ActividadRepository actividades;
    private final HitoRepository hitos;
    private final SolicitudCambioRepository solicitudes;
    private final UsuarioRepository usuarios;
    private final JdbcTemplate jdbc;

    public KpiService(KpiRepository kpis, MedicionKpiRepository mediciones, VariableKpiRepository variables,
            CalculadoraVariables calculadora, ActividadRepository actividades, HitoRepository hitos,
            SolicitudCambioRepository solicitudes, UsuarioRepository usuarios, JdbcTemplate jdbc) {
        this.kpis = kpis;
        this.mediciones = mediciones;
        this.variables = variables;
        this.calculadora = calculadora;
        this.actividades = actividades;
        this.hitos = hitos;
        this.solicitudes = solicitudes;
        this.usuarios = usuarios;
        this.jdbc = jdbc;
    }

    // ---------------------------------------------------------------- consulta

    @Transactional(readOnly = true)
    public List<KpiDto> listar(Long proyectoId, CategoriaKpi categoria) {
        List<Kpi> lista = kpis.findByProyectoIdOrderByCodigoAsc(proyectoId).stream()
                .filter(k -> categoria == null || k.getCategoria() == categoria).toList();
        Map<Long, List<MedicionKpi>> porKpi = mediciones.deProyecto(proyectoId).stream()
                .collect(Collectors.groupingBy(MedicionKpi::getKpiId));
        Map<Long, List<Long>> hitosPorKpi = hitosPorKpi(proyectoId);
        Nombres n = nombres(proyectoId);
        return lista.stream().map(k -> dto(k, porKpi.getOrDefault(k.getId(), List.of()), hitosPorKpi, n)).toList();
    }

    @Transactional(readOnly = true)
    public KpiDto obtener(Long proyectoId, Long id) {
        Kpi k = buscar(proyectoId, id);
        return dto(k, mediciones.findByKpiIdOrderByMedidoEnAscIdAsc(id), hitosPorKpi(proyectoId), nombres(proyectoId));
    }

    @Transactional(readOnly = true)
    public List<VariableDto> variables() {
        return variables.findAllByOrderByCategoriaAscCodigoAsc().stream().map(VariableDto::de).toList();
    }

    // ---------------------------------------------------------------- mantención (administrador)

    @Transactional
    public KpiDto crear(Long proyectoId, KpiRequest req) {
        String codigo = req.codigo().trim().toUpperCase();
        if (kpis.existsByProyectoIdAndCodigo(proyectoId, codigo)) {
            throw new ReglaNegocioException("CODIGO_DUPLICADO", "El proyecto ya tiene un KPI con el código " + codigo + ".");
        }
        var k = new Kpi();
        k.setProyectoId(proyectoId);
        aplicar(k, req, codigo);
        kpis.saveAndFlush(k);
        vincularHitos(k, req.hitoIds());
        medir(k, null, OrigenMedicion.RECALCULO, true);
        return obtener(proyectoId, k.getId());
    }

    @Transactional
    public KpiDto actualizar(Long proyectoId, Long id, KpiRequest req) {
        Kpi k = buscar(proyectoId, id);
        String codigo = req.codigo().trim().toUpperCase();
        if (!k.getCodigo().equals(codigo) && kpis.existsByProyectoIdAndCodigo(proyectoId, codigo)) {
            throw new ReglaNegocioException("CODIGO_DUPLICADO", "El proyecto ya tiene un KPI con el código " + codigo + ".");
        }
        aplicar(k, req, codigo);
        kpis.flush();
        vincularHitos(k, req.hitoIds());
        medir(k, null, OrigenMedicion.RECALCULO, false);
        return obtener(proyectoId, id);
    }

    @Transactional
    public void eliminar(Long proyectoId, Long id) {
        kpis.delete(buscar(proyectoId, id));
    }

    @Transactional
    public KpiDto registrarMedicion(Long proyectoId, Long id, MedicionRequest req, Long usuarioId) {
        Kpi k = buscar(proyectoId, id);
        var m = new MedicionKpi();
        m.setKpiId(k.getId());
        LocalDateTime ahora = LocalDateTime.now(ZONA);
        m.setMedidoEn(req.fecha() == null || req.fecha().equals(ahora.toLocalDate()) ? ahora : req.fecha().atTime(LocalTime.NOON));
        m.setValor(req.valor());
        m.setSemaforo(k.semaforo(req.valor()));
        m.setOrigen(OrigenMedicion.MANUAL);
        m.setRegistradoPorId(usuarioId);
        m.setObservacion(req.observacion() == null || req.observacion().isBlank() ? null : req.observacion().trim());
        mediciones.saveAndFlush(m);
        return obtener(proyectoId, id);
    }

    @Transactional(readOnly = true)
    public PruebaFormulaDto probar(Long proyectoId, PruebaFormulaRequest req) {
        Map<String, VariableKpi> catalogo = variables.findAll().stream().collect(Collectors.toMap(VariableKpi::getCodigo, v -> v));
        Set<String> usadas = EvaluadorFormula.variables(req.formula(), catalogo.keySet());
        AmbitoKpi ambito = req.ambito() == null ? AmbitoKpi.PROYECTO : req.ambito();
        var datos = calculadora.datos(proyectoId, LocalDate.now(ZONA));
        Map<String, BigDecimal> valores = calculadora.calcular(datos, ambito, req.ambitoRefId());
        Optional<BigDecimal> valor = EvaluadorFormula.evaluar(req.formula(), usadas, valores);
        List<PruebaFormulaDto.VariableValor> vs = usadas.stream().map(c -> {
            VariableKpi v = catalogo.get(c);
            return new PruebaFormulaDto.VariableValor(c, v.getDescripcion(), v.getUnidad(), valores.get(c));
        }).toList();
        return new PruebaFormulaDto(valor.orElse(null), vs, valor.isPresent() ? null
                : "Con los datos de hoy la fórmula no da un número (división por cero o una variable sin datos).");
    }

    // ---------------------------------------------------------------- mediciones automáticas

    /**
     * Mide los KPI automáticos activos del proyecto. Con origen RECALCULO guarda solo los que cambiaron de valor
     * o de semáforo (y anota la solicitud de cambio que gatilló el recálculo, si la hay). Con origen PROGRAMADA
     * guarda los que, según su periodicidad, ya debían medirse.
     */
    @Transactional
    public int medirProyecto(Long proyectoId, Long solicitudId, OrigenMedicion origen) {
        List<Kpi> activos = kpis.findByProyectoIdOrderByCodigoAsc(proyectoId).stream()
                .filter(k -> k.isActivo() && k.getFuente() == FuenteKpi.AUTOMATICA && k.getFormula() != null).toList();
        if (activos.isEmpty()) {
            return 0;
        }
        LocalDateTime ahora = LocalDateTime.now(ZONA);
        var datos = calculadora.datos(proyectoId, ahora.toLocalDate());
        Map<String, Map<String, BigDecimal>> cache = new HashMap<>();
        int guardadas = 0;
        for (Kpi k : activos) {
            Optional<MedicionKpi> ultima = mediciones.findFirstByKpiIdOrderByMedidoEnDescIdDesc(k.getId());
            if (origen == OrigenMedicion.PROGRAMADA) {
                var intervalo = k.getPeriodicidad().intervalo();
                if (intervalo == null || ultima.isPresent() && ultima.get().getMedidoEn().plus(intervalo).isAfter(ahora)) {
                    continue;
                }
            }
            Map<String, BigDecimal> valores = cache.computeIfAbsent(k.getAmbito() + ":" + k.getAmbitoRefId(),
                    x -> calculadora.calcular(datos, k.getAmbito(), k.getAmbitoRefId()));
            Optional<BigDecimal> valor = EvaluadorFormula.evaluar(k.getFormula(), k.getVariables(), valores);
            if (valor.isEmpty()) {
                continue;
            }
            Semaforo s = k.semaforo(valor.get());
            boolean cambio = ultima.isEmpty() || ultima.get().getValor().compareTo(valor.get()) != 0 || ultima.get().getSemaforo() != s;
            if (origen == OrigenMedicion.RECALCULO && !cambio) {
                continue;
            }
            guardar(k, valor.get(), s, origen, solicitudId, ahora);
            guardadas++;
        }
        mediciones.flush();
        return guardadas;
    }

    private void medir(Kpi k, Long solicitudId, OrigenMedicion origen, boolean siempre) {
        if (!k.isActivo() || k.getFuente() != FuenteKpi.AUTOMATICA || k.getFormula() == null) {
            return;
        }
        LocalDateTime ahora = LocalDateTime.now(ZONA);
        var datos = calculadora.datos(k.getProyectoId(), ahora.toLocalDate());
        Optional<BigDecimal> valor = EvaluadorFormula.evaluar(k.getFormula(), k.getVariables(),
                calculadora.calcular(datos, k.getAmbito(), k.getAmbitoRefId()));
        if (valor.isEmpty()) {
            return;
        }
        Semaforo s = k.semaforo(valor.get());
        Optional<MedicionKpi> ultima = mediciones.findFirstByKpiIdOrderByMedidoEnDescIdDesc(k.getId());
        if (siempre || ultima.isEmpty() || ultima.get().getValor().compareTo(valor.get()) != 0 || ultima.get().getSemaforo() != s) {
            guardar(k, valor.get(), s, origen, solicitudId, ahora);
            mediciones.flush();
        }
    }

    private void guardar(Kpi k, BigDecimal valor, Semaforo s, OrigenMedicion origen, Long solicitudId, LocalDateTime cuando) {
        var m = new MedicionKpi();
        m.setKpiId(k.getId());
        m.setMedidoEn(cuando);
        m.setValor(valor);
        m.setSemaforo(s);
        m.setOrigen(origen);
        m.setSolicitudId(solicitudId);
        mediciones.save(m);
    }

    // ---------------------------------------------------------------- reglas

    private void aplicar(Kpi k, KpiRequest req, String codigo) {
        if (req.ambito() == AmbitoKpi.CAMBIO && req.categoria() != CategoriaKpi.CAMBIO) {
            throw new ReglaNegocioException("AMBITO_INVALIDO", "Un KPI con ámbito «cambio» pertenece a la categoría CAMBIO.");
        }
        validarAmbito(k.getProyectoId(), req.ambito(), req.ambitoRefId());
        if (!usuarios.existsById(req.responsableId())) {
            throw new RecursoNoEncontradoException("Usuario", req.responsableId());
        }
        String formula = req.formula() == null || req.formula().isBlank() ? null : req.formula().trim();
        Set<String> usadas = new LinkedHashSet<>();
        if (formula != null) {
            Set<String> catalogo = variables.findAll().stream().map(VariableKpi::getCodigo).collect(Collectors.toSet());
            usadas = EvaluadorFormula.variables(formula, catalogo);
        } else if (req.fuente() == FuenteKpi.AUTOMATICA) {
            throw new ReglaNegocioException("FORMULA_INVALIDA", "Un KPI automático necesita una fórmula.");
        }
        if (req.verdeMin() == null && req.verdeMax() == null) {
            throw new ReglaNegocioException("UMBRAL_VERDE", "Indica al menos un límite del rango verde.");
        }
        if (req.sentido() == SentidoKpi.RANGO && (req.verdeMin() == null || req.verdeMax() == null)) {
            throw new ReglaNegocioException("UMBRAL_VERDE", "Un KPI de rango necesita el mínimo y el máximo del verde.");
        }
        if (req.verdeMin() != null && req.verdeMax() != null && req.verdeMin().compareTo(req.verdeMax()) > 0
                || req.ambarMin() != null && req.ambarMax() != null && req.ambarMin().compareTo(req.ambarMax()) > 0) {
            throw new ReglaNegocioException("UMBRAL_INVALIDO", "En cada rango el mínimo debe ser menor o igual que el máximo.");
        }
        k.setCodigo(codigo);
        k.setNombre(req.nombre().trim());
        k.setObjetivo(req.objetivo().trim());
        k.setCategoria(req.categoria());
        k.setAmbito(req.ambito());
        k.setAmbitoRefId(req.ambitoRefId());
        k.setFormula(formula);
        k.getVariables().clear();
        k.getVariables().addAll(usadas);
        k.setUnidad(req.unidad().trim());
        k.setSentido(req.sentido());
        k.setMeta(req.meta());
        k.setVerdeMin(req.verdeMin());
        k.setVerdeMax(req.verdeMax());
        k.setAmbarMin(req.ambarMin());
        k.setAmbarMax(req.ambarMax());
        k.setPeriodicidad(req.periodicidad());
        k.setFuente(req.fuente());
        k.setResponsableId(req.responsableId());
        if (req.activo() != null) {
            k.setActivo(req.activo());
        }
    }

    private void validarAmbito(Long proyectoId, AmbitoKpi ambito, Long ref) {
        switch (ambito) {
            case PROYECTO -> {
                if (ref != null) {
                    throw new ReglaNegocioException("AMBITO_INVALIDO", "El ámbito proyecto no lleva un elemento de referencia.");
                }
            }
            case ETAPA, ACTIVIDAD -> {
                if (ref == null) {
                    throw new ReglaNegocioException("AMBITO_INVALIDO", "Elige la " + (ambito == AmbitoKpi.ETAPA ? "etapa" : "actividad") + " que mide el KPI.");
                }
                Actividad a = actividades.findById(ref).filter(x -> x.getProyectoId().equals(proyectoId))
                        .orElseThrow(() -> new RecursoNoEncontradoException("Actividad", ref));
                if (ambito == AmbitoKpi.ETAPA && a.getTipo() != TipoActividad.ETAPA) {
                    throw new ReglaNegocioException("AMBITO_INVALIDO", "«" + a.getNombre() + "» no es una etapa.");
                }
            }
            case HITO -> {
                if (ref != null) {
                    hitos.findById(ref).filter(h -> h.getProyectoId().equals(proyectoId))
                            .orElseThrow(() -> new RecursoNoEncontradoException("Hito", ref));
                }
            }
            case CAMBIO -> {
                if (ref != null) {
                    solicitudes.findById(ref).filter(s -> s.getProyectoId().equals(proyectoId))
                            .orElseThrow(() -> new RecursoNoEncontradoException("Solicitud de cambio", ref));
                }
            }
        }
    }

    private void vincularHitos(Kpi k, List<Long> hitoIds) {
        if (hitoIds == null) {
            return;
        }
        Set<Long> validos = hitos.findByProyectoIdOrderByFechaComprometidaAscCodigoAsc(k.getProyectoId()).stream()
                .map(Hito::getId).collect(Collectors.toSet());
        jdbc.update("delete from kpi_hito where kpi_id = ?", k.getId());
        for (Long h : new LinkedHashSet<>(hitoIds)) {
            if (!validos.contains(h)) {
                throw new RecursoNoEncontradoException("Hito", h);
            }
            jdbc.update("insert into kpi_hito (kpi_id, hito_id) values (?, ?)", k.getId(), h);
        }
    }

    // ---------------------------------------------------------------- armado

    private record Nombres(Map<Long, String> usuarios, ArbolActividades arbol, Map<Long, Hito> hitos,
            Map<Long, SolicitudCambio> solicitudes) {
    }

    private Nombres nombres(Long proyectoId) {
        Map<Long, String> us = usuarios.findAll().stream().collect(Collectors.toMap(Usuario::getId, Usuario::getNombre));
        Map<Long, Hito> hs = hitos.findByProyectoIdOrderByFechaComprometidaAscCodigoAsc(proyectoId).stream()
                .collect(Collectors.toMap(Hito::getId, h -> h));
        Map<Long, SolicitudCambio> ss = solicitudes.findByProyectoIdOrderByCodigoAsc(proyectoId).stream()
                .collect(Collectors.toMap(SolicitudCambio::getId, s -> s));
        return new Nombres(us, ArbolActividades.de(actividades.findByProyectoIdOrderByOrdenAscIdAsc(proyectoId)), hs, ss);
    }

    private Map<Long, List<Long>> hitosPorKpi(Long proyectoId) {
        Map<Long, List<Long>> m = new HashMap<>();
        for (Object[] f : kpis.hitosDeKpis(proyectoId)) {
            m.computeIfAbsent(((Number) f[0]).longValue(), x -> new ArrayList<>()).add(((Number) f[1]).longValue());
        }
        return m;
    }

    private KpiDto dto(Kpi k, List<MedicionKpi> historial, Map<Long, List<Long>> hitosPorKpi, Nombres n) {
        List<MedicionKpi> orden = historial.stream()
                .sorted(Comparator.comparing(MedicionKpi::getMedidoEn).thenComparing(MedicionKpi::getId)).toList();
        MedicionKpi ultima = orden.isEmpty() ? null : orden.get(orden.size() - 1);
        MedicionKpi anterior = orden.size() < 2 ? null : orden.get(orden.size() - 2);
        List<MedicionDto> hs = orden.stream().map(m -> new MedicionDto(m.getId(), m.getMedidoEn(), m.getValor(), m.getSemaforo(),
                m.getOrigen(), m.getSolicitudId(),
                m.getSolicitudId() == null || !n.solicitudes().containsKey(m.getSolicitudId()) ? null
                        : n.solicitudes().get(m.getSolicitudId()).getCodigo(),
                m.getRegistradoPorId() == null ? null : n.usuarios().get(m.getRegistradoPorId()), m.getObservacion())).toList();
        return new KpiDto(k.getId(), k.getCodigo(), k.getNombre(), k.getObjetivo(), k.getCategoria(), k.getAmbito(),
                k.getAmbitoRefId(), nombreAmbito(k, n), k.getFormula(), k.getVariables(), k.getUnidad(), k.getSentido(),
                k.getMeta(), k.getVerdeMin(), k.getVerdeMax(), k.getAmbarMin(), k.getAmbarMax(), k.getPeriodicidad(),
                k.getFuente(), k.getResponsableId(), n.usuarios().get(k.getResponsableId()), k.isActivo(),
                ultima == null ? null : ultima.getValor(), ultima == null ? null : ultima.getSemaforo(),
                ultima == null ? null : ultima.getMedidoEn(), anterior == null ? null : anterior.getValor(), ultima == null,
                hitosPorKpi.getOrDefault(k.getId(), List.of()), hs);
    }

    private static String nombreAmbito(Kpi k, Nombres n) {
        Long ref = k.getAmbitoRefId();
        return switch (k.getAmbito()) {
            case PROYECTO -> "Proyecto completo";
            case ETAPA, ACTIVIDAD -> ref == null ? "—" : n.arbol().etiqueta(ref);
            case HITO -> ref == null ? "Todos los hitos"
                    : n.hitos().containsKey(ref) ? n.hitos().get(ref).getCodigo() + " " + n.hitos().get(ref).getDescripcion() : "—";
            case CAMBIO -> ref == null ? "Todas las solicitudes"
                    : n.solicitudes().containsKey(ref) ? n.solicitudes().get(ref).getCodigo() + " " + n.solicitudes().get(ref).getTitulo() : "—";
        };
    }

    private Kpi buscar(Long proyectoId, Long id) {
        return kpis.findById(id).filter(k -> k.getProyectoId().equals(proyectoId))
                .orElseThrow(() -> new RecursoNoEncontradoException("KPI", id));
    }
}
