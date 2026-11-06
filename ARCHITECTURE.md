# Arquitectura backend de VetSync

## Alcance y plataforma

Este diseño corresponde al proyecto existente: Java 21, Spring Boot, Spring Data JPA y MySQL 8. Las migraciones de esquema se gestionan con Flyway y las pruebas con JUnit y Mockito.

## A. Arquitectura del sistema

### Decisión: monolito modular por capas

Para el alcance actual —tres módulos de negocio, una API y una base relacional común— se recomienda un **monolito modular**, no microservicios.

Esta opción mantiene las operaciones que deben ser atómicas en una transacción MySQL, evita la complejidad prematura de despliegues, autenticación entre servicios, mensajería distribuida y consistencia eventual, y deja los límites necesarios para una extracción futura. Cada módulo tiene dependencias e interfaces explícitas; no debe acceder a los detalles internos de otro módulo ni a sus tablas por medio de consultas ad hoc.

### Módulos y responsabilidades

1. **Registro**
   - Mantiene propietarios, mascotas e historiales médicos.
   - Valida la pertenencia de las mascotas a propietarios.
   - Conserva el historial médico ante solicitudes de eliminación o transferencia de propiedad.
   - Expone casos de uso mediante servicios; no contiene SQL en controladores.
2. **Agenda**
   - Mantiene los perfiles de veterinarios y las citas.
   - Valida las referencias, el rango temporal, el estado y la disponibilidad.
   - Coordina la serialización de reservas concurrentes dentro de transacciones.
   - Ofrece búsquedas de agenda por veterinario y periodo.
3. **Notificaciones**
   - Detecta las citas que entran en la ventana configurable de recordatorio.
   - Construye los mensajes y coordina su entrega mediante la interfaz `EmailSender`.
   - Usa `SimulatedEmailSender` de manera predeterminada; no envía correo real.
   - Registra cada intento y resultado en `notification_history`.
4. **Identidad y autorización**
   - Mantiene cuentas de propietarios y personal en `user_accounts`, contraseñas PBKDF2 y JWT de corta duración.
   - Recibe solicitudes públicas pendientes en `owner_registration_requests`; el personal verifica identidad fuera de banda y aprueba antes de crear propietario/cuenta.
   - Aplica autorizaciones por rol y por propietario en los endpoints, independientemente de los IDs suministrados por el cliente.
   - Un propietario puede reprogramar su cita sin alterar su estado actual o cancelarla; solo personal puede confirmar o completar.
   - El despliegue debe aplicar límites de frecuencia compartidos y retención de solicitudes pendientes; la API aún no implementa esos controles operativos.

### Comunicación y dependencias

- Los clientes se comunican con la API mediante controladores REST y DTO validados.
- Un controlador delega en un servicio de aplicación; nunca accede directamente a un repositorio Spring Data ni contiene reglas de negocio.
- Los servicios dependen de interfaces de repositorio del dominio. Los adaptadores de infraestructura implementan esas interfaces usando JPA/MySQL.
- Agenda y registro cooperan mediante los identificadores y servicios/repositorios necesarios: la cita referencia una mascota registrada y el veterinario registrado. El propietario se obtiene de la mascota y debe coincidir con el de la cita.
- Notificaciones selecciona candidatos mediante el repositorio de agenda, vuelve a leer/bloquear la cita antes de procesarla y entrega el mensaje mediante `EmailSender`. El programador invoca el servicio de aplicación; no duplica reglas de elegibilidad.
- La persistencia compartida **no** autoriza a un módulo a modificar directamente las tablas de otro. Los cambios siguen las interfaces y las migraciones versionadas.

### SOLID y límites

- **S**: controladores, casos de uso, persistencia, validación HTTP y proveedores de notificación tienen responsabilidades distintas.
- **O**: se puede introducir otro proveedor de correo implementando `EmailSender`, sin reescribir los casos de uso.
- **L**: todo adaptador debe respetar el contrato de la interfaz que implementa y no cambiar su semántica.
- **I**: los puertos de persistencia y entrega son específicos de su caso de uso; no crear interfaces genéricas con métodos irrelevantes.
- **D**: las reglas de aplicación dependen de abstracciones; Spring configura e inyecta adaptadores concretos.

