# Cafetería — Arquitectura de Microservicios

Proyecto semestral de **Desarrollo FullStack 1 (DSY1103)**. Arquitectura distribuida basada en
microservicios independientes para la gestión de una cafetería: usuarios, productos, categorías,
inventario, pedidos, empleados, sucursales, proveedores y notificaciones, con autenticación
centralizada y enrutamiento mediante API Gateway.

## Estudiante

- Lillo Sebastián Vargas Ricardo

## Arquitectura general

```
Cliente / Postman
        |
        v
  API GATEWAY  :8080  (Spring Cloud Gateway MVC)
        |
        +--> AUTH-SERVICE          :8083   /api/v1/auth/**
        +--> CATEGORIAS-SERVICE    :8088   /api/categorias/**
        +--> EMPLEADOS-SERVICE     :8085   /api/empleados/**
        +--> INVENTARIO-SERVICE    :8084   /api/inventario/**
        +--> NOTIFICACIONES-SERVICE:8087   /api/notificaciones/**
        +--> PEDIDOS-SERVICE       :8095   /api/pedidos/**          (Feign -> Productos, Usuarios)
        +--> PRODUCTOS-SERVICE     :8090   /api/productos/**
        +--> PROVEEDORES-SERVICE   :8888   /api/proveedores/**
        +--> SUCURSALES-SERVICE    :8089   /api/sucursales/**
        +--> USUARIOS-SERVICE      :8086   /api/usuarios/**
        |
        v
  EUREKA SERVER :8761  (Service Discovery)
```

Cada microservicio tiene su propia base de datos MySQL (un esquema por servicio) y se registra en
Eureka para que el Gateway pueda resolverlo por nombre lógico (`lb://NOMBRE-SERVICIO`).

## Microservicios implementados (10)

| Microservicio | Puerto local | Base de datos | Persistencia |
|---|---|---|---|
| Auth (login/JWT) | 8083 | db_usuario | Flyway |
| Categorías | 8088 | db_categorias | Flyway |
| Empleados | 8085 | db_empleados | Flyway |
| Inventario | 8084 | db_inventario | Flyway |
| Notificaciones | 8087 | db_notificaciones | Flyway |
| Productos | 8090 | db_productos | Flyway |
| Proveedores | 8888 | db_proveedores | Flyway |
| Sucursales | 8089 | db_sucursales | Flyway |
| Usuarios | 8086 | db_usuarios | Flyway |
| Pedidos | 8095 | db_pedidos | Flyway |

Infraestructura: **Eureka Server** (8761) y **API Gateway** (8080).

## Funcionalidades implementadas

- CRUD completo con DTOs de request/response separados de las entidades JPA.
- Validaciones con Bean Validation (`@NotBlank`, `@Email`, `@Pattern`, etc.) y manejo centralizado
  de errores con `@RestControllerAdvice` (`GlobalExceptionHandler` + `ErrorResponse`).
- Autenticación JWT: `AUTH-SERVICE` emite el token; el resto de los microservicios lo valida con un
  `JwtAuthenticationFilter` (Swagger, Actuator y `/api/v1/auth/login` quedan públicos).
- Comunicación entre microservicios con **Feign Client**: `pedidos-service` consulta
  `usuarios-service` y `productos-service` para validar el pedido y calcular el total, reenviando
  el header `Authorization` recibido (`FeignClientConfig`).
- Documentación interactiva con **Swagger/OpenAPI** en `/doc/swagger-ui.html` de cada servicio,
  con esquema de seguridad JWT (`OpenApiConfig` / `SwaggerConfig`): boton **Authorize** para pegar
  el token y poder probar los endpoints protegidos directo desde la UI (pega solo el token, sin el
  prefijo `Bearer `).
- Logs estructurados con SLF4J en la capa de servicio.
- Pruebas unitarias con **JUnit 5 + Mockito** en la capa de servicio de los 10 microservicios,
  con cobertura mínima del 80% verificada con **JaCoCo** (`./mvnw clean verify`).
- Migraciones de base de datos versionadas con **Flyway** en los 10 microservicios.
- Despliegue containerizado con **Docker** (build multi-stage Maven + JRE 21) y orquestación con
  **Docker Compose**.

## Cómo ejecutar en local (IDE, sin Docker)

1. Tener un MySQL local corriendo en `localhost:3306` con usuario `root`.
2. Cada microservicio crea su propia base de datos automáticamente
   (`createDatabaseIfNotExist=true`) salvo los que usan Flyway, que la asumen creada.
3. Levantar en este orden:
   1. `eureka-server` (puerto 8761)
   2. `Auth` y el resto de los microservicios de negocio (cualquier orden)
   3. `api-gateway` (puerto 8080)
4. Probar vía Gateway: `http://localhost:8080/api/v1/auth/login`, etc.

## Cómo ejecutar con Docker

```bash
# Desde la carpeta donde está docker-compose.yml
docker compose up --build
```

Esto levanta los 12 contenedores (MySQL, Eureka, API Gateway, Auth y los 9 microservicios de
negocio) en la red `auth-network`, crea automáticamente todas las bases de datos
(`mysql/init/01-create-databases.sql`) y conecta cada servicio usando el perfil `docker`
(`application-docker.properties`).

