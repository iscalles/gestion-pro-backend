package cl.duoc.gestionpro.kpi;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface KpiRepository extends JpaRepository<Kpi, Long> {

    List<Kpi> findByProyectoIdOrderByCodigoAsc(Long proyectoId);

    boolean existsByProyectoIdAndCodigo(Long proyectoId, String codigo);

    /** Pares (kpi_id, hito_id) del proyecto. */
    @Query(value = "select kh.kpi_id, kh.hito_id from kpi_hito kh join kpi k on k.id = kh.kpi_id where k.proyecto_id = :proyectoId",
            nativeQuery = true)
    List<Object[]> hitosDeKpis(@Param("proyectoId") Long proyectoId);
}
