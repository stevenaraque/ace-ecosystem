# A.C.E — Arquitectura de Sistemas y Responsabilidades por Módulo

> **Estado:** En construcción / Conceptual / WIP  
> **Versión:** 0.3  
> **Fecha:** 2026-06-08  
> **Stack:** Wear OS 3+ · Android 13+ (Java 21) · Spring Boot 4.1.x · PostgreSQL (Neon/Supabase) · SQLite (APK local)  
> **Nota:** `:shared` v1.0.0 está **IMPLEMENTADO Y PUBLICADO** vía JitPack. Ver §16.

---

## 1. Visión General de la Arquitectura

A.C.E es un ecosistema de tres nodos con responsabilidades estrictamente separadas:

- **Wear OS:** Es un sensor con pantalla. Captura y transmite. No decide, no persiste, no calcula.
- **APK (Android):** Es el traductor, buffer, **calculador de XP** y orquestador offline. Recibe datos del reloj, los estructura, calcula recompensas localmente, las muestra inmediatamente al usuario, las guarda localmente y sincroniza con el backend cuando puede.
- **Backend (Spring Boot en Render):** Es la única fuente de verdad permanente. Valida, persiste, expone ranking, gestiona identidad, controla rangos y rachas. **No recalcula XP desde cero; valida la XP que le envía la APK.**
- **Base de Datos (PostgreSQL):** Es el ledger de estado. No contiene lógica de negocio, solo estado validado y transacciones inmutables.
- **:shared (Kotlin JVM):** Es el **contrato inmutable** entre backend y mobile. DTOs, enums, constantes y serialización. **Repo separado:** `reinaldojperalta/ace-shared`. **Publicado vía JitPack** como `com.github.reinaldojperalta:ace-shared:1.0.0`. **Estado: IMPLEMENTADO Y LISTO.**

**Principio rector:** *El reloj captura, el móvil calcula y transporta, el backend valida y decide, la base de datos recuerda, :shared garantiza que todos hablen el mismo idioma.*

---

## 2. Sistemas Fundamentales (Índice)

| # | Sistema | Descripción en una línea | Estado |
|---|---------|--------------------------|--------|
| 1 | **Captura de Sensor** | Del sensor de FC del reloj al buffer del móvil vía Bluetooth. | Conceptual |
| 2 | **Sesión de Ejercicio** | Ciclo de vida de una actividad: inicio, pausas, bloques, cierre. | Conceptual |
| 3 | **Sincronización Offline-First** | Cómo la APK guarda, encola y envía datos (crudos + XP calculada) al backend. | Conceptual |
| 4 | **Autenticación JWT Híbrida** | Tokens de corta y larga duración, rotación, revocación y race conditions. | Conceptual |
| 5 | **Cálculo de XP y Gamificación** | La APK calcula XP localmente usando fórmulas del backend. Recompensa inmediata offline. | Conceptual |
| 6 | **Ranking y Posicionamiento** | Clasificación global y municipal, recálculo periódico, cache en APK. | Conceptual |
| 7 | **Racha (Streaks)** | Hábito diario: detección, incremento, rotura y recordatorios. Controlado por backend. | Conceptual |
| 8 | **Notificaciones y Recordatorios** | WorkManager, foreground service y engagement del usuario. | Conceptual |
| 9 | **Historial de Sesiones** | Las últimas 5 sesiones guardadas localmente en la APK, sin discriminar categoría. | Conceptual |
| 10 | **Estadísticas Persistentes de Perfil** | Datos procesados (con XP) del usuario, sincronizados y cacheados en la APK. | Conceptual |
| **—** | **:shared (Contrato)** | DTOs, enums, constants, serialización. **IMPLEMENTADO v1.0.0.** | ✅ **LISTO** |

---

## 3. Sistema 1 — Captura de Sensor

### Propósito
Transmitir frecuencia cardíaca (FC) cruda del reloj al móvil con la menor latencia y consumo de batería posible.

### Flujo de datos
```
Sensor FC (1 Hz) → Health Services API → DataClient → Bluetooth/Wear OS Data Layer → APK Buffer Circular
```

### Responsabilidades por módulo

