-- =============================================================================
-- Gestión Pro · V4: planificación de prueba de PRY-2026-01 (Módulo 3)
-- Parte del caso de prueba de la rúbrica: 5 profesionales, 4 etapas,
-- 12 actividades, 20 subactividades y 25 dependencias con los cuatro tipos
-- (FS, SS, FF, SF) y desfases positivos y negativos.
-- Las fechas planificadas quedan vacías: las calcula el MotorProgramacion al
-- arrancar la aplicación (ProgramacionInicial). Recursos, hitos, KPI y cambios
-- se cargan en migraciones posteriores.
-- =============================================================================

INSERT INTO tipo_profesional (categoria, tarifa_hora_ref) VALUES ('Jefe de proyecto', 32000);
INSERT INTO tipo_profesional (categoria, tarifa_hora_ref) VALUES ('Analista', 22000);
INSERT INTO tipo_profesional (categoria, tarifa_hora_ref) VALUES ('Desarrollador', 24000);
INSERT INTO tipo_profesional (categoria, tarifa_hora_ref) VALUES ('Analista QA', 20000);

INSERT INTO profesional (nombre, especialidad, tipo_profesional_id, valor_hora, horas_semana_disp, usuario_id)
SELECT 'Carla Muñoz', 'Gestión de proyectos', t.id, 32000, 40, (SELECT id FROM usuario WHERE correo = 'admin@gestionpro.cl') FROM tipo_profesional t WHERE t.categoria = 'Jefe de proyecto';
INSERT INTO profesional (nombre, especialidad, tipo_profesional_id, valor_hora, horas_semana_disp, usuario_id)
SELECT 'Diego Rojas', 'Análisis funcional y UX', t.id, 22000, 40, (SELECT id FROM usuario WHERE correo = 'diego.rojas@gestionpro.cl') FROM tipo_profesional t WHERE t.categoria = 'Analista';
INSERT INTO profesional (nombre, especialidad, tipo_profesional_id, valor_hora, horas_semana_disp, usuario_id)
SELECT 'Fernanda Soto', 'Arquitectura de software', t.id, 26000, 40, (SELECT id FROM usuario WHERE correo = 'fernanda.soto@gestionpro.cl') FROM tipo_profesional t WHERE t.categoria = 'Analista';
INSERT INTO profesional (nombre, especialidad, tipo_profesional_id, valor_hora, horas_semana_disp, usuario_id)
SELECT 'Tomás Vidal', 'Desarrollo fullstack', t.id, 24000, 40, (SELECT id FROM usuario WHERE correo = 'usuario@gestionpro.cl') FROM tipo_profesional t WHERE t.categoria = 'Desarrollador';
INSERT INTO profesional (nombre, especialidad, tipo_profesional_id, valor_hora, horas_semana_disp, usuario_id)
SELECT 'Valentina Pérez', 'Aseguramiento de calidad', t.id, 20000, 40, NULL FROM tipo_profesional t WHERE t.categoria = 'Analista QA';

-- Jerarquía. Las duraciones de etapas y actividades con hijos se derivan; aquí van en 0.