No añadir colas, microservicios, un servidor SMTP ni un framework de mensajería hasta que requisitos operativos reales los justifiquen.

## B. Diseño de base de datos MySQL

Las migraciones existentes definen las tablas indicadas en esta sección. Las nuevas modificaciones deben añadirse en una nueva migración Flyway; nunca editar una migración aplicada en un entorno compartido. Fechas y horas se interpretan con `VETSYNC_ZONE` (por defecto `UTC`); configurar las instancias y la base de datos de forma consistente.

| Tabla | Columnas principales | Claves y restricciones |
|---|---|---|
| `owners` | `id BIGINT`, `full_name VARCHAR(160)`, `phone VARCHAR(32)`, `email VARCHAR(254)`, `address VARCHAR(500)` | PK `id`; datos requeridos `NOT NULL`. |
| `pets` | `id BIGINT`, `owner_id BIGINT`, `name VARCHAR(120)`, `species VARCHAR(80)`, `breed VARCHAR(120)`, `sex VARCHAR(16)`, `birth_date DATE`, `weight DECIMAL(7,3)`, `observations VARCHAR(2000)` | PK `id`; FK `owner_id → owners.id ON DELETE RESTRICT`; índices por propietario y nombre; peso positivo y sexo del enum de dominio validados por la aplicación. |
| `medical_records` | `id BIGINT`, `pet_id BIGINT`, `visit_date DATE`, `consultation VARCHAR(2000)`, `diagnosis VARCHAR(2000)`, `treatment VARCHAR(2000)`, `medication VARCHAR(2000)`, `vaccines VARCHAR(2000)`, `veterinary_observations VARCHAR(4000)` | PK `id`; FK `pet_id → pets.id ON DELETE RESTRICT`; índice `(pet_id, visit_date)`. La consulta es requerida; los demás datos clínicos pueden ser nulos. |
| `veterinarians` | `id BIGINT`, `full_name VARCHAR(160)`, `email VARCHAR(254)`, `specialty VARCHAR(120)` | PK `id`; email `NOT NULL UNIQUE`. |
| `appointments` | `id BIGINT`, `owner_id BIGINT`, `pet_id BIGINT`, `veterinarian_id BIGINT`, `appointment_date DATE`, `start_time TIME`, `end_time TIME`, `reason VARCHAR(1000)`, `status VARCHAR(16)`, `version BIGINT` | PK `id`; FK al propietario, mascota y veterinario con `ON DELETE RESTRICT`; `CHECK (end_time > start_time)`; estado restringido a `PENDING`, `CONFIRMED`, `COMPLETED`, `CANCELLED`; índice `(veterinarian_id, appointment_date, start_time, end_time, status)`; versión para control optimista. |
| `notification_history` | `id BIGINT`, `appointment_id BIGINT`, `reminder_minutes BIGINT`, `recipient_email VARCHAR(254)`, `pet_name VARCHAR(120)`, `veterinarian_name VARCHAR(160)`, `appointment_start DATETIME(6)`, `status VARCHAR(16)`, `created_at DATETIME(6)`, `updated_at DATETIME(6)`, `sent_at DATETIME(6) NULL`, `error_message VARCHAR(1000) NULL` | PK `id`; FK `appointment_id → appointments.id ON DELETE RESTRICT`; `UNIQUE (appointment_id, reminder_minutes)` como clave de idempotencia; anticipación positiva; estado `PROCESSING`, `SENT` o `FAILED`; índice de historial por fecha. |
| `user_accounts` | `id BIGINT`, `email VARCHAR(254)`, `password_hash VARCHAR(255)`, `role VARCHAR(16)`, `owner_id BIGINT NULL` | PK `id`; email y `owner_id` únicos; FK al propietario con `ON DELETE CASCADE`; rol limitado a `OWNER` y `STAFF`; `OWNER` requiere propietario y `STAFF` lo deja nulo. |
| `owner_registration_requests` | `id BIGINT`, `full_name VARCHAR(160)`, `phone VARCHAR(32)`, `email VARCHAR(254)`, `address VARCHAR(500)`, `password_hash VARCHAR(255)`, `created_at DATETIME(6)` | PK `id`; índices por email y fecha; sin restricción `UNIQUE` sobre email mientras la solicitud no haya sido verificada. |

