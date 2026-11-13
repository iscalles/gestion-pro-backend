package cl.duoc.gestionpro.cambio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import cl.duoc.gestionpro.cambio.dto.OpcionModificacionDto;
import cl.duoc.gestionpro.comun.RecursoNoEncontradoException;
import cl.duoc.gestionpro.comun.ReglaNegocioException;
import cl.duoc.gestionpro.hito.Hito;
import cl.duoc.gestionpro.hito.HitoRepository;
import cl.duoc.gestionpro.planificacion.Actividad;
import cl.duoc.gestionpro.planificacion.ActividadRepository;
import cl.duoc.gestionpro.planificacion.ArbolActividades;
import cl.duoc.gestionpro.planificacion.Dependencia;
import cl.duoc.gestionpro.planificacion.DependenciaRepository;
import cl.duoc.gestionpro.planificacion.TipoDependencia;
import cl.duoc.gestionpro.proyecto.Proyecto;
import cl.duoc.gestionpro.proyecto.ProyectoRepository;
import cl.duoc.gestionpro.recurso.AsignacionProfesional;
import cl.duoc.gestionpro.recurso.AsignacionProfesionalRepository;
import cl.duoc.gestionpro.recurso.AsignacionRecurso;
import cl.duoc.gestionpro.recurso.AsignacionRecursoRepository;
import cl.duoc.gestionpro.recurso.ProfesionalRepository;
import cl.duoc.gestionpro.recurso.Recurso;
import cl.duoc.gestionpro.recurso.RecursoRepository;
import cl.duoc.gestionpro.recurso.RecursoService;

/**
 * Sabe leer, validar y aplicar cada modificación que una solicitud de cambio puede proponer.
 * Campos admitidos:
 * <ul>
 * <li>ACTIVIDAD.duracion_plan (solo actividades sin hijos)</li>
 * <li>DEPENDENCIA.desfase_dias y DEPENDENCIA.tipo</li>
 * <li>ASIGNACION_PROFESIONAL.horas_semana</li>
 * <li>ASIGNACION_RECURSO.cantidad</li>
 * <li>RECURSO.cantidad y RECURSO.costo_unitario</li>
 * <li>HITO.fecha_comprometida</li>
 * <li>PROYECTO.presupuesto y PROYECTO.fecha_termino_plan</li>
 * </ul>
 */
@Component
public class AplicadorModificaciones {

    private static final Map<String, String[]> CAMPOS = Map.of(
            "ACTIVIDAD.duracion_plan", new String[] { "Duración (días hábiles)", "ENTERO" },
            "DEPENDENCIA.desfase_dias", new String[] { "Desfase (días hábiles)", "ENTERO" },
            "DEPENDENCIA.tipo", new String[] { "Tipo de vínculo", "LISTA" },
            "ASIGNACION_PROFESIONAL.horas_semana", new String[] { "Horas por semana", "DECIMAL" },
            "ASIGNACION_RECURSO.cantidad", new String[] { "Cantidad asignada", "DECIMAL" },
            "RECURSO.cantidad", new String[] { "Cantidad disponible", "DECIMAL" },
            "RECURSO.costo_unitario", new String[] { "Costo unitario", "DECIMAL" },
            "HITO.fecha_comprometida", new String[] { "Fecha comprometida", "FECHA" },
            "PROYECTO.presupuesto", new String[] { "Presupuesto", "DECIMAL" },
            "PROYECTO.fecha_termino_plan", new String[] { "Fecha de término planificada", "FECHA" });

    private final ProyectoRepository proyectos;
    private final ActividadRepository actividades;
    private final DependenciaRepository dependencias;
    private final AsignacionProfesionalRepository asignacionesProfesional;
    private final AsignacionRecursoRepository asignacionesRecurso;
    private final RecursoRepository recursos;
    private final ProfesionalRepository profesionales;
    private final HitoRepository hitos;
    private final RecursoService recursoService;