| Módulo | Responsabilidad | Qué NO hace |
|--------|----------------|-------------|
| **Wear OS** | Activar sensor a 1 Hz. Enviar `{"bpm": int, "timestamp": long}` por DataClient en path `/ace/health/heart_rate`. Escuchar comandos `START`/`STOP` del móvil. | No agrupa en bloques. No sabe de sesiones. No calcula promedios. No tiene lógica de negocio. |
| **APK** | Recibir muestras por DataClient. Almacenarlas en un buffer circular en memoria (RAM, no disco). Detectar desconexión (chip amarillo en UI). | No persiste muestras individuales en SQLite. No envía FC cruda al backend. |
| **Backend** | — | No habla directamente con Wear OS. |
| **DB** | — | No almacena muestras de FC individuales. |
| **:shared** | Define `DataLayerPaths.HEART_RATE` = `/ace/health/heart_rate`. Formato de primitivos JSON. **Disponible vía JitPack.** | No ejecuta código. Es contrato. |

### Contratos clave
- **Path DataClient:** `/ace/health/heart_rate` (FC en vivo), `/ace/session/{id}/status` (comando STOP).
- **Formato:** Primitivos JSON. Epoch millis para timestamps. NO segundos.
- **Buffer del SO:** Si el móvil no está al alcance, Wear OS DataClient bufferiza internamente (~100-200 items o 24h estimado).
- **:shared:** Define paths, constantes y formato de serialización. Consumido vía JitPack (`com.github.reinaldojperalta:ace-shared:1.0.0`).

---

## 4. Sistema 2 — Sesión de Ejercicio

### Propósito
Agrupar el esfuerzo del usuario en una unidad lógica con inicio, desarrollo y fin, permitiendo pausas y cambios de contexto.

### Flujo de datos
```
Usuario toca INICIAR → APK crea local_session (ACTIVE) 
→ Wear OS comienza a transmitir FC 
→ Cada 300s la APK arma un local_block 
→ Usuario toca DETENER → APK cierra sesión y bloque final 
→ APK calcula XP del bloque/sesión localmente 
→ Todo queda en SQLite esperando sync
```

### Responsabilidades por módulo

| Módulo | Responsabilidad | Qué NO hace |
|--------|----------------|-------------|
| **Wear OS** | Mostrar FC en vivo, timer, botón DETENER. Enviar señal STOP al móvil. | No sabe qué sesión pertenece a qué usuario. No inicia sesiones por sí solo. |
| **APK** | Crear `local_session` con UUID propio. Mantener estado `ACTIVE/PAUSED/COMPLETED`. Acumular FC en buffer de 300s. Generar `block_id` (UUIDv4) por cada bloque. Asociar bloques a sesión. **Calcular XP del bloque localmente** usando fórmulas cacheadas. Mostrar recompensa inmediata al usuario. | No valida si la sesión es legítima (eso es backend). No envía bloques inmediatamente (espera batch). |
| **Backend** | Aceptar o rechazar sesiones. **Solo 1 sesión ACTIVE por usuario.** Si llega una nueva, aborta la anterior. Validar que bloques pertenezcan a sesión existente. | No inicia sesiones remotamente (solo puede enviar STOP). No recalcula XP. |
| **DB** | Guardar `exercise_sessions` y `exercise_blocks`. Relación 1:N. | No calcula duraciones ni valida métricas. |
| **:shared** | Define `ExerciseBlockDto`, `SessionStatus` enum, `XpConstants.BLOCK_DURATION_SECONDS`. **Disponible vía JitPack.** | No ejecuta código. Es contrato. |

### Decisiones tomadas
- `block_id` lo genera el **móvil**, no el backend. Esto permite reenvío idempotente.
- `timestamp_start` del bloque es la fuente de verdad temporal (no `server_received_at`).
- Un bloque debe durar ~300 segundos (±10%). Fuera de ese rango = rechazo `422`.
- **:shared** define el schema del bloque (`ExerciseBlockDto`) que viaja en el batch. Backend y mobile usan la misma clase vía JitPack.

---

## 5. Sistema 3 — Sincronización Offline-First

### Propósito
Garantizar que el usuario pueda entrenar sin internet, ver su XP ganada inmediatamente, y que ningún dato se pierda cuando la conexión regresa.