### Relaciones

- `owners 1:N pets`: cada mascota tiene un propietario; un propietario puede tener varias mascotas.
- `pets 1:N medical_records`: los registros clínicos pertenecen a una mascota.
- `pets 1:N appointments` y `veterinarians 1:N appointments`: cada cita referencia exactamente una mascota y un veterinario.
- `appointments 1:N notification_history`: cada cita puede tener registros para distintas anticipaciones; una misma anticipación solo puede registrarse una vez.
- `owners 1:0..1 user_accounts`: una cuenta OWNER solo se asocia a un propietario; las cuentas STAFF no se asocian a propietarios.
- Las solicitudes de registro permanecen separadas de las cuentas activas. La aprobación crea propietario y cuenta en una transacción; tanto la aprobación como el rechazo eliminan la solicitud y el hash pendiente.
- El propietario de una cita es el propietario de su mascota. El esquema actual también conserva `appointments.owner_id` para consultas e historial. **La integridad exige que esta clave coincida con `pets.owner_id`**: comprobar esa igualdad al crear y modificar citas y al cambiar la propiedad de una mascota. Si se endurece con una migración, agregar `UNIQUE (id, owner_id)` en `pets` y una FK compuesta `(pet_id, owner_id) → pets(id, owner_id)` en `appointments`; comprobar y corregir filas incoherentes antes de aplicar la restricción. No asumir que dos FK independientes garantizan esa igualdad.
- No hace falta ninguna relación M:N ni 1:1 para los requisitos actuales. No agregar tablas puente o relaciones artificiales sin un caso de negocio verificable.
- `notification_history` conserva una instantánea del destinatario y del texto identificador relevante, para que el registro no cambie si posteriormente se edita el perfil.

### Disponibilidad e integridad temporal

Para intervalos semiabiertos `[inicio, fin)`, dos citas se solapan si `inicioExistente < finSolicitado AND finExistente > inicioSolicitado`, con el mismo veterinario y fecha. No contar las citas `CANCELLED`; permitir que una cita termine exactamente cuando comienza la siguiente.

La creación, reprogramación y cambio de estado que vuelva a ocupar un horario deben obtener un bloqueo pesimista de la fila del veterinario **antes** de comprobar la disponibilidad, en la misma transacción `READ_COMMITTED` que persiste el cambio. Así todas las instancias de la API coordinan las escrituras mediante MySQL; una consulta previa desde el cliente nunca reemplaza esa comprobación. Las actualizaciones de una cita también conservan `@Version`. Las operaciones con dos veterinarios deben bloquearlos por ID ascendente antes de consultar/persistir para reducir interbloqueos.

Una restricción `CHECK` no puede asegurar por sí sola la ausencia de intervalos solapados entre filas; no declarar que el índice resuelve esa regla. Los bloqueos de aplicación, las transacciones y la verificación en el servidor son obligatorios.

## C. Estructura de directorios

Organización alineada con la estructura Java actual. Las clases de agenda/registro comparten las capas existentes; `notifications` conserva su propio submódulo porque es una capacidad independiente.

