package cl.duoc.gestionpro.kpi;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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

/** Histórico de valores de un KPI. */
@Entity
@Table(name = "medicion_kpi")
@Getter
@Setter
@NoArgsConstructor
public class MedicionKpi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "kpi_id", nullable = false)
    private Long kpiId;

    @Column(name = "medido_en", nullable = false)
    private LocalDateTime medidoEn;

    @Column(nullable = false)
    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Semaforo semaforo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrigenMedicion origen;

    @Column(name = "solicitud_id")
    private Long solicitudId;

    @Column(name = "registrado_por_id")
    private Long registradoPorId;

    private String observacion;
}
