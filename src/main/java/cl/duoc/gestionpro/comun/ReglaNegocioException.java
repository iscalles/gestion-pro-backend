package cl.duoc.gestionpro.comun;

/** Regla del dominio incumplida (dependencia circular, cierre con actividades abiertas…). Responde 422. */
public class ReglaNegocioException extends RuntimeException {

    private final String codigo;

    public ReglaNegocioException(String codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
