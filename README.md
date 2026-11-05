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
├─ planificacion/   Módulo 3: actividades, dependencias, calendario, motor de programación
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

Hibernate no modifica el esquema (`ddl-auto: none`); cualquier cambio va en una migración nueva `V4__…`, nunca editando una ya aplicada.

Al mapear entidades:

- Las columnas `costo_plan` y `costo_total` las calcula Oracle: `@Column(insertable = false, updatable = false)`.
- Los `NUMBER(1)` usan `@Convert(converter = NumericBooleanConverter.class)`.
- Las tablas con columna `version` usan `@Version`.

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
