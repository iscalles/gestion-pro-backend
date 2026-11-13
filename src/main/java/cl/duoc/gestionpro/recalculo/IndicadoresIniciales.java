package cl.duoc.gestionpro.recalculo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import cl.duoc.gestionpro.alerta.AlertaService;
import cl.duoc.gestionpro.cambio.LineaBaseRepository;
import cl.duoc.gestionpro.cambio.LineaBaseService;
import cl.duoc.gestionpro.kpi.KpiService;
import cl.duoc.gestionpro.kpi.OrigenMedicion;
import cl.duoc.gestionpro.proyecto.Proyecto;
import cl.duoc.gestionpro.proyecto.ProyectoRepository;

/**
 * Después de programar los proyectos (ProgramacionInicial):
 * <ul>
 * <li>guarda la línea base inicial (LB-01) de los proyectos que aún no tienen una;</li>
 * <li>toma las mediciones programadas de KPI que correspondan según su periodicidad;</li>
 * <li>sincroniza las alertas.</li>
 * </ul>
 * Lo mismo, salvo la línea base, se repite todos los días a las 06:00 (hora de Chile).
 */
@Component
@Order(2)
public class IndicadoresIniciales implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(IndicadoresIniciales.class);

    private final ProyectoRepository proyectos;
    private final LineaBaseRepository lineasBase;
    private final LineaBaseService lineaBaseService;
    private final KpiService kpis;
    private final AlertaService alertas;
    private final TransactionTemplate tx;

    public IndicadoresIniciales(ProyectoRepository proyectos, LineaBaseRepository lineasBase, LineaBaseService lineaBaseService,
            KpiService kpis, AlertaService alertas, TransactionTemplate tx) {
        this.proyectos = proyectos;
        this.lineasBase = lineasBase;
        this.lineaBaseService = lineaBaseService;
        this.kpis = kpis;
        this.alertas = alertas;
        this.tx = tx;
    }

    @Override
    public void run(ApplicationArguments args) {
        for (Proyecto p : proyectos.findAllByOrderByCodigoAsc()) {
            try {
                tx.executeWithoutResult(s -> {
                    if (!lineasBase.existsByProyectoId(p.getId())
                            && lineaBaseService.guardar(p.getId(), null, p.getResponsable().getId()) != null) {
                        log.info("Línea base inicial guardada para {}", p.getCodigo());
                    }
                    kpis.medirProyecto(p.getId(), null, OrigenMedicion.PROGRAMADA);
                    alertas.sincronizar(p.getId());
                });
            } catch (RuntimeException e) {
                log.warn("No se pudieron actualizar los indicadores de {}: {}", p.getCodigo(), e.getMessage());
            }
        }
    }

    /** Medición programada diaria: KPI con periodicidad cumplida y alertas (por ejemplo, actividades que se atrasan). */
    @Scheduled(cron = "0 0 6 * * *", zone = "America/Santiago")
    public void diario() {
        for (Proyecto p : proyectos.findAllByOrderByCodigoAsc()) {
            try {
                tx.executeWithoutResult(s -> {
                    kpis.medirProyecto(p.getId(), null, OrigenMedicion.PROGRAMADA);
                    alertas.sincronizar(p.getId());
                });
            } catch (RuntimeException e) {
                log.warn("Medición diaria fallida en {}: {}", p.getCodigo(), e.getMessage());
            }
        }
    }
}
