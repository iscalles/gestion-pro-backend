package cl.duoc.gestionpro.planificacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import cl.duoc.gestionpro.comun.ReglaNegocioException;

/**
 * Proyecto de prueba que parte el martes 01-09-2026, calendario lunes a viernes con feriado el 18-09-2026.
 * La predecesora A dura 5 días hábiles: 01-09 (mar) al 07-09 (lun).
 */
class MotorProgramacionTest {

    private static final LocalDate INICIO = LocalDate.of(2026, 9, 1);
    private final CalendarioLaboral cal = CalendarioLaboral.lunesAViernes(List.of(LocalDate.of(2026, 9, 18)));
    private final MotorProgramacion motor = new MotorProgramacion();

    private List<Actividad> actividades;
    private List<Dependencia> dependencias;
    private long siguienteDep;

    @BeforeEach
    void limpiar() {
        actividades = new ArrayList<>();
        dependencias = new ArrayList<>();
        siguienteDep = 1;
    }

    private Actividad act(long id, String nombre, int duracion) {
        return act(id, nombre, duracion, null, TipoActividad.ACTIVIDAD);
    }

    private Actividad act(long id, String nombre, int duracion, Long padreId, TipoActividad tipo) {
        var a = new Actividad();
        a.setId(id);
        a.setProyectoId(1L);
        a.setNombre(nombre);
        a.setDuracionPlan(duracion);
        a.setPadreId(padreId);
        a.setTipo(tipo);
        actividades.add(a);
        return a;
    }

    private Dependencia dep(Actividad pred, Actividad suc, TipoDependencia tipo, int desfase) {
        var d = new Dependencia();
        d.setId(siguienteDep++);
        d.setProyectoId(1L);
        d.setPredecesoraId(pred.getId());
        d.setSucesoraId(suc.getId());
        d.setTipo(tipo);
        d.setDesfaseDias(desfase);
        dependencias.add(d);
        return d;
    }

    private void programar() {
        motor.programar(INICIO, actividades, dependencias, cal);
    }

    private static LocalDate f(int mes, int dia) {
        return LocalDate.of(2026, mes, dia);
    }

    private static void assertFechas(Actividad a, LocalDate inicio, LocalDate termino) {
        assertThat(a.getFechaInicioPlan()).as("inicio de %s", a.getNombre()).isEqualTo(inicio);
        assertThat(a.getFechaTerminoPlan()).as("término de %s", a.getNombre()).isEqualTo(termino);
    }

    @Test
    @DisplayName("Sin predecesoras la actividad empieza con el proyecto")
    void sinPredecesoras() {
        var a = act(1, "A", 5);
        programar();
        assertFechas(a, f(9, 1), f(9, 7));
    }

    @Test
    @DisplayName("Si el proyecto parte un sábado, las actividades parten el lunes")
    void inicioEnFinDeSemana() {
        var a = act(1, "A", 2);
        motor.programar(f(9, 5), actividades, dependencias, cal);
        assertFechas(a, f(9, 7), f(9, 8));
    }

    @Nested
    @DisplayName("Fin a inicio (FS)")
    class FinAInicio {

        @Test
        @DisplayName("Sin desfase: la sucesora empieza el día hábil siguiente al término")
        void sinDesfase() {
            var a = act(1, "A", 5);
            var b = act(2, "B", 3);
            dep(a, b, TipoDependencia.FS, 0);
            programar();
            assertFechas(b, f(9, 8), f(9, 10));
        }

        @Test
        @DisplayName("Desfase positivo: espera días hábiles")
        void desfasePositivo() {
            var a = act(1, "A", 5);
            var b = act(2, "B", 3);
            dep(a, b, TipoDependencia.FS, 2);
            programar();
            assertFechas(b, f(9, 10), f(9, 14));
        }

        @Test
        @DisplayName("Desfase negativo: se adelanta y se traslapa con la predecesora")
        void desfaseNegativo() {
            var a = act(1, "A", 5);
            var b = act(2, "B", 3);
            dep(a, b, TipoDependencia.FS, -2);
            programar();
            assertFechas(b, f(9, 4), f(9, 8));
        }

