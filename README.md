# VetSync API

API REST para registrar propietarios, mascotas e historiales medicos con Java 21, Spring Boot y MySQL 8.

## Modelo de datos

- `owners` 1:N `pets`: un propietario puede tener varias mascotas; cada mascota requiere un propietario.
- `pets` 1:N `medical_records`: cada historial medico pertenece a una mascota.
- `veterinarians` 1:N `appointments`: cada cita se asigna a un veterinario registrado, una mascota y el propietario de esta.
- `appointments` 1:N `notification_history`: cada recordatorio conserva una instantanea del destinatario, la mascota, el veterinario, el horario y el resultado del procesamiento.
- Las claves foraneas usan `ON DELETE RESTRICT`. La API tambien impide eliminar propietarios con mascotas y mascotas con historial para proteger los datos relacionados. El historial se elimina de forma individual.
- `appointments` conserva su propio estado (`PENDING`, `CONFIRMED`, `COMPLETED`, `CANCELLED`) y usa control optimista de versiones para detectar modificaciones concurrentes.

## Estructura

```text
src/main/java/com/vetsync/
  api/
    controller/          # Endpoints REST
    dto/                 # Contratos y validacion HTTP
    exception/           # Manejo centralizado de errores
  application/
    exception/           # Errores de negocio controlados
    service/             # Casos de uso y transacciones
  domain/
    model/               # Entidades y tipos del dominio
    repository/          # Interfaces de persistencia
  infrastructure/
    persistence/         # Adaptadores Spring Data JPA
src/main/resources/
  db/migration/          # Esquema MySQL versionado con Flyway
```

Los controladores dependen de servicios; los servicios dependen de interfaces del dominio; los adaptadores encapsulan Spring Data/JPA. Las entidades no se exponen directamente en las respuestas HTTP.

## Configuracion y ejecucion

Requiere Java 21, Maven, Docker y Docker Compose. Se puede usar una base MySQL existente o iniciarla localmente:

En PowerShell:

```powershell
$env:DB_PASSWORD = "<tu-clave>"
$env:MYSQL_ROOT_PASSWORD = "<clave-root-local>"
$env:VETSYNC_JWT_SECRET = "<clave-base64-estable-de-al-menos-32-bytes>"
docker compose up -d db
$env:DB_URL = "jdbc:mysql://localhost:3306/vetsync"
$env:DB_USERNAME = "vetsync"
mvn spring-boot:run
```

Flyway crea las tablas al iniciar la aplicacion; Hibernate valida el esquema.
Genera el secreto JWT aleatorio una sola vez (por ejemplo con `[Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))`) y almacena el valor de manera persistente en un gestor de secretos. No lo regeneres en cada reinicio: invalidaria todos los tokens emitidos anteriormente. La aplicacion no inicia si el secreto falta o es invalido.

Ejecutar pruebas unitarias:

```powershell
mvn test
```

Las citas usan la zona configurada en `VETSYNC_ZONE` (por defecto `UTC`). Todas las altas, modificaciones y cambios de estado toman un bloqueo pesimista de la fila del veterinario dentro de una transaccion `READ_COMMITTED`, y despues consultan si existe una cita no cancelada donde `inicio_existente < fin_solicitado` y `fin_existente > inicio_solicitado`. La serializacion por veterinario hace que una segunda solicitud concurrente espere y vuelva a evaluar la disponibilidad despues del commit de la primera; el mecanismo aplica tambien cuando hay multiples instancias de la API. La base de datos mantiene claves foraneas y restricciones para evitar horas invertidas y estados invalidos. Las modificaciones concurrentes de una misma cita se detectan adicionalmente con `@Version`.

## Notificaciones de citas

El programador busca citas pendientes o confirmadas cuya hora de inicio entra en la ventana de recordatorio configurada. Por defecto, la notificacion se procesa una hora antes y se comprueba cada minuto. Personaliza `VETSYNC_REMINDER_MINUTES`, `VETSYNC_NOTIFICATION_FIXED_DELAY` (duracion ISO 8601, por ejemplo `PT1M`) y `VETSYNC_NOTIFICATIONS_ENABLED`. La zona horaria es `VETSYNC_ZONE`.

El componente `EmailSender` abstrae al proveedor. La implementacion predeterminada `SimulatedEmailSender` solo registra un evento de correo simulado en el log; no se conecta a SMTP ni a ningun servicio externo. El mensaje incluye correo del propietario, mascota, fecha, hora y veterinario.

Antes del envio, el sistema vuelve a bloquear y comprobar la cita, para no notificar una cita cancelada o que haya sido reprogramada fuera de la ventana. El historial queda en `notification_history` con el resultado `SENT` o `FAILED` y una clave unica por cita y anticipacion. La insercion atomica impide que ejecuciones concurrentes vuelvan a procesar un recordatorio ya registrado. Los errores del proveedor dejan un evento fallido y se registran en el log; no se reintenta automaticamente, para evitar notificaciones duplicadas.

El historial de una cita se consulta en `GET /api/appointments/{id}/notifications`.

La suite incluye pruebas unitarias para el contenido del mensaje, cancelacion, reprogramacion, duplicados, fallos del proveedor y limites de la ventana. Ejecuta `mvn test`.

## Autenticacion y autorizacion

