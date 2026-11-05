package cl.duoc.gestionpro.comun;

/** El registro pedido no existe. Responde 404. */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String entidad, Object id) {
        super(entidad + " " + id + " no existe.");
    }
}