    public AplicadorModificaciones(ProyectoRepository proyectos, ActividadRepository actividades,
            DependenciaRepository dependencias, AsignacionProfesionalRepository asignacionesProfesional,
            AsignacionRecursoRepository asignacionesRecurso, RecursoRepository recursos, ProfesionalRepository profesionales,
            HitoRepository hitos, RecursoService recursoService) {
        this.proyectos = proyectos;
        this.actividades = actividades;
        this.dependencias = dependencias;
        this.asignacionesProfesional = asignacionesProfesional;
        this.asignacionesRecurso = asignacionesRecurso;
        this.recursos = recursos;
        this.profesionales = profesionales;
        this.hitos = hitos;
        this.recursoService = recursoService;
    }

    public static String nombreCampo(EntidadModificada entidad, String campo) {
        String[] c = CAMPOS.get(entidad + "." + campo);
        return c == null ? campo : c[0];
    }

    /** Valida que el campo exista, que el elemento sea del proyecto y que el valor tenga el formato correcto. */
    public void validar(Long proyectoId, EntidadModificada entidad, Long entidadId, String campo, String valor) {
        if (!CAMPOS.containsKey(entidad + "." + campo)) {
            throw new ReglaNegocioException("CAMPO_NO_MODIFICABLE", "El campo «" + campo + "» de " + entidad.name().toLowerCase()
                    + " no se puede modificar con una solicitud de cambio.");
        }
        Object actual = entidad(proyectoId, entidad, entidadId);
        if (actual instanceof Actividad a && actividades.existsByPadreId(a.getId())) {
            throw new ReglaNegocioException("SOLO_HOJAS", "«" + a.getNombre() + "» tiene subactividades: su duración se deriva de ellas.");
        }
        switch (CAMPOS.get(entidad + "." + campo)[1]) {
            case "ENTERO" -> {
                int n = entero(valor);
                if ("duracion_plan".equals(campo) && n < 0) {
                    throw new ReglaNegocioException("VALOR_INVALIDO", "La duración no puede ser negativa.");
                }
            }
            case "DECIMAL" -> {
                BigDecimal n = decimal(valor);
                boolean cero = n.signum() == 0;
                if (n.signum() < 0 || cero && !("costo_unitario".equals(campo) || "presupuesto".equals(campo))) {
                    throw new ReglaNegocioException("VALOR_INVALIDO", "El valor debe ser mayor a cero.");
                }
                if ("horas_semana".equals(campo) && n.compareTo(BigDecimal.valueOf(168)) > 0) {
                    throw new ReglaNegocioException("VALOR_INVALIDO", "Una semana tiene 168 horas.");
                }
            }
            case "FECHA" -> fecha(valor);
            case "LISTA" -> {
                try {
                    TipoDependencia.valueOf(valor.trim().toUpperCase());
                } catch (IllegalArgumentException ex) {
                    throw new ReglaNegocioException("VALOR_INVALIDO", "El tipo de vínculo debe ser FS, SS, FF o SF.");
                }
            }
            default -> throw new IllegalStateException();
        }
    }

    /** Valor vigente del campo, como texto. */
    public String valorActual(Long proyectoId, EntidadModificada entidad, Long entidadId, String campo) {
        Object e;
        try {
            e = entidad(proyectoId, entidad, entidadId);
        } catch (RecursoNoEncontradoException ex) {
            return null;
        }
        return switch (entidad + "." + campo) {
            case "ACTIVIDAD.duracion_plan" -> String.valueOf(((Actividad) e).getDuracionPlan());
            case "DEPENDENCIA.desfase_dias" -> String.valueOf(((Dependencia) e).getDesfaseDias());
            case "DEPENDENCIA.tipo" -> ((Dependencia) e).getTipo().name();
            case "ASIGNACION_PROFESIONAL.horas_semana" -> texto(((AsignacionProfesional) e).getHorasSemana());
            case "ASIGNACION_RECURSO.cantidad" -> texto(((AsignacionRecurso) e).getCantidad());
            case "RECURSO.cantidad" -> texto(((Recurso) e).getCantidad());
            case "RECURSO.costo_unitario" -> texto(((Recurso) e).getCostoUnitario());
            case "HITO.fecha_comprometida" -> ((Hito) e).getFechaComprometida().toString();
            case "PROYECTO.presupuesto" -> texto(((Proyecto) e).getPresupuesto());
            case "PROYECTO.fecha_termino_plan" -> ((Proyecto) e).getFechaTerminoPlan().toString();
            default -> null;
        };
    }

