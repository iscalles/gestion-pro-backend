package cl.duoc.gestionpro.hito;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HitoCriterioRepository extends JpaRepository<HitoCriterio, Long> {

    List<HitoCriterio> findByHitoIdOrderByOrdenAscIdAsc(Long hitoId);

    @Query("select c from HitoCriterio c where c.hitoId in (select h.id from Hito h where h.proyectoId = :proyectoId) "
            + "order by c.hitoId, c.orden, c.id")
    List<HitoCriterio> deProyecto(@Param("proyectoId") Long proyectoId);
}
