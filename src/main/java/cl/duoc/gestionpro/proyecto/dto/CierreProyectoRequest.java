package cl.duoc.gestionpro.proyecto.dto;

import java.time.LocalDate;

/** Cierre formal del proyecto. Sin fecha, se usa el término real de la última actividad (o hoy). */
public record CierreProyectoRequest(LocalDate fechaTerminoReal) {
}
