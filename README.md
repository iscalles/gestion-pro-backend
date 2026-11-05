# Gestión Pro · Backend

API del proyecto integrador de Gestión de Proyectos de Software (Duoc UC). El frontend está en [iscalles/gestion-pro](https://github.com/iscalles/gestion-pro).

**Stack:** Java 21 · Spring Boot 4.1 · Spring Data JPA · Spring Security con JWT · Flyway · Oracle Database 23ai Free · exp4j (fórmulas de KPI) · springdoc (Swagger).

## Levantar en local

Requiere Java 21 y Docker.

```bash
docker compose up -d        # Oracle Free; la primera vez tarda 1-2 minutos
./mvnw spring-boot:run      # API en http://localhost:8080
```

Al arrancar, Flyway crea el esquema y carga los datos de demostración. Documentación interactiva en http://localhost:8080/swagger-ui.html.

Cuentas (contraseña `demo1234`):

| Correo | Rol |
|---|---|
| `admin@gestionpro.cl` | Administrador |
| `usuario@gestionpro.cl` | Usuario de proyecto (miembro de PRY-2026-01) |

Para partir de cero: `docker compose down -v && docker compose up -d`.

## Arquitectura

Un solo backend (monolito modular). El recálculo que pide la evaluación (cambiar una duración, modificar recursos o aprobar un cambio y ver el efecto en Gantt, costos, KPI y tableros) corre en **una transacción sobre una base**: se aplica completo o no se aplica.

```
cl.duoc.gestionpro
├─ config/          seguridad JWT, CORS, Swagger
├─ comun/           excepciones y manejador de errores
├─ seguridad/       usuarios, roles, login
├─ proyecto/        Módulo 1 (implementado: listar, obtener, crear, editar)
├─ planificacion/   Módulo 3 (implementado): actividades, dependencias, calendario, motor de programación
├─ recurso/         Módulo 5: profesionales, recursos, asignaciones, costos, sobreasignación
├─ hito/            Módulo 6
├─ kpi/             Módulos 7 y 9: KPI configurables, semáforo, histórico
├─ cambio/          Módulo 8: solicitudes, diez etapas, línea base, trazabilidad
├─ tablero/         los dos tableros
└─ recalculo/       cadena fechas → costos → avance → KPI → alertas
```

Cada paquete tiene un `package-info.java` con lo que le corresponde. `proyecto/` sirve de patrón para el resto: entidad, repositorio, DTO de entrada y salida, servicio transaccional y controlador.

El Módulo 2 (EDT) no se construye: la jerarquía etapa → actividad → subactividad vive en `actividad.padre_id`.

## Base de datos

- `db/migration/V1__esquema.sql`: 30 tablas, 5 vistas y 3 triggers con las reglas de integridad.
- `V2__catalogos.sql`: las diez etapas del cambio y las variables disponibles para las fórmulas de KPI.
- `V3__datos_demo.sql`: usuarios, calendario 2026-2027 y cuatro proyectos.
- `V4__planificacion_demo.sql`: 5 profesionales y la planificación de PRY-2026-01 (4 etapas, 12 actividades, 20 subactividades, 25 dependencias con FS, SS, FF y SF). Sin fechas planificadas: al arrancar, `ProgramacionInicial` las calcula con el motor.

Hibernate no modifica el esquema (`ddl-auto: none`); cualquier cambio va en una migración nueva `V4__…`, nunca editando una ya aplicada.

Al mapear entidades:

- Las columnas `costo_plan` y `costo_total` las calcula Oracle: `@Column(insertable = false, updatable = false)`.
- Los `NUMBER(1)` usan `@Convert(converter = NumericBooleanConverter.class)`.
- Las tablas con columna `version` usan `@Version`.

## Módulo 3 · Planificación

| Método | Ruta | Qué hace |
|---|---|---|
| GET | `/api/proyectos/{id}/plan` | actividades en orden de árbol (con `nivel`, `esHoja`, `atrasada`) y dependencias |
| POST / PUT / DELETE | `/api/proyectos/{id}/actividades[/{actId}]` | el tipo se deduce del padre; borrar elimina descendientes y sus vínculos |
| POST / PUT / DELETE | `/api/proyectos/{id}/dependencias[/{depId}]` | FS, SS, FF, SF con desfase de −999 a 999 días hábiles |
| GET / PUT | `/api/calendarios[/{id}]` | días laborables y horas por día (PUT solo administrador) |
| POST / DELETE | `/api/calendarios/{id}/dias-no-laborables[/{diaId}]` | feriados (solo administrador) |
| GET | `/api/profesionales` | responsables para elegir |

Toda modificación recalcula el proyecto en la misma transacción y responde `{ plan, recalculadas }`, donde `recalculadas` son las actividades cuyas fechas se movieron. Cambiar el calendario reprograma todos los proyectos que lo usan. El administrador trabaja en todos los proyectos; el usuario de proyecto, en los que dirige o integra.

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

Reglas: las dependencias unen solo elementos sin hijos (como los recursos); una actividad con dependencias no puede recibir subactividades; 100 % de avance exige término real y viceversa.

## Tests

```bash
./mvnw test                               # motor y calendario (no necesita base de datos)
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
| 422 | regla de negocio (dependencia circular, credenciales, triggers ORA-20xxx) |
