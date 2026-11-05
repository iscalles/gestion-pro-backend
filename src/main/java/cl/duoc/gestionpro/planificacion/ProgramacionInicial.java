package cl.duoc.gestionpro.planificacion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import cl.duoc.gestionpro.proyecto.ProyectoRepository;
import cl.duoc.gestionpro.recalculo.RecalculoService;

/**
 * Al arrancar, programa los proyectos que tienen actividades sin fechas planificadas
 * (las cargadas por migraciones SQL, que no pasan por el motor).
 */
@Component
public class ProgramacionInicial implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ProgramacionInicial.class);

    private final ProyectoRepository proyectos;
    private final ActividadRepository actividades;
    private final RecalculoService recalculo;
    private final TransactionTemplate tx;

    public ProgramacionInicial(ProyectoRepository proyectos, ActividadRepository actividades,
            RecalculoService recalculo, TransactionTemplate tx) {
        this.proyectos = proyectos;
        this.actividades = actividades;
        this.recalculo = recalculo;
        this.tx = tx;
    }

    @Override
    public void run(ApplicationArguments args) {
        for (Long id : actividades.proyectosSinProgramar()) {
            tx.executeWithoutResult(s -> recalculo.recalcularProyecto(id));
            log.info("Proyecto {} programado al iniciar", proyectos.findById(id).map(p -> p.getCodigo()).orElse("?"));
        }
    }
}
