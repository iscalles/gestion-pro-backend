package cl.duoc.gestionpro.cambio;

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

/**
 * Modificación concreta que una solicitud propone sobre el proyecto. Al aprobarse se aplica y guarda el valor
 * anterior y la fecha: es la trazabilidad entre el cambio aprobado y lo que cambió en el proyecto.
 */
@Entity
@Table(name = "cambio_modificacion")
@Getter
@Setter
@NoArgsConstructor
public class CambioModificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "solicitud_id", nullable = false)
    private Long solicitudId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EntidadModificada entidad;

    @Column(name = "entidad_id", nullable = false)
    private Long entidadId;

    @Column(nullable = false)
    private String campo;

    @Column(name = "valor_anterior")
    private String valorAnterior;

    @Column(name = "valor_nuevo", nullable = false)
    private String valorNuevo;

    @Convert(converter = NumericBooleanConverter.class)
    @Column(nullable = false)
    private boolean aplicada;

    @Column(name = "fecha_aplicacion")
    private LocalDateTime fechaAplicacion;
}