        @Test
        @DisplayName("Salta feriados y fines de semana")
        void saltaFeriado() {
            var a = act(1, "A", 13); // 01-09 a 17-09
            var b = act(2, "B", 2);
            dep(a, b, TipoDependencia.FS, 0);
            programar();
            assertFechas(a, f(9, 1), f(9, 17));
            assertFechas(b, f(9, 21), f(9, 22)); // 18 feriado, 19-20 fin de semana
        }
    }

    @Nested
    @DisplayName("Inicio a inicio (SS)")
    class InicioAInicio {

        @Test
        @DisplayName("Sin desfase: empiezan el mismo día")
        void sinDesfase() {
            var a = act(1, "A", 5);
            var b = act(2, "B", 3);
            dep(a, b, TipoDependencia.SS, 0);
            programar();
            assertFechas(b, f(9, 1), f(9, 3));
        }

        @Test
        @DisplayName("Desfase positivo: empieza n días hábiles después del inicio")
        void desfasePositivo() {
            var a = act(1, "A", 5);
            var b = act(2, "B", 3);
            dep(a, b, TipoDependencia.SS, 3);
            programar();
            assertFechas(b, f(9, 4), f(9, 8));
        }

        @Test
        @DisplayName("Desfase negativo: no puede empezar antes que el proyecto")
        void desfaseNegativoAcotado() {
            var a = act(1, "A", 5);
            var b = act(2, "B", 3);
            dep(a, b, TipoDependencia.SS, -5);
            programar();
            assertFechas(b, f(9, 1), f(9, 3));
        }

        @Test
        @DisplayName("Desfase negativo: se adelanta respecto de una predecesora tardía")
        void desfaseNegativo() {
            var x = act(1, "X", 5);
            var a = act(2, "A", 5);
            var b = act(3, "B", 2);
            dep(x, a, TipoDependencia.FS, 0); // A: 08-09 a 14-09
            dep(a, b, TipoDependencia.SS, -2); // B: 04-09
            programar();
            assertFechas(a, f(9, 8), f(9, 14));
            assertFechas(b, f(9, 4), f(9, 7));
        }
    }

    @Nested
    @DisplayName("Fin a fin (FF)")
    class FinAFin {

        @Test
        @DisplayName("Sin desfase: terminan el mismo día")
        void sinDesfase() {
            var a = act(1, "A", 5);
            var b = act(2, "B", 3);
            dep(a, b, TipoDependencia.FF, 0);
            programar();
            assertFechas(b, f(9, 3), f(9, 7));
        }

        @Test
        @DisplayName("Desfase positivo: termina n días hábiles después")
        void desfasePositivo() {
            var a = act(1, "A", 5);
            var b = act(2, "B", 3);
            dep(a, b, TipoDependencia.FF, 2);
            programar();
            assertFechas(b, f(9, 7), f(9, 9));
        }

        @Test
        @DisplayName("Desfase negativo: termina antes que la predecesora")
        void desfaseNegativo() {
            var a = act(1, "A", 5);
            var b = act(2, "B", 2);
            dep(a, b, TipoDependencia.FF, -1);
            programar();
            assertFechas(b, f(9, 3), f(9, 4));
        }
    }

    @Nested
    @DisplayName("Inicio a fin (SF)")
    class InicioAFin {

        @Test
        @DisplayName("Sin desfase: termina justo cuando la predecesora empieza")
        void sinDesfase() {
            var x = act(1, "X", 5);
            var a = act(2, "A", 5);
            var b = act(3, "B", 3);
            dep(x, a, TipoDependencia.FS, 0); // A empieza el 08-09
            dep(a, b, TipoDependencia.SF, 0);
            programar();
            assertFechas(b, f(9, 3), f(9, 7)); // termina el día hábil anterior al 08-09
        }

        @Test
        @DisplayName("Desfase positivo: termina n días hábiles después del inicio")
        void desfasePositivo() {
            var a = act(1, "A", 5);
            var b = act(2, "B", 3);
            dep(a, b, TipoDependencia.SF, 5);
            programar();
            assertFechas(b, f(9, 3), f(9, 7));
        }