- Eureka: http://localhost:8761
- API Gateway: http://localhost:8080
- Swagger de cada servicio (vía Gateway o directo): `/doc/swagger-ui.html`

Variables de entorno relevantes (archivo `.env`): `MYSQL_ROOT_PASSWORD`, `JWT_SECRET`.

## Perfiles de configuración (dev / docker / prod)

Cada microservicio tiene 3 perfiles de Spring, además del `application.yml` base (puerto, nombre
del servicio y Swagger, comunes a los 3 entornos):

| Perfil | Archivo | Cuándo se usa | Cómo se activa |
|---|---|---|---|
| `dev` (default) | `application-dev.properties` | Desarrollo local en tu máquina, sin Docker | Automático (no hace falta variable de entorno) |
| `docker` | `application-docker.properties` | `docker compose up` en tu equipo o servidor | `SPRING_PROFILES_ACTIVE=docker` (ya seteado en `docker-compose.yml`) |
| `prod` | `application-prod.properties` | Despliegue en Railway / Render | `SPRING_PROFILES_ACTIVE=prod` |

El perfil `prod` **no trae ningún valor por defecto en datos sensibles** (password, JWT secret,
URL de Eureka): si falta la variable de entorno correspondiente, el servicio no levanta. Es
intencional — preferimos que falle al desplegar a que arranque inseguro en producción.

## Despliegue en Railway / Render (perfil prod)

Variables de entorno que hay que configurar por cada microservicio en la plataforma elegida:

| Variable | Ejemplo | Notas |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` | Activa `application-prod.properties` |
| `PORT` | (la inyecta la plataforma) | Railway/Render setean esta variable automáticamente |
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://<host>:3306/db_productos?useSSL=false&serverTimezone=UTC` | Una por servicio, contra el MySQL administrado |
| `SPRING_DATASOURCE_USERNAME` | `root` | |
| `SPRING_DATASOURCE_PASSWORD` | *(secreto, nunca en el repo)* | |
| `JWT_SECRET` | *(secreto, el mismo valor en todos los microservicios)* | Debe coincidir entre `Auth` y el resto, o los tokens no validan |
| `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` | `http://eureka-server.railway.internal:8761/eureka/` | Apunta al Eureka desplegado en la misma plataforma |

Notas importantes para que esto funcione en la nube:

- Eureka, el Gateway y los 9 microservicios deben desplegarse **dentro del mismo proyecto** de
  Railway/Render para poder usar networking privado (`*.railway.internal` en Railway, o el
  nombre del servicio interno en Render). Si cada uno queda en un proyecto separado, no se van a
  poder descubrir entre sí.
- Los 10 microservicios gestionan su esquema con **Flyway** (`src/main/resources/db/migration`).
  En `dev`/`docker` se usa `ddl-auto=none` (Flyway crea las tablas) y en `prod` se usa
  `ddl-auto=validate`: Hibernate solo verifica que el esquema coincida con las entidades.

## Pruebas unitarias y cobertura (JaCoCo)

Los 10 microservicios tienen pruebas unitarias con **JUnit 5 + Mockito** sobre la capa de
servicio (reglas de negocio), con estructura Given–When–Then y mocks de repositorios y clientes
Feign. Cada `pom.xml` integra el plugin **JaCoCo** con una regla de verificación que exige un
**mínimo de 80% de cobertura de instrucciones** sobre la lógica de negocio (se excluyen del
cálculo las clases de configuración, DTOs, entidades, seguridad y controladores, que no contienen
reglas de negocio).

```bash
# Desde la carpeta de cada microservicio
./mvnw clean verify

# El reporte HTML de cobertura queda en:
# target/site/jacoco/index.html
```

Si la cobertura de la capa de servicio baja del 80%, el build falla (`jacoco:check`).

## Pruebas de integración (Postman)

En la raíz del proyecto está la colección **`Cafeteria_Microservicios.postman_collection.json`**:

1. Importarla en Postman.
2. Levantar el ecosistema (local o Docker).
3. Ejecutar primero **Auth → Login**: el script de la petición guarda el token JWT en la variable
   de colección `{{token}}`.
4. El resto de las carpetas (CRUD de cada microservicio, inventario, pedidos con Feign,
   notificaciones) usan ese token automáticamente como `Bearer {{token}}` contra el Gateway
   (`{{base_url}}` = `http://localhost:8080`).

## Control de versiones y trabajo colaborativo

- Repositorio GitHub: `<URL del repositorio público del equipo>` (acceso otorgado al docente).
- Tablero Trello: `<URL del tablero del equipo>` (planificación, roles y seguimiento de tareas).
- El archivo `.gitignore` excluye `target/`, archivos de IDE y `.env` (configuración sensible);
  usar `.env.example` como plantilla para crear el `.env` local.

## Migraciones de base de datos (Flyway)

Cada microservicio versiona su esquema en `src/main/resources/db/migration`
(`V1__crear_tabla_*.sql` y, donde aplica, `V2__*` con datos de demostración). Flyway ejecuta las
migraciones pendientes al iniciar el servicio y registra el historial en la tabla
`flyway_schema_history` de cada base de datos.
