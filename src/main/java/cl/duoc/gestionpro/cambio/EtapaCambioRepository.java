package cl.duoc.gestionpro.cambio;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EtapaCambioRepository extends JpaRepository<EtapaCambio, Integer> {

    List<EtapaCambio> findAllByOrderByNumeroAsc();
}
