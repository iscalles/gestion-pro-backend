package cl.duoc.gestionpro.planificacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import cl.duoc.gestionpro.comun.ReglaNegocioException;
import cl.duoc.gestionpro.planificacion.dto.ActividadDto;
import cl.duoc.gestionpro.planificacion.dto.ActividadRequest;
import cl.duoc.gestionpro.planificacion.dto.DependenciaRequest;
import cl.duoc.gestionpro.planificacion.dto.PlanCambioDto;
import cl.duoc.gestionpro.proyecto.ProyectoRepository;

/**
 * Prueba de integración contra Oracle Free (docker compose up -d). No corre con ./mvnw test:
 * ./mvnw test -Dtest=PlanificacionOracleIT
 * Cada test hace rollback, así que no deja datos.
 */
@SpringBootTest
@Transactional
class PlanificacionOracleIT {

    @Autowired PlanificacionService servicio;
    @Autowired ProyectoRepository proyectos;

    private Long proyectoId() {
        return proyectos.findAll().stream().filter(p -> p.getCodigo().equals("PRY-2026-03")).findFirst().orElseThrow().getId();
    }

    private static ActividadRequest req(Long padreId, String nombre, int duracion) {
        return new ActividadRequest(padreId, nombre, null, null, duracion, null, null, null, null);
    }

    private static ActividadDto buscar(PlanCambioDto r, String nombre) {
        return r.plan().actividades().stream().filter(a -> a.nombre().equals(nombre)).findFirst().orElseThrow();
    }

    @Test
    void creaJerarquiaRecalculaSucesorasYRechazaCiclos() {
        Long pid = proyectoId(); // parte el lunes 04-01-2027; 01-01-2027 es feriado
        var r = servicio.crearActividad(pid, req(null, "Etapa IT", 0));
        Long etapa = buscar(r, "Etapa IT").id();
        Long a = buscar(servicio.crearActividad(pid, req(etapa, "A IT", 5)), "A IT").id();
        Long b = buscar(servicio.crearActividad(pid, req(etapa, "B IT", 3)), "B IT").id();

        r = servicio.crearDependencia(pid, new DependenciaRequest(a, b, TipoDependencia.FS, 0));
        assertThat(buscar(r, "A IT").fechaInicioPlan()).isEqualTo(LocalDate.of(2027, 1, 4));
        assertThat(buscar(r, "B IT").fechaInicioPlan()).isEqualTo(LocalDate.of(2027, 1, 11));
        assertThat(buscar(r, "Etapa IT").fechaTerminoPlan()).isEqualTo(LocalDate.of(2027, 1, 13));
        assertThat(buscar(r, "Etapa IT").tipo()).isEqualTo(TipoActividad.ETAPA);
        assertThat(buscar(r, "A IT").tipo()).isEqualTo(TipoActividad.ACTIVIDAD);

        // Cambiar la duración mueve a la sucesora y a la etapa
        r = servicio.actualizarActividad(pid, a, req(null, "A IT", 7));
        assertThat(buscar(r, "B IT").fechaInicioPlan()).isEqualTo(LocalDate.of(2027, 1, 13));
        assertThat(r.recalculadas()).extracting(c -> c.nombre()).contains("A IT", "B IT", "Etapa IT");

        assertThatThrownBy(() -> servicio.crearDependencia(pid, new DependenciaRequest(b, a, TipoDependencia.SS, 0)))
                .isInstanceOf(ReglaNegocioException.class)
                .extracting(e -> ((ReglaNegocioException) e).getCodigo()).isEqualTo("DEPENDENCIA_CIRCULAR");
    }
}
