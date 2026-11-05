package cl.duoc.gestionpro.planificacion.calendario;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DiaNoLaborableRepository extends JpaRepository<DiaNoLaborable, Long> {

    List<DiaNoLaborable> findByCalendarioIdOrderByFechaAsc(Long calendarioId);

    boolean existsByCalendarioIdAndFecha(Long calendarioId, LocalDate fecha);
}
