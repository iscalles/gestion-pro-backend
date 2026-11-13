package cl.duoc.gestionpro.cambio;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccionCorrectivaRepository extends JpaRepository<AccionCorrectiva, Long> {

    List<AccionCorrectiva> findBySolicitudIdOrderByFechaCompromisoAscIdAsc(Long solicitudId);

    @Query("select a from AccionCorrectiva a where a.solicitudId in "
            + "(select s.id from SolicitudCambio s where s.proyectoId = :proyectoId) order by a.fechaCompromiso, a.id")
    List<AccionCorrectiva> deProyecto(@Param("proyectoId") Long proyectoId);
}
