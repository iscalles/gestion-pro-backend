package cl.duoc.gestionpro.alerta;

import java.time.LocalDateTime;

import org.hibernate.type.NumericBooleanConverter;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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

@Entity
@Table(name = "alerta")
@Getter
@Setter
@NoArgsConstructor
public class Alerta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "proyecto_id", nullable = false)
    private Long proyectoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoAlerta tipo;

    /** ALTA o MEDIA. */
    @Column(nullable = false)
    private String severidad;

    @Column(nullable = false)
    private String mensaje;

    private String entidad;

    @Column(name = "entidad_id")
    private Long entidadId;

    @Column(name = "creada_en", nullable = false)
    private LocalDateTime creadaEn;

    @Convert(converter = NumericBooleanConverter.class)
    @Column(nullable = false)
    private boolean resuelta;

    @Column(name = "resuelta_en")
    private LocalDateTime resueltaEn;

    public String clave() {
        return tipo + "|" + entidad + "|" + entidadId + "|" + mensaje;
    }
}
