-- =============================================================================
-- Gestión Pro · V6: resto del caso de prueba de PRY-2026-01 (Módulos 5 a 9)
--   * Dotación por tipo de profesional, 10 recursos de los 4 tipos
--     (licencias, materiales, equipamiento, otros) y sus asignaciones.
--   * Asignaciones de los 5 profesionales a las actividades hoja (Tomás Vidal
--     queda sobreasignado en las semanas en que se cruzan backend y frontend).
--   * Criterios de aceptación y evidencias de los 6 hitos.
--   * 8 KPI de proyecto e hitos y 12 KPI de gestión del cambio, con histórico.
--   * 5 solicitudes de cambio en distintos estados con sus diez etapas,
--     modificaciones (aplicadas y pendientes) y acciones correctivas.
-- Las semanas de cada asignación y los costos los vuelve a calcular el backend
-- al arrancar (RecalculoService), igual que las fechas planificadas.
-- =============================================================================

-- Título corto de la solicitud y efecto real medido al aprobarla (trazabilidad)
ALTER TABLE solicitud_cambio ADD (
  titulo              VARCHAR2(150),
  impacto_plazo_real  NUMBER(5),
  impacto_costo_real  NUMBER(14,2)
);

INSERT INTO variable_kpi VALUES ('criterios_cumplidos',       'Criterios de aceptación de los hitos marcados como cumplidos', 'crit.',  'PROYECTO');
INSERT INTO variable_kpi VALUES ('criterios_total',           'Criterios de aceptación definidos en los hitos',               'crit.',  'PROYECTO');
INSERT INTO variable_kpi VALUES ('trabajo_planificado_fecha', 'Días de trabajo que según el plan debían estar completos hoy', 'días',   'PROYECTO');
INSERT INTO variable_kpi VALUES ('costo_planificado_total',   'Costo planificado total del proyecto (BAC)',                   'pesos',  'PROYECTO');

