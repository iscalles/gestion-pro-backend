package cl.duoc.gestionpro.planificacion;

/** Relaciones de precedencia de la guía. El desfase se suma en días hábiles. */
public enum TipoDependencia {
    /** Fin a inicio: la sucesora comienza después de finalizar la predecesora. */
    FS,
    /** Inicio a inicio: la sucesora comienza después de iniciarse la predecesora. */
    SS,
    /** Fin a fin: la sucesora finaliza después de finalizar la predecesora. */
    FF,
    /** Inicio a fin: la sucesora finaliza después de iniciarse la predecesora. */
    SF
}
