# VetSync

Sistema de gestión veterinaria: API REST para administrar propietarios, mascotas, historiales médicos, veterinarios y citas, con recordatorios por correo electrónico.

> Artefacto Maven: `com.vetsync:vetsync-api:0.1.0-SNAPSHOT` ("VetSync API").

## Objetivo

Centralizar la información de una clínica veterinaria (quién es el propietario de cada mascota, su historial clínico, qué veterinario atiende y cuándo) y evitar conflictos de agenda, ofreciendo acceso controlado a cada propietario sobre sus propios datos.

## Problema que resuelve

- Evita reservar dos citas solapadas con el mismo veterinario, incluso con solicitudes concurrentes.
- Mantiene el historial clínico asociado a cada mascota sin borrados en cascada accidentales.
- Restringe a cada propietario a sus datos, mascotas, citas y notificaciones.
- Registra los intentos de recordatorio para evitar duplicados.

## Funcionalidades principales

- **Autenticación JWT**: inicio de sesión para propietarios y personal (`STAFF`).
- **Registro de propietarios con aprobación**: las solicitudes públicas quedan pendientes hasta que el personal las aprueba o rechaza.
- **Gestión de propietarios, mascotas e historiales médicos.**
- **Gestión de veterinarios.**
- **Citas**: creación, consulta, reprogramación, cambio de estado y consulta de agenda por día, semana y mes.
- **Historial de notificaciones** de recordatorios por cita.

Los detalles de cada endpoint están en [API.md](API.md).

## Tecnologías

- Java 21
- Spring Boot 3.5.7 (Web, Validation, Security, OAuth2 Resource Server, Data JPA)
- MySQL 8 y Flyway (migraciones en `src/main/resources/db/migration`)
- JWT (HS256) con `spring-security-oauth2-resource-server`
- Maven
- JUnit 5, Mockito, `spring-security-test`
- Checkstyle y JaCoCo
- GitHub Actions

## Arquitectura

Monolito modular por capas, descrito en detalle en [ARCHITECTURE.md](ARCHITECTURE.md):

| Módulo / paquete | Responsabilidad |
|---|---|
| `api` | Controladores REST, DTOs de entrada/salida con validación, manejo de errores (`ApiExceptionHandler`) y autorización por recurso (`AccessAuthorization`). |
| `application` | Casos de uso y reglas de negocio (servicios como `AppointmentService`, `PetService`, `OwnerRegistrationService`). |
| `domain` | Modelos y enums del dominio (`Owner`, `Pet`, `Appointment`, `AppointmentStatus`, `Sex`, `AccountRole`) e interfaces de repositorio. |
| `infrastructure` | Adaptadores JPA/MySQL, configuración de seguridad y JWT, reloj (`Clock`) y aprovisionamiento de la cuenta de personal. |
| `notifications` | Selección de citas próximas, entrega mediante la interfaz `EmailSender`, historial de notificaciones y programador (`@Scheduled`). |

Reglas clave: los controladores no contienen SQL ni reglas de negocio; las entidades JPA no se devuelven como respuesta REST.

## Estructura de directorios

```text
Veterinaria/
├── compose.yaml                 # MySQL 8.4 para desarrollo
├── checkstyle.xml               # Reglas de estilo (Checkstyle)
├── pom.xml
├── API.md                       # Referencia de la API
├── ARCHITECTURE.md              # Decisiones de arquitectura
├── AGENTS.md                    # Reglas para agentes de desarrollo
├── .github/workflows/ci.yml     # Integración continua
└── src/
    ├── main/
    │   ├── java/com/vetsync/
    │   │   ├── VetSyncApplication.java
    │   │   ├── api/             # controller, dto, exception, security
    │   │   ├── application/     # service
    │   │   ├── domain/          # model, repository
    │   │   ├── infrastructure/  # config, persistence
    │   │   └── notifications/   # api, application, domain, infrastructure
    │   └── resources/
    │       ├── application.yml
    │       └── db/migration/    # V1..V5 (Flyway)
    └── test/java/com/vetsync/   # Pruebas unitarias e integración
```

## Requisitos previos

- **JDK 21**
- **Maven 3.9+**
- **MySQL 8** (o Docker para usar `compose.yaml`)

## Instalación y configuración

```bash
git clone <url-del-repositorio> Veterinaria
cd Veterinaria
mvn -B -DskipTests package
```

### Variables de entorno

Los valores se leen de `src/main/resources/application.yml`.

