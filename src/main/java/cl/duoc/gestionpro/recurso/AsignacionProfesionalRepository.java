package cl.duoc.gestionpro.recurso;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AsignacionProfesionalRepository extends JpaRepository<AsignacionProfesional, Long> {

    @Query("select a from AsignacionProfesional a where a.actividadId in "
            + "(select x.id from Actividad x where x.proyectoId = :proyectoId) order by a.actividadId, a.id")
    List<AsignacionProfesional> deProyecto(@Param("proyectoId") Long proyectoId);

    boolean existsByActividadIdAndProfesionalId(Long actividadId, Long profesionalId);

    boolean existsByProfesionalId(Long profesionalId);

    boolean existsByActividadId(Long actividadId);
}