-- Etapa 1: Análisis
INSERT INTO actividad (proyecto_id, padre_id, tipo, orden, nombre, responsable_id, duracion_plan, fecha_inicio_real, fecha_termino_real, avance)
VALUES ((SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01'), NULL, 'ETAPA', 1, 'Análisis', (SELECT id FROM profesional WHERE nombre = 'Diego Rojas'), 0, NULL, NULL, 0);
INSERT INTO actividad (proyecto_id, padre_id, tipo, orden, nombre, responsable_id, duracion_plan, fecha_inicio_real, fecha_termino_real, avance)
VALUES ((SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01'), (SELECT id FROM actividad WHERE nombre = 'Análisis' AND proyecto_id = (SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01')), 'ACTIVIDAD', 1, 'Levantamiento de requerimientos', (SELECT id FROM profesional WHERE nombre = 'Diego Rojas'), 0, NULL, NULL, 0);
INSERT INTO actividad (proyecto_id, padre_id, tipo, orden, nombre, responsable_id, duracion_plan, fecha_inicio_real, fecha_termino_real, avance)
VALUES ((SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01'), (SELECT id FROM actividad WHERE nombre = 'Levantamiento de requerimientos' AND proyecto_id = (SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01')), 'SUBACTIVIDAD', 1, 'Entrevistas con usuarios', (SELECT id FROM profesional WHERE nombre = 'Diego Rojas'), 5, DATE '2026-09-01', DATE '2026-09-07', 100);
INSERT INTO actividad (proyecto_id, padre_id, tipo, orden, nombre, responsable_id, duracion_plan, fecha_inicio_real, fecha_termino_real, avance)
VALUES ((SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01'), (SELECT id FROM actividad WHERE nombre = 'Levantamiento de requerimientos' AND proyecto_id = (SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01')), 'SUBACTIVIDAD', 2, 'Documento de requerimientos', (SELECT id FROM profesional WHERE nombre = 'Diego Rojas'), 5, DATE '2026-09-08', DATE '2026-09-14', 100);
INSERT INTO actividad (proyecto_id, padre_id, tipo, orden, nombre, responsable_id, duracion_plan, fecha_inicio_real, fecha_termino_real, avance)
VALUES ((SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01'), (SELECT id FROM actividad WHERE nombre = 'Análisis' AND proyecto_id = (SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01')), 'ACTIVIDAD', 2, 'Modelo de datos', (SELECT id FROM profesional WHERE nombre = 'Fernanda Soto'), 0, NULL, NULL, 0);
INSERT INTO actividad (proyecto_id, padre_id, tipo, orden, nombre, responsable_id, duracion_plan, fecha_inicio_real, fecha_termino_real, avance)
VALUES ((SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01'), (SELECT id FROM actividad WHERE nombre = 'Modelo de datos' AND proyecto_id = (SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01')), 'SUBACTIVIDAD', 1, 'Modelo conceptual', (SELECT id FROM profesional WHERE nombre = 'Fernanda Soto'), 3, DATE '2026-09-10', DATE '2026-09-14', 100);
INSERT INTO actividad (proyecto_id, padre_id, tipo, orden, nombre, responsable_id, duracion_plan, fecha_inicio_real, fecha_termino_real, avance)
VALUES ((SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01'), (SELECT id FROM actividad WHERE nombre = 'Modelo de datos' AND proyecto_id = (SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01')), 'SUBACTIVIDAD', 2, 'Modelo físico', (SELECT id FROM profesional WHERE nombre = 'Fernanda Soto'), 5, DATE '2026-09-15', DATE '2026-09-22', 100);
INSERT INTO actividad (proyecto_id, padre_id, tipo, orden, nombre, responsable_id, duracion_plan, fecha_inicio_real, fecha_termino_real, avance)
VALUES ((SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01'), (SELECT id FROM actividad WHERE nombre = 'Análisis' AND proyecto_id = (SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01')), 'ACTIVIDAD', 3, 'Plan de proyecto', (SELECT id FROM profesional WHERE nombre = 'Carla Muñoz'), 4, DATE '2026-09-09', DATE '2026-09-14', 100);

