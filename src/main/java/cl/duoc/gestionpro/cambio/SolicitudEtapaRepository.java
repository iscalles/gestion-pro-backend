package cl.duoc.gestionpro.cambio;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SolicitudEtapaRepository extends JpaRepository<SolicitudEtapa, Long> {

    List<SolicitudEtapa> findBySolicitudIdOrderByEtapaNumeroAsc(Long solicitudId);

    @Query("select e from SolicitudEtapa e where e.solicitudId in "
            + "(select s.id from SolicitudCambio s where s.proyectoId = :proyectoId) order by e.solicitudId, e.etapaNumero")
    List<SolicitudEtapa> deProyecto(@Param("proyectoId") Long proyectoId);
}
