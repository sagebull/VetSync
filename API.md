# API de VetSync

Referencia de los endpoints de la API REST de VetSync. Base URL de ejemplo: `http://localhost:8080`. Todos los cuerpos son JSON (`Content-Type: application/json`).

Consulte [README.md](README.md) para instalación, configuración y reglas generales.

## Índice

1. [Convenciones generales](#convenciones-generales)
2. [Autenticación](#1-autenticación)
3. [Registro de propietarios (administración)](#2-registro-de-propietarios-administración)
4. [Propietarios](#3-propietarios)
5. [Mascotas](#4-mascotas)
6. [Historial médico](#5-historial-médico)
7. [Veterinarios](#6-veterinarios)
8. [Citas](#7-citas)
9. [Notificaciones](#8-notificaciones)
10. [Roles, autorización y reglas de negocio](#roles-autorización-y-reglas-de-negocio)

---

## Convenciones generales

### Autenticación

Las rutas privadas requieren la cabecera:

```http
Authorization: Bearer <accessToken>
```

Solo son públicas `POST /api/auth/register` y `POST /api/auth/login`. El token caduca a los 15 minutos.

### Códigos de estado usados

| Código | Significado en la API |
|---|---|
| `200 OK` | Consulta o actualización correcta con cuerpo de respuesta. |
| `201 Created` | Recurso creado. |
| `202 Accepted` | Solicitud de registro recibida y pendiente de aprobación. |
| `204 No Content` | Eliminación o rechazo correcto sin cuerpo. |
| `400 Bad Request` | Validación fallida o JSON/parámetros no válidos. |
| `401 Unauthorized` | Falta el token, el token no es válido o las credenciales son incorrectas. |
| `403 Forbidden` | Autenticado pero sin permiso para el recurso o la operación. |
| `404 Not Found` | El recurso no existe. |
| `409 Conflict` | Conflicto de negocio o de datos (p. ej. solapamiento de citas, cuenta existente, registros dependientes). |

### Formato de error

Los errores gestionados por `ApiExceptionHandler` siguen **RFC 7807 (`ProblemDetail`)**:

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 404,
  "detail": "Pet not found",
  "instance": "/api/pets/99"
}
```

Las validaciones de cuerpo (`@Valid`) devuelven `400` con la lista `errors`:

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "La solicitud contiene datos no validos",
  "instance": "/api/auth/login",
  "errors": [
    { "field": "email", "message": "must be a well-formed email address" }
  ]
}
```

Los `409` de base de datos devuelven un mensaje genérico, sin detalles SQL. Los `401` y `403` de Spring Security no pasan por el manejador de VetSync y usan la respuesta por defecto de Spring.

### Formatos de fecha y hora

- `LocalDate`: `YYYY-MM-DD` (p. ej. `2030-03-10`).
- `LocalTime`: `HH:mm:ss` (p. ej. `10:30:00`).
- `LocalDateTime`: `YYYY-MM-DDTHH:mm:ss`.
- `Instant`: ISO-8601 UTC (p. ej. `2025-01-15T10:15:00Z`).
- Las fechas se interpretan con la zona `VETSYNC_ZONE`.

---

## 1. Autenticación

Base: `/api/auth`. Público.

### 1.1 Iniciar sesión

- **Método y URL:** `POST /api/auth/login`
- **Descripción:** autentica a un propietario o a personal y devuelve un JWT.
- **Autorización:** pública.
- **Cuerpo (`LoginRequest`):**

| Campo | Tipo | Obligatorio | Validación |
|---|---|---|---|
| `email` | `String` | Sí | `@NotBlank`, `@Email`, máximo 254 caracteres. |
| `password` | `String` | Sí | `@NotBlank`, máximo 128 caracteres. |

Ejemplo de petición:

```json
{
  "email": "maria@example.com",
  "password": "ClaveSegura2024!"
}
```

- **Respuesta `200 OK` (`AuthenticationResponse`):**

| Campo | Tipo | Descripción |
|---|---|---|
| `accessToken` | `String` | JWT firmado HS256. |
| `tokenType` | `String` | Siempre `Bearer`. |
| `expiresAt` | `Instant` | Fecha de caducidad del token. |

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJtYXJpYUBleGFtcGxlLmNvbSIsInJvbGUiOiJPV05FUiIsIm93bmVySWQiOjF9.firma",
  "tokenType": "Bearer",
  "expiresAt": "2025-01-15T10:15:00Z"
}
```

- **Errores:** `400` (validación), `401` (credenciales incorrectas).

### 1.2 Solicitar registro de propietario

- **Método y URL:** `POST /api/auth/register`
- **Descripción:** registra una solicitud pública. No crea una cuenta activa ni devuelve token; el personal debe aprobarla.
- **Autorización:** pública.
- **Cuerpo (`OwnerRegistrationRequest`):**

| Campo | Tipo | Obligatorio | Validación |
|---|---|---|---|
| `fullName` | `String` | Sí | `@NotBlank`, máximo 160. |
| `phone` | `String` | Sí | `@NotBlank`, máximo 32. |
| `email` | `String` | Sí | `@NotBlank`, `@Email`, máximo 254. |
| `address` | `String` | Sí | `@NotBlank`, máximo 500. |
| `password` | `String` | Sí | `@NotBlank`, entre 12 y 128 caracteres. |

```json
{
  "fullName": "María López",
  "phone": "+34600111222",
  "email": "maria@example.com",
  "address": "Calle Mayor 10, Madrid",
  "password": "ClaveSegura2024!"
}
```

- **Respuesta `202 Accepted` (`RegistrationResponse`):**

| Campo | Tipo | Descripción |
|---|---|---|
| `requestId` | `Long` | Identificador de la solicitud. |
| `status` | `String` | Siempre `PENDING`. |
| `message` | `String` | Mensaje informativo. |

```json
{
  "requestId": 1,
  "status": "PENDING",
  "message": "Solicitud recibida; pendiente de verificación por el personal."
}
```

- **Errores:** `400` (validación).

---

## 2. Registro de propietarios (administración)

Base: `/api/staff/owner-registration-requests`. Todos los endpoints requieren rol `STAFF`.

### 2.1 Listar solicitudes pendientes

- **Método y URL:** `GET /api/staff/owner-registration-requests`
- **Descripción:** lista las solicitudes pendientes, ordenadas por fecha de creación ascendente.
- **Parámetros:** ninguno.
- **Respuesta `200 OK`:** array de `OwnerRegistrationPendingResponse`.

| Campo | Tipo |
|---|---|
| `requestId` | `Long` |
| `fullName` | `String` |
| `phone` | `String` |
| `email` | `String` |
| `address` | `String` |
| `createdAt` | `LocalDateTime` |

```json
[
  {
    "requestId": 1,
    "fullName": "María López",
    "phone": "+34600111222",
    "email": "maria@example.com",
    "address": "Calle Mayor 10, Madrid",
    "createdAt": "2025-01-14T09:30:00"
  }
]
```

- **Errores:** `401`, `403`.

### 2.2 Aprobar solicitud

- **Método y URL:** `POST /api/staff/owner-registration-requests/{requestId}/approve`
- **Descripción:** crea el propietario y su cuenta `OWNER` a partir de la solicitud y elimina la solicitud pendiente.
- **Parámetros de ruta:** `requestId` (`Long`), identificador de la solicitud.
- **Cuerpo:** no requiere.
- **Respuesta `200 OK` (`OwnerResponse`):**

```json
{
  "id": 1,
  "fullName": "María López",
  "phone": "+34600111222",
  "email": "maria@example.com",
  "address": "Calle Mayor 10, Madrid"
}
```

- **Errores:** `401`, `403`, `404` (solicitud inexistente), `409` (ya existe una cuenta con ese correo).

### 2.3 Rechazar solicitud

- **Método y URL:** `DELETE /api/staff/owner-registration-requests/{requestId}`
- **Descripción:** rechaza y elimina la solicitud pendiente.
- **Parámetros de ruta:** `requestId` (`Long`).
- **Respuesta `204 No Content`.**
- **Errores:** `401`, `403`, `404`.

---

## 3. Propietarios

Base: `/api/owners`.

### 3.1 Crear propietario

- **Método y URL:** `POST /api/owners`
- **Descripción:** crea un propietario directamente (sin cuenta de acceso).
- **Autorización:** `STAFF`.
- **Cuerpo (`OwnerRequest`):**

| Campo | Tipo | Obligatorio | Validación |
|---|---|---|---|
| `fullName` | `String` | Sí | `@NotBlank`, máximo 160. |
| `phone` | `String` | Sí | `@NotBlank`, máximo 32. |
| `email` | `String` | Sí | `@NotBlank`, `@Email`, máximo 254. |
| `address` | `String` | Sí | `@NotBlank`, máximo 500. |

```json
{
  "fullName": "Carlos Ruiz",
  "phone": "+34611222333",
  "email": "carlos@example.com",
  "address": "Avenida del Sol 5, Sevilla"
}
```

- **Respuesta `201 Created` (`OwnerResponse`):**

```json
{
  "id": 2,
  "fullName": "Carlos Ruiz",
  "phone": "+34611222333",
  "email": "carlos@example.com",
  "address": "Avenida del Sol 5, Sevilla"
}
```

- **Errores:** `400`, `401`, `403`.

### 3.2 Listar propietarios

- **Método y URL:** `GET /api/owners`
- **Autorización:** `STAFF`.
- **Respuesta `200 OK`:** array de `OwnerResponse`.
- **Errores:** `401`, `403`.

### 3.3 Consultar propietario

- **Método y URL:** `GET /api/owners/{id}`
- **Parámetros de ruta:** `id` (`Long`).
- **Autorización:** `STAFF` o el propietario `id` (según `ownerId` del token).
- **Respuesta `200 OK`:** `OwnerResponse`.
- **Errores:** `401`, `403` (propietario distinto), `404`.

### 3.4 Actualizar propietario

- **Método y URL:** `PUT /api/owners/{id}`
- **Parámetros de ruta:** `id` (`Long`).
- **Autorización:** `STAFF` o el propietario `id`.
- **Cuerpo:** `OwnerRequest` (mismas validaciones que 3.1).
- **Respuesta `200 OK`:** `OwnerResponse` actualizado.
- **Errores:** `400`, `401`, `403`, `404`.

### 3.5 Eliminar propietario

- **Método y URL:** `DELETE /api/owners/{id}`
- **Descripción:** elimina el propietario solo si no tiene mascotas.
- **Autorización:** `STAFF`.
- **Respuesta `204 No Content`.**
- **Errores:** `401`, `403`, `404`, `409` (tiene mascotas asociadas).

---

## 4. Mascotas

Base: `/api/pets`.

### 4.1 Registrar mascota

- **Método y URL:** `POST /api/pets`
- **Autorización:** `STAFF` o el propietario indicado en `ownerId`.
- **Cuerpo (`PetRequest`):**

| Campo | Tipo | Obligatorio | Validación |
|---|---|---|---|
| `name` | `String` | Sí | `@NotBlank`, máximo 120. |
| `species` | `String` | No | Máximo 80. |
| `breed` | `String` | No | Máximo 120. |
| `sex` | `Sex` | Sí | `@NotNull`. Valores: `MALE`, `FEMALE`, `UNKNOWN`. |
| `birthDate` | `LocalDate` | Sí | `@NotNull`, `@PastOrPresent`. |
| `weight` | `BigDecimal` | Sí | `@NotNull`, `@Positive`. |
| `observations` | `String` | No | Máximo 2000. |
| `ownerId` | `Long` | Sí | `@NotNull`, `@Positive`. Debe existir. |

```json
{
  "name": "Luna",
  "species": "Perro",
  "breed": "Mestizo",
  "sex": "FEMALE",
  "birthDate": "2020-05-17",
  "weight": 12.500,
  "observations": "Alergia leve al pollo",
  "ownerId": 1
}
```

- **Respuesta `201 Created` (`PetResponse`):**

| Campo | Tipo |
|---|---|
| `id` | `Long` |
| `name` | `String` |
| `species` | `String` |
| `breed` | `String` |
| `sex` | `Sex` |
| `birthDate` | `LocalDate` |
| `weight` | `BigDecimal` |
| `observations` | `String` |
| `ownerId` | `Long` |

```json
{
  "id": 1,
  "name": "Luna",
  "species": "Perro",
  "breed": "Mestizo",
  "sex": "FEMALE",
  "birthDate": "2020-05-17",
  "weight": 12.500,
  "observations": "Alergia leve al pollo",
  "ownerId": 1
}
```

- **Errores:** `400`, `401`, `403`, `404` (propietario inexistente).

### 4.2 Buscar mascotas

- **Método y URL:** `GET /api/pets`
- **Parámetros de consulta (todos opcionales):**

| Parámetro | Tipo | Validación | Descripción |
|---|---|---|---|
| `name` | `String` | — | Coincidencia parcial del nombre de la mascota (sin distinguir mayúsculas). |
| `ownerName` | `String` | — | Coincidencia parcial del nombre del propietario. |
| `ownerId` | `Long` | `@Positive` | Filtra por propietario. |
| `id` | `Long` | `@Positive` | Filtra por identificador de mascota. |

- **Autorización:** `STAFF` ve todas las mascotas. Un propietario solo ve las suyas; si filtra por otro `ownerId`, recibe `403`.
- **Respuesta `200 OK`:** array de `PetResponse`.
- **Errores:** `400`, `401`, `403`.

Ejemplo: `GET /api/pets?name=lun`

### 4.3 Consultar mascota

- **Método y URL:** `GET /api/pets/{id}`
- **Autorización:** `STAFF` o el propietario de la mascota.
- **Respuesta `200 OK`:** `PetResponse`.
- **Errores:** `401`, `403`, `404`.

### 4.4 Actualizar mascota

- **Método y URL:** `PUT /api/pets/{id}`
- **Autorización:** `STAFF` o el propietario actual, sin cambiar el `ownerId` a otro propietario.
- **Cuerpo:** `PetRequest` (mismas validaciones que 4.1).
- **Respuesta `200 OK`:** `PetResponse`.
- **Errores:** `400`, `401`, `403`, `404`.

### 4.5 Eliminar mascota

- **Método y URL:** `DELETE /api/pets/{id}`
- **Descripción:** elimina la mascota solo si no tiene historial médico.
- **Autorización:** la misma que la consulta de la mascota (`canAccessPet`).
- **Respuesta `204 No Content`.**
- **Errores:** `401`, `403`, `404`, `409` (tiene registros médicos).

---

## 5. Historial médico

### 5.1 Crear registro médico

- **Método y URL:** `POST /api/pets/{petId}/medical-records`
- **Parámetros de ruta:** `petId` (`Long`).
- **Autorización:** `STAFF`.
- **Cuerpo (`MedicalRecordRequest`):**

| Campo | Tipo | Obligatorio | Validación |
|---|---|---|---|
| `visitDate` | `LocalDate` | Sí | `@NotNull`, `@PastOrPresent`. |
| `consultation` | `String` | Sí | `@NotBlank`, máximo 2000. |
| `diagnosis` | `String` | No | Máximo 2000. |
| `treatment` | `String` | No | Máximo 2000. |
| `medication` | `String` | No | Máximo 2000. |
| `vaccines` | `String` | No | Máximo 2000. |
| `veterinaryObservations` | `String` | No | Máximo 4000. |

```json
{
  "visitDate": "2025-01-10",
  "consultation": "Control anual y revisión general.",
  "diagnosis": "Sin hallazgos relevantes",
  "treatment": "Ninguno",
  "medication": "Desparasitante oral",
  "vaccines": "Rabia (refuerzo)",
  "veterinaryObservations": "Peso estable."
}
```

- **Respuesta `201 Created` (`MedicalRecordResponse`):**

```json
{
  "id": 1,
  "petId": 1,
  "visitDate": "2025-01-10",
  "consultation": "Control anual y revisión general.",
  "diagnosis": "Sin hallazgos relevantes",
  "treatment": "Ninguno",
  "medication": "Desparasitante oral",
  "vaccines": "Rabia (refuerzo)",
  "veterinaryObservations": "Peso estable."
}
```

- **Errores:** `400`, `401`, `403`, `404` (mascota inexistente).

### 5.2 Listar historial de una mascota

- **Método y URL:** `GET /api/pets/{petId}/medical-records`
- **Autorización:** `STAFF` o el propietario de la mascota.
- **Respuesta `200 OK`:** array de `MedicalRecordResponse`.
- **Errores:** `401`, `403`, `404`.

### 5.3 Consultar registro médico

- **Método y URL:** `GET /api/medical-records/{id}`
- **Autorización:** `STAFF` o el propietario de la mascota del registro.
- **Respuesta `200 OK`:** `MedicalRecordResponse`.
- **Errores:** `401`, `403`, `404`.

### 5.4 Actualizar registro médico

- **Método y URL:** `PUT /api/medical-records/{id}`
- **Autorización:** `STAFF`.
- **Cuerpo:** `MedicalRecordRequest`.
- **Respuesta `200 OK`:** `MedicalRecordResponse`.
- **Errores:** `400`, `401`, `403`, `404`.

### 5.5 Eliminar registro médico

- **Método y URL:** `DELETE /api/medical-records/{id}`
- **Autorización:** `STAFF`.
- **Respuesta `204 No Content`.**
- **Errores:** `401`, `403`, `404`.

---

## 6. Veterinarios

Base: `/api/veterinarians`.

### 6.1 Crear veterinario

- **Método y URL:** `POST /api/veterinarians`
- **Autorización:** `STAFF`.
- **Cuerpo (`VeterinarianRequest`):**

| Campo | Tipo | Obligatorio | Validación |
|---|---|---|---|
| `fullName` | `String` | Sí | `@NotBlank`, máximo 160. |
| `email` | `String` | Sí | `@Email`, máximo 254. Debe ser único. |
| `specialty` | `String` | No | Máximo 120. |

```json
{
  "fullName": "Dra. Ana Gómez",
  "email": "ana.gomez@clinica.example",
  "specialty": "Cirugía"
}
```

- **Respuesta `201 Created` (`VeterinarianResponse`):**

```json
{
  "id": 1,
  "fullName": "Dra. Ana Gómez",
  "email": "ana.gomez@clinica.example",
  "specialty": "Cirugía"
}
```

- **Errores:** `400`, `401`, `403`, `409` (correo duplicado).

### 6.2 Listar veterinarios

- **Método y URL:** `GET /api/veterinarians`
- **Autorización:** cualquier usuario autenticado (sin restricción de rol explícita).
- **Respuesta `200 OK`:** array de `VeterinarianResponse`.
- **Errores:** `401`.

### 6.3 Consultar veterinario

- **Método y URL:** `GET /api/veterinarians/{id}`
- **Parámetros de ruta:** `id` (`Long`).
- **Autorización:** cualquier usuario autenticado.
- **Respuesta `200 OK`:** `VeterinarianResponse`.
- **Errores:** `401`, `404`.

---

## 7. Citas

Base: `/api`. Las fechas y horas siguen los formatos de [Convenciones generales](#formatos-de-fecha-y-hora).

### Cuerpo de creación (`CreateAppointmentRequest`)

| Campo | Tipo | Obligatorio | Validación |
|---|---|---|---|
| `petId` | `Long` | Sí | `@NotNull`, `@Positive`. |
| `veterinarianId` | `Long` | Sí | `@NotNull`, `@Positive`. |
| `date` | `LocalDate` | Sí | `@NotNull`. |
| `startTime` | `LocalTime` | Sí | `@NotNull`. |
| `endTime` | `LocalTime` | Sí | `@NotNull`. Debe ser posterior a `startTime`. |
| `reason` | `String` | Sí | `@NotBlank`, máximo 1000. |

### Cuerpo de actualización (`UpdateAppointmentRequest`)

Los mismos campos que `CreateAppointmentRequest`, más:

| Campo | Tipo | Obligatorio | Validación |
|---|---|---|---|
| `status` | `AppointmentStatus` | Sí | `@NotNull`. Valores: `PENDING`, `CONFIRMED`, `COMPLETED`, `CANCELLED`. |

### Respuesta (`AppointmentResponse`)

| Campo | Tipo |
|---|---|
| `id` | `Long` |
| `ownerId` | `Long` |
| `petId` | `Long` |
| `veterinarianId` | `Long` |
| `date` | `LocalDate` |
| `startTime` | `LocalTime` |
| `endTime` | `LocalTime` |
| `reason` | `String` |
| `status` | `AppointmentStatus` |

### 7.1 Crear cita

- **Método y URL:** `POST /api/appointments`
- **Descripción:** crea una cita en estado `PENDING` tras validar horario y disponibilidad del veterinario.
- **Autorización:** `STAFF`, o el propietario de la mascota indicada.
- **Cuerpo:** `CreateAppointmentRequest`.

```json
{
  "petId": 1,
  "veterinarianId": 1,
  "date": "2030-03-10",
  "startTime": "10:00:00",
  "endTime": "10:30:00",
  "reason": "Revisión anual"
}
```

- **Respuesta `201 Created`:**

```json
{
  "id": 10,
  "ownerId": 1,
  "petId": 1,
  "veterinarianId": 1,
  "date": "2030-03-10",
  "startTime": "10:00:00",
  "endTime": "10:30:00",
  "reason": "Revisión anual",
  "status": "PENDING"
}
```

- **Errores:** `400` (validación, fecha pasada, `endTime` no posterior a `startTime`), `401`, `403`, `404` (mascota o veterinario inexistente), `409` (solapamiento con otra cita del veterinario).

### 7.2 Consultar cita

- **Método y URL:** `GET /api/appointments/{id}`
- **Parámetros de ruta:** `id` (`Long`).
- **Autorización:** `STAFF` o el propietario de la cita.
- **Respuesta `200 OK`:** `AppointmentResponse`.
- **Errores:** `401`, `403`, `404`.

### 7.3 Reprogramar o actualizar cita

- **Método y URL:** `PUT /api/appointments/{id}`
- **Autorización:**
  - `STAFF`: cualquier cambio, incluido el estado.
  - Propietario: puede cambiar fecha, horario, motivo y mascota dentro de sus datos, conservando el estado actual o cancelando (`CANCELLED`). No puede confirmar ni completar.
- **Cuerpo:** `UpdateAppointmentRequest`.

```json
{
  "petId": 1,
  "veterinarianId": 2,
  "date": "2030-03-11",
  "startTime": "09:00:00",
  "endTime": "09:30:00",
  "reason": "Revisión anual (reprogramada)",
  "status": "CONFIRMED"
}
```

- **Respuesta `200 OK`:** `AppointmentResponse` actualizado.
- **Errores:** `400`, `401`, `403`, `404`, `409` (solapamiento o cambio concurrente).

### 7.4 Cambiar estado de cita

- **Método y URL:** `PATCH /api/appointments/{id}/status`
- **Cuerpo (`AppointmentStatusRequest`):**

| Campo | Tipo | Obligatorio | Validación |
|---|---|---|---|
| `status` | `AppointmentStatus` | Sí | `@NotNull`. |

- **Autorización:** `STAFF` cambia a cualquier estado. El propietario solo puede enviar `CANCELLED`.

```json
{
  "status": "CONFIRMED"
}
```

- **Respuesta `200 OK`:** `AppointmentResponse`.
- **Errores:** `400`, `401`, `403`, `404`, `409`.

### 7.5 Agenda diaria de un veterinario

- **Método y URL:** `GET /api/veterinarians/{veterinarianId}/appointments/day?date=YYYY-MM-DD`
- **Parámetros de ruta:** `veterinarianId` (`Long`).
- **Parámetros de consulta:** `date` (`LocalDate`, obligatorio).
- **Autorización:** `STAFF`.
- **Respuesta `200 OK`:** array de `AppointmentResponse`.
- **Errores:** `400`, `401`, `403`, `404`.

Ejemplo: `GET /api/veterinarians/1/appointments/day?date=2030-03-10`

### 7.6 Agenda semanal de un veterinario

- **Método y URL:** `GET /api/veterinarians/{veterinarianId}/appointments/week?date=YYYY-MM-DD`
- **Parámetros de consulta:** `date` (`LocalDate`, obligatorio); se devuelve la semana que contiene esa fecha.
- **Autorización:** `STAFF`.
- **Respuesta `200 OK`:** array de `AppointmentResponse`.
- **Errores:** `400`, `401`, `403`, `404`.

### 7.7 Agenda mensual de un veterinario

- **Método y URL:** `GET /api/veterinarians/{veterinarianId}/appointments/month?year=YYYY&month=MM`
- **Parámetros de consulta:**

| Parámetro | Tipo | Validación |
|---|---|---|
| `year` | `int` | Entre 1 y 9999. |
| `month` | `int` | Entre 1 y 12. |

- **Autorización:** `STAFF`.
- **Respuesta `200 OK`:** array de `AppointmentResponse`.
- **Errores:** `400`, `401`, `403`, `404`.

Ejemplo: `GET /api/veterinarians/1/appointments/month?year=2030&month=3`

---

## 8. Notificaciones

### 8.1 Historial de notificaciones de una cita

- **Método y URL:** `GET /api/appointments/{appointmentId}/notifications`
- **Descripción:** devuelve los intentos de recordatorio registrados para la cita.
- **Parámetros de ruta:** `appointmentId` (`Long`).
- **Autorización:** `STAFF` o el propietario de la cita.
- **Respuesta `200 OK`:** array de `NotificationHistoryResponse`.

| Campo | Tipo | Descripción |
|---|---|---|
| `id` | `Long` | Identificador del registro. |
| `appointmentId` | `Long` | Cita asociada. |
| `reminderMinutes` | `Long` | Anticipación del recordatorio, en minutos. |
| `recipientEmail` | `String` | Destinatario. |
| `petName` | `String` | Nombre de la mascota. |
| `veterinarianName` | `String` | Nombre del veterinario. |
| `appointmentStart` | `LocalDateTime` | Inicio de la cita. |
| `status` | `NotificationDeliveryStatus` | `PROCESSING`, `SENT` o `FAILED`. |
| `createdAt` | `LocalDateTime` | Creación del registro. |
| `sentAt` | `LocalDateTime` | Envío (nulo si no se completó). |
| `errorMessage` | `String` | Error del proveedor (nulo si no hubo). |

```json
[
  {
    "id": 1,
    "appointmentId": 10,
    "reminderMinutes": 60,
    "recipientEmail": "maria@example.com",
    "petName": "Luna",
    "veterinarianName": "Dra. Ana Gómez",
    "appointmentStart": "2030-03-10T10:00:00",
    "status": "SENT",
    "createdAt": "2030-03-10T09:00:00",
    "sentAt": "2030-03-10T09:00:01",
    "errorMessage": null
  }
]
```

- **Errores:** `401`, `403`, `404` (cita inexistente).

**Nota:** el envío de correo es simulado en la implementación actual (`SimulatedEmailSender`); el estado `SENT` no implica entrega real.

---

## Roles, autorización y reglas de negocio

### Roles

| Rol | Origen | Acceso |
|---|---|---|
| `OWNER` | Cuenta creada al aprobar una solicitud de registro. | Solo sus propios propietario, mascotas, historial, citas y notificaciones. |
| `STAFF` | Cuenta creada al arrancar con `VETSYNC_STAFF_EMAIL` y `VETSYNC_STAFF_PASSWORD`. | Acceso completo a administración, agenda y registros médicos. |

### Claims del JWT

| Claim | Descripción |
|---|---|
| `sub` | Correo electrónico del usuario. |
| `role` | `OWNER` o `STAFF`. |
| `ownerId` | Presente solo en tokens de `OWNER`. Identifica el propietario y se usa para filtrar todas las consultas. |
| `iss` | `vetsync`. |

Los IDs enviados por el cliente (por ejemplo, `ownerId` en una mascota) se validan contra el token; un propietario no puede actuar sobre otro propietario.

### Reglas de negocio

- **Sin solapamientos:** dos citas del mismo veterinario no pueden solaparse en fecha y horario. Los intervalos `[inicio, fin)` adyacentes sí se permiten.
- **Citas canceladas:** una cita `CANCELLED` no bloquea horario.
- **Fechas futuras:** al crear o reprogramar, la fecha y hora de inicio deben ser posteriores al momento actual.
- **Horario válido:** `endTime` debe ser posterior a `startTime`.
- **Estados:** `PENDING`, `CONFIRMED`, `COMPLETED`, `CANCELLED`. Solo `STAFF` confirma o completa.
- **Concurrencia:** las reservas bloquean al veterinario con bloqueo pesimista de MySQL dentro de la transacción.
- **Integridad:** no se elimina un propietario con mascotas (`409`), ni una mascota con historial médico (`409`).
- **Mascota-propietario:** la cita obtiene el propietario a partir de la mascota.

### Seguridad

- Todas las rutas excepto `/api/auth/register` y `/api/auth/login` exigen JWT válido.
- Los tokens caducan a los 15 minutos; no hay revocación en servidor.
- Las contraseñas se almacenan con PBKDF2; los hashes nunca se devuelven.
- Use HTTPS fuera de entornos locales.
- El registro público no activa cuentas: requiere aprobación de personal.
- Los errores no muestran SQL ni trazas de pila.
