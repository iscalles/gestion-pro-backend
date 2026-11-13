package cl.duoc.gestionpro.evidencia;

import java.nio.charset.StandardCharsets;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import cl.duoc.gestionpro.evidencia.dto.EvidenciaDto;
import cl.duoc.gestionpro.proyecto.AccesoProyecto;
import cl.duoc.gestionpro.recalculo.RecalculoService;

@RestController
public class EvidenciaController {

    private final EvidenciaService servicio;
    private final AccesoProyecto acceso;
    private final RecalculoService recalculo;

    public EvidenciaController(EvidenciaService servicio, AccesoProyecto acceso, RecalculoService recalculo) {
        this.servicio = servicio;
        this.acceso = acceso;
        this.recalculo = recalculo;
    }

    @PostMapping(path = "/api/proyectos/{proyectoId}/hitos/{hitoId}/evidencias", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public EvidenciaDto subirAHito(@AuthenticationPrincipal Jwt jwt, @PathVariable Long proyectoId, @PathVariable Long hitoId,
            @RequestParam("archivo") MultipartFile archivo) {
        acceso.verificar(jwt, proyectoId);
        EvidenciaDto dto = servicio.subirAHito(proyectoId, hitoId, archivo, Long.valueOf(jwt.getSubject()));
        recalculo.recalcularProyecto(proyectoId); // "hitos cumplidos con evidencia" puede cambiar
        return dto;
    }

    @PostMapping(path = "/api/proyectos/{proyectoId}/cambios/{solicitudId}/evidencias", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public EvidenciaDto subirASolicitud(@AuthenticationPrincipal Jwt jwt, @PathVariable Long proyectoId,
            @PathVariable Long solicitudId, @RequestParam("archivo") MultipartFile archivo) {
        acceso.verificar(jwt, proyectoId);
        return servicio.subirASolicitud(proyectoId, solicitudId, archivo, Long.valueOf(jwt.getSubject()));
    }

    @GetMapping("/api/evidencias/{id}/archivo")
    public ResponseEntity<byte[]> descargar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        acceso.verificar(jwt, servicio.proyectoDe(id));
        Evidencia e = servicio.archivo(id);
        MediaType tipo;
        try {
            tipo = MediaType.parseMediaType(e.getTipoMime());
        } catch (IllegalArgumentException ex) {
            tipo = MediaType.APPLICATION_OCTET_STREAM;
        }
        return ResponseEntity.ok()
                .contentType(tipo)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(e.getNombreArchivo(), StandardCharsets.UTF_8).build().toString())
                .body(e.getContenido());
    }

    @DeleteMapping("/api/evidencias/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        Long proyectoId = servicio.proyectoDe(id);
        acceso.verificar(jwt, proyectoId);
        Long hito = servicio.eliminar(id, Long.valueOf(jwt.getSubject()), "ADMINISTRADOR".equals(jwt.getClaimAsString("rol")));
        if (hito != null) {
            recalculo.recalcularProyecto(proyectoId);
        }
    }
}