-- Etapa 2: Diseño
INSERT INTO actividad (proyecto_id, padre_id, tipo, orden, nombre, responsable_id, duracion_plan, fecha_inicio_real, fecha_termino_real, avance)
VALUES ((SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01'), NULL, 'ETAPA', 2, 'Diseño', (SELECT id FROM profesional WHERE nombre = 'Fernanda Soto'), 0, NULL, NULL, 0);
INSERT INTO actividad (proyecto_id, padre_id, tipo, orden, nombre, responsable_id, duracion_plan, fecha_inicio_real, fecha_termino_real, avance)
VALUES ((SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01'), (SELECT id FROM actividad WHERE nombre = 'Diseño' AND proyecto_id = (SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01')), 'ACTIVIDAD', 1, 'Diseño de arquitectura', (SELECT id FROM profesional WHERE nombre = 'Fernanda Soto'), 0, NULL, NULL, 0);
INSERT INTO actividad (proyecto_id, padre_id, tipo, orden, nombre, responsable_id, duracion_plan, fecha_inicio_real, fecha_termino_real, avance)
VALUES ((SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01'), (SELECT id FROM actividad WHERE nombre = 'Diseño de arquitectura' AND proyecto_id = (SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01')), 'SUBACTIVIDAD', 1, 'Definición de componentes', (SELECT id FROM profesional WHERE nombre = 'Fernanda Soto'), 4, DATE '2026-09-23', DATE '2026-09-28', 100);
INSERT INTO actividad (proyecto_id, padre_id, tipo, orden, nombre, responsable_id, duracion_plan, fecha_inicio_real, fecha_termino_real, avance)
VALUES ((SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01'), (SELECT id FROM actividad WHERE nombre = 'Diseño de arquitectura' AND proyecto_id = (SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01')), 'SUBACTIVIDAD', 2, 'Diseño de integraciones', (SELECT id FROM profesional WHERE nombre = 'Fernanda Soto'), 5, DATE '2026-09-28', DATE '2026-10-02', 100);
INSERT INTO actividad (proyecto_id, padre_id, tipo, orden, nombre, responsable_id, duracion_plan, fecha_inicio_real, fecha_termino_real, avance)
VALUES ((SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01'), (SELECT id FROM actividad WHERE nombre = 'Diseño' AND proyecto_id = (SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01')), 'ACTIVIDAD', 2, 'Prototipo de interfaz', (SELECT id FROM profesional WHERE nombre = 'Diego Rojas'), 0, NULL, NULL, 0);
INSERT INTO actividad (proyecto_id, padre_id, tipo, orden, nombre, responsable_id, duracion_plan, fecha_inicio_real, fecha_termino_real, avance)
VALUES ((SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01'), (SELECT id FROM actividad WHERE nombre = 'Prototipo de interfaz' AND proyecto_id = (SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01')), 'SUBACTIVIDAD', 1, 'Prototipo de baja fidelidad', (SELECT id FROM profesional WHERE nombre = 'Diego Rojas'), 5, DATE '2026-09-25', DATE '2026-10-01', 100);
INSERT INTO actividad (proyecto_id, padre_id, tipo, orden, nombre, responsable_id, duracion_plan, fecha_inicio_real, fecha_termino_real, avance)
VALUES ((SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01'), (SELECT id FROM actividad WHERE nombre = 'Prototipo de interfaz' AND proyecto_id = (SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01')), 'SUBACTIVIDAD', 2, 'Prototipo navegable', (SELECT id FROM profesional WHERE nombre = 'Diego Rojas'), 8, DATE '2026-10-02', DATE '2026-10-15', 100);
INSERT INTO actividad (proyecto_id, padre_id, tipo, orden, nombre, responsable_id, duracion_plan, fecha_inicio_real, fecha_termino_real, avance)
VALUES ((SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01'), (SELECT id FROM actividad WHERE nombre = 'Diseño' AND proyecto_id = (SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01')), 'ACTIVIDAD', 3, 'Diseño de pruebas', (SELECT id FROM profesional WHERE nombre = 'Valentina Pérez'), 6, DATE '2026-09-28', NULL, 70);

