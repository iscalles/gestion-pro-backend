package cl.duoc.gestionpro.evidencia;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EvidenciaRepository extends JpaRepository<Evidencia, Long> {

    /** Datos de la evidencia sin el contenido. */
    record Resumen(Long id, Long hitoId, Long solicitudId, String nombreArchivo, String tipoMime, Long tamanoBytes,
            Long subidoPorId, LocalDateTime subidoEn) {}

    @Query("select new cl.duoc.gestionpro.evidencia.EvidenciaRepository$Resumen(e.id, e.hitoId, e.solicitudId, e.nombreArchivo, "
            + "e.tipoMime, e.tamanoBytes, e.subidoPorId, e.subidoEn) from Evidencia e "
            + "where e.hitoId in (select h.id from Hito h where h.proyectoId = :proyectoId) "
            + "or e.solicitudId in (select s.id from SolicitudCambio s where s.proyectoId = :proyectoId) order by e.subidoEn, e.id")
    List<Resumen> deProyecto(@Param("proyectoId") Long proyectoId);
}
