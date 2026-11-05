package cl.duoc.gestionpro.recurso;

import java.math.BigDecimal;

import org.hibernate.type.NumericBooleanConverter;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Profesional del catálogo compartido. Por ahora solo se lee (responsable de actividades);
 * el CRUD, las asignaciones y la sobreasignación llegan con el Módulo 5.
 */
@Entity
@Table(name = "profesional")
@Getter
@Setter
@NoArgsConstructor
public class Profesional {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String especialidad;

    @Column(name = "tipo_profesional_id", nullable = false)
    private Long tipoProfesionalId;

    @Column(name = "valor_hora", nullable = false)
    private BigDecimal valorHora;

    @Column(name = "horas_semana_disp", nullable = false)
    private BigDecimal horasSemanaDisp;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Convert(converter = NumericBooleanConverter.class)
    @Column(nullable = false)
    private boolean activo = true;
}