### Flujo de datos
```
Wear OS → APK (SQLite: local_blocks.status = PENDING, XP ya calculada) 
→ ¿Hay internet? 
  NO: WorkManager reintenta cada 15 min. El usuario ya vio su XP localmente. 
  SÍ: AuthInterceptor valida token → Envía batch (máx 20 bloques + XP calculada) → Backend 
→ Backend valida sanidad de XP (¿es consistente con métricas?) 
→ Backend responde: xp_accepted, rank_changed, server_session_id 
→ APK actualiza local_blocks.status = SYNCED + cached_ranking
```

### Responsabilidades por módulo

| Módulo | Responsabilidad | Qué NO hace |
|--------|----------------|-------------|
| **Wear OS** | — | No bufferiza en disco. No sabe de internet. |
| **APK** | Guardar bloques en SQLite (`local_blocks`) con **XP ya calculada**. Gestionar estados `PENDING/SYNCING/SYNCED/ERROR`. Armar batches de máximo 20 bloques. Cada bloque en el batch incluye `xp_calculated`. Reintentos con backoff exponencial (WorkManager). Resolver `server_session_id` cuando el backend confirma creación de sesión. Cachear ranking. | No descarta bloques fallidos automáticamente. No sincroniza sin JWT válido. No envía datos sin XP calculada. |
| **Backend** | Recibir batch. Validar JWT. Validar sanidad de XP recibida (anti-trampa básico: ¿es razonable esta XP para estos datos?). Insertar bloques con `ON CONFLICT (id) DO NOTHING` (idempotencia). Insertar `xp_transaction` con el valor recibido del móvil. Responder con confirmación y ranking actualizado. | **No recalcula XP desde cero.** Si la XP del móvil no pasa validación, rechaza el bloque con `422`. No pide bloques al móvil. |
| **DB** | Almacenar bloques definitivos con XP recibida. `xp_transactions`: append-only. Índice en `(user_id, timestamp_start)` para historial. | No sabe qué bloques tiene el móvil en local. No recalcula XP. |
| **:shared** | Define `SyncBatchRequestDto`, `BlockStatus` enum, `SyncConstants.BATCH_MAX_SIZE = 20`. **Disponible vía JitPack.** | No ejecuta código. Es contrato. |

### Estados de un bloque en la APK
- `PENDING` → `SYNCING` → `SYNCED`
- `PENDING` → `SYNCING` → `ERROR` (después de 5 reintentos o rechazo de XP por backend)

---

## 6. Sistema 4 — Autenticación JWT Híbrida

### Propósito
Mantener al usuario autenticado durante días sin pedirle contraseña, con capacidad de revocación inmediata si hay robo de dispositivo.

### Flujo de datos
```
Login → Backend emite access_token (15 min) + refresh_token (7 días) 
→ APK guarda ambos en SQLite (local_user) 
→ Cada request lleva access_token 
→ Access expirado → 401 → AuthInterceptor dispara refresh 
→ Backend rota refresh (revoca anterior, crea nuevo par) 
→ APK guarda nuevos tokens y reintenta request original
```

### Responsabilidades por módulo

| Módulo | Responsabilidad | Qué NO hace |
|--------|----------------|-------------|
| **Wear OS** | — | No se autentica. No tiene tokens. Hereda identidad del móvil. |
| **APK** | Almacenar tokens en SQLite. Añadir `Authorization: Bearer` en cada request. Detectar `401`. Serializar refreshes (evitar race condition). Reintentar requests encoladas tras refresh exitoso. Forzar logout si refresh es rechazado. Generar `device_id` único por instalación. | No refresca en paralelo. No almacena contraseñas. |
| **Backend** | Validar credenciales (BCrypt). Generar JWT firmados. Validar access_token en cada request. Recibir refresh_token, verificar en DB (no revocado, no expirado), rotar par completo (nuevo access + nuevo refresh). Marcar anterior como revocado. | No mantiene sesiones de servidor (stateless). No envía tokens por Bluetooth. |
| **DB** | Tabla `refresh_tokens`: token_hash, expires_at, revoked_at, replaced_by. Transacción atómica para rotación (`SELECT FOR UPDATE`). | No valida JWT (eso es lógica del backend). |
| **:shared** | Define `AuthRequestDto`, `AuthResponseDto`, `RefreshTokenRequestDto`, `AuthConstants`. **Disponible vía JitPack.** | No ejecuta código. Es contrato. |

### Race Condition — Control
**Escenario:** Dos requests paralelas con access token expirado.

