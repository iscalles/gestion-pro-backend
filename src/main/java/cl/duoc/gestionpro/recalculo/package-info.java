/**
 * Cadena única de recálculo que llaman todas las operaciones que modifican datos:
 * fechas (MotorProgramacion) → costos → avance del proyecto → KPI (nuevas mediciones) → alertas.
 * Corre dentro de la transacción de quien la llama, para que un cambio se aplique completo o no se aplique.
 */
package cl.duoc.gestionpro.recalculo;
