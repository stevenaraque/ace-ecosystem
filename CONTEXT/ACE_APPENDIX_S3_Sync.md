# Apéndice 3 — Sistema 3: Sincronización Offline-First (Especificación Conceptual)

> **Estado:** Aprobado  
> **Versión:** 1.0  
> **Fecha:** 2026-06-02  
> **Depende de:** Documento base A.C.E v0.2 (Sistema 3) · Apéndice 2 (Sistema 2)

---

## 1. Propósito del Apéndice

Este documento descompone el Sistema 3 en sus cinco subsistemas conceptuales, definiendo **qué hace cada uno, qué entradas recibe, qué salidas produce y qué invariantes debe respetar**. No contiene código fuente; su función es servir como contrato de comportamiento antes de la implementación.

Los cinco subsistemas son:

1. **Subsistema de Cola Local** (gestión de bloques pendientes en SQLite).
2. **Subsistema de Ensamblaje de Batch** (agrupación de bloques para envío eficiente).
3. **Subsistema de Transporte** (envío HTTP al backend y manejo de respuesta).
4. **Subsistema de Reintentos y Backoff** (política de reenvío ante fallos de red).
5. **Subsistema de Resolución de Respuesta** (actualización de estado local tras confirmación o rechazo del backend).

---

## 2. Subsistema de Cola Local

### 2.1. Principio rector
El móvil es la fuente de verdad **temporal** de todos los bloques generados. Hasta que el backend no confirme la recepción, el bloque existe solo en la base de datos local de la APK. La sincronización no es inmediata; es **eventual**.

### 2.2. Entrada
El Sistema 2 materializa bloques en `local_blocks` con estado `PENDING`. El Sistema 3 consume exclusivamente de esta cola.

### 2.3. Estados de un bloque en la cola
| Estado | Significado | Quién lo asigna |
|--------|-------------|-----------------|
| **PENDING** | Bloque listo para sincronizar. Tiene XP calculada. No está en vuelo. | Sistema 2, al cerrar el bloque. |
| **SYNCING** | Bloque seleccionado para un batch que está siendo enviado. | Sistema 3, al armar el batch. |
| **SYNCED** | Backend confirmó recepción con `201`. | Sistema 3, al recibir respuesta exitosa. |
| **ERROR** | Backend rechazó el bloque (`422`) o fallaron todos los reintentos de red. | Sistema 3, tras agotar backoff o recibir rechazo. |

### 2.4. Reglas de la cola
- Solo los bloques en estado `PENDING` son elegibles para formar un batch.
- Un bloque en estado `SYNCING` no puede ser seleccionado para otro batch simultáneo. Esto evita duplicados en vuelo.
- El orden de la cola es **FIFO por `timestamp_start`**: el bloque más antiguo se envía primero.
- No se descartan bloques automáticamente. Un bloque en `ERROR` permanece en la cola hasta que el usuario tome acción manual o se implemente un mecanismo de diagnóstico.

### 2.5. Qué NO hace la cola
- No almacena el contenido crudo de las muestras de FC (eso se perdió al cerrar el bloque en el Sistema 2).
- No reordena bloques por prioridad.
- No sincroniza sin un token de autenticación válido (ver Sistema 4).

---

## 3. Subsistema de Ensamblaje de Batch

### 3.1. Principio rector
Para reducir overhead de red y evitar timeouts en el backend (alojado en Render con PostgreSQL en Neon/Supabase), los bloques se envían en **batches** de tamaño limitado.

### 3.2. Tamaño del batch
- **Máximo 20 bloques** por batch.
- Si hay menos de 20 bloques `PENDING`, se envía el lote disponible (puede ser 1 bloque).
- El batch incluye únicamente bloques consecutivos en orden temporal. No se salta bloques.

### 3.3. Contenido del batch
Cada elemento del batch es una representación estructurada de un bloque que incluye:

- `block_id`: UUID generado por el móvil (idempotencia).
- `session_id`: UUID de la sesión padre.
- `timestamp_start`, `timestamp_end`, `duration_seconds`.
- `avg_bpm`, `max_bpm`, `min_bpm`, `sample_count`.
- `sport_type`.
- `xp_calculated`: la recompensa que el móvil computó localmente (Sistema 5).