**Solución:**
1. **Backend:** La rotación de refresh token es una transacción atómica. El primer refresh revoca el token; el segundo que intente usarlo recibe `401 REFRESH_REUSED`.
2. **APK:** El `AuthInterceptor` tiene un flag `isRefreshing`. Si una segunda request detecta `401` mientras otra está refrescando, se encola y espera el resultado. Solo 1 refresh llega al backend.
3. **Fallback:** Si el refresh devuelve `REFRESH_REUSED`, la APK limpia tokens y fuerza re-login (posible robo de token).

---

## 7. Sistema 5 — Cálculo de XP y Gamificación

### Propósito
Transformar esfuerzo físico (bloques de FC) en progresión numérica y hitos visuales, con **recompensa inmediata visible incluso sin internet**.

### Flujo de datos
```
APK cierra un bloque de 5 min 
→ Lee fórmulas cacheadas (descargadas del backend en login) 
→ Strategy según sport_type: Running = 2 XP/min si avg_bpm > 80 
→ Calcula XP del bloque (ej. 5 min × 2 = 10 XP) 
→ Muestra al usuario inmediatamente: "+10 XP" 
→ Guarda en local_blocks.xp_calculated 
→ Al sync, envía bloque + XP al backend 
→ Backend valida sanidad (¿10 XP es razonable para 5 min a 145 bpm?) 
→ Si pasa: persiste en xp_transactions 
→ Evalúa si sube de rango (rank_catalog) 
→ Responde al móvil: {xp_accepted, new_total, rank_changed}
```

### Responsabilidades por módulo

| Módulo | Responsabilidad | Qué NO hace |
|--------|----------------|-------------|
| **Wear OS** | — | No calcula nada. No muestra XP. |
| **APK** | **Calcular XP localmente** usando fórmulas descargadas del backend. Aplicar Strategy según deporte. Mostrar recompensa inmediata al usuario (offline). Enviar `xp_calculated` en el batch de sync. Cachear fórmulas y actualizarlas cuando el backend las modifique. | No confía ciegamente en su cálculo si el backend rechaza (muestra corrección). No envía bloques sin XP calculada. |
| **Backend** | Exponer endpoint con **fórmulas actuales de XP** para que la APK las descargue/cachee. Al recibir batch, **validar sanidad de XP** (rangos razonables, consistencia con métricas). Si pasa validación: insertar `xp_transaction` con valor recibido. Evaluar cambio de rango. Si falla validación: rechazar bloque con `422` y explicar por qué. | **No recalcula XP desde cero.** No aplica fórmulas server-side al bloque recibido. |
| **DB** | `xp_transactions`: append-only, nunca UPDATE/DELETE. Almacena la XP que la APK reportó (si fue validada). `rank_catalog`: umbrales fijos. `user_ranks`: snapshot actual. | No aplica fórmulas. No recalcula XP. |
| **:shared** | Define `XpFormulaDto`, `XpAwardedResponseDto`, `XpConstants`. **Disponible vía JitPack.** | No ejecuta código. Es contrato. |

### Decisiones
- La APK es la **fuente de cálculo primaria**. El backend es **validador y auditor**.
- El backend expone `GET /api/xp/formulas` para que la APK descargue las reglas actuales (ej. `{"RUNNING": {"min_bpm": 80, "xp_per_min": 2}}`).
- Si un tramposo modifica la APK para reportar 999 XP, el backend detecta inconsistencia (999 XP para 5 min a 80 bpm es imposible) y rechaza el bloque.
- `balance_after` en cada transacción permite saber el total sin hacer `SUM()` en toda la tabla.
- Corrección de XP: si hay error, se inserta una transacción negativa, no se borra la original.

---

## 8. Sistema 6 — Ranking y Posicionamiento

### Propósito
Competencia social: el usuario ve dónde está respecto a otros (global y por ciudad).

### Flujo de datos
```
Cada hora (job programado): 
  Backend SUM(xp_transactions.amount) por usuario 
  → UPDATE ranking_global (position, total_xp) 
  → UPDATE ranking_municipal (position, total_xp, city_id) 

APK solicita ranking → Backend responde con posición + top N 
→ APK guarda en cached_ranking (válido por 1 hora)
```

### Responsabilidades por módulo

