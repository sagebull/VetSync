# Instrucciones obligatorias para agentes — VetSync

Estas reglas aplican a cualquier agente que lea, modifique, pruebe o documente el código de VetSync. Son requisitos, no sugerencias. En caso de conflicto, preservar primero la integridad clínica y de agenda, los datos existentes y los contratos públicos; solicitar autorización antes de eliminar comportamiento o realizar una migración destructiva.

## 1. Plataforma y arquitectura

- Mantener el stack del proyecto: **Java 21, Spring Boot, Spring Data JPA, MySQL 8 y Flyway**. No cambiar a Node.js o Python ni crear microservicios sin aprobación explícita.
- Mantener un monolito modular, organizado en `api`, `application`, `domain`, `infrastructure` y el submódulo `notifications`.
- Mantener las reglas de negocio en servicios/dominio; controladores gestionan HTTP y DTOs; adaptadores encapsulan JPA/MySQL.
- Inyectar dependencias por constructor. Las reglas de aplicación dependen de interfaces de repositorio y de proveedores definidos en el dominio, no de clases concretas de infraestructura.
- No ejecutar SQL ni invocar directamente Spring Data desde un controlador. No devolver entidades JPA directamente como respuestas REST.
- No introducir microservicios, broker, dependencias, frameworks o abstracciones adicionales sin una necesidad comprobable y aprobación del responsable del proyecto.

## 2. Convenciones de código

- Usar `camelCase` para variables, parámetros y métodos; usar `PascalCase` para clases, interfaces, enums y records; usar `UPPER_SNAKE_CASE` para constantes; usar paquetes Java en minúsculas.
- Usar tipos explícitos y genéricos. **Prohibido** usar tipos raw, `Object` como sustituto de un tipo conocido, casts inseguros o una abstracción que suprima la seguridad de tipos.
- Preferir tipos inmutables y DTOs con tipos apropiados. Representar fechas con `LocalDate`, horas con `LocalTime` y timestamps con `LocalDateTime`/`Clock` en la zona configurada; no depender implícitamente de la zona local de la máquina.
- Limitar comentarios y Javadoc a contratos públicos o decisiones no evidentes. Documentar configuración, semántica de API y restricciones operativas cuando ayude a evitar un error de integración.
- Seguir el formato, los paquetes, convenciones de nombres y patrones ya usados. No realizar refactorizaciones no relacionadas en la misma modificación.

## 3. SOLID, responsabilidades y errores

- Aplicar responsabilidad única, abierto/cerrado, sustitución de Liskov, segregación de interfaces e inversión de dependencias.
- Definir interfaces específicas al introducir un puerto externo o de persistencia. Toda implementación alternativa debe respetar completamente ese contrato.
- Validar datos de entrada con Bean Validation en DTOs y comprobar las invariantes del negocio en la capa de aplicación. La validación del cliente no sustituye la del servidor.
- Traducir errores conocidos a excepciones controladas y a respuestas HTTP coherentes con `ApiExceptionHandler`.
- Usar `try/catch` únicamente cuando el código pueda tomar una acción válida: traducir el error, registrar contexto y devolver un resultado explícito o reintentar conforme a una política documentada.
- **Nunca** dejar bloques `catch` vacíos, ignorar excepciones, devolver éxito ante un fallo ni ocultar la causa original. No capturar `Exception` o `RuntimeException` de forma general si no se puede manejar correctamente. Permitir el rollback de Spring cuando corresponda.
- No concatenar SQL con datos del usuario; usar parámetros. No incluir datos sensibles, credenciales, correos completos ni información clínica en logs salvo que sea imprescindible y esté adecuadamente protegida.

## 4. Invariantes de VetSync

### Autenticación y autorización

- Toda ruta API privada requiere JWT válido; solo solicitud de registro e inicio de sesión son públicos. No agregar rutas públicas para consultar, modificar o eliminar datos.
- No crear una cuenta de propietario activa ni emitir un JWT a partir de un correo autodeclarado. Guardar solicitudes públicas separadas, con contraseña hasheada y sin unicidad que permita reservar el email. Solo personal autenticado puede activar una cuenta tras verificar identidad fuera de banda.
- Los propietarios solo pueden consultar sus propios datos, mascotas, citas y notificaciones. Filtrar búsquedas por el `ownerId` confiable del token; rechazar IDs suministrados por el cliente que intenten elevar o cambiar el alcance.
- Solo `STAFF` consulta calendarios completos, administra propietarios/veterinarios y crea, modifica o elimina registros médicos. Los propietarios pueden reprogramar sus citas conservando su estado actual y cancelarlas; solo `STAFF` puede confirmar o completar citas. Todas las operaciones de agenda siguen además las reglas de concurrencia de citas.
- Aplicar autorización en cada ruta y a nivel del recurso asociado; autenticar no implica acceso global. Añadir pruebas que demuestren aislamiento entre dos propietarios y rechazo sin token.
- Usar el `PasswordEncoder` configurado (PBKDF2) para guardar/validar contraseñas y JWT firmados de corta duración con rol y propietario mínimos. No guardar tokens de texto, exponer hashes ni escribir credenciales en logs.
- Requerir `VETSYNC_JWT_SECRET` aleatorio Base64 de al menos 32 bytes, HTTPS fuera de entornos locales y protección en un gestor de secretos. Nunca usar un secreto de ejemplo, rotarlo en cada inicio ni incluirlo en código o reportes.
- Provisionar `STAFF` únicamente por proceso privado y controlado. Nunca añadir una ruta de alta pública de STAFF ni cambiar credenciales existentes silenciosamente.