### 3.4. Invariantes
- Un batch **nunca** se arma con bloques que no tengan `xp_calculated` definido.
- Un batch **nunca** incluye bloques en estado `SYNCING` o `SYNCED`.
- El batch se serializa en el cuerpo de una petición HTTP POST.

---

## 4. Subsistema de Transporte

### 4.1. Interfaz de salida
El móvil realiza una petición HTTP POST autenticada al endpoint del backend encargado de recibir batches de ejercicio.

### 4.2. Autenticación
Cada petición lleva el token de acceso JWT en el header `Authorization: Bearer <access_token>`. Si el token expiró, el Sistema 4 (Autenticación JWT Híbrida) gestiona la renovación antes de que esta petición viaje.

### 4.3. Payload
El cuerpo de la petición contiene:
- Un array de bloques (máximo 20).
- Cada bloque incluye sus métricas agregadas y su `xp_calculated`.
- El móvil no envía muestras individuales de FC.

### 4.4. Respuestas esperadas
| Código HTTP | Significado | Acción del móvil |
|-------------|-------------|------------------|
| **201 Created** | Batch aceptado. Todos los bloques fueron persistidos. | Marcar bloques como `SYNCED`. Actualizar `server_session_id` si es la primera vez que el backend ve esta sesión. |
| **401 Unauthorized** | Token de acceso inválido o expirado. | Delegar al Sistema 4 para refresh. Reintentar la petición original tras renovación exitosa. |
| **422 Unprocessable Entity** | Al menos un bloque del batch falló validación (XP inconsistente, duración fuera de rango, etc.). | El backend indica qué bloques fallaron. El móvil los marca como `ERROR`. Los bloques válidos se marcan como `SYNCED`. |
| **429 Too Many Requests** | Rate limit del backend. | Aplicar backoff exponencial y reintentar más tarde. |
| **5xx** | Error del servidor. | Aplicar backoff exponencial y reintentar. |

### 4.5. Idempotencia
El backend utiliza `ON CONFLICT (block_id) DO NOTHING` al insertar bloques. Si el móvil reenvía un batch porque no recibió la respuesta (timeout de red), el backend ignora los duplicados sin error. El móvil, al recibir el `201`, marca todos los bloques como `SYNCED` de todas formas.

---

## 5. Subsistema de Reintentos y Backoff

### 5.1. Principio rector
Si un batch falla por problemas de red o error del servidor, el móvil debe reintentar sin intervención del usuario, pero sin saturar la red ni la batería del dispositivo.

### 5.2. Agente de reintentos
Los reintentos son gestionados por **WorkManager**, no por hilos en primer plano. Esto garantiza que:
- Los reintentos sobreviven al cierre de la app.
- Los reintentos respetan las restricciones de batería de Android (Doze, App Standby).
- Los reintentos se ejecutan cuando hay conectividad de red disponible.

### 5.3. Política de backoff
| Intento | Delay aproximado | Condición |
|---------|------------------|-----------|
| 1 | 15 minutos | Fallo inicial. |
| 2 | 30 minutos | Fallo del primer reintento. |
| 3 | 1 hora | Fallo del segundo reintento. |
| 4 | 2 horas | Fallo del tercer reintento. |
| 5 | 4 horas | Fallo del cuarto reintento. |
| 6+ | **No reintenta.** Marca bloques como `ERROR`. | Agotados los 5 reintentos. |

### 5.4. Reglas de reintento
- Solo se reintenta si el error fue de red (timeout, 5xx, 429) o de autenticación transitoria (401 resuelto por refresh).
- **No se reintenta** si el backend respondió `422` (rechazo de sanidad). Eso es un error permanente.
- Si un batch parcial falla (algunos bloques `422`, otros `201`), solo los bloques en error se marcan como `ERROR`; los aceptados se marcan como `SYNCED`. El WorkManager reintentará solo con los bloques `PENDING` restantes.