| Módulo | Responsabilidad | Qué NO hace |
|--------|----------------|-------------|
| **Wear OS** | — | No muestra ranking. |
| **APK** | Mostrar ranking desde `cached_ranking`. Solicitar actualización al backend si cache tiene > 1 hora. Mostrar "Cargando..." si no hay cache. | No calcula posiciones. No mantiene lista completa de usuarios. |
| **Backend** | Job `@Scheduled` recalcula posiciones cada hora. Endpoints `GET /ranking/global` y `/municipal/{cityId}`. Paginación (top 100). | No calcula ranking on-write (cada bloque). Es batch por performance. |
| **DB** | Tablas materializadas `ranking_global` y `ranking_municipal`. Índice en `(position)` para lecturas rápidas. | No recalcula sola (necesita el job). |
| **:shared** | Define `RankingResponseDto`, `RankingEntryDto`, `RankingConstants`. **Disponible vía JitPack.** | No ejecuta código. Es contrato. |

### Decisiones
- Ranking es **eventual**: puede tardar hasta 1 hora en reflejar un bloque recién sincronizado.
- La APK cachea solo **tu posición + top 10**. No descarga el ranking completo.
- Si un usuario cambia de ciudad, pierde su posición municipal anterior y empieza de cero en la nueva (snapshot).

---

## 9. Sistema 7 — Racha (Streaks)

### Propósito
Crear hábito diario mediante recompensa psicológica de continuidad.

### Flujo de datos
```
Backend recibe bloque válido (con XP aceptada) 
→ Compara timestamp_start con last_exercise_date del usuario 
→ Si es mismo día: ignora. 
→ Si es día siguiente: current_streak += 1. 
→ Si hay hueco > 1 día: current_streak = 1 (se rompe). 
→ Actualiza best_streak si aplica.

APK (8:00 PM): WorkManager revisa last_exercise_date 
→ Si no es hoy: notificación local "No has entrenado hoy 🔥"
```

### Responsabilidades por módulo

| Módulo | Responsabilidad | Qué NO hace |
|--------|----------------|-------------|
| **Wear OS** | — | No sabe de rachas. |
| **APK** | Recibir `last_exercise_date` del backend en cada sync. Cachearlo localmente. WorkManager `CheckStreakWorker` a las 8 PM. Notificación local si no entrenó. | No decide si la racha sube o se rompe. No calcula streaks localmente. |
| **Backend** | Evaluar racha al procesar cada bloque. Usar `timestamp_start` del bloque (no `server_received_at`). Actualizar `user_streaks`. Incluir `last_exercise_date` en respuestas de sync. | No envía push notifications (usa notificación local del móvil). |
| **DB** | `user_streaks`: current_streak, best_streak, last_exercise_date. | No evalúa lógica de fechas. |
| **:shared** | Define `StreakStateDto`, `StreakConstants`. **Disponible vía JitPack.** | No ejecuta código. Es contrato. |

### Decisiones
- La racha se evalúa en el backend, no en el móvil. Si el móvil envía bloques atrasados, no afectan la racha actual.
- Un bloque válido = racha +1. No se necesita una sesión completa, solo 1 bloque.
- La notificación es **local** (WorkManager). No requiere FCM ni backend activo a las 8 PM.

---

## 10. Sistema 8 — Notificaciones y Recordatorios

### Propósito
Mantener al usuario informado de su estado de sync y motivarlo a entrenar sin ser intrusivo.

### Flujo de datos
```
Durante sesión activa: 
  ExerciseSyncService (Foreground) → Notificación persistente 
  "A.C.E sincronizando... FC: 145 bpm"

A las 8:00 PM: 
  WorkManager → CheckStreakWorker → Lee local_user.last_exercise_date 
  → Si no es hoy: Notificación local "No has entrenado hoy 🔥"

Cuando sync falla 5 veces: 
  → Notificación local "Tienes X bloques sin sincronizar"
```

### Responsabilidades por módulo

| Módulo | Responsabilidad | Qué NO hace |
|--------|----------------|-------------|
| **Wear OS** | Vibrar en logros futuros (máximo). | No envía notificaciones al móvil. |
| **APK** | `ExerciseSyncService` (Foreground `health`): notificación persistente durante sesión. `WorkManager`: recordatorio 8 PM, reintentos de sync. Notificación de error si hay bloques en estado ERROR. | No envía notificaciones push desde servidor. Todo es local o background. |
| **Backend** | — | No envía FCM/push. No programa notificaciones. |
| **DB** | — | No almacena preferencias de notificación. |
| **:shared** | Define `NotificationChannelId` enum con IDs de canales. **Disponible vía JitPack.** | No ejecuta código. Es contrato. |

