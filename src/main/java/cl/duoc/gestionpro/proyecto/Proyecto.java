package cl.duoc.gestionpro.proyecto;

import java.math.BigDecimal;
import java.time.LocalDate;

import cl.duoc.gestionpro.seguridad.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "proyecto")
@Getter
@Setter
@NoArgsConstructor
public class Proyecto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String codigo;

    @Column(nullable = false)
    private String nombre;

    private String descripcion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "responsable_id")
    private Usuario responsable;

    @Column(name = "calendario_id", nullable = false)
    private Long calendarioId;

    @Column(name = "fecha_inicio_plan", nullable = false)
    private LocalDate fechaInicioPlan;

    @Column(name = "fecha_termino_plan", nullable = false)
    private LocalDate fechaTerminoPlan;

    @Column(name = "fecha_inicio_real")
    private LocalDate fechaInicioReal;

    @Column(name = "fecha_termino_real")
    private LocalDate fechaTerminoReal;

    @Column(nullable = false)
    private BigDecimal presupuesto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoProyecto estado = EstadoProyecto.PLANIFICADO;

    /** Lo escribe el servicio de recálculo; no se edita desde la API. */
    @Column(nullable = false)
    private BigDecimal avance = BigDecimal.ZERO;

    @Version
    private Long version;
}
