package cl.duoc.gestionpro.proyecto;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProyectoMiembroRepository extends JpaRepository<ProyectoMiembro, ProyectoMiembro.Clave> {

    List<ProyectoMiembro> findByProyectoId(Long proyectoId);

    void deleteByProyectoId(Long proyectoId);
}
