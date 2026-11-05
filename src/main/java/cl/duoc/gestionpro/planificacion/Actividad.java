package cl.duoc.gestionpro.planificacion;

import java.math.BigDecimal;
import java.time.LocalDate;

import cl.duoc.gestionpro.recurso.Profesional;
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

/**
 * Etapa, actividad o subactividad. La jerarquía vive en padreId (sin códigos EDT: el Módulo 2 no se construye).
 * Las fechas planificadas las escribe el MotorProgramacion; en los elementos con hijos también la duración,
 * el avance y las fechas reales, que se derivan de sus descendientes.
 */
@Entity
@Table(name = "actividad")
@Getter
@Setter
@NoArgsConstructor
public class Actividad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "proyecto_id", nullable = false)
    private Long proyectoId;

    @Column(name = "padre_id")
    private Long padreId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoActividad tipo;

    @Column(nullable = false)
    private Integer orden = 0;

    @Column(nullable = false)
    private String nombre;

    private String descripcion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsable_id")
    private Profesional responsable;

    @Column(name = "duracion_plan", nullable = false)
    private Integer duracionPlan;

    @Column(name = "duracion_real")
    private Integer duracionReal;

    @Column(name = "fecha_inicio_plan")
    private LocalDate fechaInicioPlan;

    @Column(name = "fecha_termino_plan")
    private LocalDate fechaTerminoPlan;

    @Column(name = "fecha_inicio_real")
    private LocalDate fechaInicioReal;

    @Column(name = "fecha_termino_real")
    private LocalDate fechaTerminoReal;

    @Column(nullable = false)
    private BigDecimal avance = BigDecimal.ZERO;

    @Version
    private Long version;
}
