package cl.duoc.gestionpro.proyecto;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.duoc.gestionpro.comun.RecursoNoEncontradoException;
import cl.duoc.gestionpro.comun.ReglaNegocioException;
import cl.duoc.gestionpro.proyecto.dto.ProyectoDto;
import cl.duoc.gestionpro.proyecto.dto.ProyectoRequest;
import cl.duoc.gestionpro.seguridad.UsuarioRepository;

@Service
public class ProyectoService {

    private final ProyectoRepository proyectos;
    private final UsuarioRepository usuarios;

    public ProyectoService(ProyectoRepository proyectos, UsuarioRepository usuarios) {
        this.proyectos = proyectos;
        this.usuarios = usuarios;
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
        // TODO: al terminar el módulo de actividades, impedir TERMINADO con actividades abiertas
        // o solicitudes de cambio sin cerrar, y llamar al servicio de recálculo.
    }

    private Proyecto buscar(Long id) {
        return proyectos.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Proyecto", id));
    }
}