### Propietarios, mascotas e historial clínico

- Una mascota debe referenciar un propietario existente; un propietario puede tener varias mascotas.
- Un historial médico debe referenciar una mascota existente. No borrar propietarios o mascotas dejando referencias clínicas huérfanas.
- Conservar `ON DELETE RESTRICT` y el comportamiento que protege los historiales. No implementar cascadas destructivas de historial sin autorización y una política de retención aprobada.
- La mascota, el propietario de la cita y cualquier instantánea relacionada deben referirse a la misma relación real. Si la cita guarda `owner_id` y `pet_id`, comprobar que el propietario corresponde a la mascota tanto en la aplicación como en el esquema o en una migración segura.

### Veterinarios, citas y concurrencia

- Cada cita debe referenciar una mascota y un veterinario existentes y reflejar al propietario de esa mascota.
- Los estados permitidos de la cita son `PENDING`, `CONFIRMED`, `COMPLETED` y `CANCELLED`. Las citas canceladas no reservan horario.
- Validar en el servidor fecha futura para crear o reprogramar, `endTime > startTime`, transiciones de estado y superposición de intervalos. Dos intervalos `[inicio, fin)` se solapan si `inicioExistente < finSolicitado && finExistente > inicioSolicitado`; permitir intervalos adyacentes.
- Nunca confiar únicamente en una consulta previa, un bloqueo del cliente o una validación de UI para determinar disponibilidad.
- Cada operación que reserve o reactive un horario debe tomar, dentro de la transacción, el bloqueo pesimista MySQL del veterinario antes de consultar disponibilidad; mantener la consulta y escritura bajo el mismo aislamiento `READ_COMMITTED`.
- Mantener control optimista de versión de la cita y restricciones de base de datos. Un índice de agenda mejora consultas, pero **no** garantiza por sí solo la ausencia de solapamientos.
- Si una operación bloquea más de un veterinario, tomar todos los bloqueos en orden ascendente de ID antes de validar disponibilidad, para reducir interbloqueos. Mantener las lecturas/escrituras compatibles con otros flujos de modificación.
- Verificar pruebas concurrentes con MySQL; mocks unitarios no demuestran el comportamiento de locks transaccionales.

### Recordatorios y envío

- El proveedor de correo debe implementarse detrás de `EmailSender`. La implementación predeterminada es simulada y **no** puede enviar mensajes reales.
- Procesar solo citas elegibles que sigan pendientes o confirmadas en el momento de enviar; volver a consultar y bloquear la cita y revalidar el horario tras detectar candidatos.
- No enviar recordatorios de citas canceladas ni que hayan sido reprogramadas fuera de la ventana configurada.
- Mantener idempotencia persistente única por cita y anticipación en MySQL; un check en memoria no es suficiente entre instancias o reinicios.
- Registrar resultado y error de forma explícita. No borrar el historial ni reintentar automáticamente fallos salvo que exista una política aprobada que también preserve idempotencia.
- Mantener la configuración validada para anticipación, intervalo y zona horaria. No crear secretos ni destinatarios reales en configuración de ejemplo.

## 5. Persistencia y migraciones

- Todas las modificaciones de esquema van en una migración Flyway nueva, incremental y ordenada. **Nunca editar ni renumerar una migración ya compartida o aplicada.**
- Diseñar PK, FK, índices, `NOT NULL`, `UNIQUE`, `CHECK` y `ON DELETE` según las invariantes del dominio. La lógica SQL de persistencia reside en infraestructura, jamás en controladores.
- Examinar las filas y dependencias existentes antes de agregar restricciones o transformar columnas. Si una migración necesita corregir o descartar datos, detenerse y pedir autorización.
- Mantener Hibernate en modo `validate`; no activar `ddl-auto=create`, `create-drop` o `update` en configuración de la aplicación.
- No guardar contraseñas en texto plano ni agregar credenciales, tokens, claves API o datos de producción a código, migraciones, fixtures o logs.
- Configurar secretos mediante variables de entorno o un gestor de secretos. Validar propiedades obligatorias al inicio; no registrar su valor.