Para llamadas autenticadas, envia `Authorization: Bearer <JWT>`; nunca incluyas el token en la URL o en registros.

Las rutas de la API requieren un JWT firmado (`Authorization: Bearer <token>`), excepto el envio de solicitud en `POST /api/auth/register` y el inicio de sesion en `POST /api/auth/login`. Los tokens HS256 duran 15 minutos. `VETSYNC_JWT_SECRET` debe ser una clave aleatoria Base64 de al menos 32 bytes, generada una vez y protegida en un gestor de secretos; usa HTTPS en todo entorno compartido.

El registro publico solo crea una solicitud pendiente en `owner_registration_requests` y devuelve `202 Accepted`; **no** crea una cuenta activa ni emite un token. El personal autentificado verifica la identidad fuera de banda y gestiona solicitudes mediante `GET /api/staff/owner-registration-requests`, `POST /api/staff/owner-registration-requests/{requestId}/approve` y `DELETE /api/staff/owner-registration-requests/{requestId}`. Aprobar crea, en una misma transaccion, el propietario y su cuenta; aprobar o rechazar elimina la solicitud y su hash de contrasena. Una direccion pendiente no es unica ni reserva una cuenta, previniendo suplantacion por auto-registro y bloqueo de correo.

Una cuenta OWNER solo consulta sus propios datos, mascotas, citas e historial de notificaciones. Solo STAFF administra propietarios/veterinarios, accede a agendas completas y crea o modifica el historial clinico. Los propietarios pueden reprogramar sus propias citas conservando el estado actual y cancelarlas; solo STAFF puede confirmar o completar citas. Los identificadores que envia el cliente nunca determinan por si solos el alcance de acceso.

El registro pendiente y el inicio de sesion aun requieren limites de frecuencia y una politica de retencion operativa antes de exponerse a Internet. Configura esos controles en un gateway/proxy compartido por todas las instancias y elimina periodicamente solicitudes pendientes antiguas conforme a la politica de privacidad del despliegue. El correo simulado no verifica identidad.

Las cuentas STAFF se provisionan en un despliegue privado configurando temporalmente `VETSYNC_STAFF_EMAIL` y `VETSYNC_STAFF_PASSWORD`; se exige una contrasena de al menos 16 caracteres. El proceso de arranque crea la cuenta con hash PBKDF2, no sobreescribe una cuenta existente y falla si las credenciales se configuran a medias. Quita los valores temporales del entorno tras provisionar y no los guardes en el repositorio. La verificacion de identidad del propietario es una aprobacion de personal fuera de banda; el correo simulado no se utiliza para verificacion.

## Endpoints

| Metodo | Ruta | Funcion |
|---|---|---|
| POST, GET, GET por ID, PUT, DELETE | `/api/owners`, `/api/owners/{id}` | CRUD de propietarios |
| POST, GET, GET por ID, PUT, DELETE | `/api/pets`, `/api/pets/{id}` | CRUD y busqueda de mascotas |
| GET | `/api/pets?name=Luna&ownerName=Ana&ownerId=1&id=2` | Filtros opcionales combinables por nombre de mascota, nombre/ID de propietario e identificador |
| POST, GET | `/api/pets/{petId}/medical-records` | Crear y listar el historial de una mascota |
| GET por ID, PUT, DELETE | `/api/medical-records/{id}` | Consultar, actualizar o eliminar un registro medico |
| POST | `/api/auth/register` | Enviar una solicitud de registro de propietario pendiente de verificacion |
| POST | `/api/auth/login` | Obtener un JWT para una cuenta activa |
| GET, POST aprobar, DELETE rechazar | `/api/staff/owner-registration-requests` | Gestionar solicitudes pendientes (solo personal) |
| POST, GET, GET por ID | `/api/veterinarians`, `/api/veterinarians/{id}` | Registrar y consultar veterinarios |
| POST, GET por ID, PUT | `/api/appointments`, `/api/appointments/{id}` | Agendar, consultar y reprogramar citas |
| PATCH | `/api/appointments/{id}/status` | Confirmar, completar o cancelar una cita |
| GET | `/api/appointments/{id}/notifications` | Consultar el historial de recordatorios de una cita |
| GET | `/api/veterinarians/{id}/appointments/day?date=2026-11-06` | Consultar la agenda diaria |
| GET | `/api/veterinarians/{id}/appointments/week?date=2026-11-06` | Consultar la agenda semanal (lunes a lunes) |
| GET | `/api/veterinarians/{id}/appointments/month?year=2026&month=11` | Consultar la agenda mensual |

Al crear una cita se requiere `petId`, `veterinarianId`, `date`, `startTime`, `endTime` y `reason`; el propietario se deriva de la mascota para mantener la relacion consistente. El alta inicia en `PENDING`. El endpoint de estado acepta `PENDING`, `CONFIRMED`, `COMPLETED` o `CANCELLED`; cancelar libera el horario. Los horarios nuevos deben ser futuros, y una cita solo puede marcarse completada una vez finalizada.

Las altas devuelven `201`, los borrados `204`, las validaciones `400`, los recursos inexistentes `404` y los conflictos de integridad o disponibilidad `409`. El campo `sex` acepta `MALE`, `FEMALE` o `UNKNOWN`.
