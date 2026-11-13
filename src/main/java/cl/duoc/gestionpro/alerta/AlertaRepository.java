package cl.duoc.gestionpro.alerta;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertaRepository extends JpaRepository<Alerta, Long> {

    List<Alerta> findByProyectoIdAndResueltaFalseOrderByCreadaEnDescIdDesc(Long proyectoId);

    List<Alerta> findByProyectoIdOrderByCreadaEnDescIdDesc(Long proyectoId);
}
