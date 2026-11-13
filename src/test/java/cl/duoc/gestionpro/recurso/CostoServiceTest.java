package cl.duoc.gestionpro.recurso;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import cl.duoc.gestionpro.planificacion.Actividad;
import cl.duoc.gestionpro.planificacion.ArbolActividades;
import cl.duoc.gestionpro.planificacion.CalendarioLaboral;
import cl.duoc.gestionpro.planificacion.TipoActividad;
import cl.duoc.gestionpro.recalculo.RecalculoService;

/** Etapa 1 con dos actividades hoja: A (lun 07-09 a vie 11-09) y B (lun 14-09 a vie 18-09). */
class CostoServiceTest {

    private final CalendarioLaboral cal = CalendarioLaboral.lunesAViernes(List.of());

    private static Actividad act(long id, Long padre, TipoActividad tipo, int orden, LocalDate ini, LocalDate fin, String avance) {
        var a = new Actividad();
        a.setId(id);
        a.setProyectoId(1L);
        a.setPadreId(padre);
        a.setTipo(tipo);
        a.setOrden(orden);
        a.setNombre("A" + id);
        a.setDuracionPlan(5);
        a.setFechaInicioPlan(ini);
        a.setFechaTerminoPlan(fin);
        a.setAvance(new BigDecimal(avance));
        return a;
    }

    private final Actividad etapa = act(1, null, TipoActividad.ETAPA, 1, LocalDate.of(2026, 9, 7), LocalDate.of(2026, 9, 18), "50");
    private final Actividad a = act(2, 1L, TipoActividad.ACTIVIDAD, 1, LocalDate.of(2026, 9, 7), LocalDate.of(2026, 9, 11), "100");
    private final Actividad b = act(3, 1L, TipoActividad.ACTIVIDAD, 2, LocalDate.of(2026, 9, 14), LocalDate.of(2026, 9, 18), "0");

    private static AsignacionProfesional prof(long actividad, String horasSemana, String semanas, String valorHora, String real) {
        var x = new AsignacionProfesional();
        x.setActividadId(actividad);
        x.setProfesionalId(1L);
        x.setHorasSemana(new BigDecimal(horasSemana));
        x.setSemanas(new BigDecimal(semanas));
        x.setValorHora(new BigDecimal(valorHora));
        x.setCostoReal(new BigDecimal(real));
        return x;
    }

    private static AsignacionRecurso rec(long actividad, String cantidad, String costoUnitario) {
        var x = new AsignacionRecurso();
        x.setActividadId(actividad);
        x.setRecursoId(1L);
        x.setCantidad(new BigDecimal(cantidad));
        x.setCostoUnitario(new BigDecimal(costoUnitario));
        x.setPeriodo(1);
        return x;
    }

    @Test
    @DisplayName("El costo de las hojas se acumula en la etapa; EV y PV siguen al avance y al calendario")
    void consolida() {
        var c = CostoService.calcular(List.of(etapa, a, b),
                List.of(prof(2, "40", "1", "10000", "420000"), prof(3, "20", "1", "10000", "0")),
                List.of(rec(3, "2", "50000")), cal, LocalDate.of(2026, 9, 14));
        assertThat(c.de(2L).plan()).isEqualByComparingTo("400000");
        assertThat(c.de(3L).plan()).isEqualByComparingTo("300000"); // 200.000 profesional + 100.000 recurso
        assertThat(c.de(1L).plan()).isEqualByComparingTo("700000");
        assertThat(c.de(1L).real()).isEqualByComparingTo("420000");
        assertThat(c.plan()).isEqualByComparingTo("700000");
        assertThat(c.valorGanado()).isEqualByComparingTo("400000"); // A al 100 %, B al 0 %
        // el lunes 14-09 transcurrió 1 de 5 días de B: PV = 400.000 + 300.000 × 0,2
        assertThat(c.valorPlanificado()).isEqualByComparingTo("460000");
    }

    @Test
    @DisplayName("Semanas de una asignación = duración / días hábiles por semana")
    void semanas() {
        assertThat(RecalculoService.semanas(18, 5)).isEqualByComparingTo("3.60");
        assertThat(RecalculoService.semanas(0, 5)).isEqualByComparingTo("0.20");
    }

    @Test
    @DisplayName("Código EDT derivado del orden del árbol")
    void codigoEdt() {
        var arbol = ArbolActividades.de(List.of(b, etapa, a));
        assertThat(arbol.codigo(1L)).isEqualTo("1");
        assertThat(arbol.codigo(2L)).isEqualTo("1.1");
        assertThat(arbol.codigo(3L)).isEqualTo("1.2");
        assertThat(arbol.hojasDe(1L)).extracting(Actividad::getId).containsExactly(2L, 3L);
    }
}
