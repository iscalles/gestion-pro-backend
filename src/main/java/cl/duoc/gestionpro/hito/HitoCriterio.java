package cl.duoc.gestionpro.hito;

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

/** Criterio de aceptación de un hito. El hito solo se marca cumplido con todos sus criterios cumplidos. */
@Entity
@Table(name = "hito_criterio")
@Getter
@Setter
@NoArgsConstructor
public class HitoCriterio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hito_id", nullable = false)
    private Long hitoId;

    @Column(nullable = false)
    private Integer orden = 1;

    @Column(nullable = false)
    private String descripcion;

    @Convert(converter = NumericBooleanConverter.class)
    @Column(nullable = false)
    private boolean cumplido;
}