-- Etapa 3: Construcción
INSERT INTO actividad (proyecto_id, padre_id, tipo, orden, nombre, responsable_id, duracion_plan, fecha_inicio_real, fecha_termino_real, avance)
VALUES ((SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01'), NULL, 'ETAPA', 3, 'Construcción', (SELECT id FROM profesional WHERE nombre = 'Tomás Vidal'), 0, NULL, NULL, 0);
INSERT INTO actividad (proyecto_id, padre_id, tipo, orden, nombre, responsable_id, duracion_plan, fecha_inicio_real, fecha_termino_real, avance)
VALUES ((SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01'), (SELECT id FROM actividad WHERE nombre = 'Construcción' AND proyecto_id = (SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01')), 'ACTIVIDAD', 1, 'Módulo de inventario', (SELECT id FROM profesional WHERE nombre = 'Tomás Vidal'), 0, NULL, NULL, 0);
INSERT INTO actividad (proyecto_id, padre_id, tipo, orden, nombre, responsable_id, duracion_plan, fecha_inicio_real, fecha_termino_real, avance)
VALUES ((SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01'), (SELECT id FROM actividad WHERE nombre = 'Módulo de inventario' AND proyecto_id = (SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01')), 'SUBACTIVIDAD', 1, 'Backend API inventario', (SELECT id FROM profesional WHERE nombre = 'Tomás Vidal'), 15, DATE '2026-10-05', NULL, 60);
INSERT INTO actividad (proyecto_id, padre_id, tipo, orden, nombre, responsable_id, duracion_plan, fecha_inicio_real, fecha_termino_real, avance)
VALUES ((SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01'), (SELECT id FROM actividad WHERE nombre = 'Módulo de inventario' AND proyecto_id = (SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01')), 'SUBACTIVIDAD', 2, 'Frontend inventario', (SELECT id FROM profesional WHERE nombre = 'Tomás Vidal'), 15, DATE '2026-10-19', NULL, 25);
INSERT INTO actividad (proyecto_id, padre_id, tipo, orden, nombre, responsable_id, duracion_plan, fecha_inicio_real, fecha_termino_real, avance)
VALUES ((SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01'), (SELECT id FROM actividad WHERE nombre = 'Construcción' AND proyecto_id = (SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01')), 'ACTIVIDAD', 2, 'Módulo de reportes', (SELECT id FROM profesional WHERE nombre = 'Tomás Vidal'), 0, NULL, NULL, 0);
INSERT INTO actividad (proyecto_id, padre_id, tipo, orden, nombre, responsable_id, duracion_plan, fecha_inicio_real, fecha_termino_real, avance)
VALUES ((SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01'), (SELECT id FROM actividad WHERE nombre = 'Módulo de reportes' AND proyecto_id = (SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01')), 'SUBACTIVIDAD', 1, 'Consultas y vistas', (SELECT id FROM profesional WHERE nombre = 'Tomás Vidal'), 8, NULL, NULL, 0);
INSERT INTO actividad (proyecto_id, padre_id, tipo, orden, nombre, responsable_id, duracion_plan, fecha_inicio_real, fecha_termino_real, avance)
VALUES ((SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01'), (SELECT id FROM actividad WHERE nombre = 'Módulo de reportes' AND proyecto_id = (SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01')), 'SUBACTIVIDAD', 2, 'Pantallas de reportes', (SELECT id FROM profesional WHERE nombre = 'Diego Rojas'), 7, NULL, NULL, 0);
INSERT INTO actividad (proyecto_id, padre_id, tipo, orden, nombre, responsable_id, duracion_plan, fecha_inicio_real, fecha_termino_real, avance)
VALUES ((SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01'), (SELECT id FROM actividad WHERE nombre = 'Construcción' AND proyecto_id = (SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01')), 'ACTIVIDAD', 3, 'Integración de módulos', (SELECT id FROM profesional WHERE nombre = 'Fernanda Soto'), 0, NULL, NULL, 0);
INSERT INTO actividad (proyecto_id, padre_id, tipo, orden, nombre, responsable_id, duracion_plan, fecha_inicio_real, fecha_termino_real, avance)
VALUES ((SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01'), (SELECT id FROM actividad WHERE nombre = 'Integración de módulos' AND proyecto_id = (SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01')), 'SUBACTIVIDAD', 1, 'Integración de servicios', (SELECT id FROM profesional WHERE nombre = 'Fernanda Soto'), 6, NULL, NULL, 0);
INSERT INTO actividad (proyecto_id, padre_id, tipo, orden, nombre, responsable_id, duracion_plan, fecha_inicio_real, fecha_termino_real, avance)
VALUES ((SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01'), (SELECT id FROM actividad WHERE nombre = 'Integración de módulos' AND proyecto_id = (SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01')), 'SUBACTIVIDAD', 2, 'Pruebas de integración', (SELECT id FROM profesional WHERE nombre = 'Valentina Pérez'), 4, NULL, NULL, 0);

