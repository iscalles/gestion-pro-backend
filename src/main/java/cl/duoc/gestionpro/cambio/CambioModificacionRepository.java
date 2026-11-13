package cl.duoc.gestionpro.cambio;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CambioModificacionRepository extends JpaRepository<CambioModificacion, Long> {

    List<CambioModificacion> findBySolicitudIdOrderByIdAsc(Long solicitudId);

    @Query("select m from CambioModificacion m where m.solicitudId in "
            + "(select s.id from SolicitudCambio s where s.proyectoId = :proyectoId) order by m.fechaAplicacion, m.id")
    List<CambioModificacion> deProyecto(@Param("proyectoId") Long proyectoId);
}
