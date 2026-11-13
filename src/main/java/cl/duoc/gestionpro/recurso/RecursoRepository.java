package cl.duoc.gestionpro.recurso;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RecursoRepository extends JpaRepository<Recurso, Long> {

    List<Recurso> findByProyectoIdOrderByTipoAscDescripcionAsc(Long proyectoId);
}