| Variable | Obligatoria | Valor por defecto | Descripción |
|---|---|---|---|
| `VETSYNC_JWT_SECRET` | Sí | — | Clave Base64 aleatoria de al menos 32 bytes para firmar los JWT. Si falta o es inválida, la aplicación no arranca. |
| `VETSYNC_STAFF_EMAIL` | No | — | Correo de la cuenta `STAFF` que se crea al arrancar. |
| `VETSYNC_STAFF_PASSWORD` | No | — | Contraseña de esa cuenta (mínimo 16 caracteres). Ambas variables deben definirse juntas. |
| `DB_URL` | No | `jdbc:mysql://localhost:3306/vetsync?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC` | URL JDBC de MySQL. |
| `DB_USERNAME` | No | `vetsync` | Usuario de la base de datos. |
| `DB_PASSWORD` | No | (vacío) | Contraseña de la base de datos. |
| `PORT` | No | `8080` | Puerto HTTP. |
| `VETSYNC_ZONE` | No | `UTC` | Zona horaria para fechas y reloj de la aplicación. |
| `VETSYNC_NOTIFICATIONS_ENABLED` | No | `true` | Activa el programador de recordatorios. |
| `VETSYNC_REMINDER_MINUTES` | No | `60` | Anticipación del recordatorio, en minutos. |
| `VETSYNC_NOTIFICATION_FIXED_DELAY` | No | `PT1M` | Intervalo entre ejecuciones del programador (formato ISO-8601). |

Generar un secreto de ejemplo para uso local:

```bash
openssl rand -base64 48
```

No use secretos de ejemplo en entornos compartidos ni los versione en el repositorio.

### Base de datos MySQL

Con Docker, `compose.yaml` levanta MySQL 8.4 en el puerto 3306 con la base `vetsync` y el usuario `vetsync`. Antes de iniciarlo debe definir:

```bash
export DB_PASSWORD='<contraseña-de-vetsync>'
export MYSQL_ROOT_PASSWORD='<contraseña-root>'
docker compose up -d db
```

Sin Docker, cree la base y el usuario manualmente:

```sql
CREATE DATABASE vetsync CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER 'vetsync'@'localhost' IDENTIFIED BY '<contraseña>';
GRANT ALL PRIVILEGES ON vetsync.* TO 'vetsync'@'localhost';
```

Flyway aplica las migraciones V1 a V5 automáticamente al arrancar. Hibernate está configurado con `ddl-auto: validate`, por lo que el esquema no se crea ni modifica desde JPA.

## Ejecución

```bash
export VETSYNC_JWT_SECRET='<secreto-base64>'
export VETSYNC_STAFF_EMAIL='staff@clinica.example'
export VETSYNC_STAFF_PASSWORD='<contraseña-de-al-menos-16-caracteres>'
export DB_PASSWORD='<contraseña-de-vetsync>'
mvn spring-boot:run
```

La API queda disponible en `http://localhost:8080`. Las rutas bajo `/api/**` requieren JWT, salvo `/api/auth/register` y `/api/auth/login`.

## Pruebas y calidad

```bash
mvn -B verify
```

Este comando ejecuta:

- **Checkstyle** (fase `validate`) con `checkstyle.xml`; las violaciones fallan la compilación. Las fuentes de prueba están excluidas.
- **Pruebas** JUnit 5 y Mockito, con `spring-security-test` para la seguridad.
- **JaCoCo** (fase `verify`): exige una cobertura de línea mínima del **80 %** por bundle. Si no se alcanza, el build falla.

Los informes se generan en `target/site/jacoco` y `target/surefire-reports`.

## Integración continua

El workflow `.github/workflows/ci.yml`:

- Se ejecuta en `push` a `main` y manualmente (`workflow_dispatch`).
- Usa `ubuntu-latest`, JDK 21 (Temurin) con caché de Maven.
- Ejecuta `mvn -B verify` (Checkstyle, pruebas y verificación de cobertura).
- Sube los informes de JaCoCo y Surefire como artefactos y publica los resultados de las pruebas con `dorny/test-reporter`.

## Ejemplos de uso

Todos los ejemplos asumen `BASE=http://localhost:8080`.

**1. Solicitud pública de registro (queda pendiente de aprobación):**

```bash
curl -s -X POST "$BASE/api/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
        "fullName": "María López",
        "phone": "+34600111222",
        "email": "maria@example.com",
        "address": "Calle Mayor 10, Madrid",
        "password": "ClaveSegura2024!"
      }'
```

Respuesta `202 Accepted`:

```json
{ "requestId": 1, "status": "PENDING", "message": "Solicitud recibida; pendiente de verificación por el personal." }
```

**2. Inicio de sesión:**

```bash
curl -s -X POST "$BASE/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{ "email": "maria@example.com", "password": "ClaveSegura2024!" }'
```

