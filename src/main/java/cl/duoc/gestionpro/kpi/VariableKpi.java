package cl.duoc.gestionpro.kpi;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Variable que el sistema sabe calcular y que se puede usar en las fórmulas de los KPI. */
@Entity
@Table(name = "variable_kpi")
@Getter
@Setter
@NoArgsConstructor
public class VariableKpi {

    @Id
    private String codigo;

    @Column(nullable = false)
    private String descripcion;

    @Column(nullable = false)
    private String unidad;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CategoriaKpi categoria;
}
