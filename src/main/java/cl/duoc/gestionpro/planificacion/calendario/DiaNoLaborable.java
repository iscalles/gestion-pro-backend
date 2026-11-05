package cl.duoc.gestionpro.planificacion.calendario;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Feriado o día sin trabajo dentro de un calendario. */
@Entity
@Table(name = "dia_no_laborable")
@Getter
@Setter
@NoArgsConstructor
public class DiaNoLaborable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "calendario_id", nullable = false)
    private Long calendarioId;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false)
    private String motivo;
}