### 5.5. Qué NO hace el reintento
- No notifica al usuario en cada fallo. Solo notifica tras el quinto reintento (ver Sistema 8).
- No reintenta en bucle infinito.
- No envía batches de un solo bloque si hay muchos pendientes; siempre intenta agrupar hasta 20.

---

## 6. Subsistema de Resolución de Respuesta

### 6.1. Responsabilidad
Traducir la respuesta del backend en actualizaciones concretas del estado local de cada bloque, y propagar la información derivada (ranking, racha, estadísticas) a otros sistemas.

### 6.2. Respuesta 201 — Éxito total
Para cada bloque del batch:
- Estado local pasa a `SYNCED`.
- Si el backend asignó un `server_session_id` (porque era la primera vez que veía esa sesión), se guarda en `local_sessions`.
- El backend puede devolver en la respuesta:
  - `xp_accepted`: confirmación de que la XP fue aceptada.
  - `new_total_xp`: total acumulado del usuario.
  - `rank_changed`: booleano indicando si el usuario subió de rango.
  - `current_streak`: valor actualizado de la racha.
- El Sistema 3 propaga estos valores al **Sistema 6 (Ranking)**, **Sistema 7 (Racha)** y **Sistema 10 (Estadísticas de Perfil)** para actualizar sus caches locales.

### 6.3. Respuesta 422 — Rechazo de sanidad
Si el backend detecta que la `xp_calculated` de un bloque es inconsistente con sus métricas (ej. 999 XP para 5 minutos a 80 bpm):
- El bloque se marca como `ERROR`.
- La XP localmente ganada se revierte en el **Sistema 10 (Estadísticas de Perfil)**.
- El usuario ve una corrección en la UI (ej. "-10 XP, bloque rechazado").
- El bloque en `ERROR` no se vuelve a enviar automáticamente.

### 6.4. Respuesta de red fallida (timeout, 5xx, 429)
- Los bloques permanecen en `PENDING` (si nunca salieron) o vuelven a `PENDING` desde `SYNCING`.
- WorkManager programa el siguiente reintento según backoff.

### 6.5. Invariantes
- Un bloque nunca puede estar `SYNCED` y `ERROR` simultáneamente.
- El `server_session_id` solo se resuelve una vez por sesión (la primera vez que un bloque de esa sesión es aceptado).
- La suma de `xp_calculated` de bloques `SYNCED` en el móvil debe ser consistente con el `new_total_xp` reportado por el backend. Si hay discrepancia, prima el backend y el Sistema 10 ajusta.

---

## 7. Flujo Integrado de Datos