        @Test
        @DisplayName("Desfase negativo: termina antes del inicio de la predecesora")
        void desfaseNegativo() {
            var x = act(1, "X", 10); // 01-09 a 14-09
            var a = act(2, "A", 2);
            var b = act(3, "B", 2);
            dep(x, a, TipoDependencia.FS, 0); // A empieza el 15-09
            dep(a, b, TipoDependencia.SF, -2);
            programar();
            assertFechas(b, f(9, 9), f(9, 10)); // termina 3 días hábiles antes del 15-09
        }
    }

    @Test
    @DisplayName("Con varias predecesoras manda la restricción más tardía")
    void variasPredecesoras() {
        var a = act(1, "A", 5); // termina 07-09
        var b = act(2, "B", 8); // termina 10-09
        var c = act(3, "C", 2);
        dep(a, c, TipoDependencia.FS, 0);
        dep(b, c, TipoDependencia.FS, 0);
        programar();
        assertFechas(c, f(9, 11), f(9, 14));
    }

    @Test
    @DisplayName("Cambiar una duración recalcula toda la cadena de sucesoras")
    void recalculaSucesoras() {
        var a = act(1, "A", 5);
        var b = act(2, "B", 3);
        var c = act(3, "C", 2);
        var d = act(4, "D", 1);
        dep(a, b, TipoDependencia.FS, 0);
        dep(b, c, TipoDependencia.SS, 1);
        dep(c, d, TipoDependencia.FF, 0);
        programar();
        assertFechas(d, f(9, 10), f(9, 10));

        a.setDuracionPlan(10); // A termina el 14-09
        programar();
        assertFechas(b, f(9, 15), f(9, 17));
        assertFechas(c, f(9, 16), f(9, 17));
        assertFechas(d, f(9, 17), f(9, 17));
    }

    @Test
    @DisplayName("Las etapas y actividades padre derivan fechas, duración y avance de sus hijos")
    void derivaPadres() {
        var etapa = act(1, "Etapa", 0, null, TipoActividad.ETAPA);
        var act = act(2, "Actividad", 0, 1L, TipoActividad.ACTIVIDAD);
        var s1 = act(3, "Sub 1", 5, 2L, TipoActividad.SUBACTIVIDAD);
        var s2 = act(4, "Sub 2", 3, 2L, TipoActividad.SUBACTIVIDAD);
        var hoja = act(5, "Hoja", 2, 1L, TipoActividad.ACTIVIDAD);
        dep(s1, s2, TipoDependencia.FS, 0);
        s1.setAvance(BigDecimal.valueOf(100));
        s1.setFechaInicioReal(f(9, 1));
        s1.setFechaTerminoReal(f(9, 7));
        s2.setAvance(BigDecimal.valueOf(50));
        s2.setFechaInicioReal(f(9, 8));
        programar();

        assertFechas(act, f(9, 1), f(9, 10));
        assertThat(act.getDuracionPlan()).isEqualTo(8);
        assertThat(act.getAvance()).isEqualByComparingTo("81.25"); // (100·5 + 50·3) / 8
        assertThat(act.getFechaInicioReal()).isEqualTo(f(9, 1));
        assertThat(act.getFechaTerminoReal()).isNull();

        assertFechas(etapa, f(9, 1), f(9, 10));
        assertThat(etapa.getAvance()).isEqualByComparingTo("65.00"); // (500 + 150 + 0·2) / 10
        assertThat(hoja.getFechaInicioPlan()).isEqualTo(f(9, 1));
    }

    @Test
    @DisplayName("El avance consolidado solo llega a 100 % cuando todas las hojas terminaron")
    void avanceNoLlegaACienAntesDeTiempo() {
        var a = act(1, "A", 10_000);
        var b = act(2, "B", 1);
        a.setAvance(BigDecimal.valueOf(100));
        b.setAvance(BigDecimal.valueOf(50));
        assertThat(MotorProgramacion.avancePonderado(List.of(a, b))).isEqualByComparingTo("99.99");
        b.setAvance(BigDecimal.valueOf(100));
        assertThat(MotorProgramacion.avancePonderado(List.of(a, b))).isEqualByComparingTo("100");
    }

