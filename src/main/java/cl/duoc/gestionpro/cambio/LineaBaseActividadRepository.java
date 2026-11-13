package cl.duoc.gestionpro.cambio;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LineaBaseActividadRepository extends JpaRepository<LineaBaseActividad, LineaBaseActividad.Clave> {

    List<LineaBaseActividad> findByLineaBaseId(Long lineaBaseId);
}
