package cl.duoc.gestionpro.evidencia;

import java.time.LocalDateTime;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Archivo de respaldo de un hito o de una solicitud de cambio (máximo 10 MB, guardado como BLOB). */
@Entity
@Table(name = "evidencia")
@Getter
@Setter
@NoArgsConstructor
public class Evidencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hito_id")
    private Long hitoId;

    @Column(name = "solicitud_id")
    private Long solicitudId;

    @Column(name = "nombre_archivo", nullable = false)
    private String nombreArchivo;

    @Column(name = "tipo_mime", nullable = false)
    private String tipoMime;

    @Column(name = "tamano_bytes", nullable = false)
    private Long tamanoBytes;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(nullable = false)
    private byte[] contenido;

    @Column(name = "subido_por_id", nullable = false)
    private Long subidoPorId;

    @Column(name = "subido_en", nullable = false)
    private LocalDateTime subidoEn;
}
