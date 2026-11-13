package cl.duoc.gestionpro.cambio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "solicitud_cambio")
@Getter
@Setter
@NoArgsConstructor
public class SolicitudCambio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "proyecto_id", nullable = false)
    private Long proyectoId;

    @Column(nullable = false)
    private String codigo;

    private String titulo;

    @Column(nullable = false)
    private String solicitante;

    @Column(name = "fecha_solicitud", nullable = false)
    private LocalDate fechaSolicitud;

    @Column(nullable = false)
    private String descripcion;

    @Column(nullable = false)
    private String justificacion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoCambio tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Prioridad prioridad;

    private String riesgo;

    @Enumerated(EnumType.STRING)
    private Probabilidad probabilidad;

    @Enumerated(EnumType.STRING)
    private Impacto impacto;

    @Column(name = "impacto_plazo_dias", nullable = false)
    private Integer impactoPlazoDias = 0;

    @Column(name = "impacto_costo", nullable = false)
    private BigDecimal impactoCosto = BigDecimal.ZERO;

    @Column(name = "impacto_recursos")
    private String impactoRecursos;

    @Column(name = "impacto_alcance")
    private String impactoAlcance;

    @Column(name = "responsable_id", nullable = false)
    private Long responsableId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoSolicitud estado = EstadoSolicitud.ABIERTO;

    @Column(name = "etapa_actual", nullable = false)
    private Integer etapaActual = 1;

    @Column(name = "fecha_comprometida", nullable = false)
    private LocalDate fechaComprometida;

    @Column(name = "fecha_real")
    private LocalDate fechaReal;

    @Column(name = "costo_estimado", nullable = false)
    private BigDecimal costoEstimado = BigDecimal.ZERO;

    @Column(name = "costo_real")
    private BigDecimal costoReal;

    @Column(name = "aprobado_por_id")
    private Long aprobadoPorId;

    @Column(name = "fecha_decision")
    private LocalDateTime fechaDecision;

    private String resultado;

    private BigDecimal efectividad;

    private BigDecimal adopcion;

    /** Días hábiles que el cambio movió el término del proyecto (se calcula al aprobar). */
    @Column(name = "impacto_plazo_real")
    private Integer impactoPlazoReal;

    /** Diferencia del costo planificado del proyecto al aplicar el cambio. */
    @Column(name = "impacto_costo_real")
    private BigDecimal impactoCostoReal;

    @Version
    private Long version;

    /** Riesgo crítico: probabilidad e impacto altos. */
    public boolean riesgoCritico() {
        return probabilidad == Probabilidad.ALTA && impacto == Impacto.ALTO;
    }

    /** Costo real si existe; si no, el estimado. */
    public BigDecimal costoVigente() {
        return costoReal != null ? costoReal : costoEstimado;
    }
}
