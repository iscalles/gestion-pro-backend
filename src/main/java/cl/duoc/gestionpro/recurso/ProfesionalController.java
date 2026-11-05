package cl.duoc.gestionpro.recurso;

import java.util.List;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Lista de profesionales activos para elegir responsables. El CRUD completo es parte del Módulo 5. */
@RestController
@RequestMapping("/api/profesionales")
public class ProfesionalController {

    public record ProfesionalResumen(Long id, String nombre, String especialidad) {}

    private final ProfesionalRepository profesionales;

    public ProfesionalController(ProfesionalRepository profesionales) {
        this.profesionales = profesionales;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<ProfesionalResumen> listar() {
        return profesionales.findByActivoTrueOrderByNombreAsc().stream()
                .map(p -> new ProfesionalResumen(p.getId(), p.getNombre(), p.getEspecialidad()))
                .toList();
    }
}
