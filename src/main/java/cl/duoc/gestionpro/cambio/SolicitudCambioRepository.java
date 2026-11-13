package cl.duoc.gestionpro.cambio;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SolicitudCambioRepository extends JpaRepository<SolicitudCambio, Long> {

    List<SolicitudCambio> findByProyectoIdOrderByCodigoAsc(Long proyectoId);

    @Query("select count(s) > 0 from SolicitudCambio s where s.proyectoId = :proyectoId and s.estado not in "
            + "(cl.duoc.gestionpro.cambio.EstadoSolicitud.CERRADO, cl.duoc.gestionpro.cambio.EstadoSolicitud.RECHAZADO)")
    boolean existenAbiertas(@Param("proyectoId") Long proyectoId);

    @Query("select s.codigo from SolicitudCambio s where s.proyectoId = :proyectoId")
    List<String> codigos(@Param("proyectoId") Long proyectoId);
}
