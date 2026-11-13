-- SC-005 reduce los traslados a bodegas regionales de 4 a 1. La única asignación de ese recurso
-- (Sesiones de capacitación) usa 4 viajes, así que la solicitud también debe bajar esa asignación;
-- si no, al aprobarla el recurso quedaría con menos capacidad que lo asignado (RECURSO_INSUFICIENTE).
-- La capacidad se valida después de aplicar todas las modificaciones, por lo que el orden no importa.
INSERT INTO cambio_modificacion (solicitud_id, entidad, entidad_id, campo, valor_anterior, valor_nuevo, aplicada, fecha_aplicacion)
SELECT s.id, 'ASIGNACION_RECURSO', ar.id, 'cantidad', '4', '1', 0, NULL
  FROM solicitud_cambio s
  JOIN proyecto p ON p.id = s.proyecto_id AND p.codigo = 'PRY-2026-01'
  JOIN actividad a ON a.proyecto_id = p.id AND a.nombre = 'Sesiones de capacitación'
  JOIN asignacion_recurso ar ON ar.actividad_id = a.id
  JOIN recurso r ON r.id = ar.recurso_id AND r.descripcion = 'Traslados a bodegas regionales'
 WHERE s.codigo = 'SC-005'
   AND NOT EXISTS (SELECT 1 FROM cambio_modificacion m
                    WHERE m.solicitud_id = s.id AND m.entidad = 'ASIGNACION_RECURSO' AND m.entidad_id = ar.id AND m.campo = 'cantidad');
