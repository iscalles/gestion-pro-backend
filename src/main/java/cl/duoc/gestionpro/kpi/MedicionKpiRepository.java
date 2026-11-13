package cl.duoc.gestionpro.kpi;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MedicionKpiRepository extends JpaRepository<MedicionKpi, Long> {

    List<MedicionKpi> findByKpiIdOrderByMedidoEnAscIdAsc(Long kpiId);

    Optional<MedicionKpi> findFirstByKpiIdOrderByMedidoEnDescIdDesc(Long kpiId);

    @Query("select m from MedicionKpi m where m.kpiId in (select k.id from Kpi k where k.proyectoId = :proyectoId) "
            + "order by m.kpiId, m.medidoEn, m.id")
    List<MedicionKpi> deProyecto(@Param("proyectoId") Long proyectoId);
}