    @Nested
    @DisplayName("Dependencias circulares")
    class Ciclos {

        @Test
        @DisplayName("Rechaza un ciclo de tres actividades e informa el camino")
        void cicloDeTres() {
            var a = act(1, "Diseño", 2);
            var b = act(2, "Construcción", 2);
            var c = act(3, "Pruebas", 2);
            dep(a, b, TipoDependencia.FS, 0);
            dep(b, c, TipoDependencia.SS, 0);
            dep(c, a, TipoDependencia.FF, 1);
            assertThatThrownBy(MotorProgramacionTest.this::programar)
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessageContaining("Dependencia circular")
                    .hasMessageContaining("«Diseño»").hasMessageContaining("«Construcción»").hasMessageContaining("«Pruebas»")
                    .extracting(e -> ((ReglaNegocioException) e).getCodigo()).isEqualTo("DEPENDENCIA_CIRCULAR");
        }

        @Test
        @DisplayName("Rechaza un ciclo directo entre dos actividades")
        void cicloDeDos() {
            var a = act(1, "A", 2);
            var b = act(2, "B", 2);
            dep(a, b, TipoDependencia.FS, 0);
            dep(b, a, TipoDependencia.SF, 0);
            assertThatThrownBy(() -> motor.validarSinCiclos(actividades, dependencias))
                    .isInstanceOf(ReglaNegocioException.class)
                    .extracting(e -> ((ReglaNegocioException) e).getCodigo()).isEqualTo("DEPENDENCIA_CIRCULAR");
        }

        @Test
        @DisplayName("Un ciclo no deja fechas a medias: falla antes de calcular")
        void noModificaFechas() {
            var a = act(1, "A", 2);
            var b = act(2, "B", 2);
            var libre = act(3, "Libre", 2);
            dep(a, b, TipoDependencia.FS, 0);
            dep(b, a, TipoDependencia.FS, 0);
            assertThatThrownBy(MotorProgramacionTest.this::programar).isInstanceOf(ReglaNegocioException.class);
            assertThat(libre.getFechaInicioPlan()).isNull();
        }

        @Test
        @DisplayName("Un rombo (dos caminos al mismo destino) no es un ciclo")
        void romboNoEsCiclo() {
            var a = act(1, "A", 2);
            var b = act(2, "B", 2);
            var c = act(3, "C", 4);
            var d = act(4, "D", 1);
            dep(a, b, TipoDependencia.FS, 0);
            dep(a, c, TipoDependencia.FS, 0);
            dep(b, d, TipoDependencia.FS, 0);
            dep(c, d, TipoDependencia.FS, 0);
            programar();
            assertFechas(d, f(9, 9), f(9, 9));
        }

        @Test
        @DisplayName("Camino encuentra la cadena que cerraría un ciclo")
        void camino() {
            var a = act(1, "A", 1);
            var b = act(2, "B", 1);
            var c = act(3, "C", 1);
            dep(a, b, TipoDependencia.FS, 0);
            dep(b, c, TipoDependencia.FS, 0);
            assertThat(MotorProgramacion.camino(dependencias, 1L, 3L)).containsExactly(1L, 2L, 3L);
            assertThat(MotorProgramacion.camino(dependencias, 3L, 1L)).isEmpty();
        }
    }

    @Test
    @DisplayName("Rechaza dependencias que apuntan a una actividad con hijos")
    void dependenciaSoloEntreHojas() {
        var etapa = act(1, "Etapa", 0, null, TipoActividad.ETAPA);
        act(2, "Hija", 2, 1L, TipoActividad.ACTIVIDAD);
        var otra = act(3, "Otra", 2, null, TipoActividad.ETAPA);
        dep(etapa, otra, TipoDependencia.FS, 0);
        assertThatThrownBy(this::programar)
                .isInstanceOf(ReglaNegocioException.class)
                .extracting(e -> ((ReglaNegocioException) e).getCodigo()).isEqualTo("DEPENDENCIA_SOLO_HOJAS");
    }
}
