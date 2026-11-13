package cl.duoc.gestionpro.recurso;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RequerimientoProfesionalRepository extends JpaRepository<RequerimientoProfesional, Long> {

    List<RequerimientoProfesional> findByProyectoIdOrderByIdAsc(Long proyectoId);

    boolean existsByProyectoIdAndTipoProfesionalId(Long proyectoId, Long tipoProfesionalId);
}