Respuesta `200 OK`:

```json
{ "accessToken": "eyJhbGciOiJIUzI1NiJ9...", "tokenType": "Bearer", "expiresAt": "2025-01-15T10:15:00Z" }
```

**3. Consulta de mascotas propias (con el token):**

```bash
curl -s "$BASE/api/pets" -H "Authorization: Bearer $TOKEN"
```

**4. Creación de una cita por personal (`STAFF`):**

```bash
curl -s -X POST "$BASE/api/appointments" \
  -H "Authorization: Bearer $STAFF_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
        "petId": 1,
        "veterinarianId": 1,
        "date": "2030-03-10",
        "startTime": "10:00:00",
        "endTime": "10:30:00",
        "reason": "Revisión anual"
      }'
```

Consulte [API.md](API.md) para todos los campos, códigos de estado y errores.

## Seguridad

- **Autenticación**: JWT HS256 de corta duración (15 minutos), emisor `vetsync`, sin estado de sesión en servidor. Claims: `sub` (correo), `role` (`OWNER` o `STAFF`) y `ownerId` (solo para propietarios).
- **Contraseñas**: PBKDF2 mediante `Pbkdf2PasswordEncoder` (`defaultsForSpringSecurity_v5_8`). Nunca se guardan en texto plano.
- **Autorización**: por rol (`@PreAuthorize`) y por recurso (`AccessAuthorization`). Los propietarios solo acceden a sus datos; el `ownerId` se toma del token, no del cliente.
- **Registro público**: no crea cuentas activas. El personal debe verificar la identidad fuera de banda y aprobar la solicitud.
- **Cuenta de personal**: se crea al arrancar solo si se definen `VETSYNC_STAFF_EMAIL` y `VETSYNC_STAFF_PASSWORD`. No existe ruta pública para dar de alta personal.
- **Secretos**: deben proporcionarse por variables de entorno o un gestor de secretos; rótelos si se exponen. No los registre en logs.
- **Transporte**: use HTTPS fuera de entornos locales; la configuración de ejemplo usa `useSSL=false` solo para desarrollo.
- **Concurrencia**: las reservas bloquean al veterinario con un bloqueo pesimista de MySQL dentro de la transacción.
- **Integridad clínica**: las claves foráneas usan `ON DELETE RESTRICT`; no se borran propietarios ni mascotas con historial o citas asociadas.

## Reglas de negocio de agenda

- Una cita referencia una mascota y un veterinario existentes; el propietario se deriva de la mascota.
- `startTime` debe ser anterior a `endTime`.
- Al crear o reprogramar, la fecha y hora de inicio deben ser futuras.
- No se permiten solapamientos entre citas del mismo veterinario. Los intervalos adyacentes sí están permitidos.
- Las citas `CANCELLED` no bloquean horario.
- Estados: `PENDING`, `CONFIRMED`, `COMPLETED`, `CANCELLED`. Solo el personal confirma o completa citas; un propietario puede cancelar y reprogramar conservando el estado actual.

## Notificaciones

El módulo `notifications` busca citas `PENDING` o `CONFIRMED` dentro de la ventana de recordatorio, registra cada envío en `notification_history` (clave única por cita y anticipación) y lo entrega mediante `EmailSender`.

**Estado actual:** la implementación predeterminada es `SimulatedEmailSender`, que solo escribe en el log y **no envía correos reales**.

## Pendiente / incompleto

- **Envío real de correo**: falta implementar un `EmailSender` real (SMTP o proveedor externo). Hoy el envío es simulado.
- **Verificación de identidad de propietarios**: el flujo consiste en que el personal apruebe o rechace solicitudes; no hay verificación automatizada ni notificación al solicitante.
- **Solicitudes pendientes**: el correo de `owner_registration_requests` no es único; no hay retención ni límite de frecuencia para solicitudes de registro.
- **Errores de autorización de búsqueda de mascotas**: un propietario que filtra por otro `ownerId` recibe el 403 por defecto de Spring Security, no el formato `ProblemDetail` del manejador global.
- **Lectura de veterinarios**: `GET /api/veterinarians` y `GET /api/veterinarians/{id}` solo exigen autenticación; cualquier usuario autenticado puede consultarlos.
- **Gestión de usuarios**: no existe API para cambiar contraseñas, desactivar cuentas o rotar tokens.
- **Operación**: no hay métricas, límites de frecuencia ni documentación de despliegue productivo.

## Autor y licencia

Pendiente de información del responsable del proyecto. No se incluye autor ni licencia hasta que el propietario los proporcione.