DECLARE
  v_pry      NUMBER;
  v_carla    NUMBER;
  v_tomas    NUMBER;
  v_diego    NUMBER;
  v_fernanda NUMBER;
  v_id       NUMBER;

  FUNCTION usr(p_correo VARCHAR2) RETURN NUMBER IS
    v NUMBER;
  BEGIN
    SELECT id INTO v FROM usuario WHERE correo = p_correo;
    RETURN v;
  END;

  FUNCTION act(p_nombre VARCHAR2) RETURN NUMBER IS
    v NUMBER;
  BEGIN
    SELECT id INTO v FROM actividad WHERE proyecto_id = v_pry AND nombre = p_nombre;
    RETURN v;
  END;

  FUNCTION prof(p_nombre VARCHAR2) RETURN NUMBER IS
    v NUMBER;
  BEGIN
    SELECT id INTO v FROM profesional WHERE nombre = p_nombre;
    RETURN v;
  END;

  FUNCTION rec(p_descripcion VARCHAR2) RETURN NUMBER IS
    v NUMBER;
  BEGIN
    SELECT id INTO v FROM recurso WHERE proyecto_id = v_pry AND descripcion = p_descripcion;
    RETURN v;
  END;

  FUNCTION hito_id(p_codigo VARCHAR2) RETURN NUMBER IS
    v NUMBER;
  BEGIN
    SELECT id INTO v FROM hito WHERE proyecto_id = v_pry AND codigo = p_codigo;
    RETURN v;
  END;

  FUNCTION sol(p_codigo VARCHAR2) RETURN NUMBER IS
    v NUMBER;
  BEGIN
    SELECT id INTO v FROM solicitud_cambio WHERE proyecto_id = v_pry AND codigo = p_codigo;
    RETURN v;
  END;

  FUNCTION kpi_id(p_codigo VARCHAR2) RETURN NUMBER IS
    v NUMBER;
  BEGIN
    SELECT id INTO v FROM kpi WHERE proyecto_id = v_pry AND codigo = p_codigo;
    RETURN v;
  END;

  FUNCTION asignacion_de(p_actividad VARCHAR2, p_profesional VARCHAR2) RETURN NUMBER IS
    v_act  NUMBER := act(p_actividad);
    v_prof NUMBER := prof(p_profesional);
    v      NUMBER;
  BEGIN
    SELECT id INTO v FROM asignacion_profesional WHERE actividad_id = v_act AND profesional_id = v_prof;
    RETURN v;
  END;

  PROCEDURE requerimiento(p_categoria VARCHAR2, p_cantidad NUMBER, p_tarifa NUMBER) IS
  BEGIN
    INSERT INTO requerimiento_profesional (proyecto_id, tipo_profesional_id, cantidad_requerida, tarifa_hora)
    SELECT v_pry, id, p_cantidad, p_tarifa FROM tipo_profesional WHERE categoria = p_categoria;
  END;

  -- semanas = duración en días hábiles / 5 (el backend las recalcula con el calendario)
  PROCEDURE asig_prof(p_actividad VARCHAR2, p_profesional VARCHAR2, p_horas NUMBER, p_horas_reales NUMBER DEFAULT 0) IS
    v_act   NUMBER := act(p_actividad);
    v_prof  NUMBER := prof(p_profesional);
    v_valor NUMBER;
    v_dur   NUMBER;
  BEGIN
    SELECT valor_hora INTO v_valor FROM profesional WHERE id = v_prof;
    SELECT duracion_plan INTO v_dur FROM actividad WHERE id = v_act;
    INSERT INTO asignacion_profesional (actividad_id, profesional_id, horas_semana, semanas, valor_hora, horas_reales, costo_real)
    VALUES (v_act, v_prof, p_horas, ROUND(GREATEST(v_dur, 1) / 5, 2), v_valor, p_horas_reales, p_horas_reales * v_valor);
  END;

  PROCEDURE recurso_(p_tipo VARCHAR2, p_descripcion VARCHAR2, p_detalle VARCHAR2, p_unidad VARCHAR2,
                     p_cantidad NUMBER, p_costo NUMBER, p_periodo NUMBER) IS
  BEGIN
    INSERT INTO recurso (proyecto_id, tipo, descripcion, detalle, unidad, cantidad, costo_unitario, periodo)
    VALUES (v_pry, p_tipo, p_descripcion, p_detalle, p_unidad, p_cantidad, p_costo, p_periodo);
  END;

  PROCEDURE asig_rec(p_actividad VARCHAR2, p_recurso VARCHAR2, p_cantidad NUMBER, p_periodo NUMBER, p_real NUMBER DEFAULT 0) IS
    v_act   NUMBER := act(p_actividad);
    v_rec   NUMBER := rec(p_recurso);
    v_costo NUMBER;
  BEGIN
    SELECT costo_unitario INTO v_costo FROM recurso WHERE id = v_rec;
    INSERT INTO asignacion_recurso (actividad_id, recurso_id, cantidad, costo_unitario, periodo, costo_real)
    VALUES (v_act, v_rec, p_cantidad, v_costo, p_periodo, p_real);
  END;

  PROCEDURE criterio(p_hito VARCHAR2, p_orden NUMBER, p_descripcion VARCHAR2, p_cumplido NUMBER) IS
    v_h NUMBER := hito_id(p_hito);
  BEGIN
    INSERT INTO hito_criterio (hito_id, orden, descripcion, cumplido) VALUES (v_h, p_orden, p_descripcion, p_cumplido);
  END;

  PROCEDURE evidencia_hito(p_hito VARCHAR2, p_nombre VARCHAR2, p_texto VARCHAR2, p_usuario NUMBER, p_fecha DATE) IS
    v_h NUMBER := hito_id(p_hito);
  BEGIN
    INSERT INTO evidencia (hito_id, nombre_archivo, tipo_mime, tamano_bytes, contenido, subido_por_id, subido_en)
    VALUES (v_h, p_nombre, 'text/plain', LENGTHB(p_texto), TO_BLOB(UTL_RAW.CAST_TO_RAW(p_texto)), p_usuario,
            CAST(p_fecha AS TIMESTAMP) + INTERVAL '10' HOUR);
  END;

  PROCEDURE evidencia_sol(p_sol VARCHAR2, p_nombre VARCHAR2, p_texto VARCHAR2, p_usuario NUMBER, p_fecha DATE) IS
    v_s NUMBER := sol(p_sol);
  BEGIN
    INSERT INTO evidencia (solicitud_id, nombre_archivo, tipo_mime, tamano_bytes, contenido, subido_por_id, subido_en)
    VALUES (v_s, p_nombre, 'text/plain', LENGTHB(p_texto), TO_BLOB(UTL_RAW.CAST_TO_RAW(p_texto)), p_usuario,
            CAST(p_fecha AS TIMESTAMP) + INTERVAL '10' HOUR);
  END;

  -- Las variables usadas por la fórmula se registran en kpi_variable (igual que hace el backend al guardar)
  PROCEDURE kpi_(p_codigo VARCHAR2, p_nombre VARCHAR2, p_objetivo VARCHAR2, p_categoria VARCHAR2, p_ambito VARCHAR2,
                 p_ref NUMBER, p_formula VARCHAR2, p_unidad VARCHAR2, p_sentido VARCHAR2, p_meta NUMBER,
                 p_verde_min NUMBER, p_verde_max NUMBER, p_ambar_min NUMBER, p_ambar_max NUMBER,
                 p_periodicidad VARCHAR2, p_fuente VARCHAR2, p_responsable NUMBER) IS
    v_k NUMBER;
  BEGIN
    INSERT INTO kpi (proyecto_id, codigo, nombre, objetivo, categoria, ambito, ambito_ref_id, formula, unidad, sentido, meta,
                     verde_min, verde_max, ambar_min, ambar_max, periodicidad, fuente, responsable_id)
    VALUES (v_pry, p_codigo, p_nombre, p_objetivo, p_categoria, p_ambito, p_ref, p_formula, p_unidad, p_sentido, p_meta,
            p_verde_min, p_verde_max, p_ambar_min, p_ambar_max, p_periodicidad, p_fuente, p_responsable)
    RETURNING id INTO v_k;
    IF p_formula IS NOT NULL THEN
      INSERT INTO kpi_variable (kpi_id, variable_codigo)
      SELECT v_k, codigo FROM variable_kpi
       WHERE REGEXP_LIKE(p_formula, '(^|[^a-z0-9_])' || codigo || '($|[^a-z0-9_])');
    END IF;
  END;

  PROCEDURE vincular_kpi_hito(p_kpi VARCHAR2, p_hito VARCHAR2) IS
    v_k NUMBER := kpi_id(p_kpi);
    v_h NUMBER := hito_id(p_hito);
  BEGIN
    INSERT INTO kpi_hito (kpi_id, hito_id) VALUES (v_k, v_h);
  END;

  -- Histórico: el semáforo se calcula con los mismos límites que usa el backend
  PROCEDURE med(p_kpi VARCHAR2, p_fecha DATE, p_valor NUMBER, p_origen VARCHAR2 DEFAULT 'PROGRAMADA', p_obs VARCHAR2 DEFAULT NULL) IS
    k     kpi%ROWTYPE;
    v_sem VARCHAR2(5);
    v_reg NUMBER;
  BEGIN
    SELECT * INTO k FROM kpi WHERE proyecto_id = v_pry AND codigo = p_kpi;
    IF (k.verde_min IS NULL OR p_valor >= k.verde_min) AND (k.verde_max IS NULL OR p_valor <= k.verde_max) THEN
      v_sem := 'VERDE';
    ELSIF (k.ambar_min IS NOT NULL OR k.ambar_max IS NOT NULL)
          AND (k.ambar_min IS NULL OR p_valor >= k.ambar_min) AND (k.ambar_max IS NULL OR p_valor <= k.ambar_max) THEN
      v_sem := 'AMBAR';
    ELSE
      v_sem := 'ROJO';
    END IF;
    IF p_origen = 'MANUAL' THEN
      v_reg := k.responsable_id;
    END IF;
    INSERT INTO medicion_kpi (kpi_id, medido_en, valor, semaforo, origen, registrado_por_id, observacion)
    VALUES (k.id, CAST(p_fecha AS TIMESTAMP) + INTERVAL '9' HOUR, p_valor, v_sem, p_origen, v_reg, p_obs);
  END;

  -- Serie semanal desde p_desde (cada valor, una semana después del anterior)
  PROCEDURE serie(p_kpi VARCHAR2, p_desde DATE, p_v1 NUMBER, p_v2 NUMBER, p_v3 NUMBER, p_v4 NUMBER, p_v5 NUMBER,
                  p_v6 NUMBER, p_v7 NUMBER DEFAULT NULL) IS
    TYPE t_valores IS TABLE OF NUMBER;
    v t_valores := t_valores(p_v1, p_v2, p_v3, p_v4, p_v5, p_v6, p_v7);
  BEGIN
    FOR i IN 1 .. v.COUNT LOOP
      IF v(i) IS NOT NULL THEN
        med(p_kpi, p_desde + 7 * (i - 1), v(i));
      END IF;
    END LOOP;
  END;

  PROCEDURE etapa(p_sol VARCHAR2, p_numero NUMBER, p_estado VARCHAR2, p_inicio DATE, p_fin DATE, p_limite DATE,
                  p_responsable NUMBER, p_observacion VARCHAR2 DEFAULT NULL) IS
    v_s NUMBER := sol(p_sol);
  BEGIN
    INSERT INTO solicitud_etapa (solicitud_id, etapa_numero, estado, responsable_id, fecha_inicio, fecha_fin, fecha_limite, observacion)
    VALUES (v_s, p_numero, p_estado, p_responsable, p_inicio, p_fin, p_limite, p_observacion);
  END;

  PROCEDURE modif(p_sol VARCHAR2, p_entidad VARCHAR2, p_entidad_id NUMBER, p_campo VARCHAR2,
                  p_anterior VARCHAR2, p_nuevo VARCHAR2, p_aplicada DATE DEFAULT NULL) IS
    v_s NUMBER := sol(p_sol);
  BEGIN
    INSERT INTO cambio_modificacion (solicitud_id, entidad, entidad_id, campo, valor_anterior, valor_nuevo, aplicada, fecha_aplicacion)
    VALUES (v_s, p_entidad, p_entidad_id, p_campo, p_anterior, p_nuevo,
            CASE WHEN p_aplicada IS NULL THEN 0 ELSE 1 END,
            CASE WHEN p_aplicada IS NULL THEN NULL ELSE CAST(p_aplicada AS TIMESTAMP) + INTERVAL '11' HOUR END);
  END;

  PROCEDURE accion(p_sol VARCHAR2, p_descripcion VARCHAR2, p_responsable NUMBER, p_compromiso DATE, p_cierre DATE DEFAULT NULL) IS
    v_s NUMBER := sol(p_sol);
  BEGIN
    INSERT INTO accion_correctiva (solicitud_id, descripcion, responsable_id, fecha_compromiso, fecha_cierre, estado)
    VALUES (v_s, p_descripcion, p_responsable, p_compromiso, p_cierre, CASE WHEN p_cierre IS NULL THEN 'ABIERTA' ELSE 'CERRADA' END);
  END;

