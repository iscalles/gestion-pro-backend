-- =============================================================================
-- Gestión Pro · V2: catálogos fijos
-- Etapas del flujo de cambio y variables disponibles para las fórmulas de KPI.
-- =============================================================================

-- Flujo de diez etapas propuesto por el equipo (ajustable si el profesor define otro)
INSERT INTO etapa_cambio (numero, nombre, descripcion) VALUES (1,  'Registro',             'Se ingresa la solicitud con su descripción y justificación.');
INSERT INTO etapa_cambio (numero, nombre, descripcion) VALUES (2,  'Análisis de impacto',  'Se estima el efecto en plazo, costo, recursos y alcance.');
INSERT INTO etapa_cambio (numero, nombre, descripcion) VALUES (3,  'Evaluación de riesgo', 'Se califican probabilidad e impacto y se proponen mitigaciones.');
INSERT INTO etapa_cambio (numero, nombre, descripcion) VALUES (4,  'Aprobación',           'El administrador aprueba o rechaza. Al aprobar se guarda la línea base.');
INSERT INTO etapa_cambio (numero, nombre, descripcion) VALUES (5,  'Planificación',        'Se aplican las modificaciones al cronograma y a los recursos.');
INSERT INTO etapa_cambio (numero, nombre, descripcion) VALUES (6,  'Comunicación',         'Se informa el cambio a los afectados.');
INSERT INTO etapa_cambio (numero, nombre, descripcion) VALUES (7,  'Implementación',       'Se ejecuta el cambio.');
INSERT INTO etapa_cambio (numero, nombre, descripcion) VALUES (8,  'Verificación',         'Se comprueba que el cambio cumple lo solicitado.');
INSERT INTO etapa_cambio (numero, nombre, descripcion) VALUES (9,  'Adopción',             'Se mide el uso efectivo por parte de los usuarios.');
INSERT INTO etapa_cambio (numero, nombre, descripcion) VALUES (10, 'Cierre',               'Se registra el resultado, la efectividad y las lecciones.');

-- Variables de proyecto e hitos
INSERT INTO variable_kpi VALUES ('trabajo_completado',                'Suma de duración × avance de las actividades hoja',                 'días',   'PROYECTO');
INSERT INTO variable_kpi VALUES ('trabajo_total',                     'Suma de duraciones planificadas de las actividades hoja',          'días',   'PROYECTO');
INSERT INTO variable_kpi VALUES ('hitos_cumplidos_en_plazo',          'Hitos cumplidos con fecha real ≤ comprometida',                    'hitos',  'PROYECTO');
INSERT INTO variable_kpi VALUES ('hitos_vencidos_o_completados',      'Hitos cumplidos más hitos pendientes con fecha vencida',           'hitos',  'PROYECTO');
INSERT INTO variable_kpi VALUES ('hitos_cumplidos',                   'Hitos en estado cumplido',                                         'hitos',  'PROYECTO');
INSERT INTO variable_kpi VALUES ('hitos_cumplidos_con_evidencia',     'Hitos cumplidos con al menos una evidencia',                       'hitos',  'PROYECTO');
INSERT INTO variable_kpi VALUES ('costo_real',                        'Costo real acumulado a la fecha',                                  'pesos',  'PROYECTO');
INSERT INTO variable_kpi VALUES ('costo_planificado_fecha',           'Costo planificado prorrateado hasta hoy',                          'pesos',  'PROYECTO');
INSERT INTO variable_kpi VALUES ('presupuesto',                       'Presupuesto autorizado del proyecto',                              'pesos',  'PROYECTO');
INSERT INTO variable_kpi VALUES ('actividades_atrasadas',             'Actividades hoja con término planificado vencido y avance < 100',  'activ.', 'PROYECTO');
INSERT INTO variable_kpi VALUES ('actividades_evaluadas',             'Actividades hoja que ya debían haber comenzado',                   'activ.', 'PROYECTO');
INSERT INTO variable_kpi VALUES ('actividades_terminadas',            'Actividades hoja con avance 100',                                  'activ.', 'PROYECTO');
INSERT INTO variable_kpi VALUES ('actividades_terminadas_en_plazo',   'Terminadas con término real ≤ término planificado',                'activ.', 'PROYECTO');
INSERT INTO variable_kpi VALUES ('horas_asignadas',                   'Horas semanales asignadas a profesionales en la semana actual',   'horas',  'PROYECTO');
INSERT INTO variable_kpi VALUES ('horas_disponibles',                 'Horas semanales disponibles de los profesionales asignados',      'horas',  'PROYECTO');
INSERT INTO variable_kpi VALUES ('valor_ganado',                      'Costo planificado × avance real (EV)',                             'pesos',  'PROYECTO');
INSERT INTO variable_kpi VALUES ('valor_planificado',                 'Costo planificado a la fecha (PV)',                                'pesos',  'PROYECTO');

-- Variables de gestión del cambio
INSERT INTO variable_kpi VALUES ('cambios_registrados',               'Solicitudes registradas en el periodo',                            'solic.', 'CAMBIO');
INSERT INTO variable_kpi VALUES ('cambios_aprobados',                 'Solicitudes aprobadas (incluye en implementación y cerradas)',     'solic.', 'CAMBIO');
INSERT INTO variable_kpi VALUES ('cambios_decididos',                 'Solicitudes aprobadas o rechazadas',                               'solic.', 'CAMBIO');
INSERT INTO variable_kpi VALUES ('cambios_en_curso',                  'Solicitudes no cerradas ni rechazadas',                            'solic.', 'CAMBIO');
INSERT INTO variable_kpi VALUES ('cambios_atrasados',                 'En curso con fecha comprometida vencida',                          'solic.', 'CAMBIO');
INSERT INTO variable_kpi VALUES ('dias_evaluacion_promedio',          'Días promedio entre registro y decisión',                          'días',   'CAMBIO');
INSERT INTO variable_kpi VALUES ('dias_implementacion_promedio',      'Días promedio entre aprobación y cierre',                          'días',   'CAMBIO');
INSERT INTO variable_kpi VALUES ('costo_cambios',                     'Costo real (o estimado si no hay real) de cambios aprobados',      'pesos',  'CAMBIO');
INSERT INTO variable_kpi VALUES ('etapas_completadas_a_tiempo',       'Etapas completadas dentro de su fecha límite',                     'etapas', 'CAMBIO');
INSERT INTO variable_kpi VALUES ('etapas_vencidas_o_completadas',     'Etapas completadas más etapas con fecha límite vencida',           'etapas', 'CAMBIO');
INSERT INTO variable_kpi VALUES ('efectividad_promedio',              'Promedio de efectividad de los cambios cerrados',                  '%',      'CAMBIO');
INSERT INTO variable_kpi VALUES ('adopcion_promedio',                 'Promedio de adopción medida de los cambios implementados',         '%',      'CAMBIO');
INSERT INTO variable_kpi VALUES ('riesgos_criticos_abiertos',         'Solicitudes en curso con probabilidad e impacto altos',            'solic.', 'CAMBIO');
INSERT INTO variable_kpi VALUES ('acciones_correctivas',              'Acciones correctivas registradas',                                 'acc.',   'CAMBIO');
INSERT INTO variable_kpi VALUES ('acciones_correctivas_cerradas',     'Acciones correctivas cerradas',                                    'acc.',   'CAMBIO');
INSERT INTO variable_kpi VALUES ('dias_desviacion_por_cambios',       'Días hábiles agregados al término por cambios aprobados',          'días',   'CAMBIO');
