package cl.duoc.gestionpro.hito;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HitoRepository extends JpaRepository<Hito, Long> {

    @EntityGraph(attributePaths = "responsable")
    List<Hito> findByProyectoIdOrderByFechaComprometidaAscCodigoAsc(Long proyectoId);

    /** Pares (hito_id, actividad_id) del proyecto. */
    @Query(value = "select hito_id, actividad_id from hito_actividad where proyecto_id = :proyectoId", nativeQuery = true)
    List<Object[]> actividadesDeHitos(@Param("proyectoId") Long proyectoId);
}
