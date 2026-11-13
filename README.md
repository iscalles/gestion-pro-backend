# Gestión Pro · Backend

API del proyecto integrador de Gestión de Proyectos de Software (Duoc UC). El frontend está en [iscalles/gestion-pro](https://github.com/iscalles/gestion-pro), junto con la [documentación técnica](https://github.com/iscalles/gestion-pro/blob/main/docs/documentacion-tecnica.md) y el [guion de la demostración](https://github.com/iscalles/gestion-pro/blob/main/docs/guion-demostracion.md).

**Stack:** Java 21 · Spring Boot 4.1 · Spring Data JPA · Spring Security con JWT · Flyway · Oracle Database 23ai Free · exp4j (fórmulas de KPI) · springdoc (Swagger).

## Levantar en local

Requiere Java 21 y Docker.

```bash
docker compose up -d        # Oracle Free; la primera vez tarda 1-2 minutos
./mvnw spring-boot:run      # API en http://localhost:8080
```

Al arrancar, Flyway crea el esquema y carga el caso de prueba; luego `ProgramacionInicial` calcula las fechas planificadas y los costos, e `IndicadoresIniciales` guarda la línea base LB-01 y la primera medición de cada KPI. Documentación interactiva en http://localhost:8080/swagger-ui.html.

Cuentas de demostración (contraseña `demo1234`):

| Correo | Rol |
|---|---|
| `admin@gestionpro.cl` | Administrador |
| `usuario@gestionpro.cl` | Usuario de proyecto (miembro de PRY-2026-01) |

### Si el puerto 8080 está ocupado

En algunos equipos el 8080 lo usa otro programa (por ejemplo, NVIDIA Broadcast). Ciérralo o levanta el backend en el 8081:

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

y el frontend con `npx ng serve --configuration puerto-8081`.

### Volver al caso de prueba original

Para ensayar la demostración varias veces partiendo de los mismos datos:

1. Detén el backend.
2. `docker exec -i gestion-pro-oracle sqlplus -s gestion/gestion@FREEPDB1 < scripts/reiniciar-datos.sql`
3. Vuelve a iniciar el backend: Flyway recrea todo (V1 a V7).

`docker compose down -v && docker compose up -d` hace lo mismo borrando el volumen, pero tarda más.

## Arquitectura

Un solo backend (monolito modular). El recálculo que pide la evaluación (cambiar una duración, modificar recursos o aprobar un cambio y ver el efecto en Gantt, costos, KPI y tableros) corre en **una transacción sobre una base**: se aplica completo o no se aplica.

```
cl.duoc.gestionpro
├─ config/          seguridad JWT, CORS, Swagger
├─ comun/           excepciones, manejador de errores, acceso por proyecto
├─ seguridad/       usuarios, roles, login
├─ proyecto/        Módulo 1: proyectos (crear, editar, cerrar, eliminar)
├─ planificacion/   Módulos 2 y 3: EDT, actividades, dependencias, calendario, motor de programación
├─ recurso/         Módulo 5: profesionales, recursos, asignaciones, costos, valor ganado, carga y sobreasignación
├─ hito/            Módulo 6: hitos, criterios de aceptación, actividades asociadas
├─ evidencia/       archivos adjuntos de hitos y solicitudes de cambio
├─ kpi/             Módulos 7 y 9: KPI configurables, fórmulas, semáforo, histórico
├─ cambio/          Módulo 8: solicitudes, diez etapas, modificaciones, líneas base, trazabilidad
├─ alerta/          alertas abiertas, actualizadas y resueltas en cada recálculo
├─ tablero/         los dos tableros de comando
└─ recalculo/       cadena fechas → costos → avance → KPI → alertas
```

Cada paquete tiene un `package-info.java` con lo que le corresponde. Todos siguen el mismo patrón: entidad, repositorio, DTO de entrada y salida, servicio transaccional y controlador.

La EDT no tiene tabla propia: la jerarquía etapa → actividad → subactividad vive en `actividad.padre_id` y los códigos (1, 1.1, 1.1.1) se derivan del orden del árbol, así que nunca quedan desfasados al mover o borrar elementos.

### Cadena de recálculo

`RecalculoService.recalcular(proyectoId)` se llama al final de toda operación que cambie el plan (actividades, dependencias, calendario, asignaciones, recursos, hitos, aprobación de un cambio):

1. **Fechas**: `MotorProgramacion` reprograma las hojas por sus dependencias y deriva etapas y actividades con hijos.
2. **Costos**: las semanas de cada asignación = duración / días hábiles por semana del calendario; costo planificado = horas × semanas × valor hora + recursos.
3. **Avance**: ponderado por duración, de las hojas hacia el proyecto.
4. **KPI**: se evalúa cada fórmula; se guarda una medición nueva si cambia el valor o el semáforo, anotando la solicitud de cambio que la gatilló.
5. **Alertas**: se abren, actualizan o resuelven según el estado resultante.

## Base de datos

| Migración | Contenido |
|---|---|
| `V1__esquema.sql` | 30 tablas, 5 vistas (costos por actividad, etapa y proyecto, estado de hitos, último valor de KPI) y 3 triggers de integridad |
| `V2__catalogos.sql` | las diez etapas del cambio y las variables disponibles para las fórmulas de KPI |
| `V3__datos_demo.sql` | usuarios, calendario 2026-2027 con feriados y cuatro proyectos |
| `V4__planificacion_demo.sql` | 5 profesionales y la planificación de PRY-2026-01: 4 etapas, 12 actividades, 20 subactividades, 25 dependencias FS, SS, FF y SF |
| `V5__hitos_demo.sql` | los 6 hitos de PRY-2026-01 |
| `V6__caso_prueba_completo.sql` | 10 recursos de los 4 tipos, asignaciones, criterios y evidencias de hitos, 8 KPI de proyecto e hitos, 12 KPI de gestión del cambio con histórico, 5 solicitudes de cambio con sus etapas, modificaciones y acciones correctivas |
| `V7__ajuste_sc005.sql` | SC-005 también baja la asignación del recurso que reduce, para que su aprobación no deje capacidad negativa |

Hibernate no modifica el esquema (`ddl-auto: none`); cualquier cambio va en una migración nueva `V8__…`, nunca editando una ya aplicada.

Al mapear entidades:

- Las columnas `costo_plan` y `costo_total` las calcula Oracle: `@Column(insertable = false, updatable = false)`.
- Los `NUMBER(1)` usan `@Convert(converter = NumericBooleanConverter.class)`.
- Las tablas con columna `version` usan `@Version`.

## API

Todas las rutas (salvo el login) exigen `Authorization: Bearer <token>`. El administrador trabaja en todos los proyectos; el usuario de proyecto, solo en los que dirige o integra (`AccesoProyecto`). Las marcadas con **A** son solo del administrador.

| Módulo | Rutas |
|---|---|
| Sesión | `POST /api/auth/login` · `GET /api/usuarios` |
| 1 · Proyectos | `GET /api/proyectos[/{id}]` · `POST`, `PUT /{id}`, `POST /{id}/cerrar`, `DELETE /{id}` **A** |
| 3 · Planificación | `GET /api/proyectos/{id}/plan` · `POST/PUT/DELETE …/actividades[/{actId}]` · `POST/PUT/DELETE …/dependencias[/{depId}]` |
| 3 · Calendario | `GET /api/calendarios[/{id}]` · `PUT /{id}`, `POST/DELETE /{id}/dias-no-laborables` **A** |
| 5 · Recursos | `GET …/costos` · `GET …/carga` · `POST/PUT/DELETE …/recursos`, `…/requerimientos`, `…/asignaciones-profesional`, `…/asignaciones-recurso` |
| 5 · Catálogo | `GET /api/profesionales`, `/api/profesionales/carga`, `/api/tipos-profesional` · `POST/PUT/DELETE` **A** |
| 6 · Hitos | `GET/POST …/hitos` · `GET/PUT/DELETE …/hitos/{id}` · `PUT …/hitos/{id}/criterios/{criterioId}` |
| Evidencias | `POST …/hitos/{id}/evidencias`, `POST …/cambios/{id}/evidencias` (multipart, máx. 10 MB) · `GET /api/evidencias/{id}/archivo` · `DELETE /api/evidencias/{id}` |
| 7 y 9 · KPI | `GET /api/kpi/variables` · `GET …/kpi[/{id}]` · `POST …/kpi/{id}/mediciones` · `POST …/kpi/probar` · `POST/PUT/DELETE …/kpi` **A** |
| 8 · Cambios | `GET/POST …/cambios` · `GET/PUT …/cambios/{id}` · `PUT …/seguimiento` · `POST …/avanzar`, `…/cerrar` · `PUT …/etapas/{n}` · `POST …/aprobar`, `…/rechazar`, `DELETE …/cambios/{id}` **A** |
| 8 · Modificaciones | `GET …/opciones-modificacion` · `POST/DELETE …/cambios/{id}/modificaciones` · `POST/PUT/DELETE …/cambios/{id}/acciones` · `GET …/acciones-correctivas` |
| 8 · Trazabilidad | `GET …/trazabilidad` · `GET …/lineas-base` · `GET …/lineas-base/{id}/comparacion` |
| Alertas | `GET …/alertas` |
| Tableros | `GET /api/tableros/proyecto?proyectoId=&etapaId=&desde=&hasta=&responsableId=` · `GET /api/tableros/cambio?proyectoId=&tipo=&prioridad=&desde=&hasta=` |

`…` = `/api/proyectos/{proyectoId}`. El detalle de cada cuerpo y respuesta está en Swagger.

Toda modificación del plan responde con el resultado del recálculo; las de planificación devuelven `{ plan, recalculadas }`, donde `recalculadas` son las actividades cuyas fechas se movieron. Aprobar un cambio devuelve la solicitud, el término y el costo antes y después, las actividades reprogramadas, la línea base creada y los KPI afectados.

## Módulo 3 · Motor de programación

**MotorProgramacion** (`planificacion/MotorProgramacion.java`):

1. Ordena las hojas por sus dependencias (orden topológico de Kahn). Si queda alguna sin ordenar hay un ciclo y responde 422 `DEPENDENCIA_CIRCULAR` con el camino.
2. Cada hoja empieza en el primer día hábil que cumple todas sus predecesoras y el inicio del proyecto.
3. Etapas y actividades con hijos se derivan: inicio mínimo, término máximo, duración en días hábiles, fechas reales y avance ponderado por duración. El avance del proyecto se calcula igual.

Días inclusivos (una tarea de 1 día empieza y termina el mismo día) y desfase `d` en días hábiles, con la convención de MS Project:

| Tipo | Regla | Ejemplo: A del 01-09 (mar) al 07-09 (lun), B de 3 días |
|---|---|---|
| FS | inicio B ≥ término A + 1 + d | d = 0: B 08-09 → 10-09 · d = −2: B 04-09 → 08-09 |
| SS | inicio B ≥ inicio A + d | d = 3: B 04-09 → 08-09 |
| FF | término B ≥ término A + d | d = 0: B 03-09 → 07-09 |
| SF | término B ≥ inicio A − 1 + d | d = 5: B 03-09 → 07-09 |

Reglas: las dependencias unen solo elementos sin hijos (como los recursos); una actividad con dependencias no puede recibir subactividades; 100 % de avance exige término real y viceversa. Cambiar el calendario reprograma todos los proyectos que lo usan.

## Tests

```bash
./mvnw test                               # motor, calendario, costos, fórmulas, semáforo, curva S, alertas (sin base de datos)
./mvnw test -Dtest=PlanificacionOracleIT  # integración contra Oracle; requiere docker compose up -d
```

## Errores

Todas las respuestas de error tienen la misma forma:

```json
{ "estado": 409, "codigo": "CK_PROYECTO_TERMINADO", "mensaje": "…", "campos": {}, "fecha": "…" }
```

| Estado | Cuándo |
|---|---|
| 400 | validación de campos (`campos` trae el detalle) |
| 401 / 403 | sin sesión / rol sin permiso |
| 404 | registro inexistente |
| 409 | una restricción de Oracle rechazó el dato (`codigo` = nombre de la constraint) |
| 422 | regla de negocio (dependencia circular, recurso insuficiente, etapa fuera de orden, credenciales, triggers ORA-20xxx) |
