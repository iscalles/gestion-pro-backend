package cl.duoc.gestionpro.proyecto;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProyectoRepository extends JpaRepository<Proyecto, Long> {

    @EntityGraph(attributePaths = "responsable")
    List<Proyecto> findAllByOrderByCodigoAsc();

    /** Proyectos visibles para un usuario de proyecto: los que dirige o en los que participa. */
    @EntityGraph(attributePaths = "responsable")
    @Query("""
            select p from Proyecto p
             where p.responsable.id = :usuarioId
                or p.id in (select m.proyectoId from ProyectoMiembro m where m.usuarioId = :usuarioId)
             order by p.codigo
            """)
    List<Proyecto> visiblesPara(@Param("usuarioId") Long usuarioId);

    boolean existsByCodigo(String codigo);

    List<Proyecto> findByCalendarioId(Long calendarioId);

    /** El usuario dirige el proyecto o es miembro de él. */
    @Query("""
            select count(p) > 0 from Proyecto p
             where p.id = :proyectoId
               and (p.responsable.id = :usuarioId
                    or exists (select 1 from ProyectoMiembro m where m.proyectoId = p.id and m.usuarioId = :usuarioId))
            """)
    boolean esVisiblePara(@Param("proyectoId") Long proyectoId, @Param("usuarioId") Long usuarioId);
}
