package cl.duoc.gestionpro.kpi;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;

import org.hibernate.type.NumericBooleanConverter;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Indicador configurable: fórmula sobre variables del catálogo, meta, sentido y límites del semáforo. */
@Entity
@Table(name = "kpi")
@Getter
@Setter
@NoArgsConstructor
public class Kpi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "proyecto_id", nullable = false)
    private Long proyectoId;

    @Column(nullable = false)
    private String codigo;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String objetivo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CategoriaKpi categoria;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AmbitoKpi ambito;

    @Column(name = "ambito_ref_id")
    private Long ambitoRefId;

    private String formula;

    @Column(nullable = false)
    private String unidad;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SentidoKpi sentido;

    @Column(nullable = false)
    private BigDecimal meta;

    @Column(name = "verde_min")
    private BigDecimal verdeMin;

    @Column(name = "verde_max")
    private BigDecimal verdeMax;

    @Column(name = "ambar_min")
    private BigDecimal ambarMin;

    @Column(name = "ambar_max")
    private BigDecimal ambarMax;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Periodicidad periodicidad;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FuenteKpi fuente;

    @Column(name = "responsable_id", nullable = false)
    private Long responsableId;

    @Convert(converter = NumericBooleanConverter.class)
    @Column(nullable = false)
    private boolean activo = true;

    /** Variables que usa la fórmula (las registra el servicio al guardar). */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "kpi_variable", joinColumns = @JoinColumn(name = "kpi_id"))
    @Column(name = "variable_codigo")
    private Set<String> variables = new LinkedHashSet<>();

    public Semaforo semaforo(BigDecimal valor) {
        return Semaforo.de(valor, verdeMin, verdeMax, ambarMin, ambarMax);
    }
}
