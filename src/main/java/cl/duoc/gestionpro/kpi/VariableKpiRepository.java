package cl.duoc.gestionpro.kpi;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VariableKpiRepository extends JpaRepository<VariableKpi, String> {

    List<VariableKpi> findAllByOrderByCategoriaAscCodigoAsc();
}