### Decisiones
- Foreground Service tipo `health` es obligatorio en Android 10+ para recibir datos del reloj en background.
- WorkManager sobrevive reinicios del teléfono. Si el usuario reinicia a las 7 PM, la notificación de las 8 PM sigue programada.
- Sin FCM en MVP. Todo es local. El backend no "empuja" nada al móvil.

---

## 11. Sistema 9 — Historial de Sesiones

### Propósito
Mostrar al usuario un resumen rápido de sus últimas actividades, sin importar el deporte, disponible **offline**.

### Flujo de datos
```
APK cierra una sesión 
→ Guarda datos sin procesar (raw) en local_session_history 
→ Mantiene máximo 5 sesiones (FIFO: la más antigua se descarta) 
→ No discrimina categoría (Running, Cycling, etc. se muestran juntas) 
→ UI de perfil/historial lee directamente de SQLite
```

### Responsabilidades por módulo

| Módulo | Responsabilidad | Qué NO hace |
|--------|----------------|-------------|
| **Wear OS** | — | No guarda historial. |
| **APK** | Guardar las últimas **5 sesiones completadas** en `local_session_history`. Datos sin procesar: fecha, duración, sport_type, FC promedio, bloques generados. No discrimina categoría en la lista. Mostrar lista en UI de perfil. | No calcula XP aquí (eso va a Estadísticas). No envía historial al backend (el backend ya tiene los bloques). |
| **Backend** | — | No expone endpoint de "últimas 5 sesiones". El historial completo está en `exercise_sessions` si se necesita. |
| **DB** | `exercise_sessions` almacena todas las sesiones permanentemente. | No mantiene límite de 5. |
| **:shared** | Define `HistoryConstants.MAX_LOCAL_SESSIONS = 5`. **Disponible vía JitPack.** | No ejecuta código. Es contrato. |

### Decisiones
- El historial local es **solo vista rápida**. Si el usuario quiere ver todo su historial, se consulta al backend (paginado).
- Las 5 sesiones son **sin discriminar categoría**: se muestran cronológicamente, mezclando Running, Cycling, etc.
- Los datos son **sin procesar** (raw): duración, FC promedio, tipo. No incluyen XP calculada ni ranking.
- Si la APK se reinstala, este historial local se pierde. El backend puede reconstruirlo si se solicita.

---

## 12. Sistema 10 — Estadísticas Persistentes de Perfil

### Propósito
Mantener un resumen agregado del progreso del usuario (total XP, sesiones completadas, promedios), visible offline y sincronizado con el backend.

### Flujo de datos
```
APK calcula XP de un bloque 
→ Actualiza estadísticas locales inmediatamente: 
   total_xp += xp_calculated 
   total_sessions += 1 (si es nueva sesión) 
   avg_bpm_all_time = recalcular 
→ Al sync, envía estadísticas actuales al backend 
→ Backend valida consistencia (sanidad) y persiste en user_stats 
→ Backend responde con estadísticas oficiales (por si hay corrección) 
→ APK actualiza cache local
```

### Responsabilidades por módulo

| Módulo | Responsabilidad | Qué NO hace |
|--------|----------------|-------------|
| **Wear OS** | — | No tiene estadísticas. |
| **APK** | Calcular y mantener **estadísticas procesadas** (con XP) en SQLite: `total_xp`, `total_sessions`, `avg_bpm`, `total_duration`, etc. Mostrar en UI de perfil inmediatamente. Sincronizar con backend en cada batch. Recibir correcciones del backend (si una XP fue rechazada, revertir estadísticas locales). | No es la fuente de verdad final (el backend sí). No mantiene estadísticas por categoría en MVP. |
| **Backend** | Recibir estadísticas del móvil. Validar consistencia (¿el total_xp reportado es coherente con los bloques recibidos?). Persistir en tabla `user_stats`. Exponer en endpoint de perfil. | No recalcula estadísticas desde cero a menos que haya auditoría. |
| **DB** | `user_stats`: total_xp, total_sessions, avg_bpm, updated_at. | No calcula agregaciones en tiempo real. |
| **:shared** | Define `ClientStatsDto`. **Disponible vía JitPack.** | No ejecuta código. Es contrato. |

