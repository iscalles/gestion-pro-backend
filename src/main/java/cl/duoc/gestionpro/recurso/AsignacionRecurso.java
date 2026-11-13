package cl.duoc.gestionpro.recurso;

import java.math.BigDecimal;
import java.math.RoundingMode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Recurso no humano asignado a una actividad hoja. costo material = cantidad × valor unitario × periodo. */
@Entity
@Table(name = "asignacion_recurso")
@Getter
@Setter
@NoArgsConstructor
public class AsignacionRecurso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "actividad_id", nullable = false)
    private Long actividadId;

    @Column(name = "recurso_id", nullable = false)
    private Long recursoId;

    @Column(nullable = false)
    private BigDecimal cantidad;

    /** Copia del costo unitario del recurso al asignar. */
    @Column(name = "costo_unitario", nullable = false)
    private BigDecimal costoUnitario;

    @Column(nullable = false)
    private Integer periodo = 1;

    @Column(name = "costo_real", nullable = false)
    private BigDecimal costoReal = BigDecimal.ZERO;

    public BigDecimal costoPlan() {
        return cantidad.multiply(costoUnitario).multiply(BigDecimal.valueOf(periodo)).setScale(2, RoundingMode.HALF_UP);
    }

    /** Unidades × periodos que consume del recurso. */
    public BigDecimal consumo() {
        return cantidad.multiply(BigDecimal.valueOf(periodo));
    }
}
