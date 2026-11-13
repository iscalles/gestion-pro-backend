package cl.duoc.gestionpro.cambio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Foto del plan (fechas, duraciones y costos) antes de aplicar un cambio aprobado. LB-01 es la inicial. */
@Entity
@Table(name = "linea_base")
@Getter
@Setter
@NoArgsConstructor
public class LineaBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "proyecto_id", nullable = false)
    private Long proyectoId;

    @Column(nullable = false)
    private Integer numero;

    @Column(name = "solicitud_id")
    private Long solicitudId;

    @Column(name = "creada_en", nullable = false)
    private LocalDateTime creadaEn;

    @Column(name = "creada_por_id", nullable = false)
    private Long creadaPorId;

    @Column(name = "fecha_termino", nullable = false)
    private LocalDate fechaTermino;

    @Column(name = "costo_total", nullable = false)
    private BigDecimal costoTotal;

    public String codigo() {
        return String.format("LB-%02d", numero);
    }
}
