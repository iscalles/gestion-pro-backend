package cl.duoc.gestionpro.planificacion;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DependenciaRepository extends JpaRepository<Dependencia, Long> {

    List<Dependencia> findByProyectoIdOrderByIdAsc(Long proyectoId);

    boolean existsByPredecesoraIdOrSucesoraId(Long predecesoraId, Long sucesoraId);

    boolean existsByPredecesoraIdAndSucesoraId(Long predecesoraId, Long sucesoraId);

    long countBySucesoraIdIn(Collection<Long> ids);
}
