package cl.duoc.gestionpro.cambio;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "linea_base_actividad")
@IdClass(LineaBaseActividad.Clave.class)
@Getter
@Setter
@NoArgsConstructor
public class LineaBaseActividad {

    @Id
    @Column(name = "linea_base_id")
    private Long lineaBaseId;

    @Id
    @Column(name = "actividad_id")
    private Long actividadId;

    @Column(name = "fecha_inicio_plan", nullable = false)
    private LocalDate fechaInicioPlan;

    @Column(name = "fecha_termino_plan", nullable = false)
    private LocalDate fechaTerminoPlan;

    @Column(name = "duracion_plan", nullable = false)
    private Integer duracionPlan;

    @Column(name = "costo_plan", nullable = false)
    private BigDecimal costoPlan;

    @EqualsAndHashCode
    @NoArgsConstructor
    public static class Clave implements Serializable {
        private Long lineaBaseId;
        private Long actividadId;
    }
}
