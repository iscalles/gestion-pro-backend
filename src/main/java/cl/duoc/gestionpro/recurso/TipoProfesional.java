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

/** Categoría de profesional (jefe de proyecto, analista…) con su tarifa de referencia. */
@Entity
@Table(name = "tipo_profesional")
@Getter
@Setter
@NoArgsConstructor
public class TipoProfesional {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String categoria;

    @Column(name = "tarifa_hora_ref", nullable = false)
    private BigDecimal tarifaHoraRef;
}
