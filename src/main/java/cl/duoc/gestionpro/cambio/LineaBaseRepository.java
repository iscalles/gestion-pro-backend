package cl.duoc.gestionpro.cambio;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LineaBaseRepository extends JpaRepository<LineaBase, Long> {

    List<LineaBase> findByProyectoIdOrderByNumeroAsc(Long proyectoId);

    Optional<LineaBase> findFirstByProyectoIdOrderByNumeroDesc(Long proyectoId);

    Optional<LineaBase> findBySolicitudId(Long solicitudId);

    boolean existsByProyectoId(Long proyectoId);
}
