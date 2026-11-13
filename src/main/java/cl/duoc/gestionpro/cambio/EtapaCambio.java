package cl.duoc.gestionpro.cambio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Las 10 etapas del proceso de cambio, guardadas como datos. */
@Entity
@Table(name = "etapa_cambio")
@Getter
@Setter
@NoArgsConstructor
public class EtapaCambio {

    @Id
    private Integer numero;

    @Column(nullable = false)
    private String nombre;

    private String descripcion;
}
