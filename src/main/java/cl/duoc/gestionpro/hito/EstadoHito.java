package cl.duoc.gestionpro.hito;

/** Estados guardados. "Atrasado" no se guarda: se calcula (pendiente con fecha comprometida vencida). */
public enum EstadoHito {
    PENDIENTE,
    CUMPLIDO,
    CANCELADO
}
