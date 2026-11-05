package cl.duoc.gestionpro.planificacion;

/** Nivel en la jerarquía: Proyecto → Etapa → Actividad → Subactividad. */
public enum TipoActividad {
    ETAPA,
    ACTIVIDAD,
    SUBACTIVIDAD;

    /** Tipo que debe tener el padre; nulo para las etapas, que cuelgan del proyecto. */
    public TipoActividad tipoPadre() {
        return switch (this) {
            case ETAPA -> null;
            case ACTIVIDAD -> ETAPA;
            case SUBACTIVIDAD -> ACTIVIDAD;
        };
    }
}