### Decisiones
- Las estadísticas son **procesadas** (incluyen XP calculada). El historial (Sistema 9) es **raw** (sin XP).
- La APK muestra estadísticas locales inmediatamente. Si el backend corrige algo, la APK ajusta en el siguiente sync.
- En MVP, las estadísticas son **globales** (no por deporte). En fases futuras se pueden separar por `sport_type`.

---

## 13. Glosario de Entidades y Estados

### Entidades Core
- **Muestra de FC:** `{"bpm": int, "timestamp": long}` — dato crudo del reloj. No se persiste en backend.
- **Bloque (ExerciseBlock):** Unidad de 5 minutos con métricas agregadas y **XP calculada por la APK**. Tiene `block_id` UUID generado por el móvil.
- **Sesión (ExerciseSession):** Contenedor de bloques. 1 sesión = N bloques. Solo 1 sesión ACTIVE por usuario.
- **Transacción XP:** Registro inmutable de puntos otorgados (valor reportado por APK y validado por backend). `balance_after` permite consultar total sin SUM.
- **Rango (Rank):** Umbral de XP (Bronce, Plata, Oro...). Snapshot en `user_ranks`. Controlado por backend.
- **Racha (Streak):** Días consecutivos con al menos 1 bloque válido. Controlada por backend.
- **Historial Local:** Últimas 5 sesiones en SQLite de la APK, sin discriminar categoría, datos raw.
- **Estadísticas de Perfil:** Datos procesados (con XP) del usuario, sincronizados APK ↔ Backend.
- **:shared:** Módulo Kotlin JVM con DTOs, enums, constantes. **Repo separado** `reinaldojperalta/ace-shared`. **Publicado vía JitPack** como `com.github.reinaldojperalta:ace-shared:1.0.0`.

### Estados de Bloque (APK)
- `PENDING` → `SYNCING` → `SYNCED`
- `PENDING` → `SYNCING` → `ERROR` (después de 5 reintentos o rechazo de XP por backend)

### Estados de Sesión (Backend)
- `ACTIVE`: En curso. Solo 1 por usuario.
- `COMPLETED`: Cerrada normalmente.
- `ABORTED`: Cerrada por nueva sesión o error.

---

## 14. Decisiones Arquitectónicas Consolidadas

| Decisión | Valor | Justificación |
|----------|-------|---------------|
| `block_id` generado por | **Móvil** | Idempotencia de red. Reenvío sin duplicar. |
| Sesiones ACTIVE por usuario | **1** | Evita contaminación de XP y ranking. |
| Timestamp de bloque | **`timestamp_start`** | Bloques atrasados no rompen racha ni estadísticas. |
| Batch de sync | **Máximo 20 bloques** | Balance entre eficiencia de red y timeout de Neon/Render. |
| Race condition refresh | **Serialización APK + Transacción DB** | Un solo refresh llega al backend; el segundo token revocado fuerza logout. |
| Cálculo de XP | **APK calcula, backend valida** | Recompensa inmediata offline. El backend actúa como auditor, no como calculador primario. |
| Fórmulas de XP | **Backend expone, APK cachea** | La APK descarga `GET /api/xp/formulas` y aplica localmente. |
| Ranking | **Recálculo batch cada hora** | Performance. El ranking no necesita ser exacto al segundo. |
| Racha | **Backend controla** | Fuente de verdad única. El móvil solo cachea y notifica. |
| Notificaciones | **WorkManager local** | Sin FCM en MVP. Sobrevive reinicios y no depende de backend online. |
| Historial local | **5 sesiones, sin discriminar categoría** | Vista rápida offline. Datos raw. |
| Estadísticas de perfil | **APK calcula, backend valida** | Progreso visible inmediatamente. Sincronización eventual. |
| Wear OS | **NO `:shared`, NO Room** | Reduce APK del reloj. Primitivos JSON por DataClient. |
| **:shared** | **Repo separado, JitPack** | `reinaldojperalta/ace-shared`. Compilación automática por tag. Coordenada: `com.github.reinaldojperalta:ace-shared`. **v1.0.0 IMPLEMENTADO.** | Contrato inmutable entre backend y mobile. Sin fricción de JAR manual ni monorepo mixto. |

---

## 15. Mapa de Interacciones entre Módulos

