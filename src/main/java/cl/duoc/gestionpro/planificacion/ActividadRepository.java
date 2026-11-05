package cl.duoc.gestionpro.planificacion;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ActividadRepository extends JpaRepository<Actividad, Long> {

    @EntityGraph(attributePaths = "responsable")
    List<Actividad> findByProyectoIdOrderByOrdenAscIdAsc(Long proyectoId);

    boolean existsByPadreId(Long padreId);

    /** Hay alguna hoja con avance menor a 100 %. */
    @Query("""
            select count(a) > 0 from Actividad a
             where a.proyectoId = :proyectoId and a.avance < 100
               and not exists (select 1 from Actividad h where h.padreId = a.id)
            """)
    boolean existsAbiertasEnProyecto(@Param("proyectoId") Long proyectoId);

    @Query("select distinct a.proyectoId from Actividad a where a.fechaInicioPlan is null")
    List<Long> proyectosSinProgramar();

    @Query("select coalesce(max(a.orden), 0) from Actividad a where a.proyectoId = :proyectoId and "
            + "((:padreId is null and a.padreId is null) or a.padreId = :padreId)")
    int maxOrden(@Param("proyectoId") Long proyectoId, @Param("padreId") Long padreId);
}
