package cl.duoc.gestionpro.hito;

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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "hito")
@Getter
@Setter
@NoArgsConstructor
public class Hito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "proyecto_id", nullable = false)
    private Long proyectoId;

    @Column(nullable = false)
    private String codigo;

    @Column(nullable = false)
    private String descripcion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "responsable_id")
    private Profesional responsable;

    @Column(name = "fecha_comprometida", nullable = false)
    private LocalDate fechaComprometida;

    @Column(name = "fecha_real")
    private LocalDate fechaReal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoHito estado = EstadoHito.PENDIENTE;
}
