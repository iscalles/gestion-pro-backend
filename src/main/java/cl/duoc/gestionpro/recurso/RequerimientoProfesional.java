package cl.duoc.gestionpro.recurso;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Dotación que el proyecto necesita de un tipo de profesional ("tipo de profesional" de la guía). */
@Entity
@Table(name = "requerimiento_profesional")
@Getter
@Setter
@NoArgsConstructor
public class RequerimientoProfesional {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "proyecto_id", nullable = false)
    private Long proyectoId;

    @Column(name = "tipo_profesional_id", nullable = false)
    private Long tipoProfesionalId;

    @Column(name = "cantidad_requerida", nullable = false)
    private Integer cantidadRequerida;

    @Column(name = "tarifa_hora", nullable = false)
    private BigDecimal tarifaHora;
}
