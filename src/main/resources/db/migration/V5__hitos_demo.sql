-- =============================================================================
-- Gestión Pro · V5: hitos de PRY-2026-01 (parte del caso de prueba)
-- Seis hitos asociados a actividades: dos cumplidos (uno fuera de plazo),
-- uno vencido sin cumplir y tres pendientes. "Atrasado" no se guarda: se calcula.
-- Los criterios de aceptación y evidencias llegan con el Módulo 6.
-- =============================================================================

INSERT INTO hito (proyecto_id, codigo, descripcion, responsable_id, fecha_comprometida, fecha_real, estado)
SELECT p.id, 'H1', 'Requerimientos aprobados por el cliente', pr.id, DATE '2026-09-15', DATE '2026-09-14', 'CUMPLIDO' FROM proyecto p, profesional pr
 WHERE p.codigo = 'PRY-2026-01' AND pr.nombre = 'Diego Rojas';
INSERT INTO hito_actividad (hito_id, actividad_id, proyecto_id)
SELECT h.id, a.id, h.proyecto_id FROM hito h JOIN actividad a ON a.proyecto_id = h.proyecto_id
 WHERE h.proyecto_id = (SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01') AND h.codigo = 'H1' AND a.nombre = 'Documento de requerimientos';
INSERT INTO hito_actividad (hito_id, actividad_id, proyecto_id)
SELECT h.id, a.id, h.proyecto_id FROM hito h JOIN actividad a ON a.proyecto_id = h.proyecto_id
 WHERE h.proyecto_id = (SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01') AND h.codigo = 'H1' AND a.nombre = 'Plan de proyecto';

INSERT INTO hito (proyecto_id, codigo, descripcion, responsable_id, fecha_comprometida, fecha_real, estado)
SELECT p.id, 'H2', 'Diseño de arquitectura e interfaz aprobado', pr.id, DATE '2026-10-09', DATE '2026-10-15', 'CUMPLIDO' FROM proyecto p, profesional pr
 WHERE p.codigo = 'PRY-2026-01' AND pr.nombre = 'Fernanda Soto';
INSERT INTO hito_actividad (hito_id, actividad_id, proyecto_id)
SELECT h.id, a.id, h.proyecto_id FROM hito h JOIN actividad a ON a.proyecto_id = h.proyecto_id
 WHERE h.proyecto_id = (SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01') AND h.codigo = 'H2' AND a.nombre = 'Diseño de integraciones';
INSERT INTO hito_actividad (hito_id, actividad_id, proyecto_id)
SELECT h.id, a.id, h.proyecto_id FROM hito h JOIN actividad a ON a.proyecto_id = h.proyecto_id
 WHERE h.proyecto_id = (SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01') AND h.codigo = 'H2' AND a.nombre = 'Prototipo navegable';

INSERT INTO hito (proyecto_id, codigo, descripcion, responsable_id, fecha_comprometida, fecha_real, estado)
SELECT p.id, 'H3', 'Módulo de inventario en ambiente de pruebas', pr.id, DATE '2026-10-30', NULL, 'PENDIENTE' FROM proyecto p, profesional pr
 WHERE p.codigo = 'PRY-2026-01' AND pr.nombre = 'Tomás Vidal';
INSERT INTO hito_actividad (hito_id, actividad_id, proyecto_id)
SELECT h.id, a.id, h.proyecto_id FROM hito h JOIN actividad a ON a.proyecto_id = h.proyecto_id
 WHERE h.proyecto_id = (SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01') AND h.codigo = 'H3' AND a.nombre = 'Backend API inventario';
INSERT INTO hito_actividad (hito_id, actividad_id, proyecto_id)
SELECT h.id, a.id, h.proyecto_id FROM hito h JOIN actividad a ON a.proyecto_id = h.proyecto_id
 WHERE h.proyecto_id = (SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01') AND h.codigo = 'H3' AND a.nombre = 'Frontend inventario';

INSERT INTO hito (proyecto_id, codigo, descripcion, responsable_id, fecha_comprometida, fecha_real, estado)
SELECT p.id, 'H4', 'Integración de módulos completada', pr.id, DATE '2026-11-27', NULL, 'PENDIENTE' FROM proyecto p, profesional pr
 WHERE p.codigo = 'PRY-2026-01' AND pr.nombre = 'Fernanda Soto';
INSERT INTO hito_actividad (hito_id, actividad_id, proyecto_id)
SELECT h.id, a.id, h.proyecto_id FROM hito h JOIN actividad a ON a.proyecto_id = h.proyecto_id
 WHERE h.proyecto_id = (SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01') AND h.codigo = 'H4' AND a.nombre = 'Pruebas de integración';

INSERT INTO hito (proyecto_id, codigo, descripcion, responsable_id, fecha_comprometida, fecha_real, estado)
SELECT p.id, 'H5', 'Aceptación formal del cliente', pr.id, DATE '2026-12-16', NULL, 'PENDIENTE' FROM proyecto p, profesional pr
 WHERE p.codigo = 'PRY-2026-01' AND pr.nombre = 'Valentina Pérez';
INSERT INTO hito_actividad (hito_id, actividad_id, proyecto_id)
SELECT h.id, a.id, h.proyecto_id FROM hito h JOIN actividad a ON a.proyecto_id = h.proyecto_id
 WHERE h.proyecto_id = (SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01') AND h.codigo = 'H5' AND a.nombre = 'Pruebas de aceptación';

INSERT INTO hito (proyecto_id, codigo, descripcion, responsable_id, fecha_comprometida, fecha_real, estado)
SELECT p.id, 'H6', 'Sistema en producción con usuarios capacitados', pr.id, DATE '2027-01-08', NULL, 'PENDIENTE' FROM proyecto p, profesional pr
 WHERE p.codigo = 'PRY-2026-01' AND pr.nombre = 'Carla Muñoz';
INSERT INTO hito_actividad (hito_id, actividad_id, proyecto_id)
SELECT h.id, a.id, h.proyecto_id FROM hito h JOIN actividad a ON a.proyecto_id = h.proyecto_id
 WHERE h.proyecto_id = (SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01') AND h.codigo = 'H6' AND a.nombre = 'Paso a producción';
INSERT INTO hito_actividad (hito_id, actividad_id, proyecto_id)
SELECT h.id, a.id, h.proyecto_id FROM hito h JOIN actividad a ON a.proyecto_id = h.proyecto_id
 WHERE h.proyecto_id = (SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01') AND h.codigo = 'H6' AND a.nombre = 'Sesiones de capacitación';