```
┌─────────────────────────────────────────────────────────────────────┐
│                         SQLite (APK local)                           │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │  local_blocks (status = PENDING)                              │  │
│  │  block_id | session_id | metrics | xp_calculated | timestamp   │  │
│  └──────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────┘
                              │
                              │ Sistema 3: selecciona hasta 20
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    Subsistema de Ensamblaje de Batch                  │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │  Array de bloques ordenados por timestamp_start (FIFO)        │  │
│  │  Cada bloque: {block_id, session_id, metrics, xp_calculated}  │  │
│  └──────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────┘
                              │
                              │ HTTP POST + JWT
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│                         BACKEND (Render)                           │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────────────┐  │
│  │ Auth (JWT)   │───►│ Validar XP   │───►│ Persistir bloques   │  │
│  │              │    │ (sanidad)    │    │ (ON CONFLICT IGNORE)│  │
│  └──────────────┘    └──────────────┘    └──────────────────────┘  │
│                              │                    │                 │
│                              ▼                    ▼                 │
│                        ┌──────────────┐    ┌──────────────┐        │
│                        │ Rechazar 422 │    │ Insertar XP  │        │
│                        │ (explicar)   │    │ transactions │        │
│                        └──────────────┘    └──────────────┘        │
└─────────────────────────────────────────────────────────────────────┘
                              │
                              │ Respuesta HTTP (201 / 422 / 5xx)
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    Subsistema de Resolución de Respuesta              │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │  201 → Marcar SYNCED, actualizar ranking, racha, stats      │  │
│  │  422 → Marcar ERROR, revertir XP local, notificar usuario   │  │
│  │  5xx → Volver a PENDING, WorkManager programa reintento     │  │
│  └──────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 8. Decisiones Arquitectónicas Consolidadas (Sistema 3)

| Decisión | Valor | Justificación |
|----------|-------|---------------|
| **Fuente de verdad temporal** | SQLite local del móvil | El usuario puede entrenar sin internet. Los datos existen localmente hasta el sync. |
| **Tamaño de batch** | Máximo 20 bloques | Balance entre eficiencia de red y timeout de Neon/Render. |
| **Orden de envío** | FIFO por `timestamp_start` | Mantiene la secuencia temporal. Evita que el backend reciba bloques de una sesión antes de saber que la sesión existe. |
| **Agente de reintentos** | WorkManager | Sobrevive cierre de app, Doze, y reinicios del teléfono. |
| **Política de backoff** | Exponencial: 15min → 30min → 1h → 2h → 4h | No agota la batería ni satura el backend. |
| **Límite de reintentos** | 5 intentos | Después de 5 fallos, se considera un problema persistente (red o backend caído). Requiere intervención o diagnóstico. |
| **Idempotencia** | `block_id` generado por móvil + `ON CONFLICT DO NOTHING` en backend | Reenvío seguro sin duplicar datos. |
| **No envío sin XP** | Bloque debe tener `xp_calculated` | El backend no recalcula XP desde cero; solo valida. |
| **No envío sin auth** | AuthInterceptor bloquea sync si no hay JWT válido | Protege contra envíos anónimos. |
| **Resolución de sesión** | `server_session_id` resuelto en primer batch aceptado | El móvil no conoce el ID del servidor hasta que este confirma la sesión. |

---

## 9. Glosario de Términos (Sistema 3)

| Término | Definición |
|---------|------------|
| **Batch** | Conjunto de hasta 20 bloques que se envían en una sola petición HTTP. |
| **Cola** | Conjunto de bloques en estado `PENDING` en SQLite, esperando ser sincronizados. |
| **SYNCING** | Estado transitorio de un bloque que forma parte de un batch en vuelo hacia el backend. |
| **SYNCED** | Estado definitivo de un bloque que el backend confirmó con `201`. |
| **ERROR** | Estado de un bloque que fue rechazado por el backend (`422`) o agotó todos los reintentos de red. |
| **Backoff** | Política de espera progresiva entre reintentos de sync para no saturar recursos. |
| **WorkManager** | API de Android para tareas diferidas y garantizadas que sobreviven al ciclo de vida de la app. |
| **Idempotencia** | Propiedad de una operación que produce el mismo resultado si se ejecuta una o varias veces. |
| **server_session_id** | Identificador que el backend asigna a una sesión la primera vez que recibe un bloque de ella. |

---

## 10. Interacciones con otros sistemas

| Sistema | Interacción | Dirección |
|---------|-------------|-----------|
| **Sistema 2 — Sesión** | El Sistema 2 produce bloques `PENDING` que el Sistema 3 consume. | Sistema 2 → Sistema 3 |
| **Sistema 4 — Auth** | El Sistema 3 delega al Sistema 4 cuando recibe `401` (token expirado). | Sistema 3 → Sistema 4 |
| **Sistema 5 — XP** | El Sistema 3 envía `xp_calculated` al backend. Si es rechazada, notifica al Sistema 5 para corrección visual. | Sistema 3 ↔ Sistema 5 |
| **Sistema 6 — Ranking** | Tras un `201`, el backend puede indicar `rank_changed`. El Sistema 3 propaga esto al cache de ranking. | Sistema 3 → Sistema 6 |
| **Sistema 7 — Racha** | Tras un `201`, el backend devuelve `current_streak`. El Sistema 3 actualiza el cache local de racha. | Sistema 3 → Sistema 7 |
| **Sistema 8 — Notificaciones** | Si hay bloques en `ERROR` o tras 5 reintentos fallidos, el Sistema 3 dispara notificación local. | Sistema 3 → Sistema 8 |
| **Sistema 10 — Estadísticas** | Tras `201`, actualiza totales locales. Tras `422`, revierte XP local. | Sistema 3 ↔ Sistema 10 |
