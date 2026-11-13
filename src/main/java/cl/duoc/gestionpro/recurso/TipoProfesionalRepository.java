package cl.duoc.gestionpro.recurso;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TipoProfesionalRepository extends JpaRepository<TipoProfesional, Long> {

    List<TipoProfesional> findAllByOrderByCategoriaAsc();

    boolean existsByCategoriaIgnoreCase(String categoria);
}