    /** Aplica la modificación y devuelve el valor que había antes. */
    public String aplicar(Long proyectoId, CambioModificacion m) {
        validar(proyectoId, m.getEntidad(), m.getEntidadId(), m.getCampo(), m.getValorNuevo());
        String anterior = valorActual(proyectoId, m.getEntidad(), m.getEntidadId(), m.getCampo());
        Object e = entidad(proyectoId, m.getEntidad(), m.getEntidadId());
        String v = m.getValorNuevo().trim();
        switch (m.getEntidad() + "." + m.getCampo()) {
            case "ACTIVIDAD.duracion_plan" -> ((Actividad) e).setDuracionPlan(entero(v));
            case "DEPENDENCIA.desfase_dias" -> ((Dependencia) e).setDesfaseDias(entero(v));
            case "DEPENDENCIA.tipo" -> ((Dependencia) e).setTipo(TipoDependencia.valueOf(v.toUpperCase()));
            case "ASIGNACION_PROFESIONAL.horas_semana" -> ((AsignacionProfesional) e).setHorasSemana(decimal(v));
            case "ASIGNACION_RECURSO.cantidad" -> ((AsignacionRecurso) e).setCantidad(decimal(v));
            case "RECURSO.cantidad" -> ((Recurso) e).setCantidad(decimal(v));
            case "RECURSO.costo_unitario" -> {
                Recurso r = (Recurso) e;
                r.setCostoUnitario(decimal(v));
                asignacionesRecurso.findByRecursoId(r.getId()).forEach(a -> a.setCostoUnitario(r.getCostoUnitario()));
            }
            case "HITO.fecha_comprometida" -> ((Hito) e).setFechaComprometida(fecha(v));
            case "PROYECTO.presupuesto" -> ((Proyecto) e).setPresupuesto(decimal(v));
            case "PROYECTO.fecha_termino_plan" -> {
                Proyecto p = (Proyecto) e;
                LocalDate f = fecha(v);
                if (f.isBefore(p.getFechaInicioPlan())) {
                    throw new ReglaNegocioException("VALOR_INVALIDO", "El término no puede ser anterior al inicio del proyecto.");
                }
                p.setFechaTerminoPlan(f);
            }
            default -> throw new IllegalStateException();
        }
        return anterior;
    }

    /**
     * Tras aplicar todas las modificaciones, la capacidad de cada recurso tocado debe cubrir lo asignado
     * (se valida al final para que el orden de las modificaciones no importe).
     */
    public void validarRecursos(Long proyectoId, List<CambioModificacion> aplicadas) {
        Set<Long> ids = new HashSet<>();
        for (CambioModificacion m : aplicadas) {
            if (m.getEntidad() == EntidadModificada.RECURSO) {
                ids.add(m.getEntidadId());
            } else if (m.getEntidad() == EntidadModificada.ASIGNACION_RECURSO) {
                asignacionesRecurso.findById(m.getEntidadId()).ifPresent(a -> ids.add(a.getRecursoId()));
            }
        }
        for (Long id : ids) {
            recursos.findById(id).ifPresent(recursoService::validarCapacidad);
        }
    }