```
┌─────────────┐     Bluetooth/DataClient      ┌─────────────┐
│   WEAR OS   │ ─────────────────────────────►│     APK     │
│  (sensor)   │  {"bpm": 145, "ts": 123456}   │  (buffer +  │
│             │◄──────────────────────────────│   SQLite)   │
│             │         START / STOP            │             │
└─────────────┘                               └──────┬──────┘
                                                     │
                              REST (batch + XP calc)  │
                              HTTPS                  ▼
                     ┌──────────────────────────────────────────┐
                     │              BACKEND (Render)            │
                     │  ┌─────────┐  ┌─────────┐  ┌─────────┐  │
                     │  │  Auth   │  │ Exercise│  │   XP    │  │
                     │  │ Service │  │ Service │  │ Validate│  │
                     │  └────┬────┘  └────┬────┘  └────┬────┘  │
                     │       │            │            │        │
                     │       └────────────┴────────────┘        │
                     │                    │                     │
                     │                    ▼                     │
                     │              ┌─────────────┐             │
                     │              │  PostgreSQL   │             │
                     │              │   (Neon)      │             │
                     │              └─────────────┘             │
                     └──────────────────────────────────────────┘
                              │
                              │ JitPack (publicación por tag)
                              ▼
                     ┌──────────────────────────────────────────┐
                     │              :shared (JAR)             │
                     │  DTOs · Enums · Constants · Serializers  │
                     │  com.github.reinaldojperalta:ace-shared  │
                     │  Repo: reinaldojperalta/ace-shared       │
                     │  Estado: ✅ v1.0.0 IMPLEMENTADO           │
                     └──────────────────────────────────────────┘
                              │
                              │ Consumido por backend y mobile
                              ▼
```

**Nota:** No hay flecha directa Wear OS → Backend. Toda comunicación pasa obligatoriamente por la APK. La APK es el único módulo que calcula XP antes de enviar.

**:shared** es consumido por backend y mobile vía JitPack. Wear OS no lo consume (envía primitivos JSON).

---

## 16. Estado de Implementación por Módulo

| Módulo | Estado | Versión | Publicación | Notas |
|--------|--------|---------|-------------|-------|
| **:shared** | ✅ **IMPLEMENTADO** | 1.0.0 | JitPack | 40 archivos, 17 tests pasando. Kotlin 2.2.21, Gson 2.11.0, kotlinx-serialization 1.8.0. |
| **ace-backend** | 🔄 Conceptual | — | — | Depende de `:shared` vía JitPack. Pendiente scaffold Spring Boot 4.0.6. |
| **ace-mobile** | 🔄 Conceptual | — | — | Depende de `:shared` vía JitPack. Pendiente scaffold AGP 9.0.1. |
| **ace-wear** | 🔄 Conceptual | — | — | NO consume `:shared`. Paths hardcodeados como strings literales. |

### Cómo consumir `:shared` (backend)

```kotlin
// ace-backend/build.gradle.kts
repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("com.github.reinaldojperalta:ace-shared:1.0.0")
}
```

### Cómo consumir `:shared` (mobile)

```kotlin
// ace-mobile/app/build.gradle.kts
repositories {
    google()
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("com.github.reinaldojperalta:ace-shared:1.0.0")
}
```

### Reglas de oro para consumidores de `:shared`

1. **Nunca duplicar** un DTO, enum o constante en el consumidor. Si necesitas un tipo que está en `:shared`, importa desde `com.ace.shared.*`.
2. **Nunca modificar** `:shared` sin coordinar con el otro equipo. Un breaking change requiere que ambos lados actualicen simultáneamente.
3. **Versionado semántico:**
   - `MAJOR` cambio → breaking change (campo obligatorio nuevo, renombre, tipo cambiado)
   - `MINOR` cambio → adición opcional (campo nuevo con default)
   - `PATCH` cambio → fix (corrección de adapter)
4. **Proceso de cambio:**
   - Proponer cambio en `reinaldojperalta/ace-shared` con PR.
   - Revisión por Reinaldo (backend) y Steven (mobile).
   - Si es breaking change, ambos equipos deben actualizar antes de mergear.
   - Crear tag y push. JitPack compila automáticamente.
   - Ambos equipos actualizan versión en `build.gradle.kts` y adaptan código.

---

*Documento vivo. Cada sistema aquí descrito es candidato a exploración profunda en conversaciones futuras. Nada es inmutable.*
