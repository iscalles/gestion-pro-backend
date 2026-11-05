package cl.duoc.gestionpro.planificacion.calendario;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CalendarioRepository extends JpaRepository<Calendario, Long> {

    List<Calendario> findAllByOrderByNombreAsc();
}
