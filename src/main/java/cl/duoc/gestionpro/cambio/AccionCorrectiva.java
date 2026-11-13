package cl.duoc.gestionpro.cambio;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "accion_correctiva")
@Getter
@Setter
@NoArgsConstructor
public class AccionCorrectiva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "solicitud_id", nullable = false)
    private Long solicitudId;

    @Column(nullable = false)
    private String descripcion;

    @Column(name = "responsable_id", nullable = false)
    private Long responsableId;

    @Column(name = "fecha_compromiso", nullable = false)
    private LocalDate fechaCompromiso;

    @Column(name = "fecha_cierre")
    private LocalDate fechaCierre;

    /** ABIERTA o CERRADA. */
    @Column(nullable = false)
    private String estado = "ABIERTA";

    public boolean cerrada() {
        return "CERRADA".equals(estado);
    }
}
