package cl.duoc.gestionpro.proyecto;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "proyecto_miembro")
@IdClass(ProyectoMiembro.Clave.class)
@Getter
@Setter
@NoArgsConstructor
public class ProyectoMiembro {

    @Id
    @Column(name = "proyecto_id")
    private Long proyectoId;

    @Id
    @Column(name = "usuario_id")
    private Long usuarioId;

    @EqualsAndHashCode
    @NoArgsConstructor
    public static class Clave implements Serializable {
        private Long proyectoId;
        private Long usuarioId;
    }
}