-- Etapa 4: Implantación
INSERT INTO actividad (proyecto_id, padre_id, tipo, orden, nombre, responsable_id, duracion_plan, fecha_inicio_real, fecha_termino_real, avance)
VALUES ((SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01'), NULL, 'ETAPA', 4, 'Implantación', (SELECT id FROM profesional WHERE nombre = 'Carla Muñoz'), 0, NULL, NULL, 0);
INSERT INTO actividad (proyecto_id, padre_id, tipo, orden, nombre, responsable_id, duracion_plan, fecha_inicio_real, fecha_termino_real, avance)
VALUES ((SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01'), (SELECT id FROM actividad WHERE nombre = 'Implantación' AND proyecto_id = (SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01')), 'ACTIVIDAD', 1, 'Pruebas integrales', (SELECT id FROM profesional WHERE nombre = 'Valentina Pérez'), 0, NULL, NULL, 0);
INSERT INTO actividad (proyecto_id, padre_id, tipo, orden, nombre, responsable_id, duracion_plan, fecha_inicio_real, fecha_termino_real, avance)
VALUES ((SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01'), (SELECT id FROM actividad WHERE nombre = 'Pruebas integrales' AND proyecto_id = (SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01')), 'SUBACTIVIDAD', 1, 'Pruebas funcionales', (SELECT id FROM profesional WHERE nombre = 'Valentina Pérez'), 8, NULL, NULL, 0);
INSERT INTO actividad (proyecto_id, padre_id, tipo, orden, nombre, responsable_id, duracion_plan, fecha_inicio_real, fecha_termino_real, avance)
VALUES ((SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01'), (SELECT id FROM actividad WHERE nombre = 'Pruebas integrales' AND proyecto_id = (SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01')), 'SUBACTIVIDAD', 2, 'Pruebas de aceptación', (SELECT id FROM profesional WHERE nombre = 'Valentina Pérez'), 5, NULL, NULL, 0);
INSERT INTO actividad (proyecto_id, padre_id, tipo, orden, nombre, responsable_id, duracion_plan, fecha_inicio_real, fecha_termino_real, avance)
VALUES ((SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01'), (SELECT id FROM actividad WHERE nombre = 'Implantación' AND proyecto_id = (SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01')), 'ACTIVIDAD', 2, 'Capacitación', (SELECT id FROM profesional WHERE nombre = 'Diego Rojas'), 0, NULL, NULL, 0);
INSERT INTO actividad (proyecto_id, padre_id, tipo, orden, nombre, responsable_id, duracion_plan, fecha_inicio_real, fecha_termino_real, avance)
VALUES ((SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01'), (SELECT id FROM actividad WHERE nombre = 'Capacitación' AND proyecto_id = (SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01')), 'SUBACTIVIDAD', 1, 'Material de capacitación', (SELECT id FROM profesional WHERE nombre = 'Diego Rojas'), 5, NULL, NULL, 0);
INSERT INTO actividad (proyecto_id, padre_id, tipo, orden, nombre, responsable_id, duracion_plan, fecha_inicio_real, fecha_termino_real, avance)
VALUES ((SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01'), (SELECT id FROM actividad WHERE nombre = 'Capacitación' AND proyecto_id = (SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01')), 'SUBACTIVIDAD', 2, 'Sesiones de capacitación', (SELECT id FROM profesional WHERE nombre = 'Diego Rojas'), 5, NULL, NULL, 0);
INSERT INTO actividad (proyecto_id, padre_id, tipo, orden, nombre, responsable_id, duracion_plan, fecha_inicio_real, fecha_termino_real, avance)
VALUES ((SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01'), (SELECT id FROM actividad WHERE nombre = 'Implantación' AND proyecto_id = (SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01')), 'ACTIVIDAD', 3, 'Puesta en producción', (SELECT id FROM profesional WHERE nombre = 'Carla Muñoz'), 0, NULL, NULL, 0);
INSERT INTO actividad (proyecto_id, padre_id, tipo, orden, nombre, responsable_id, duracion_plan, fecha_inicio_real, fecha_termino_real, avance)
VALUES ((SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01'), (SELECT id FROM actividad WHERE nombre = 'Puesta en producción' AND proyecto_id = (SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01')), 'SUBACTIVIDAD', 1, 'Migración de datos', (SELECT id FROM profesional WHERE nombre = 'Tomás Vidal'), 4, NULL, NULL, 0);
INSERT INTO actividad (proyecto_id, padre_id, tipo, orden, nombre, responsable_id, duracion_plan, fecha_inicio_real, fecha_termino_real, avance)
VALUES ((SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01'), (SELECT id FROM actividad WHERE nombre = 'Puesta en producción' AND proyecto_id = (SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01')), 'SUBACTIVIDAD', 2, 'Paso a producción', (SELECT id FROM profesional WHERE nombre = 'Carla Muñoz'), 3, NULL, NULL, 0);

