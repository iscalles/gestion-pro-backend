package cl.duoc.gestionpro.proyecto;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import cl.duoc.gestionpro.comun.RecursoNoEncontradoException;

/**
 * Quién puede trabajar en un proyecto: el administrador en todos; el usuario de proyecto en los que dirige
 * o en los que es miembro (pueden verlos y editar su planificación).
 */
@Component
public class AccesoProyecto {

    private final ProyectoRepository proyectos;

    public AccesoProyecto(ProyectoRepository proyectos) {
        this.proyectos = proyectos;
    }

    @Transactional(readOnly = true)
    public void verificar(Jwt jwt, Long proyectoId) {
        if (!proyectos.existsById(proyectoId)) {
            throw new RecursoNoEncontradoException("Proyecto", proyectoId);
        }
        if ("ADMINISTRADOR".equals(jwt.getClaimAsString("rol"))) {
            return;
        }
        if (!proyectos.esVisiblePara(proyectoId, Long.valueOf(jwt.getSubject()))) {
            throw new AccessDeniedException("Sin acceso al proyecto " + proyectoId);
        }
    }
}