```text
src/
  main/
    java/com/vetsync/
      api/
        controller/                  # REST de registro, agenda y veterinarios
        dto/                         # Solicitudes/respuestas y límites de API
        exception/                   # Traducción centralizada a HTTP
      application/
        service/                     # Casos de uso de registro y agenda
        exception/                   # Errores de aplicación
      domain/
        model/                       # Owner, Pet, MedicalRecord, Veterinarian, Appointment
        repository/                  # Contratos de persistencia
      infrastructure/
        config/                      # Reloj, propiedades e inicialización
        persistence/                 # Spring Data, adaptadores y entidades de persistencia
      notifications/
        api/                         # Consulta del historial de notificaciones
        application/                 # Entrega, procesamiento y configuración del recordatorio
        domain/                      # EmailSender, mensaje y contratos/historial
        infrastructure/              # Scheduler, JPA y proveedor simulado
    resources/
      application.yml                # Valores no secretos y referencias a variables de entorno
      db/migration/                  # Migraciones Flyway inmutables y versionadas
  test/
    java/com/vetsync/
      application/service/           # Pruebas unitarias de registro y agenda
      notifications/application/     # Pruebas de procesamiento e idempotencia
      notifications/infrastructure/  # Pruebas del programador/adaptadores cuando apliquen
```

Los puertos de repositorio y envío actúan como interfaces. La validación de entrada HTTP se ubica en DTOs y Bean Validation; invariantes y reglas de dominio se vuelven a verificar en los servicios. Solo añadir middleware/filtros cuando exista una preocupación transversal concreta, como autenticación; no duplicar el manejador REST de excepciones.

## D. Justificación técnica

- **Escalabilidad:** escalar la API como una sola unidad conserva transacciones y unicidad de agenda en la base relacional. La consulta de recordatorios e índices están limitados por fecha. Extraer notificaciones a otro proceso puede ser un paso futuro si hay carga o necesidades de disponibilidad separadas; entonces se deberá definir entrega de eventos, reintentos e idempotencia distribuida.
- **Mantenibilidad:** capas con responsabilidades identificables, contratos de repositorio y migraciones incrementales evitan que las reglas se dispersen en SQL, HTTP o tareas programadas.
- **Seguridad:** validar entradas del servidor; usar parámetros de consulta; configurar secretos exclusivamente mediante entorno/secret manager; no registrar credenciales ni datos clínicos o PII innecesarios. Usar JWT firmado de corta duración, hash PBKDF2, HTTPS, aprobación de identidad fuera de banda y autorización tanto por rol como por propietario.
- **Reutilización:** `AppointmentService`, los servicios de registro y `EmailSender` son puntos explícitos para reutilizar casos de uso sin acoplarlos a los controladores ni al proveedor.
- **Testabilidad:** inyección por constructor, `Clock` controlable, puertos sustituibles y pruebas de servicio con mocks; complementar reglas MySQL con pruebas de integración cuando se valide SQL, restricciones, migraciones y bloqueos reales.
- **SOLID:** las dependencias apuntan a contratos de dominio; JPA, REST, el programador y el proveedor simulado quedan en los bordes reemplazables.
- **Sin redundancia innecesaria:** no modelar relaciones M:N inexistentes ni datos de propietario dentro de la mascota. Mantener el propietario explícito en la cita solo con la invariancia adicional descrita en el apartado de base de datos.

## E. TDD y estrategia de pruebas

Aplicar **Rojo → Verde → Refactorizar** en cada cambio: escribir una prueba de comportamiento que falle; implementar el cambio mínimo; repetir las pruebas focalizadas; refactorizar sin alterar los resultados y ejecutar la suite relevante.

| Módulo | Primero especificar con pruebas |
|---|---|
| Registro | DTO inválido, creación y consulta, mascota vinculada al propietario indicado, historial asociado a la mascota, recurso inexistente, edición y conflictos de borrado sin pérdida del historial. |
| Agenda | Horario futuro válido, fin anterior o igual al inicio, intervalos adyacentes, superposición parcial/completa, cita cancelada que libera el horario, reprogramación y transición de estado, mascota/veterinario inexistente. Probar concurrencia y restricciones con MySQL; Mockito no prueba bloqueos reales. |
| Notificaciones | Cita dentro/fuera de la ventana, cita cancelada, reprogramación, datos del propietario/mascota/veterinario en el mensaje, proveedor fallido, historial y un solo reclamo por cita/anticipación. |

Las pruebas unitarias no deben acceder a una base de datos ni a servicios de correo reales. Las de integración usan una base MySQL aislada y cubren Flyway, FK, índice único, transacciones y bloqueos; nunca dependen de datos personales reales.