-- Dependencias entre elementos hoja
INSERT INTO dependencia (proyecto_id, predecesora_id, sucesora_id, tipo, desfase_dias)
SELECT a.proyecto_id, a.id, b.id, 'FS', 0 FROM actividad a JOIN actividad b ON b.proyecto_id = a.proyecto_id
 WHERE a.proyecto_id = (SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01') AND a.nombre = 'Entrevistas con usuarios' AND b.nombre = 'Documento de requerimientos';
INSERT INTO dependencia (proyecto_id, predecesora_id, sucesora_id, tipo, desfase_dias)
SELECT a.proyecto_id, a.id, b.id, 'SS', 2 FROM actividad a JOIN actividad b ON b.proyecto_id = a.proyecto_id
 WHERE a.proyecto_id = (SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01') AND a.nombre = 'Documento de requerimientos' AND b.nombre = 'Modelo conceptual';
INSERT INTO dependencia (proyecto_id, predecesora_id, sucesora_id, tipo, desfase_dias)
SELECT a.proyecto_id, a.id, b.id, 'FS', 0 FROM actividad a JOIN actividad b ON b.proyecto_id = a.proyecto_id
 WHERE a.proyecto_id = (SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01') AND a.nombre = 'Modelo conceptual' AND b.nombre = 'Modelo físico';
INSERT INTO dependencia (proyecto_id, predecesora_id, sucesora_id, tipo, desfase_dias)
SELECT a.proyecto_id, a.id, b.id, 'FF', 0 FROM actividad a JOIN actividad b ON b.proyecto_id = a.proyecto_id
 WHERE a.proyecto_id = (SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01') AND a.nombre = 'Documento de requerimientos' AND b.nombre = 'Plan de proyecto';
INSERT INTO dependencia (proyecto_id, predecesora_id, sucesora_id, tipo, desfase_dias)
SELECT a.proyecto_id, a.id, b.id, 'FS', 0 FROM actividad a JOIN actividad b ON b.proyecto_id = a.proyecto_id
 WHERE a.proyecto_id = (SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01') AND a.nombre = 'Modelo físico' AND b.nombre = 'Definición de componentes';
INSERT INTO dependencia (proyecto_id, predecesora_id, sucesora_id, tipo, desfase_dias)
SELECT a.proyecto_id, a.id, b.id, 'FS', -1 FROM actividad a JOIN actividad b ON b.proyecto_id = a.proyecto_id
 WHERE a.proyecto_id = (SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01') AND a.nombre = 'Definición de componentes' AND b.nombre = 'Diseño de integraciones';
INSERT INTO dependencia (proyecto_id, predecesora_id, sucesora_id, tipo, desfase_dias)
SELECT a.proyecto_id, a.id, b.id, 'SS', 2 FROM actividad a JOIN actividad b ON b.proyecto_id = a.proyecto_id
 WHERE a.proyecto_id = (SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01') AND a.nombre = 'Definición de componentes' AND b.nombre = 'Prototipo de baja fidelidad';
INSERT INTO dependencia (proyecto_id, predecesora_id, sucesora_id, tipo, desfase_dias)
SELECT a.proyecto_id, a.id, b.id, 'FS', 0 FROM actividad a JOIN actividad b ON b.proyecto_id = a.proyecto_id
 WHERE a.proyecto_id = (SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01') AND a.nombre = 'Prototipo de baja fidelidad' AND b.nombre = 'Prototipo navegable';
INSERT INTO dependencia (proyecto_id, predecesora_id, sucesora_id, tipo, desfase_dias)
SELECT a.proyecto_id, a.id, b.id, 'SS', 0 FROM actividad a JOIN actividad b ON b.proyecto_id = a.proyecto_id
 WHERE a.proyecto_id = (SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01') AND a.nombre = 'Diseño de integraciones' AND b.nombre = 'Diseño de pruebas';
INSERT INTO dependencia (proyecto_id, predecesora_id, sucesora_id, tipo, desfase_dias)
SELECT a.proyecto_id, a.id, b.id, 'FS', 0 FROM actividad a JOIN actividad b ON b.proyecto_id = a.proyecto_id
 WHERE a.proyecto_id = (SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01') AND a.nombre = 'Diseño de integraciones' AND b.nombre = 'Backend API inventario';
INSERT INTO dependencia (proyecto_id, predecesora_id, sucesora_id, tipo, desfase_dias)
SELECT a.proyecto_id, a.id, b.id, 'SS', 5 FROM actividad a JOIN actividad b ON b.proyecto_id = a.proyecto_id
 WHERE a.proyecto_id = (SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01') AND a.nombre = 'Backend API inventario' AND b.nombre = 'Frontend inventario';
INSERT INTO dependencia (proyecto_id, predecesora_id, sucesora_id, tipo, desfase_dias)
SELECT a.proyecto_id, a.id, b.id, 'FS', 0 FROM actividad a JOIN actividad b ON b.proyecto_id = a.proyecto_id
 WHERE a.proyecto_id = (SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01') AND a.nombre = 'Prototipo navegable' AND b.nombre = 'Frontend inventario';
INSERT INTO dependencia (proyecto_id, predecesora_id, sucesora_id, tipo, desfase_dias)
SELECT a.proyecto_id, a.id, b.id, 'FS', 0 FROM actividad a JOIN actividad b ON b.proyecto_id = a.proyecto_id
 WHERE a.proyecto_id = (SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01') AND a.nombre = 'Backend API inventario' AND b.nombre = 'Consultas y vistas';
INSERT INTO dependencia (proyecto_id, predecesora_id, sucesora_id, tipo, desfase_dias)
SELECT a.proyecto_id, a.id, b.id, 'FS', -2 FROM actividad a JOIN actividad b ON b.proyecto_id = a.proyecto_id
 WHERE a.proyecto_id = (SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01') AND a.nombre = 'Consultas y vistas' AND b.nombre = 'Pantallas de reportes';
INSERT INTO dependencia (proyecto_id, predecesora_id, sucesora_id, tipo, desfase_dias)
SELECT a.proyecto_id, a.id, b.id, 'FS', 0 FROM actividad a JOIN actividad b ON b.proyecto_id = a.proyecto_id
 WHERE a.proyecto_id = (SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01') AND a.nombre = 'Frontend inventario' AND b.nombre = 'Integración de servicios';
INSERT INTO dependencia (proyecto_id, predecesora_id, sucesora_id, tipo, desfase_dias)
SELECT a.proyecto_id, a.id, b.id, 'FS', 0 FROM actividad a JOIN actividad b ON b.proyecto_id = a.proyecto_id
 WHERE a.proyecto_id = (SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01') AND a.nombre = 'Pantallas de reportes' AND b.nombre = 'Integración de servicios';
INSERT INTO dependencia (proyecto_id, predecesora_id, sucesora_id, tipo, desfase_dias)
SELECT a.proyecto_id, a.id, b.id, 'FS', 0 FROM actividad a JOIN actividad b ON b.proyecto_id = a.proyecto_id
 WHERE a.proyecto_id = (SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01') AND a.nombre = 'Integración de servicios' AND b.nombre = 'Pruebas de integración';
INSERT INTO dependencia (proyecto_id, predecesora_id, sucesora_id, tipo, desfase_dias)
SELECT a.proyecto_id, a.id, b.id, 'FS', 0 FROM actividad a JOIN actividad b ON b.proyecto_id = a.proyecto_id
 WHERE a.proyecto_id = (SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01') AND a.nombre = 'Pruebas de integración' AND b.nombre = 'Pruebas funcionales';
INSERT INTO dependencia (proyecto_id, predecesora_id, sucesora_id, tipo, desfase_dias)
SELECT a.proyecto_id, a.id, b.id, 'FS', 0 FROM actividad a JOIN actividad b ON b.proyecto_id = a.proyecto_id
 WHERE a.proyecto_id = (SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01') AND a.nombre = 'Diseño de pruebas' AND b.nombre = 'Pruebas funcionales';
INSERT INTO dependencia (proyecto_id, predecesora_id, sucesora_id, tipo, desfase_dias)
SELECT a.proyecto_id, a.id, b.id, 'FS', 0 FROM actividad a JOIN actividad b ON b.proyecto_id = a.proyecto_id
 WHERE a.proyecto_id = (SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01') AND a.nombre = 'Pruebas funcionales' AND b.nombre = 'Pruebas de aceptación';
INSERT INTO dependencia (proyecto_id, predecesora_id, sucesora_id, tipo, desfase_dias)
SELECT a.proyecto_id, a.id, b.id, 'SF', 5 FROM actividad a JOIN actividad b ON b.proyecto_id = a.proyecto_id
 WHERE a.proyecto_id = (SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01') AND a.nombre = 'Pruebas funcionales' AND b.nombre = 'Material de capacitación';
INSERT INTO dependencia (proyecto_id, predecesora_id, sucesora_id, tipo, desfase_dias)
SELECT a.proyecto_id, a.id, b.id, 'FS', 0 FROM actividad a JOIN actividad b ON b.proyecto_id = a.proyecto_id
 WHERE a.proyecto_id = (SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01') AND a.nombre = 'Material de capacitación' AND b.nombre = 'Sesiones de capacitación';
INSERT INTO dependencia (proyecto_id, predecesora_id, sucesora_id, tipo, desfase_dias)
SELECT a.proyecto_id, a.id, b.id, 'FS', 0 FROM actividad a JOIN actividad b ON b.proyecto_id = a.proyecto_id
 WHERE a.proyecto_id = (SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01') AND a.nombre = 'Pruebas de aceptación' AND b.nombre = 'Migración de datos';
INSERT INTO dependencia (proyecto_id, predecesora_id, sucesora_id, tipo, desfase_dias)
SELECT a.proyecto_id, a.id, b.id, 'FF', 2 FROM actividad a JOIN actividad b ON b.proyecto_id = a.proyecto_id
 WHERE a.proyecto_id = (SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01') AND a.nombre = 'Sesiones de capacitación' AND b.nombre = 'Paso a producción';
INSERT INTO dependencia (proyecto_id, predecesora_id, sucesora_id, tipo, desfase_dias)
SELECT a.proyecto_id, a.id, b.id, 'FS', 0 FROM actividad a JOIN actividad b ON b.proyecto_id = a.proyecto_id
 WHERE a.proyecto_id = (SELECT id FROM proyecto WHERE codigo = 'PRY-2026-01') AND a.nombre = 'Migración de datos' AND b.nombre = 'Paso a producción';
