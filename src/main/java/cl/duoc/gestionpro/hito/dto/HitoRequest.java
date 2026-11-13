package cl.duoc.gestionpro.hito.dto;

import java.time.LocalDate;
import java.util.List;

import cl.duoc.gestionpro.hito.EstadoHito;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record HitoRequest(
        @NotBlank(message = "Escribe el código.") @Size(max = 10, message = "Máximo 10 caracteres.") String codigo,
        @NotBlank(message = "Escribe la descripción.") @Size(max = 200) String descripcion,
        @NotNull(message = "Elige el responsable.") Long responsableId,
        @NotNull(message = "Indica la fecha comprometida.") LocalDate fechaComprometida,
        LocalDate fechaReal,
        EstadoHito estado,
        List<Long> actividadIds,
        @Valid List<CriterioRequest> criterios,
        List<Long> kpiIds) {

    public record CriterioRequest(Long id, @NotBlank(message = "Describe el criterio.") @Size(max = 300) String descripcion,
            boolean cumplido) {
    }
}