    /** Nombre legible del elemento modificado. */
    public String nombre(Long proyectoId, EntidadModificada entidad, Long entidadId, ArbolActividades arbol) {
        try {
            Object e = entidad(proyectoId, entidad, entidadId);
            return switch (entidad) {
                case ACTIVIDAD -> arbol.etiqueta(entidadId);
                case DEPENDENCIA -> {
                    Dependencia d = (Dependencia) e;
                    yield arbol.get(d.getPredecesoraId()).getNombre() + " → " + arbol.get(d.getSucesoraId()).getNombre();
                }
                case ASIGNACION_PROFESIONAL -> {
                    AsignacionProfesional a = (AsignacionProfesional) e;
                    yield profesionales.findById(a.getProfesionalId()).map(p -> p.getNombre()).orElse("?") + " en "
                            + arbol.get(a.getActividadId()).getNombre();
                }
                case ASIGNACION_RECURSO -> {
                    AsignacionRecurso a = (AsignacionRecurso) e;
                    yield recursos.findById(a.getRecursoId()).map(Recurso::getDescripcion).orElse("?") + " en "
                            + arbol.get(a.getActividadId()).getNombre();
                }
                case RECURSO -> ((Recurso) e).getDescripcion();
                case HITO -> ((Hito) e).getCodigo() + " " + ((Hito) e).getDescripcion();
                case PROYECTO -> ((Proyecto) e).getCodigo() + " " + ((Proyecto) e).getNombre();
            };
        } catch (RecursoNoEncontradoException ex) {
            return "(eliminado)";
        }
    }

    /** Todo lo que se puede modificar en el proyecto, para armar el formulario del frontend. */
    public List<OpcionModificacionDto> opciones(Long proyectoId) {
        Proyecto p = proyectos.findById(proyectoId).orElseThrow(() -> new RecursoNoEncontradoException("Proyecto", proyectoId));
        ArbolActividades arbol = ArbolActividades.de(actividades.findByProyectoIdOrderByOrdenAscIdAsc(proyectoId));
        List<OpcionModificacionDto> salida = new ArrayList<>();
        salida.add(opcion(proyectoId, EntidadModificada.PROYECTO, p.getId(), p.getCodigo() + " " + p.getNombre(),
                "presupuesto", "fecha_termino_plan"));
        for (Actividad a : arbol.hojas()) {
            salida.add(opcion(proyectoId, EntidadModificada.ACTIVIDAD, a.getId(), arbol.etiqueta(a.getId()), "duracion_plan"));
        }
        for (Dependencia d : dependencias.findByProyectoIdOrderByIdAsc(proyectoId)) {
            salida.add(opcion(proyectoId, EntidadModificada.DEPENDENCIA, d.getId(), nombre(proyectoId, EntidadModificada.DEPENDENCIA,
                    d.getId(), arbol), "tipo", "desfase_dias"));
        }
        for (AsignacionProfesional a : asignacionesProfesional.deProyecto(proyectoId)) {
            salida.add(opcion(proyectoId, EntidadModificada.ASIGNACION_PROFESIONAL, a.getId(),
                    nombre(proyectoId, EntidadModificada.ASIGNACION_PROFESIONAL, a.getId(), arbol), "horas_semana"));
        }
        for (AsignacionRecurso a : asignacionesRecurso.deProyecto(proyectoId)) {
            salida.add(opcion(proyectoId, EntidadModificada.ASIGNACION_RECURSO, a.getId(),
                    nombre(proyectoId, EntidadModificada.ASIGNACION_RECURSO, a.getId(), arbol), "cantidad"));
        }
        for (Recurso r : recursos.findByProyectoIdOrderByTipoAscDescripcionAsc(proyectoId)) {
            salida.add(opcion(proyectoId, EntidadModificada.RECURSO, r.getId(), r.getDescripcion(), "cantidad", "costo_unitario"));
        }
        for (Hito h : hitos.findByProyectoIdOrderByFechaComprometidaAscCodigoAsc(proyectoId)) {
            salida.add(opcion(proyectoId, EntidadModificada.HITO, h.getId(), h.getCodigo() + " " + h.getDescripcion(), "fecha_comprometida"));
        }
        return salida;
    }

