/**
 * Módulo 3: actividades (jerarquía etapa → actividad → subactividad), dependencias FS/SS/FF/SF
 * con desfase, calendario laboral y el MotorProgramacion.
 *
 * MotorProgramacion: orden topológico de las dependencias (rechaza ciclos con ReglaNegocioException
 * "DEPENDENCIA_CIRCULAR"), calcula inicio y término planificados en días hábiles saltando fines de semana
 * y días no laborables, y deriva las fechas de etapas y actividades padre desde sus hijos.
 */
package cl.duoc.gestionpro.planificacion;