BEGIN
  SELECT id INTO v_pry FROM proyecto WHERE codigo = 'PRY-2026-01';
  v_carla    := usr('admin@gestionpro.cl');
  v_tomas    := usr('usuario@gestionpro.cl');
  v_diego    := usr('diego.rojas@gestionpro.cl');
  v_fernanda := usr('fernanda.soto@gestionpro.cl');

  -- ---------------------------------------------------------------------------
  -- Módulo 5 · Dotación requerida por tipo de profesional
  -- ---------------------------------------------------------------------------
  requerimiento('Jefe de proyecto', 1, 32000);
  requerimiento('Analista',         2, 24000);
  requerimiento('Desarrollador',    2, 24000);   -- solo hay uno asignado: la brecha se ve en Recursos
  requerimiento('Analista QA',      1, 20000);

  -- ---------------------------------------------------------------------------
  -- Módulo 5 · Recursos de los cuatro tipos (costo total = cantidad × costo unitario × periodo)
  -- ---------------------------------------------------------------------------
  recurso_('LICENCIA',     'IntelliJ IDEA Ultimate',              'Suscripción mensual por usuario',  'licencia', 3,  55000, 4);
  recurso_('LICENCIA',     'Base de datos en la nube (QA)',       'Instancia gestionada, plan mensual', 'instancia', 1, 180000, 4);
  recurso_('LICENCIA',     'Figma Professional',                  'Editor por usuario',               'licencia', 2,  15000, 2);
  recurso_('MATERIAL',     'Etiquetas térmicas para inventario',  'Rollo de 1.000 etiquetas',         'rollo',    40,  6500, 1);
  recurso_('MATERIAL',     'Manuales impresos de capacitación',   'Impresión a color, anillado',      'ejemplar', 30,  4500, 1);
  recurso_('EQUIPAMIENTO', 'Servidor de pruebas',                 'Arriendo mensual',                 'servidor', 1, 350000, 4);
  recurso_('EQUIPAMIENTO', 'Lectores de código de barras',        'Compra para bodegas regionales',   'unidad',   6,  85000, 1);
  recurso_('EQUIPAMIENTO', 'Notebook de desarrollo',              'Arriendo mensual',                 'equipo',   2, 120000, 4);
  recurso_('OTRO',         'Traslados a bodegas regionales',      'Pasaje y viático por visita',      'viaje',    4, 180000, 1);
  recurso_('OTRO',         'Servicio de cafetería',               'Sesiones de capacitación',         'sesión',   3,  60000, 1);

  -- ---------------------------------------------------------------------------
  -- Módulo 5 · Profesionales en actividades hoja (horas por semana; horas reales en lo ejecutado)
  -- ---------------------------------------------------------------------------
  asig_prof('Entrevistas con usuarios',     'Diego Rojas',     40, 42);
  asig_prof('Entrevistas con usuarios',     'Carla Muñoz',      8,  8);
  asig_prof('Documento de requerimientos',  'Diego Rojas',     40, 44);
  asig_prof('Modelo conceptual',            'Fernanda Soto',   40, 26);
  asig_prof('Modelo físico',                'Fernanda Soto',   40, 46);
  asig_prof('Plan de proyecto',             'Carla Muñoz',     30, 26);
  asig_prof('Definición de componentes',    'Fernanda Soto',   40, 32);
  asig_prof('Diseño de integraciones',      'Fernanda Soto',   40, 40);
  asig_prof('Diseño de integraciones',      'Tomás Vidal',     10, 10);
  asig_prof('Prototipo de baja fidelidad',  'Diego Rojas',     40, 38);
  asig_prof('Prototipo navegable',          'Diego Rojas',     40, 70);
  asig_prof('Diseño de pruebas',            'Valentina Pérez', 40, 36);
  asig_prof('Backend API inventario',       'Tomás Vidal',     30, 60);
  asig_prof('Backend API inventario',       'Fernanda Soto',   10, 16);
  asig_prof('Frontend inventario',          'Tomás Vidal',     25, 20);   -- se cruza con el backend: 55 h > 40 h
  asig_prof('Frontend inventario',          'Diego Rojas',     10,  6);
  asig_prof('Consultas y vistas',           'Tomás Vidal',     40);
  asig_prof('Pantallas de reportes',        'Diego Rojas',     40);
  asig_prof('Integración de servicios',     'Fernanda Soto',   40);
  asig_prof('Integración de servicios',     'Tomás Vidal',     20);
  asig_prof('Pruebas de integración',       'Valentina Pérez', 40);
  asig_prof('Pruebas funcionales',          'Valentina Pérez', 40);
  asig_prof('Pruebas de aceptación',        'Valentina Pérez', 30);
  asig_prof('Pruebas de aceptación',        'Diego Rojas',     10);
  asig_prof('Material de capacitación',     'Diego Rojas',     30);
  asig_prof('Sesiones de capacitación',     'Diego Rojas',     30);
  asig_prof('Sesiones de capacitación',     'Carla Muñoz',     10);
  asig_prof('Migración de datos',           'Tomás Vidal',     40);
  asig_prof('Paso a producción',            'Carla Muñoz',     20);
  asig_prof('Paso a producción',            'Tomás Vidal',     40);
  asig_prof('Paso a producción',            'Fernanda Soto',   20);

  -- Recursos en actividades hoja (cantidad × periodo no puede superar lo disponible)
  asig_rec('Prototipo de baja fidelidad', 'Figma Professional',                 1, 1, 15000);
  asig_rec('Prototipo navegable',         'Figma Professional',                 2, 1, 30000);
  asig_rec('Backend API inventario',      'IntelliJ IDEA Ultimate',             2, 1, 110000);
  asig_rec('Backend API inventario',      'Notebook de desarrollo',             1, 1, 120000);
  asig_rec('Frontend inventario',         'IntelliJ IDEA Ultimate',             1, 1, 55000);
  asig_rec('Frontend inventario',         'Notebook de desarrollo',             1, 1, 120000);
  asig_rec('Consultas y vistas',          'IntelliJ IDEA Ultimate',             1, 1);
  asig_rec('Integración de servicios',    'IntelliJ IDEA Ultimate',             2, 1);
  asig_rec('Integración de servicios',    'Base de datos en la nube (QA)',      1, 1);
  asig_rec('Pruebas de integración',      'Servidor de pruebas',                1, 1);
  asig_rec('Pruebas funcionales',         'Servidor de pruebas',                1, 1);
  asig_rec('Pruebas funcionales',         'Base de datos en la nube (QA)',      1, 1);
  asig_rec('Pruebas de aceptación',       'Servidor de pruebas',                1, 1);
  asig_rec('Pruebas de aceptación',       'Base de datos en la nube (QA)',      1, 1);
  asig_rec('Sesiones de capacitación',    'Manuales impresos de capacitación', 30, 1);
  asig_rec('Sesiones de capacitación',    'Traslados a bodegas regionales',     4, 1);
  asig_rec('Sesiones de capacitación',    'Servicio de cafetería',              3, 1);
  asig_rec('Paso a producción',           'Etiquetas térmicas para inventario', 30, 1);
  asig_rec('Paso a producción',           'Lectores de código de barras',       6, 1);

  -- ---------------------------------------------------------------------------
  -- Módulo 6 · Criterios de aceptación y evidencias
  -- ---------------------------------------------------------------------------
  criterio('H1', 1, 'Documento de requerimientos firmado por el cliente', 1);
  criterio('H1', 2, 'Plan de proyecto aprobado por el comité', 1);
  criterio('H2', 1, 'Arquitectura revisada por el área de TI del cliente', 1);
  criterio('H2', 2, 'Prototipo navegable validado con 5 usuarios de bodega', 1);
  criterio('H3', 1, 'API de inventario desplegada en el ambiente de pruebas', 0);
  criterio('H3', 2, 'Pantallas de inventario conectadas a la API', 0);
  criterio('H4', 1, 'Servicios integrados sin errores bloqueantes', 1);
  criterio('H4', 2, 'Pruebas de integración con 95 % de casos aprobados', 0);
  criterio('H4', 3, 'Informe de pruebas de integración publicado', 0);
  criterio('H5', 1, 'Pruebas de aceptación firmadas por el cliente', 0);
  criterio('H5', 2, 'Sin incidencias críticas abiertas', 0);
  criterio('H6', 1, 'Datos migrados y conciliados', 0);
  criterio('H6', 2, 'Usuarios de las 4 bodegas capacitados', 0);
  criterio('H6', 3, 'Sistema operando en producción una semana sin incidentes', 0);

  evidencia_hito('H1', 'acta-aprobacion-requerimientos.txt',
    'Acta de aprobación de requerimientos - PRY-2026-01' || CHR(10) ||
    'Fecha: 14-09-2026. Participantes: cliente (jefatura de bodegas), Carla Muñoz, Diego Rojas.' || CHR(10) ||
    'Se aprueba el documento de requerimientos v1.2 y el plan de proyecto.', v_diego, DATE '2026-09-14');

  -- ---------------------------------------------------------------------------
  -- Módulos 7 y 9 · KPI (sentido, meta y límites del semáforo justificados en la documentación)
  -- ---------------------------------------------------------------------------
  kpi_('KP-01', 'Avance del proyecto',
       'Trabajo completado respecto del total planificado, ponderado por duración. Verde desde 40 %: lo que la línea base espera a mediados de noviembre.',
       'PROYECTO', 'PROYECTO', NULL, 'trabajo_completado / trabajo_total * 100', '%', 'MAYOR', 100, 40, NULL, 30, 40, 'SEMANAL', 'AUTOMATICA', v_carla);
  kpi_('KP-02', 'Cumplimiento de hitos',
       'Hitos cumplidos en plazo sobre los hitos vencidos o completados.',
       'PROYECTO', 'HITO', NULL, 'hitos_cumplidos_en_plazo / hitos_vencidos_o_completados * 100', '%', 'MAYOR', 90, 90, NULL, 70, 90, 'SEMANAL', 'AUTOMATICA', v_carla);
  kpi_('KP-03', 'Desviación de costos',
       'Diferencia entre el costo real y el planificado a la fecha. Negativo significa ahorro.',
       'PROYECTO', 'PROYECTO', NULL, '(costo_real - costo_planificado_fecha) / costo_planificado_fecha * 100', '%', 'MENOR', 0, NULL, 5, 5, 10, 'SEMANAL', 'AUTOMATICA', v_carla);
  kpi_('KP-04', 'Actividades atrasadas',
       'Actividades con término planificado vencido y sin terminar sobre las que ya debían haber comenzado.',
       'PROYECTO', 'PROYECTO', NULL, 'actividades_atrasadas / actividades_evaluadas * 100', '%', 'MENOR', 5, NULL, 10, 10, 20, 'DIARIA', 'AUTOMATICA', v_tomas);
  kpi_('KP-05', 'Utilización de profesionales',
       'Horas asignadas en la semana sobre las horas disponibles de los profesionales del proyecto. Sobre 100 % hay sobreasignación.',
       'PROYECTO', 'PROYECTO', NULL, 'horas_asignadas / horas_disponibles * 100', '%', 'RANGO', 85, 70, 100, 50, 110, 'SEMANAL', 'AUTOMATICA', v_carla);
  kpi_('KP-06', 'Cumplimiento de actividades',
       'Actividades terminadas dentro del plazo planificado sobre las actividades terminadas.',
       'PROYECTO', 'PROYECTO', NULL, 'actividades_terminadas_en_plazo / actividades_terminadas * 100', '%', 'MAYOR', 90, 85, NULL, 70, 85, 'SEMANAL', 'AUTOMATICA', v_tomas);
  kpi_('KP-07', 'Índice de desempeño del cronograma (SPI)',
       'Valor ganado sobre valor planificado. Bajo 1 el proyecto produce menos de lo que el plan esperaba a la fecha.',
       'PROYECTO', 'PROYECTO', NULL, 'valor_ganado / valor_planificado', 'índice', 'MAYOR', 1, 0.95, NULL, 0.85, 0.95, 'SEMANAL', 'AUTOMATICA', v_carla);
  SELECT id INTO v_id FROM hito WHERE proyecto_id = v_pry AND codigo = 'H4';
  kpi_('KP-08', 'Criterios de aceptación del hito H4',
       'Porcentaje de criterios de aceptación de la integración de módulos ya cumplidos.',
       'PROYECTO', 'HITO', v_id, 'criterios_cumplidos / criterios_total * 100', '%', 'MAYOR', 100, 100, NULL, 50, 100, 'AL_EVENTO', 'AUTOMATICA', v_fernanda);

  kpi_('KC-01', 'Tasa de aprobación de cambios',
       'Solicitudes aprobadas sobre las decididas. Fuera del rango indica control laxo (muy alta) o mala formulación (muy baja).',
       'CAMBIO', 'CAMBIO', NULL, 'cambios_aprobados / cambios_decididos * 100', '%', 'RANGO', 70, 50, 85, 30, 95, 'SEMANAL', 'AUTOMATICA', v_carla);
  kpi_('KC-02', 'Tiempo promedio de evaluación',
       'Días entre el registro y la decisión de aprobar o rechazar.',
       'CAMBIO', 'CAMBIO', NULL, 'dias_evaluacion_promedio', 'días', 'MENOR', 5, NULL, 5, 5, 10, 'SEMANAL', 'AUTOMATICA', v_carla);
  kpi_('KC-03', 'Tiempo promedio de implementación',
       'Días entre la aprobación y el cierre de los cambios.',
       'CAMBIO', 'CAMBIO', NULL, 'dias_implementacion_promedio', 'días', 'MENOR', 15, NULL, 15, 15, 25, 'SEMANAL', 'AUTOMATICA', v_carla);
  kpi_('KC-04', 'Cambios atrasados',
       'Solicitudes en curso con la fecha comprometida vencida.',
       'CAMBIO', 'CAMBIO', NULL, 'cambios_atrasados / cambios_en_curso * 100', '%', 'MENOR', 0, NULL, 10, 10, 25, 'DIARIA', 'AUTOMATICA', v_carla);
  kpi_('KC-05', 'Costo de cambios sobre presupuesto',
       'Costo real (o estimado) de los cambios aprobados sobre el presupuesto del proyecto.',
       'CAMBIO', 'CAMBIO', NULL, 'costo_cambios / presupuesto * 100', '%', 'MENOR', 5, NULL, 5, 5, 10, 'SEMANAL', 'AUTOMATICA', v_carla);
  kpi_('KC-06', 'Cumplimiento de etapas del cambio',
       'Etapas completadas dentro de su fecha límite sobre las completadas o vencidas.',
       'CAMBIO', 'CAMBIO', NULL, 'etapas_completadas_a_tiempo / etapas_vencidas_o_completadas * 100', '%', 'MAYOR', 90, 85, NULL, 70, 85, 'SEMANAL', 'AUTOMATICA', v_fernanda);
  kpi_('KC-07', 'Efectividad de los cambios',
       'Promedio de la efectividad evaluada al cerrar cada cambio.',
       'CAMBIO', 'CAMBIO', NULL, 'efectividad_promedio', '%', 'MAYOR', 85, 80, NULL, 60, 80, 'MENSUAL', 'AUTOMATICA', v_carla);
  kpi_('KC-08', 'Adopción del cambio',
       'Promedio del uso efectivo medido tras implementar cada cambio.',
       'CAMBIO', 'CAMBIO', NULL, 'adopcion_promedio', '%', 'MAYOR', 80, 75, NULL, 50, 75, 'MENSUAL', 'AUTOMATICA', v_diego);
  kpi_('KC-09', 'Riesgos críticos abiertos',
       'Solicitudes en curso con probabilidad e impacto altos.',
       'CAMBIO', 'CAMBIO', NULL, 'riesgos_criticos_abiertos', 'solic.', 'MENOR', 0, NULL, 0, 0, 1, 'DIARIA', 'AUTOMATICA', v_carla);
  kpi_('KC-10', 'Cierre de acciones correctivas',
       'Acciones correctivas cerradas sobre las registradas.',
       'CAMBIO', 'CAMBIO', NULL, 'acciones_correctivas_cerradas / acciones_correctivas * 100', '%', 'MAYOR', 100, 80, NULL, 50, 80, 'SEMANAL', 'AUTOMATICA', v_fernanda);
  kpi_('KC-11', 'Desviación de plazo por cambios',
       'Días hábiles que los cambios aprobados agregaron al término del proyecto.',
       'CAMBIO', 'CAMBIO', NULL, 'dias_desviacion_por_cambios', 'días', 'MENOR', 5, NULL, 5, 5, 10, 'AL_EVENTO', 'AUTOMATICA', v_carla);
  kpi_('KC-12', 'Satisfacción con los cambios',
       'Resultado de la encuesta mensual a los usuarios afectados. Sin fuente automática: se ingresa a mano.',
       'CAMBIO', 'CAMBIO', NULL, NULL, '%', 'MAYOR', 80, 80, NULL, 60, 80, 'MENSUAL', 'MANUAL', v_diego);

  vincular_kpi_hito('KP-02', 'H1'); vincular_kpi_hito('KP-02', 'H2'); vincular_kpi_hito('KP-02', 'H3');
  vincular_kpi_hito('KP-02', 'H4'); vincular_kpi_hito('KP-02', 'H5'); vincular_kpi_hito('KP-02', 'H6');
  vincular_kpi_hito('KP-08', 'H4');
  vincular_kpi_hito('KP-04', 'H3');
  vincular_kpi_hito('KP-06', 'H5');
  vincular_kpi_hito('KP-01', 'H6');

  -- Histórico semanal (mediciones programadas)
  serie('KP-01', DATE '2026-09-28', 12,   18,   24,   29,   34,   39,   43);
  serie('KP-02', DATE '2026-09-28', 100,  100,  50,   50,   50,   33.33, 33.33);
  serie('KP-03', DATE '2026-09-28', -2.1, 1.5,  3.2,  4.8,  6.1,  5.2,  4.4);
  serie('KP-04', DATE '2026-09-28', 0,    0,    10,   12.5, 18.2, 15.4, 14.3);
  serie('KP-05', DATE '2026-09-28', 78,   82,   95,   118,  121,  119,  112);
  serie('KP-06', DATE '2026-09-28', 100,  100,  85.7, 77.8, 77.8, 77.8, 77.8);
  serie('KP-07', DATE '2026-09-28', 1.02, 0.99, 0.96, 0.92, 0.89, 0.9,  0.91);
  serie('KP-08', DATE '2026-09-28', 0,    0,    0,    0,    33.33, 33.33, 33.33);

  serie('KC-01', DATE '2026-10-05', 100,  100,  100,  66.67, 66.67, 66.67);
  serie('KC-02', DATE '2026-10-05', 2,    3,    3,    4.67, 4.67, 4.67);
  serie('KC-03', DATE '2026-10-05', 13,   13,   13,   13,   13,   13);
  serie('KC-04', DATE '2026-10-05', 0,    0,    0,    0,    0,    33.33);
  serie('KC-05', DATE '2026-10-05', 1.07, 1.07, 1.24, 1.24, 1.9,  2.41);
  serie('KC-06', DATE '2026-10-05', 100,  90,   85.7, 80,   78.9, 76.2);
  serie('KC-07', DATE '2026-10-05', 90,   90,   90,   90,   90,   90);
  serie('KC-08', DATE '2026-10-05', 85,   85,   85,   85,   85,   85);
  serie('KC-09', DATE '2026-10-05', 0,    0,    1,    0,    0,    1);
  serie('KC-10', DATE '2026-10-05', 100,  50,   50,   66.67, 66.67, 50);
  serie('KC-11', DATE '2026-10-05', 1,    1,    1,    1,    1,    1);
  med('KC-12', DATE '2026-09-30', 70, 'MANUAL', 'Encuesta de septiembre (12 respuestas).');
  med('KC-12', DATE '2026-10-30', 74, 'MANUAL', 'Encuesta de octubre (15 respuestas).');

  -- ---------------------------------------------------------------------------
  -- Módulo 8 · Cinco solicitudes de cambio
  -- ---------------------------------------------------------------------------
  -- SC-001: aprobada, implementada y cerrada
  INSERT INTO solicitud_cambio (proyecto_id, codigo, titulo, solicitante, fecha_solicitud, descripcion, justificacion, tipo, prioridad,
         riesgo, probabilidad, impacto, impacto_plazo_dias, impacto_costo, impacto_recursos, impacto_alcance,
         responsable_id, estado, etapa_actual, fecha_comprometida, fecha_real, costo_estimado, costo_real,
         aprobado_por_id, fecha_decision, resultado, efectividad, adopcion, impacto_plazo_real, impacto_costo_real)
  VALUES (v_pry, 'SC-001', 'Agregar lote y vencimiento al modelo de datos', 'Marcela Ortiz (jefa de bodega Antofagasta)', DATE '2026-09-16',
         'Registrar número de lote y fecha de vencimiento por producto para controlar insumos perecibles.',
         'Las bodegas del norte reciben productos químicos con vencimiento; hoy se controlan en planillas.',
         'ALCANCE', 'ALTA', 'Retrasar el modelo físico si los campos obligan a rediseñar tablas.', 'BAJA', 'MEDIO',
         1, 520000, 'Fernanda Soto, un día adicional en el modelo físico.', 'Dos campos nuevos y su validación.',
         v_fernanda, 'CERRADO', 10, DATE '2026-10-02', DATE '2026-10-01', 520000, 480000,
         v_carla, TIMESTAMP '2026-09-18 12:00:00', 'Campos incorporados al modelo físico y al diccionario de datos. Las bodegas del norte ya registran lotes.',
         90, 85, 1, 208000);
  -- SC-002: aprobada, en implementación y con plazo vencido
  INSERT INTO solicitud_cambio (proyecto_id, codigo, titulo, solicitante, fecha_solicitud, descripcion, justificacion, tipo, prioridad,
         riesgo, probabilidad, impacto, impacto_plazo_dias, impacto_costo, impacto_recursos, impacto_alcance,
         responsable_id, estado, etapa_actual, fecha_comprometida, fecha_real, costo_estimado, costo_real,
         aprobado_por_id, fecha_decision, resultado, efectividad, adopcion, impacto_plazo_real, impacto_costo_real)
  VALUES (v_pry, 'SC-002', 'Integrar lectores de código de barras', 'Jefatura de operaciones', DATE '2026-10-05',
         'Comprar seis lectores (en vez de cuatro) e integrarlos al registro de entradas y salidas.',
         'Dos bodegas nuevas se suman al piloto y digitar códigos genera errores de stock.',
         'TECNOLOGIA', 'MEDIA', 'El proveedor puede demorar la entrega de los equipos.', 'MEDIA', 'MEDIO',
         0, 600000, 'Compra de 2 lectores adicionales.', 'Lectura de códigos en las pantallas de movimiento.',
         v_tomas, 'EN_IMPLEMENTACION', 7, DATE '2026-11-06', NULL, 600000, 690000,
         v_carla, TIMESTAMP '2026-10-09 16:30:00', NULL, NULL, NULL, 0, 170000);
  -- SC-003: rechazada
  INSERT INTO solicitud_cambio (proyecto_id, codigo, titulo, solicitante, fecha_solicitud, descripcion, justificacion, tipo, prioridad,
         riesgo, probabilidad, impacto, impacto_plazo_dias, impacto_costo, impacto_recursos, impacto_alcance,
         responsable_id, estado, etapa_actual, fecha_comprometida, fecha_real, costo_estimado, costo_real,
         aprobado_por_id, fecha_decision, resultado)
  VALUES (v_pry, 'SC-003', 'App móvil nativa para conteo de stock', 'Gerencia comercial', DATE '2026-10-12',
         'Desarrollar una aplicación nativa para Android e iOS que permita contar stock sin conexión.',
         'Los supervisores recorren bodegas sin cobertura de red.',
         'ALCANCE', 'BAJA', 'Duplica el esfuerzo de construcción y pone en riesgo la fecha de producción.', 'ALTA', 'ALTO',
         25, 9800000, 'Un desarrollador móvil adicional por tres meses.', 'Nueva aplicación fuera del alcance contratado.',
         v_diego, 'RECHAZADO', 4, DATE '2026-10-23', NULL, 9800000, NULL,
         v_carla, TIMESTAMP '2026-10-20 10:00:00', 'Rechazada: fuera del alcance contratado. Se propone evaluarla en una segunda fase.');
  -- SC-004: en evaluación, esperando la aprobación (se usa en la demostración)
  INSERT INTO solicitud_cambio (proyecto_id, codigo, titulo, solicitante, fecha_solicitud, descripcion, justificacion, tipo, prioridad,
         riesgo, probabilidad, impacto, impacto_plazo_dias, impacto_costo, impacto_recursos, impacto_alcance,
         responsable_id, estado, etapa_actual, fecha_comprometida, costo_estimado)
  VALUES (v_pry, 'SC-004', 'Transferencias de stock entre bodegas', 'Jefatura de operaciones', DATE '2026-11-02',
         'Ampliar la API de inventario con transferencias entre bodegas y su trazabilidad.',
         'Hoy una transferencia se registra como una salida y una entrada sin vínculo, lo que descuadra el stock en tránsito.',
         'ALCANCE', 'ALTA', 'Puede desplazar la integración de módulos y el hito H4.', 'MEDIA', 'ALTO',
         3, 2100000, 'Tomás Vidal +5 h/semana en el backend.', 'Nuevo endpoint de transferencias y su pantalla.',
         v_tomas, 'EN_EVALUACION', 4, DATE '2026-12-04', 2100000);
  -- SC-005: recién registrada, con riesgo crítico
  INSERT INTO solicitud_cambio (proyecto_id, codigo, titulo, solicitante, fecha_solicitud, descripcion, justificacion, tipo, prioridad,
         riesgo, probabilidad, impacto, impacto_plazo_dias, impacto_costo, impacto_recursos, impacto_alcance,
         responsable_id, estado, etapa_actual, fecha_comprometida, costo_estimado)
  VALUES (v_pry, 'SC-005', 'Capacitación en modalidad virtual', 'Recursos humanos', DATE '2026-11-09',
         'Reemplazar las sesiones presenciales en regiones por capacitación virtual con un proveedor externo.',
         'Los traslados a regiones encarecen la capacitación y dos bodegas no pueden liberar personal esos días.',
         'ORGANIZACION', 'MEDIA', 'Baja asistencia y menor adopción del sistema si la modalidad virtual no funciona en bodega.', 'ALTA', 'ALTO',
         -2, 350000, 'Proveedor externo de capacitación.', 'Cambia la modalidad de las sesiones.',
         v_fernanda, 'ABIERTO', 1, DATE '2026-11-20', 350000);

  -- Etapas (estado, inicio, fin, límite, responsable)
  etapa('SC-001', 1,  'COMPLETADA', DATE '2026-09-16', DATE '2026-09-16', DATE '2026-09-16', v_fernanda);
  etapa('SC-001', 2,  'COMPLETADA', DATE '2026-09-16', DATE '2026-09-17', DATE '2026-09-17', v_fernanda, 'Impacto: +1 día en el modelo físico.');
  etapa('SC-001', 3,  'COMPLETADA', DATE '2026-09-17', DATE '2026-09-17', DATE '2026-09-18', v_fernanda);
  etapa('SC-001', 4,  'COMPLETADA', DATE '2026-09-18', DATE '2026-09-18', DATE '2026-09-18', v_carla, 'Aprobada por Carla Muñoz.');
  etapa('SC-001', 5,  'COMPLETADA', DATE '2026-09-18', DATE '2026-09-18', DATE '2026-09-21', v_carla, 'Se aplicó 1 modificación al plan.');
  etapa('SC-001', 6,  'COMPLETADA', DATE '2026-09-21', DATE '2026-09-21', DATE '2026-09-22', v_fernanda);
  etapa('SC-001', 7,  'COMPLETADA', DATE '2026-09-21', DATE '2026-09-29', DATE '2026-09-28', v_fernanda, 'Terminó un día después del límite.');
  etapa('SC-001', 8,  'COMPLETADA', DATE '2026-09-29', DATE '2026-09-29', DATE '2026-09-29', v_diego);
  etapa('SC-001', 9,  'COMPLETADA', DATE '2026-09-29', DATE '2026-09-30', DATE '2026-09-30', v_diego, 'Adopción medida: 85 % de los productos perecibles con lote.');
  etapa('SC-001', 10, 'COMPLETADA', DATE '2026-10-01', DATE '2026-10-01', DATE '2026-10-02', v_carla);

  etapa('SC-002', 1,  'COMPLETADA', DATE '2026-10-05', DATE '2026-10-05', DATE '2026-10-05', v_tomas);
  etapa('SC-002', 2,  'COMPLETADA', DATE '2026-10-05', DATE '2026-10-07', DATE '2026-10-07', v_tomas);
  etapa('SC-002', 3,  'COMPLETADA', DATE '2026-10-07', DATE '2026-10-08', DATE '2026-10-08', v_tomas);
  etapa('SC-002', 4,  'COMPLETADA', DATE '2026-10-09', DATE '2026-10-09', DATE '2026-10-09', v_carla, 'Aprobada por Carla Muñoz.');
  etapa('SC-002', 5,  'COMPLETADA', DATE '2026-10-09', DATE '2026-10-13', DATE '2026-10-13', v_carla, 'Se aplicó 1 modificación al plan.');
  etapa('SC-002', 6,  'COMPLETADA', DATE '2026-10-13', DATE '2026-10-16', DATE '2026-10-14', v_tomas, 'La comunicación a bodegas se atrasó dos días.');
  etapa('SC-002', 7,  'EN_CURSO',   DATE '2026-10-19', NULL,              DATE '2026-10-30', v_tomas, 'Faltan dos lectores por entregar.');
  etapa('SC-002', 8,  'PENDIENTE',  NULL,              NULL,              DATE '2026-11-03', v_diego);
  etapa('SC-002', 9,  'PENDIENTE',  NULL,              NULL,              DATE '2026-11-05', v_diego);
  etapa('SC-002', 10, 'PENDIENTE',  NULL,              NULL,              DATE '2026-11-06', v_carla);

  etapa('SC-003', 1,  'COMPLETADA', DATE '2026-10-12', DATE '2026-10-12', DATE '2026-10-12', v_diego);
  etapa('SC-003', 2,  'COMPLETADA', DATE '2026-10-13', DATE '2026-10-15', DATE '2026-10-15', v_diego, 'Impacto: +25 días y $9,8 millones.');
  etapa('SC-003', 3,  'COMPLETADA', DATE '2026-10-15', DATE '2026-10-16', DATE '2026-10-16', v_diego, 'Riesgo crítico: probabilidad e impacto altos.');
  etapa('SC-003', 4,  'COMPLETADA', DATE '2026-10-19', DATE '2026-10-20', DATE '2026-10-19', v_carla, 'Rechazada por Carla Muñoz.');
  etapa('SC-003', 5,  'OMITIDA',    NULL, NULL, NULL, NULL);
  etapa('SC-003', 6,  'OMITIDA',    NULL, NULL, NULL, NULL);
  etapa('SC-003', 7,  'OMITIDA',    NULL, NULL, NULL, NULL);
  etapa('SC-003', 8,  'OMITIDA',    NULL, NULL, NULL, NULL);
  etapa('SC-003', 9,  'OMITIDA',    NULL, NULL, NULL, NULL);
  etapa('SC-003', 10, 'OMITIDA',    NULL, NULL, NULL, NULL);

  etapa('SC-004', 1,  'COMPLETADA', DATE '2026-11-02', DATE '2026-11-02', DATE '2026-11-02', v_tomas);
  etapa('SC-004', 2,  'COMPLETADA', DATE '2026-11-02', DATE '2026-11-05', DATE '2026-11-05', v_tomas, 'Impacto: +3 días en el backend y +5 h/semana de Tomás Vidal.');
  etapa('SC-004', 3,  'COMPLETADA', DATE '2026-11-05', DATE '2026-11-06', DATE '2026-11-06', v_fernanda, 'Mitigación: el frontend sigue con datos simulados mientras tanto.');
  etapa('SC-004', 4,  'EN_CURSO',   DATE '2026-11-09', NULL,              DATE '2026-11-13', v_carla);
  etapa('SC-004', 5,  'PENDIENTE',  NULL, NULL, DATE '2026-11-16', v_carla);
  etapa('SC-004', 6,  'PENDIENTE',  NULL, NULL, DATE '2026-11-17', v_tomas);
  etapa('SC-004', 7,  'PENDIENTE',  NULL, NULL, DATE '2026-11-27', v_tomas);
  etapa('SC-004', 8,  'PENDIENTE',  NULL, NULL, DATE '2026-12-01', v_fernanda);
  etapa('SC-004', 9,  'PENDIENTE',  NULL, NULL, DATE '2026-12-03', v_diego);
  etapa('SC-004', 10, 'PENDIENTE',  NULL, NULL, DATE '2026-12-04', v_carla);

  etapa('SC-005', 1,  'EN_CURSO',   DATE '2026-11-09', NULL, DATE '2026-11-10', v_fernanda);
  etapa('SC-005', 2,  'PENDIENTE',  NULL, NULL, DATE '2026-11-11', v_fernanda);
  etapa('SC-005', 3,  'PENDIENTE',  NULL, NULL, DATE '2026-11-12', v_fernanda);
  etapa('SC-005', 4,  'PENDIENTE',  NULL, NULL, DATE '2026-11-13', v_carla);
  etapa('SC-005', 5,  'PENDIENTE',  NULL, NULL, DATE '2026-11-16', v_carla);
  etapa('SC-005', 6,  'PENDIENTE',  NULL, NULL, DATE '2026-11-17', v_fernanda);
  etapa('SC-005', 7,  'PENDIENTE',  NULL, NULL, DATE '2026-11-18', v_diego);
  etapa('SC-005', 8,  'PENDIENTE',  NULL, NULL, DATE '2026-11-19', v_diego);
  etapa('SC-005', 9,  'PENDIENTE',  NULL, NULL, DATE '2026-11-19', v_diego);
  etapa('SC-005', 10, 'PENDIENTE',  NULL, NULL, DATE '2026-11-20', v_fernanda);

  -- Modificaciones: las de SC-001 y SC-002 ya están aplicadas (V4 y este archivo tienen los valores nuevos);
  -- las de SC-004 y SC-005 se aplican al aprobar.
  modif('SC-001', 'ACTIVIDAD', act('Modelo físico'), 'duracion_plan', '4', '5', DATE '2026-09-18');
  modif('SC-002', 'RECURSO', rec('Lectores de código de barras'), 'cantidad', '4', '6', DATE '2026-10-09');
  modif('SC-003', 'PROYECTO', v_pry, 'presupuesto', '48500000', '58300000');
  modif('SC-004', 'ACTIVIDAD', act('Backend API inventario'), 'duracion_plan', '15', '18');
  v_id := asignacion_de('Backend API inventario', 'Tomás Vidal');
  modif('SC-004', 'ASIGNACION_PROFESIONAL', v_id, 'horas_semana', '30', '35');
  modif('SC-004', 'PROYECTO', v_pry, 'presupuesto', '48500000', '50600000');
  modif('SC-005', 'ACTIVIDAD', act('Sesiones de capacitación'), 'duracion_plan', '5', '3');
  modif('SC-005', 'RECURSO', rec('Traslados a bodegas regionales'), 'cantidad', '4', '1');

  accion('SC-001', 'Actualizar el diccionario de datos con los campos de lote y vencimiento', v_fernanda, DATE '2026-09-25', DATE '2026-09-24');
  accion('SC-002', 'Coordinar con el proveedor la entrega de los dos lectores faltantes', v_tomas, DATE '2026-11-10');
  accion('SC-002', 'Ajustar el instructivo de recepción para el uso de lectores', v_diego, DATE '2026-10-23', DATE '2026-10-22');
  accion('SC-004', 'Validar con la arquitecta la estimación de +3 días', v_tomas, DATE '2026-11-16');

  evidencia_sol('SC-001', 'informe-cierre-SC-001.txt',
    'Informe de cierre SC-001' || CHR(10) ||
    'Campos lote y vencimiento disponibles desde el 01-10-2026. 85 % de los productos perecibles registrados con lote.', v_fernanda, DATE '2026-10-01');
END;
/
