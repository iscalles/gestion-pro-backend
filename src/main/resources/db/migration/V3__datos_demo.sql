-- =============================================================================
-- Gestión Pro · V3: datos mínimos para trabajar en local
-- Cuentas de demostración (contraseña demo1234), calendario 2026-2027 y los
-- proyectos que muestra el frontend. El caso de prueba completo va en V4.
-- =============================================================================

INSERT INTO usuario (nombre, correo, clave_hash, rol) VALUES
  ('Carla Muñoz', 'admin@gestionpro.cl', '$2a$10$jeial9s1zd/xCU26fJ.V8Opmjz7vPW7Eue.dePNQ13D.CSPXdK3rC', 'ADMINISTRADOR');
INSERT INTO usuario (nombre, correo, clave_hash, rol) VALUES
  ('Tomás Vidal', 'usuario@gestionpro.cl', '$2a$10$jeial9s1zd/xCU26fJ.V8Opmjz7vPW7Eue.dePNQ13D.CSPXdK3rC', 'USUARIO_PROYECTO');
INSERT INTO usuario (nombre, correo, clave_hash, rol) VALUES
  ('Diego Rojas', 'diego.rojas@gestionpro.cl', '$2a$10$jeial9s1zd/xCU26fJ.V8Opmjz7vPW7Eue.dePNQ13D.CSPXdK3rC', 'USUARIO_PROYECTO');
INSERT INTO usuario (nombre, correo, clave_hash, rol) VALUES
  ('Fernanda Soto', 'fernanda.soto@gestionpro.cl', '$2a$10$jeial9s1zd/xCU26fJ.V8Opmjz7vPW7Eue.dePNQ13D.CSPXdK3rC', 'USUARIO_PROYECTO');

INSERT INTO calendario (nombre) VALUES ('Chile lunes a viernes');

INSERT INTO dia_no_laborable (calendario_id, fecha, motivo)
SELECT c.id, d.fecha, d.motivo
  FROM calendario c,
       (SELECT DATE '2026-09-18' fecha, 'Independencia Nacional' motivo FROM dual UNION ALL
        SELECT DATE '2026-09-19', 'Glorias del Ejército' FROM dual UNION ALL
        SELECT DATE '2026-10-12', 'Encuentro de Dos Mundos' FROM dual UNION ALL
        SELECT DATE '2026-10-31', 'Iglesias Evangélicas' FROM dual UNION ALL
        SELECT DATE '2026-11-01', 'Todos los Santos' FROM dual UNION ALL
        SELECT DATE '2026-12-08', 'Inmaculada Concepción' FROM dual UNION ALL
        SELECT DATE '2026-12-25', 'Navidad' FROM dual UNION ALL
        SELECT DATE '2027-01-01', 'Año Nuevo' FROM dual) d
 WHERE c.nombre = 'Chile lunes a viernes';

INSERT INTO proyecto (codigo, nombre, descripcion, responsable_id, calendario_id, fecha_inicio_plan, fecha_termino_plan, fecha_inicio_real, presupuesto, estado, avance)
SELECT 'PRY-2026-01', 'Sistema de Inventario Regional', 'Control de stock en bodegas regionales.', u.id, c.id,
       DATE '2026-09-01', DATE '2027-01-29', DATE '2026-09-01', 48500000, 'EN_EJECUCION', 36
  FROM usuario u, calendario c WHERE u.correo = 'admin@gestionpro.cl';
INSERT INTO proyecto (codigo, nombre, descripcion, responsable_id, calendario_id, fecha_inicio_plan, fecha_termino_plan, fecha_inicio_real, presupuesto, estado, avance)
SELECT 'PRY-2026-02', 'Portal de Proveedores', 'Autoatención para proveedores.', u.id, c.id,
       DATE '2026-07-15', DATE '2026-12-18', DATE '2026-07-15', 62000000, 'EN_EJECUCION', 71
  FROM usuario u, calendario c WHERE u.correo = 'diego.rojas@gestionpro.cl';
INSERT INTO proyecto (codigo, nombre, descripcion, responsable_id, calendario_id, fecha_inicio_plan, fecha_termino_plan, presupuesto, estado, avance)
SELECT 'PRY-2026-03', 'App de Inspecciones en Terreno', 'Registro móvil de inspecciones.', u.id, c.id,
       DATE '2027-01-04', DATE '2027-06-30', 37800000, 'PLANIFICADO', 0
  FROM usuario u, calendario c WHERE u.correo = 'admin@gestionpro.cl';
INSERT INTO proyecto (codigo, nombre, descripcion, responsable_id, calendario_id, fecha_inicio_plan, fecha_termino_plan, fecha_inicio_real, fecha_termino_real, presupuesto, estado, avance)
SELECT 'PRY-2026-04', 'Migración de Mesa de Ayuda', 'Cambio de plataforma de tickets.', u.id, c.id,
       DATE '2026-03-02', DATE '2026-07-31', DATE '2026-03-02', DATE '2026-07-31', 21400000, 'TERMINADO', 100
  FROM usuario u, calendario c WHERE u.correo = 'fernanda.soto@gestionpro.cl';

INSERT INTO proyecto_miembro (proyecto_id, usuario_id)
SELECT p.id, u.id FROM proyecto p, usuario u
 WHERE p.codigo = 'PRY-2026-01' AND u.correo = 'usuario@gestionpro.cl';
