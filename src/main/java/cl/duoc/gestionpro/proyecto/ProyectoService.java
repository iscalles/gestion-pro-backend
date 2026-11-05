package cl.duoc.gestionpro.proyecto;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.duoc.gestionpro.comun.RecursoNoEncontradoException;
import cl.duoc.gestionpro.comun.ReglaNegocioException;
import cl.duoc.gestionpro.proyecto.dto.ProyectoDto;
import cl.duoc.gestionpro.proyecto.dto.ProyectoRequest;
import cl.duoc.gestionpro.planificacion.ActividadRepository;
import cl.duoc.gestionpro.recalculo.RecalculoService;
import cl.duoc.gestionpro.seguridad.UsuarioRepository;

@Service
public class ProyectoService {

    private final ProyectoRepository proyectos;
    private final UsuarioRepository usuarios;
    private final ActividadRepository actividades;
    private final RecalculoService recalculo;

    public ProyectoService(ProyectoRepository proyectos, UsuarioRepository usuarios, ActividadRepository actividades,
            RecalculoService recalculo) {
        this.proyectos = proyectos;
        this.usuarios = usuarios;
        this.actividades = actividades;
        this.recalculo = recalculo;
    }

    @Transactional(readOnly = true)
    public List<ProyectoDto> listar(Long usuarioId, boolean esAdmin) {
        var lista = esAdmin ? proyectos.findAllByOrderByCodigoAsc() : proyectos.visiblesPara(usuarioId);
        return lista.stream().map(ProyectoDto::de).toList();
    }

    @Transactional(readOnly = true)
    public ProyectoDto obtener(Long id) {
        return ProyectoDto.de(buscar(id));
    }

    @Transactional
    public ProyectoDto crear(ProyectoRequest req) {
        if (proyectos.existsByCodigo(req.codigo())) {
            throw new ReglaNegocioException("CODIGO_DUPLICADO", "Ya existe un proyecto con el código " + req.codigo() + ".");
        }
        var p = new Proyecto();
        aplicar(p, req);
        return ProyectoDto.de(proyectos.save(p));
    }

    @Transactional
    public ProyectoDto actualizar(Long id, ProyectoRequest req) {
        var p = buscar(id);
        aplicar(p, req);
        if (p.getEstado() == EstadoProyecto.TERMINADO && actividades.existsAbiertasEnProyecto(id)) {
            throw new ReglaNegocioException("ACTIVIDADES_ABIERTAS",
                    "No puedes cerrar el proyecto: tiene actividades sin terminar.");
        }
        proyectos.flush();
        recalculo.recalcularProyecto(id); // la fecha de inicio o el calendario pueden haber cambiado
        return ProyectoDto.de(p);
    }

    private void aplicar(Proyecto p, ProyectoRequest req) {
        p.setCodigo(req.codigo().trim());
        p.setNombre(req.nombre().trim());
        p.setDescripcion(req.descripcion());
        p.setResponsable(usuarios.findById(req.responsableId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario", req.responsableId())));
        p.setCalendarioId(req.calendarioId());
        p.setFechaInicioPlan(req.fechaInicio());
        p.setFechaTerminoPlan(req.fechaTermino());
        p.setPresupuesto(req.presupuesto());
        if (req.estado() != null) {
            p.setEstado(req.estado());
        }
        // TODO (Módulo 8): impedir TERMINADO con solicitudes de cambio sin cerrar.
    }

    private Proyecto buscar(Long id) {
        return proyectos.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Proyecto", id));
    }
}