    private OpcionModificacionDto opcion(Long proyectoId, EntidadModificada entidad, Long id, String nombre, String... campos) {
        List<OpcionModificacionDto.Campo> cs = new ArrayList<>();
        for (String c : campos) {
            String[] def = CAMPOS.get(entidad + "." + c);
            cs.add(new OpcionModificacionDto.Campo(c, def[0], def[1], valorActual(proyectoId, entidad, id, c),
                    "LISTA".equals(def[1]) ? List.of("FS", "SS", "FF", "SF") : null));
        }
        return new OpcionModificacionDto(entidad, id, nombre, cs);
    }

    private Object entidad(Long proyectoId, EntidadModificada entidad, Long id) {
        RecursoNoEncontradoException noExiste = new RecursoNoEncontradoException(etiqueta(entidad), id);
        return switch (entidad) {
            case ACTIVIDAD -> actividades.findById(id).filter(a -> a.getProyectoId().equals(proyectoId)).orElseThrow(() -> noExiste);
            case DEPENDENCIA -> dependencias.findById(id).filter(d -> d.getProyectoId().equals(proyectoId)).orElseThrow(() -> noExiste);
            case ASIGNACION_PROFESIONAL -> {
                AsignacionProfesional a = asignacionesProfesional.findById(id).orElseThrow(() -> noExiste);
                actividades.findById(a.getActividadId()).filter(x -> x.getProyectoId().equals(proyectoId)).orElseThrow(() -> noExiste);
                yield a;
            }
            case ASIGNACION_RECURSO -> {
                AsignacionRecurso a = asignacionesRecurso.findById(id).orElseThrow(() -> noExiste);
                actividades.findById(a.getActividadId()).filter(x -> x.getProyectoId().equals(proyectoId)).orElseThrow(() -> noExiste);
                yield a;
            }
            case RECURSO -> recursos.findById(id).filter(r -> r.getProyectoId().equals(proyectoId)).orElseThrow(() -> noExiste);
            case HITO -> hitos.findById(id).filter(h -> h.getProyectoId().equals(proyectoId)).orElseThrow(() -> noExiste);
            case PROYECTO -> {
                if (!id.equals(proyectoId)) {
                    throw noExiste;
                }
                yield proyectos.findById(id).orElseThrow(() -> noExiste);
            }
        };
    }

    private static String etiqueta(EntidadModificada e) {
        Map<EntidadModificada, String> m = new HashMap<>();
        m.put(EntidadModificada.ACTIVIDAD, "Actividad");
        m.put(EntidadModificada.DEPENDENCIA, "Dependencia");
        m.put(EntidadModificada.ASIGNACION_PROFESIONAL, "Asignación de profesional");
        m.put(EntidadModificada.ASIGNACION_RECURSO, "Asignación de recurso");
        m.put(EntidadModificada.RECURSO, "Recurso");
        m.put(EntidadModificada.HITO, "Hito");
        m.put(EntidadModificada.PROYECTO, "Proyecto");
        return m.get(e);
    }

    private static int entero(String v) {
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException ex) {
            throw new ReglaNegocioException("VALOR_INVALIDO", "«" + v + "» no es un número entero.");
        }
    }

    private static BigDecimal decimal(String v) {
        try {
            return new BigDecimal(v.trim().replace(',', '.'));
        } catch (NumberFormatException ex) {
            throw new ReglaNegocioException("VALOR_INVALIDO", "«" + v + "» no es un número.");
        }
    }

    private static LocalDate fecha(String v) {
        try {
            return LocalDate.parse(v.trim());
        } catch (DateTimeParseException ex) {
            throw new ReglaNegocioException("VALOR_INVALIDO", "«" + v + "» no es una fecha (AAAA-MM-DD).");
        }
    }

    private static String texto(BigDecimal n) {
        return n.stripTrailingZeros().toPlainString();
    }
}
