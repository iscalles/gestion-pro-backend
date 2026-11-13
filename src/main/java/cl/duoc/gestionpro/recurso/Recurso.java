package cl.duoc.gestionpro.recurso;

import java.math.BigDecimal;
import java.math.RoundingMode;

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

/**
 * Licencia, material, equipamiento u otro recurso del proyecto. costo_total es una columna virtual
 * en Oracle; aquí se calcula igual para que siempre esté al día dentro de la transacción.
 */
@Entity
@Table(name = "recurso")
@Getter
@Setter
@NoArgsConstructor
public class Recurso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "proyecto_id", nullable = false)
    private Long proyectoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoRecurso tipo;

    @Column(nullable = false)
    private String descripcion;

    private String detalle;

    private String unidad;

    @Column(nullable = false)
    private BigDecimal cantidad;

    @Column(name = "costo_unitario", nullable = false)
    private BigDecimal costoUnitario;

    /** Meses de licencia o arriendo; 1 si no aplica. */
    @Column(nullable = false)
    private Integer periodo = 1;

    /** cantidad × costo unitario × periodo */
    public BigDecimal costoTotal() {
        return cantidad.multiply(costoUnitario).multiply(BigDecimal.valueOf(periodo)).setScale(2, RoundingMode.HALF_UP);
    }

    /** Unidades × periodos disponibles para asignar. */
    public BigDecimal capacidad() {
        return cantidad.multiply(BigDecimal.valueOf(periodo));
    }
}
