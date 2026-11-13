package cl.duoc.gestionpro.cambio;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Avance de una solicitud en cada una de las 10 etapas. */
@Entity
@Table(name = "solicitud_etapa")
@Getter
@Setter
@NoArgsConstructor
public class SolicitudEtapa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "solicitud_id", nullable = false)
    private Long solicitudId;

    @Column(name = "etapa_numero", nullable = false)
    private Integer etapaNumero;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoEtapa estado = EstadoEtapa.PENDIENTE;

    @Column(name = "responsable_id")
    private Long responsableId;

    @Column(name = "fecha_inicio")
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    @Column(name = "fecha_limite")
    private LocalDate fechaLimite;

    private String observacion;

    /** Completada dentro de su fecha límite (o sin límite). */
    public boolean aTiempo() {
        return estado == EstadoEtapa.COMPLETADA && (fechaLimite == null || !fechaFin.isAfter(fechaLimite));
    }

    /** Pendiente o en curso con la fecha límite vencida. */
    public boolean vencida(LocalDate hoy) {
        return (estado == EstadoEtapa.PENDIENTE || estado == EstadoEtapa.EN_CURSO) && fechaLimite != null && fechaLimite.isBefore(hoy);
    }
}
