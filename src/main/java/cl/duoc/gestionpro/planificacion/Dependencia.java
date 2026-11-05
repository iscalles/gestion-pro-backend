package cl.duoc.gestionpro.planificacion;

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

/** Precedencia entre dos actividades hoja del mismo proyecto. Los ciclos se rechazan antes de guardar. */
@Entity
@Table(name = "dependencia")
@Getter
@Setter
@NoArgsConstructor
public class Dependencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "proyecto_id", nullable = false)
    private Long proyectoId;

    @Column(name = "predecesora_id", nullable = false)
    private Long predecesoraId;

    @Column(name = "sucesora_id", nullable = false)
    private Long sucesoraId;

    // CHAR(2) en Oracle
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "char(2)")
    private TipoDependencia tipo;

    /** Días hábiles, positivo (espera) o negativo (adelanto). */
    @Column(name = "desfase_dias", nullable = false)
    private Integer desfaseDias = 0;
}
