package cl.duoc.gestionpro.planificacion.calendario;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.EnumSet;
import java.util.Set;

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

/** Calendario laboral: días de la semana hábiles y horas por día. Lo comparten los proyectos que lo usan. */
@Entity
@Table(name = "calendario")
@Getter
@Setter
@NoArgsConstructor
public class Calendario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Convert(converter = NumericBooleanConverter.class)
    @Column(nullable = false)
    private boolean lunes = true;

    @Convert(converter = NumericBooleanConverter.class)
    @Column(nullable = false)
    private boolean martes = true;

    @Convert(converter = NumericBooleanConverter.class)
    @Column(nullable = false)
    private boolean miercoles = true;

    @Convert(converter = NumericBooleanConverter.class)
    @Column(nullable = false)
    private boolean jueves = true;

    @Convert(converter = NumericBooleanConverter.class)
    @Column(nullable = false)
    private boolean viernes = true;

    @Convert(converter = NumericBooleanConverter.class)
    @Column(nullable = false)
    private boolean sabado;

    @Convert(converter = NumericBooleanConverter.class)
    @Column(nullable = false)
    private boolean domingo;

    @Column(name = "horas_dia", nullable = false)
    private BigDecimal horasDia = BigDecimal.valueOf(8);

    public Set<DayOfWeek> diasLaborables() {
        Set<DayOfWeek> dias = EnumSet.noneOf(DayOfWeek.class);
        if (lunes) dias.add(DayOfWeek.MONDAY);
        if (martes) dias.add(DayOfWeek.TUESDAY);
        if (miercoles) dias.add(DayOfWeek.WEDNESDAY);
        if (jueves) dias.add(DayOfWeek.THURSDAY);
        if (viernes) dias.add(DayOfWeek.FRIDAY);
        if (sabado) dias.add(DayOfWeek.SATURDAY);
        if (domingo) dias.add(DayOfWeek.SUNDAY);
        return dias;
    }

    public void setDiasLaborables(Set<DayOfWeek> dias) {
        lunes = dias.contains(DayOfWeek.MONDAY);
        martes = dias.contains(DayOfWeek.TUESDAY);
        miercoles = dias.contains(DayOfWeek.WEDNESDAY);
        jueves = dias.contains(DayOfWeek.THURSDAY);
        viernes = dias.contains(DayOfWeek.FRIDAY);
        sabado = dias.contains(DayOfWeek.SATURDAY);
        domingo = dias.contains(DayOfWeek.SUNDAY);
    }
}