## 6. TDD y pruebas

- Para cada comportamiento nuevo o corregido, seguir **Rojo → Verde → Refactorizar**: prueba que falle por la conducta pedida, implementación mínima, refactorización respaldada por pruebas.
- Escribir pruebas unitarias para servicios, invariantes, validaciones y casos límite. Cubrir al menos éxito, inexistentes, duplicados, conflictos, límites temporales y errores del proveedor cuando apliquen.
- Escribir pruebas de integración cuando la garantía dependa de MySQL, Flyway, claves foráneas, restricciones, transacciones, bloqueo o consultas Spring Data.
- Para citas, incluir solapamiento parcial/completo, intervalos adyacentes, cancelación, reactivación, cambios de veterinario, solicitudes concurrentes y tiempos límite.
- Para notificaciones, incluir ventana, cancelación, reprogramación, duplicado, ejecución concurrente, destinatario y contenido, historial y falla del proveedor.
- No modificar ni eliminar pruebas para ocultar fallos. Cambiar una prueba preexistente solo si el requisito cambió legítimamente; explicar el motivo y conservar cobertura equivalente.
- Usar `Clock` inyectado o un reloj fijo en pruebas temporales. No depender de `LocalDateTime.now()` real cuando se necesite una prueba determinista.
- Ejecutar primero pruebas focalizadas y después `mvn test` para verificar el conjunto. Si no pueden ejecutarse, declarar exactamente la causa; nunca afirmar que pasan si solo compilaron.

## 7. Seguridad y privacidad

- No almacenar ni publicar información sensible. No registrar en texto los mensajes completos, el cuerpo de correos, credenciales ni datos clínicos.
- Validar formato, longitud, rango y nulabilidad en el límite HTTP y las reglas semánticas en servicios. No confiar en datos del cliente ni en identificadores no comprobados.
- No agregar endpoints públicos que expongan datos personales o de salud sin autorización y autenticación aprobadas. La existencia de validación de entrada no sustituye el control de acceso.
- Mantener DTOs de respuesta con el mínimo de información necesaria y errores explícitos que no filtren consultas SQL, stack traces o secretos.

## 8. Cambios y compatibilidad

- Modificar exclusivamente archivos necesarios para la tarea. Preservar funcionalidad, API, datos y pruebas existentes que no estén dentro del alcance.
- No eliminar rutas, estados, tablas, campos, migraciones, registros médicos, historial de notificaciones ni reglas de agenda sin autorización explícita.
- Mantener contratos HTTP y semántica de excepciones. Si un cambio incompatible es imprescindible, documentarlo, migrarlo de forma segura y obtener autorización antes de publicarlo.
- No cambiar automáticamente entre las convenciones por capas existentes y una organización por funcionalidades; mantener consistente la estructura actual y pedir aprobación para una reorganización amplia.
- No afirmar garantías que el código o las pruebas no demuestren. Describir los límites de las pruebas, especialmente para concurrencia, entrega de correo y datos existentes.

## 9. Flujo obligatorio de trabajo

1. Leer las clases, contratos, migraciones, configuración y pruebas que afectan a la solicitud.
2. Identificar invariantes, compatibilidad, riesgos de datos y el alcance mínimo de los archivos.
3. Escribir primero las pruebas de comportamiento y límites.
4. Implementar únicamente el cambio necesario, con validación en servidor, interfaces y manejo explícito de errores.
5. Añadir una nueva migración solo si la estructura persistida necesita cambiar.
6. Ejecutar pruebas focalizadas y `mvn test`; ejecutar compilación, análisis estático o integración si el cambio los exige.
7. Actualizar `README.md` o `ARCHITECTURE.md` solo cuando cambie la configuración, un contrato, el esquema o una decisión operativa.
8. Informar los cambios, archivos relevantes, validaciones ejecutadas y limitaciones pendientes. Indicar claramente toda prueba que no pudo ejecutarse.

## 10. Criterio de aceptación

Una tarea no está completa hasta que:

- el comportamiento solicitado está implementado sin violar las invariantes de VetSync;
- las pruebas nuevas cubren el camino normal y los casos límite relevantes;
- las pruebas y la compilación alcanzables no presentan errores atribuibles al cambio;
- las migraciones son incrementales y preservan los datos existentes;
- no se han agregado credenciales, dependencias innecesarias, bloques `catch` vacíos, lógica duplicada ni modificaciones fuera del alcance.
