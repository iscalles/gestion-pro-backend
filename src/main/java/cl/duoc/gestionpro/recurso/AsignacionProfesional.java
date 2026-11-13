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

/**
 * Profesional asignado a una actividad hoja. Las semanas las deriva el recálculo desde la duración de la
 * actividad, así que cambiar una duración cambia el costo.
 * costo profesional = horas semanales × semanas × valor hora (columna virtual en Oracle; aquí se calcula igual).
 */
@Entity
@Table(name = "asignacion_profesional")
@Getter
@Setter
@NoArgsConstructor
public class AsignacionProfesional {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "actividad_id", nullable = false)
    private Long actividadId;

    @Column(name = "profesional_id", nullable = false)
    private Long profesionalId;

    @Column(name = "horas_semana", nullable = false)
    private BigDecimal horasSemana;

    @Column(nullable = false)
    private BigDecimal semanas;

    /** Copia del valor hora al asignar: un alza posterior no cambia lo planificado. */
    @Column(name = "valor_hora", nullable = false)
    private BigDecimal valorHora;

    @Column(name = "horas_reales", nullable = false)
    private BigDecimal horasReales = BigDecimal.ZERO;

    @Column(name = "costo_real", nullable = false)
    private BigDecimal costoReal = BigDecimal.ZERO;

    public BigDecimal costoPlan() {
        return horasSemana.multiply(semanas).multiply(valorHora).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal horasPlan() {
        return horasSemana.multiply(semanas).setScale(2, RoundingMode.HALF_UP);
    }
}
