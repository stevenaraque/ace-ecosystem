# A.C.E — Deuda Técnica: S2 (Sesión) + S4 (Auth) + S6 (Ranking) + S10 (Perfil)

> **Versión:** 1.0
> **Fecha:** 2026-06-24
> **Tipo:** Deuda técnica + features faltantes detectadas tras auditoría integral de los 3 módulos (`ace-backend`, `ace-mobile`, `ace-wear`) + `:shared`
> **Origen:** Contraste del código real contra los Apéndices S1–S10 y `DEUDA_TECNICA_S3_S5_v1.md`, cotejado con los DTOs de `:shared` y el seed de la BD.
> **Documento hermano:** `CONTEXT/DEUDA_TECNICA_S3_S5_v1.md` (este documento retoma su deuda y añade la de S2/S4/S6/S10).
> **Modo de uso:** Delegable. Cada ítem es ejecutable por un LLM sin contexto adicional. No marcar como hecho sin releer el archivo tras el cambio.

---

## 0. Cómo leer este documento

Cada ítem tiene:
- **ID** (`F#` = feature nueva faltante, `C#` = deuda crítica heredada, `M#` = deuda menor).
- **Severidad** y **bloquea a**.
- **Archivo(s)** con ruta exacta y línea(s), verificados en código a 2026-06-24.
- **Qué hay ahora** (estado real verificado leyendo el archivo).
- **Qué debe quedar** (objetivo).
- **Acción correctiva** concreta.
- **Estado** (`⬜ Pendiente` / `✅ Hecho`).

**Reglas de ejecución:**
1. Ejecutar en el orden del §"Plan de acción" (está ordenado por dependencia: `:shared` → backend → mobile).
2. Antes de cada ítem, leer el archivo citado para confirmar que el estado coincide.
3. Cada ítem crítico debe dejar el proyecto compilable.
4. Tras tocar `:shared`, crear tag + bump de versión en backend, mobile y wear.
5. **No tocar** los Apéndices S1–S10, ni `ACE_SYSTEMS_ARCHITECTURE_v0.3.md`, ni los `ACE_DesignSystem_*` (son de fase posterior).

**Decisiones del equipo ya tomadas (NO cambiar):**
- ✅ **JWT único por dispositivo** = **desalojo en login** (F7). Al hacer login/register se revocan todos los refresh tokens previos del usuario.
- ✅ **Auto-pausa por FC** se aplica **solo en el móvil**; el reloj **sigue midiendo** FC (no se extiende el protocolo wear). (F6)
- ✅ **Pausa manual + automática**: botón Pausar/Reanudar en UI + lógica automática por FC. (F6)
- ✅ La duración de bloque se reescribió a **60s** (`BLOCK_TIMER_MS = 60_000L`) y `MIN_DURATION=10` como decisión consciente del equipo (bloques cortos aceptados). **No revertir a 270–330.** La deuda C1/C2 de S3/S5 queda cerrada por decisión del equipo.
- ✅ Ruta de sync unificada a `/api/exercise/batch` (M5 resuelto). No cambiar.
- ✅ Credenciales hardcodeadas en `application.yml` se quedan (proyecto académico).
- ✅ `:shared` hibrido en wear: `WearMessageClient` usa `DataLayerPaths` de `:shared`; `WearDataClient` usa `WearDataLayerPaths` local. **No unificar.**

---

## 1. Resumen ejecutivo

| Sistema | Avance real | Veredicto |
|---|---|---|
| **S2 Sesión** (mobile/wear) | 🟡 ~60% | Captura + cierre de bloque funcionan; **NO existe pausa/resumen** (ni manual ni por FC) |
| **S4 Auth / JWT** | 🟡 ~85% | Login/register/refresh sólidos; **falta desalojo de sesión al loguear** (no hay JWT único) |
| **S5 XP** | 🟡 ~70% | Funciona con bugs heredados (bonusMultiplier, XpTransaction mapping) |
| **S6 Ranking** | 🟡 ~75% | Recálculo y rango funcionan; **municipal roto** (sin cityId) + **sin auto-refresh** |
| **S10 Perfil/Stats** | 🟡 ~70% | Stats funcionan; **editar perfil no existe** (controller/service vacíos) |
| **Crear cuenta (UI)** | ❌ 0% | Endpoint hecho; botón "CREAR CUENTA" sin acción, sin pantalla |
| **Fórmulas deportes** | 🟡 ~33% | Solo RUNNING con seed; CYCLING/WALKING sin fórmula |

**Conclusión:** hay **7 features críticas (F1–F7)** pendientes + **3 deudas críticas heredadas (C3, C4, C5)** que afectan consistencia de XP/ranking. El ranking no será fiable hasta cerrar C3 (bonusMultiplier) y el perfil (F1/F2), que alimenta el municipal.

---

## 2. Deuda CRÍTICA + Features faltantes (bloqueantes)

### 🔴 F1 — Editar perfil (backend): controller y service vacíos

- **Severidad:** CRÍTICA (bloquea F2, F4)
- **Archivo:**
  - `ace-backend/src/main/kotlin/sena/adso/ace_backend/user/controller/ProfileController.kt` → **vacío (0 bytes / 1 línea)**
  - `ace-backend/src/main/kotlin/sena/adso/ace_backend/user/service/ProfileService.kt` → **vacío (0 bytes / 1 línea)**
- **Estado:** ⬜ Pendiente

**Qué hay ahora:**
`ProfileController` y `ProfileService` existen pero están **completamente vacíos**. NO hay endpoints de perfil.

**Qué debe quedar (según S10):**
- `GET /api/profile` → devuelve el `UserProfile` del usuario autenticado.
- `PUT /api/profile` → actualiza `username`, `nickname`, `cityId`, `weightKg`, `birthDate`.

**Qué ya existe (reutilizar, no crear):**
- `UserProfile` entity: `userId, username, nickname, cityId, weightKg, birthDate` (completo).
- `UserProfileRepository` (completo).

**Acción:**
1. **Primero en `:shared`** (ver §3 Plan de acción, orden): crear DTOs
   - `UserProfileDto(userId, username, nickname, cityId, weightKg, birthDate)`.
   - `UpdateProfileRequestDto(username?, nickname?, cityId?, weightKg?, birthDate?)` (todos opcionales → patch parcial).
   - Bump `:shared` 1.0.4 → 1.0.6, tag `v1.0.6`, push, esperar JitPack verde.
2. Implementar `ProfileService`:
   - `getProfile(userId)`: leer `UserProfileRepository.findById(userId)`. Si no existe, crear uno vacío (lazy init).
   - `updateProfile(userId, request)`: cargar, aplicar campos no-nulos, guardar.
3. Implementar `ProfileController`:
   - `@RestController @RequestMapping("/api/profile")`.
   - `GET` y `PUT` con `@AuthenticationPrincipal userDetails: String` (igual que `ExerciseController`).
4. No añadir a `SecurityConfig` público — queda protegido por JWT por defecto.

---

### 🔴 F2 — Editar perfil (mobile): UserApi y UserRepository vacíos, sin UI

- **Severidad:** CRÍTICA (bloquea F4 indirectamente)
- **Archivos:**
  - `ace-mobile/.../data/remote/api/UserApi.kt` → **vacío (0 bytes)**
  - `ace-mobile/.../data/repository/UserRepository.kt` → **vacío (0 bytes)**
  - `ace-mobile/.../presentation/profile/ProfileScreen.kt` → solo avatar + logout + versión.
  - `ace-mobile/.../presentation/profile/ProfileViewModel.kt:29` → solo `logout()`.
- **Estado:** ⬜ Pendiente

**Qué hay ahora:**
La pantalla de perfil muestra avatar, título "Tu Perfil / Configuración y cuenta", botón Logout y "A.C.E v1.0.5". Nada editable. `UserApi`/`UserRepository` son archivos de 0 bytes.

**Qué debe quedar:**
- Formulario editable: `username`, `nickname`, `cityId` (selector de ciudad/municipio), `weightKg`, `birthDate`.
- Al guardar, persistir vía `PUT /api/profile` y actualizar `cityId` en `UserPreferencesDataStore` (esto desbloquea el ranking municipal — ver F4).

**Acción:**
1. Implementar `UserApi` (Retrofit): `GET /api/profile`, `PUT /api/profile` usando los DTOs de `:shared` 1.0.6.
2. Implementar `UserRepository`: `getProfile()`, `updateProfile(request)`; cache local del perfil en Room o DataStore.
3. Añadir a `ProfileViewModel`:
   - `StateFlow<ProfileUiState>` con los datos cargados (modo lectura/edición).
   - `loadProfile()`, `updateProfile(...)`.
4. Reescribir `ProfileScreen` con campos editables + botón Guardar. Incluir un **selector de ciudad/municipio** para `cityId`.
5. Tras guardar, llamar `userPreferencesDataStore.setCityId(cityId)` (este método existe pero **nunca se invoca** — ver F4).

---

### 🔴 F3 — Crear cuenta (mobile): botón sin acción, sin pantalla de registro

- **Severidad:** CRÍTICA
- **Archivos:**
  - `ace-mobile/.../presentation/auth/LoginScreen.kt:399` → botón "CREAR CUENTA" con `onClick = { }` vacío.
  - `ace-mobile/.../presentation/auth/LoginScreen.kt:337` → botón "¿Olvidaste tu contraseña?" con `onClick = { }` vacío (fuera de alcance de este doc; documentar).
- **Estado:** ⬜ Pendiente

**Qué hay ahora:**
El endpoint `POST /api/auth/register` **sí está hecho** (`AuthController` + `AuthService.register`). El móvil tiene `AuthApi.register` y `AuthRepository.register(...)` **cableados pero son código muerto**: ningún ViewModel/UI los invoca.

**Qué debe quedar:**
- Nueva `RegisterScreen` + `RegisterViewModel` con campos email + password (+ confirmación).
- Al registrar, llamar `AuthRepository.register(email, password, deviceId)` → en éxito, navegar a Home (igual que login).
- Conectar el botón "CREAR CUENTA" del `LoginScreen` a `navController.navigate("register_screen_route")`.

**Acción:**
1. Crear `presentation/auth/RegisterScreen.kt` (puede replicar el estilo visual del `LoginScreen`).
2. Crear `RegisterViewModel` que inyecte `AuthRepository` y `DeviceIdManager`, exponga `StateFlow<RegisterUiState>` + `register(email, password)`.
3. Añadir `register_screen_route` al `NavHost` en `MainActivity.kt`.
4. En `LoginScreen.kt:399`, reemplazar `onClick = { }` por navegación a registro.
5. Validar: email no vacío + formato, password ≥ 8 caracteres, confirmación coincide. Mostrar error del backend (409 si email ya existe).

---

### 🔴 F4 — Ranking municipal roto + sin auto-refresh

- **Severidad:** CRÍTICA (UX)
- **Archivos:**
  - `ace-mobile/.../data/local/preferences/UserPreferencesDataStore.kt` → `setCityId(...)` existe pero **no se invoca en ningún sitio** (grep: 0 callers).
  - `ace-mobile/.../presentation/ranking/RankingScreen.kt` + `RankingViewModel.kt` → carga en `init` y al cambiar tab; sin polling ni pull-to-refresh.
  - `ace-backend/.../ranking/service/RankingRecalculationJob.kt` → recálculo cada hora ✅ (no tocar).
- **Estado:** ⬜ Pendiente

**Qué hay ahora:**
1. **Municipal siempre falla**: `cityId` siempre es `null` porque `setCityId()` nunca se llama → el tab Municipal muestra siempre "No tienes ciudad configurada".
2. **El ranking no se actualiza** salvo al entrar a la pantalla o cambiar de tab. No hay refresh al volver de una sesión, ni pull-to-refresh, ni invalidación del cache cuando `rankChanged=true`.

**Aclaración importante (no es bug):**
- **NO falta un endpoint de actualizar rango.** El rango se **deriva** del XP total (BRONZE/SILVER/GOLD/PLATINUM/DIAMOND por umbrales en `rank_catalog`). El `RankingRecalculationJob` ya recalcula cada hora. Esto es *by design* (S6 §2.1).
- El delay de hasta 1h en reflejar un bloque nuevo es **aceptable** según S6 §7. No hay que corregirlo.

**Acción:**
1. **Resolver el cityId** (depende de F2): la pantalla de editar perfil debe guardar `cityId` y llamar `setCityId()`. Sin F2, el municipal no puede funcionar.
2. **Auto-refresh de la UI**:
   - Invalidar el cache de ranking cuando un sync responda `rankChanged = true` (S6 §5.4). El `RankingCacheRepository` ya tiene lógica de TTL; añadir un `invalidate()` y llamarlo desde el flujo de sync.
   - En `RankingScreen`, refrescar al volver a la pantalla (`LaunchedEffect(Unit)` o `lifecycle STARTED`) si el cache está stale.
   - Añadir **pull-to-refresh** (gesto) conectado a `RankingViewModel.refresh()` (el método existe pero no está cableado a UI).

---

### 🔴 F5 — Fórmulas de deportes: solo RUNNING con seed

- **Severidad:** CRÍTICA (alcance funcional)
- **Archivos:**
  - `ace-backend/src/main/resources/db/migration/V4__seed_data.sql` → solo seed RUNNING.
  - `ace-mobile/.../presentation/exercise/SessionScreen.kt` → selección de deporte (verificar si existe selector).
  - `:shared/.../enums/SportType.kt` → define `RUNNING, CYCLING, WALKING`.
- **Estado:** ⬜ Pendiente

**Qué hay ahora:**
El `SportType` enum define 3 deportes, pero solo `RUNNING` tiene fórmula (`min_bpm=80, xp_per_minute=2, max_xp_per_block=30`). El mecanismo de carga/cálculo (`CacheXpFormulasUseCase`, `BuildExerciseBlockUseCase`) **funciona**; faltan los datos de CYCLING y WALKING.

**Qué debe quedar (según S5):**
- Seed de fórmulas para los 3 deportes.
- Selector de deporte en `SessionScreen` antes de iniciar.

**Acción:**
1. Crear migración `V7__seed_more_sports.sql`:
   ```sql
   INSERT INTO xp_formulas (formula_id, sport_type, min_bpm, xp_per_minute, bonus_multiplier, max_xp_per_block, is_active, created_at)
   VALUES
     (gen_random_uuid(), 'CYCLING',  90, 2, 1.0, 30, true, now()),
     (gen_random_uuid(), 'WALKING',  70, 1, 1.0, 20, true, now())
   ON CONFLICT (sport_type) DO NOTHING;
   ```
   > **Nota:** si se aplica C3 (eliminar `bonus_multiplier`) antes, quitar esa columna del INSERT. Coordinar con C3.
   > **Nota 2:** los valores de `min_bpm/xp_per_minute/max_xp_per_block` son propuesta inicial; confirmar con el equipo si hay criterio deportivo. RUNNING sirve de referencia.
2. En `SessionScreen`, añadir un selector de deporte (Chips o Dropdown) que alimente `SessionViewModel.startSession(sportType, userId)`. Hoy el deporte puede estar fijo a RUNNING — verificar.
3. Verificar que `BuildExerciseBlockUseCase` calcula XP correctamente con la fórmula del deporte seleccionado (ya lo hace vía `xpFormulaDao.getFormula(sportType.name)`).

---

### 🔴 F6 — Pausa manual + auto-pausa por FC baja (mobile, solo móvil)

- **Severidad:** CRÍTICA
- **Archivos:**
  - `ace-mobile/.../domain/usecase/exercise/PauseSessionUseCase.kt` → **vacío (0 bytes)**
  - `ace-mobile/.../domain/usecase/exercise/ResumeSessionUseCase.kt` → **vacío (0 bytes)**
  - `ace-mobile/.../presentation/exercise/SessionViewModel.kt` → sin estado `Paused`, sin watcher de FC, timer nunca se pausa (`:218` `while(_uiState is Active)`).
  - `ace-mobile/.../presentation/exercise/SessionUiState.kt` → solo `Idle/Loading/Active/Stopping/Completed/Error` (verificar; sin `Paused`).
  - `ace-mobile/.../data/repository/SessionSampleBufferImpl.kt` → buffer de muestras y cierre de bloque por timer.
- **Estado:** ⬜ Pendiente

**Qué hay ahora:**
- `PauseSessionUseCase` y `ResumeSessionUseCase` son archivos de **0 bytes** y **no se referencian en ningún sitio** (grep: 0 usos).
- `SessionUiState` **no tiene variante `Paused`**.
- El timer `startElapsedTimer()` (`:215`) corre incondicionalmente mientras `Active`; no existe concepto de "tiempo activo vs tiempo pausado".
- El literal `110` **no aparece en lógica de sesión** (solo en `LoginScreen.kt:64` como tamaño de un cubo visual 3D).
- El reloj **solo tiene START/STOP** (sin PAUSE/RESUME) — esto es **correcto** según la decisión del equipo: la pausa es solo móvil, el reloj sigue midiendo.

**Reglas a implementar (según decisión del equipo):**
1. **Pausa manual**: botón Pausar en UI → la sesión pasa a `Paused`.
2. **Auto-pausa por FC**: si la frecuencia cardíaca permanece **< 110 BPM** por **más de 30 segundos** → pausar automáticamente.
3. **Reanudación automática**: en el momento que se detecte **FC ≥ 110 BPM** → reanudar el conteo del bloque.
4. Al pausar, el **cronómetro del bloque se congela** (sigue el mismo bloque OPEN, según S2 §3.3: *"Si el usuario pausa la sesión, el cronómetro del bloque se congela. Al reanudar, continúa el mismo bloque OPEN."*).
5. **El reloj sigue midiendo** FC durante la pausa (no se envía PAUSE/RESUME al wear). El móvil ignora el conteo de tiempo pero sigue recibiendo muestras para evaluar el reanudado.

**Acción:**

A. **Estado de UI**:
   - Añadir `data class Paused(val session: ExerciseSession) : SessionUiState` a `SessionUiState`.
   - Transiciones: `Active → Paused` (pausa manual o auto), `Paused → Active` (reanudar).

B. **Use cases** (implementar los 0 bytes):
   - `PauseSessionUseCase(sessionId)`: marcar la sesión local como `PAUSED`, pausar el timer del buffer (`SessionSampleBuffer.pauseBlockTimer()`), NO enviar nada al reloj.
   - `ResumeSessionUseCase(sessionId)`: marcar `ACTIVE`, reanudar el timer del buffer. El bloque OPEN continúa con las muestras acumuladas.

C. **`SessionSampleBufferImpl`**:
   - Añadir soporte para pausar/reanudar el **timer de cierre de bloque** sin perder el bloque OPEN en curso. El buffer sigue recibiendo muestras (para evaluar FC) pero el reloj de duración del bloque se congela.
   - Exponer un `StateFlow<Boolean> isPaused` o que el watcher de FC consulte el último BPM.

D. **Auto-pausa (watcher de FC) en `SessionViewModel`**:
   - Constantes nuevas:
     ```kotlin
     private const val PAUSE_BPM_THRESHOLD = 110
     private const val LOW_BPM_PAUSE_SECONDS = 30
     ```
   - Observar `heartRate` flow. Mientras `Active`:
     - Si `bpm < PAUSE_BPM_THRESHOLD` → iniciar/continuar un contador de "segundos bajos". Si alcanza `LOW_BPM_PAUSE_SECONDS` → `pauseSession(auto = true)`.
     - Si `bpm >= PAUSE_BPM_THRESHOLD` → resetear el contador. Si la sesión está `Paused` (por auto-pausa) → `resumeSession(auto = true)`.
   - Distinguir pausa manual de auto-pausa (flag) para que una pausa manual NO se reanude sola por FC alta.

E. **Botones en UI (`SessionScreen`)**:
   - Estado `Active`: botones **Pausar** + **Detener**.
   - Estado `Paused`: botones **Reanudar** + **Detener**. Indicador visual "Pausado (FC baja)" si fue auto-pausa.

F. **Backend (sin cambios de protocolo)**: la pausa es local del móvil. Los bloques que se generen ya tendrán `durationSeconds` reducido (porque el timer estuvo congelado). El backend los valida normalmente. **No requiere tocar `:shared` ni wear.**

---

### 🔴 F7 — JWT único por dispositivo (desalojo en login)

- **Severidad:** CRÍTICA (seguridad / unicidad de sesión)
- **Archivos:**
  - `ace-backend/.../auth/service/AuthService.kt:24` (`login`), `:50` (`register`) → NO revocan tokens previos.
  - `ace-backend/.../auth/service/RefreshTokenService.kt:88` → `revokeAllUserTokens(userId)` **existe pero nunca se invoca**.
- **Estado:** ⬜ Pendiente

**Qué hay ahora:**
- `deviceId` se genera por instalación y viaja en login/register/refresh. ✅
- `RefreshTokenService.rotateRefreshToken()` valida `deviceId` mismatch al refrescar. ✅
- **PERO**: `login()` y `register()` **no revocan** los refresh tokens previos del usuario. Si entras desde el dispositivo B, el dispositivo A **conserva su refresh token válido hasta expirar (7 días)**. No hay desalojo inmediato → **no hay JWT único real**.

**Qué debe quedar (decisión del equipo: desalojo en login):**
Al hacer login o register, **antes** de crear el nuevo refresh token, revocar todos los refresh tokens activos del usuario. El dispositivo anterior pierde acceso en su próximo `refresh` (el backend lanza `REFRESH_REVOKED` → el `AuthInterceptor` del móvil limpia tokens → logout automático).

**Acción:**
1. En `AuthService.login()`, antes de `refreshTokenService.createRefreshToken(...)`:
   ```kotlin
   refreshTokenService.revokeAllUserTokens(user.id!!)
   ```
2. En `AuthService.register()`, lo mismo (aunque un usuario nuevo no suele tener tokens, es defensivo y barato):
   ```kotlin
   refreshTokenService.revokeAllUserTokens(savedUser.id!!)
   ```
3. `revokeAllUserTokens` ya está implementado (`RefreshTokenService:88`) — solo falta invocarlo.
4. Verificar que el efecto en el dispositivo anterior es: próximo request → access token expira → `refresh` → backend devuelve `REFRESH_REVOKED` (401) → `AuthInterceptor` limpia tokens → redirección a login. (Comportamiento ya soportado por el interceptor móvil.)

---

## 3. Deuda CRÍTICA heredada (de S3/S5, sigue pendiente)

> Estos ítems provienen de `DEUDA_TECNICA_S3_S5_v1.md`. Se re-verificaron contra el código actual el 2026-06-24. **C1, C2 y M5 ya están cerrados por decisión del equipo** (bloque de 60s, MIN_DURATION=10, ruta batch unificada). Quedan C3, C4, C5.

### 🔴 C3 — `bonusMultiplier` no existe en el apéndice ni en `:shared`

- **Severidad:** CRÍTICA (XP inconsistente móvil ↔ backend → ranking corrupto)
- **Archivos:**
  - `ace-backend/.../xp/model/XpFormula.kt:26` → campo `bonusMultiplier: Double`
  - `ace-backend/.../xp/service/XpSanityValidator.kt:89` → `minutes * formula.xpPerMinute * formula.bonusMultiplier`
  - `ace-backend/src/main/resources/db/migration/V5__align_entities.sql:11` → `ADD COLUMN bonus_multiplier ... DEFAULT 1.0`
- **Estado:** ⬜ Pendiente (re-confirmado: la línea `:89` sigue multiplicando por `bonusMultiplier`)

**Qué hay ahora:**
El backend calcula XP teórica multiplicando por `bonusMultiplier`, pero `XpFormulaDto` de `:shared` **no tiene** ese campo, y el móvil (`BuildExerciseBlockUseCase:158`) calcula `minutes * formula.xpPerMinute` **sin** el multiplicador. Hoy el seed lo deja en `1.0` (no rompe), pero es un desync latente: si alguien cambia el seed a `1.2`, el backend rechazará bloques válidos del móvil (R5).

**Acción (recomendada: eliminar):**
1. Crear migración `V6__drop_bonus_multiplier.sql`: `ALTER TABLE xp_formulas DROP COLUMN IF EXISTS bonus_multiplier;`
2. Eliminar `bonusMultiplier` de `XpFormula.kt`.
3. En `XpSanityValidator.calculateMaxTheoreticalXp` (`:89`): quitar `* formula.bonusMultiplier`.
4. Verificar que F5 (seed de más deportes) no incluya `bonus_multiplier` en su INSERT.

---

### 🔴 C4 — `XpTransaction` diverge de la BD (nombre y tipo)

- **Severidad:** CRÍTICA (mapeo Hibernate + overflow a largo plazo)
- **Archivo:** `ace-backend/.../xp/model/XpTransaction.kt:25,28`
- **Estado:** ⬜ Pendiente (re-confirmado)

**Qué hay ahora:**
```kotlin
@Column(nullable = false)
val xpAmount: Int,              // la BD la llama `amount`
@Column(nullable = false)
val balanceAfter: Int,          // la BD es `balance_after BIGINT`
```
1. `xpAmount` sin `@Column(name = "amount")` → Hibernate mapea por convención a `xp_amount`, que **no existe** en la BD. (Hoy funciona porque `ddl-auto` está en modo permisivo en dev; en prod `validate` puede fallar.)
2. `balanceAfter: Int` pero la BD es `BIGINT` → overflow a largo plazo. El código compensa con `.toLong()` por todas partes (`SyncBatchService`, `XpSanityValidator`), lo que confirma la fricción.

**Acción:**
1. `@Column(name = "amount", nullable = false) val xpAmount: Int`.
2. `val balanceAfter: Long` (y `getCurrentBalance()` en `XpTransactionService:55` ya retorna `Int` → cambiar a `Long`).
3. Actualizar todos los `.toLong()` compensatorios en `SyncBatchService` y `XpSanityValidator` para que operen con `Long` de forma natural.

---

### 🔴 C5 — Streak se evalúa con el primer bloque aceptado, no el último

- **Severidad:** CRÍTICA (racha mal calculada con bloques offline/atrás)
- **Archivo:** `ace-backend/.../exercise/service/SyncBatchService.kt:85`
- **Estado:** ⬜ Pendiente (re-confirmado: sigue `.find`)

**Qué hay ahora:**
```kotlin
val lastAcceptedBlock = request.blocks.find { it.blockId in acceptedBlocks }
```
`.find` devuelve el **primero** de la lista. Para la racha importa el `timestampStart` **más reciente** (Apéndice S7 §2.3).

**Acción:**
```kotlin
val lastAcceptedBlock = request.blocks
    .filter { it.blockId in acceptedBlocks }
    .maxByOrNull { it.timestampStart }
```

---

## 4. Deuda MENOR (no bloquea pero conviene corregir)

### 🟡 M1 — `XpSanityValidator` duplica constantes y R4 contradice el seed

- **Archivo:** `ace-backend/.../xp/service/XpSanityValidator.kt:93-101`
- **Estado:** ⬜ Pendiente (re-confirmado)

**Qué hay ahora:**
```kotlin
const val MIN_BPM = 30.0          // ya está en XpConstants.MIN_BPM_PHYSIOLOGICAL
const val MAX_BPM = 250.0         // ya está en XpConstants.MAX_BPM_PHYSIOLOGICAL
const val MIN_DURATION = 10       // (decisión del equipo, mantener)
const val MAX_DURATION = 330      // mantener
const val SAMPLE_TOLERANCE_PERCENT = 20
const val MAX_XP_PER_BLOCK = 50   // ← contradice el seed (max=30 para RUNNING)
const val XP_TOLERANCE = 5
```

**Acción:**
1. Importar `XpConstants` y reemplazar `MIN_BPM`/`MAX_BPM`.
2. **Eliminar R4** (`:62-66`): el techo `MAX_XP_PER_BLOCK=50` contradice el seed (30) y R5 ya valida contra `formula.maxXpPerBlock`. R4 es redundante y rechaza bloques válidos.
3. Eliminar `MAX_XP_PER_BLOCK`. Revisar si `XP_TOLERANCE` aún se usa tras eliminar R4 (sí, en R5 `:71`).

---

### 🟡 M2 — R3 (sample_count) demasiado estricta para Bluetooth intermitente

- **Archivo:** `ace-backend/.../xp/service/XpSanityValidator.kt:53-60`
- **Estado:** ⬜ Pendiente (re-confirmado)

**Qué hay ahora:** exige densidad ~1Hz (`expectedSamples = durationSeconds` ±20%). Con desconexiones Bluetooth (timeout 5s contemplado en S1), el móvil tendrá menos muestras → rechazo de bloques válidos.

**Acción:** relajar a densidad ≥ 0.5Hz:
```kotlin
val minExpectedSamples = block.durationSeconds / 2
if (block.sampleCount < minExpectedSamples) {
    return ValidationResult.Invalid(block.blockId, "sampleCount too low: ${block.sampleCount}")
}
```
(O eliminar la regla en MVP y dejar R1/R2/R5.)

---

### 🟡 M3 — `OfficialStatsDto` no es realmente oficial

- **Archivo:** `ace-backend/.../exercise/service/SyncBatchService.kt:131-140`
- **Estado:** ⬜ Pendiente

**Qué hay ahora:** copia `totalSessions` y `avgBpmAllTime` del `clientStats` del móvil. Solo `totalXp` (balance) y `totalDurationSeconds` se calculan. No hay recálculo desde BD (S10 §4.2 exige leer de `xp_transactions` y bloques SYNCED).

**Acción (Hito de stats):** leer los totales oficiales de `user_stats` (que `StatsPersistenceService` ya alimenta) y compararlos con `clientStats`; aplicar corrección si difiere >10 XP (S10 §5.3).

---

### 🟡 M4 — `XpFormulaDto.version` siempre devuelve 1

- **Archivos:**
  - `ace-backend/.../xp/service/FormulaService.kt` → `version = 1` hardcoded.
  - `ace-backend/.../xp/controller/XpController.kt` → no añade header `X-Formula-Version`.
- **Estado:** ⬜ Pendiente

**Acción:**
1. En `FormulaService.getActiveFormulas()`: leer `formula.version` de la BD (verificar si el campo existe en `XpFormula`; si no, añadirlo).
2. En `XpController`: calcular `max(version)` y añadir `response.header(XpConstants.FORMULA_VERSION_HEADER, maxVersion.toString())`.
3. El móvil ya puede detectar versión vieja y recargar.

---

### ✅ M5 — Ruta del endpoint de exercise unificada — RESUELTO

- **Estado:** ✅ Hecho. Backend (`ExerciseController.kt:30` `@PostMapping("/batch")`) y mobile (`ExerciseApi.kt:14` `@POST("/api/exercise/batch")`) coinciden. `ApiEndpoints.EXERCISE_BLOCKS` en `:shared` aún dice `/api/exercise/blocks` (cosmético, no se usa en mobile); dejar o sincronizar cuando se toque `:shared`.

---

## 5. Plan de acción recomendado

Orden por dependencia. `:shared` primero (F1 necesita DTOs); luego backend; luego mobile.

| Orden | Ítem | Severidad | Archivo principal | Dificultad |
|---|---|---|---|---|
| 1 | **F1 (shared)** Crear DTOs de perfil + bump 1.0.6 + tag | 🔴 | `ace-shared/.../dto/*` | Trivial |
| 2 | **F1 (backend)** `ProfileController` + `ProfileService` | 🔴 | `user/controller`, `user/service` | Fácil |
| 3 | **F7** Desalojo en login/register | 🔴 | `AuthService.kt:24,50` | Trivial |
| 4 | **C5** Streak con último bloque | 🔴 | `SyncBatchService.kt:85` | Trivial |
| 5 | **C3** Eliminar `bonusMultiplier` | 🔴 | `XpFormula.kt`, `XpSanityValidator.kt:89`, V6 | Media (migración) |
| 6 | **C4** `XpTransaction` nombre + tipo | 🔴 | `XpTransaction.kt`, `XpTransactionService.kt` | Media |
| 7 | **F5** Seed CYCLING/WALKING + selector deporte | 🔴 | `V7__seed_more_sports.sql`, `SessionScreen` | Fácil |
| 8 | **F2 (mobile)** Editar perfil + cityId | 🔴 | `UserApi`, `UserRepository`, `ProfileScreen` | Media |
| 9 | **F4 (mobile)** Ranking auto-refresh + invalidar cache | 🔴 | `RankingScreen`, `RankingCacheRepository` | Fácil |
| 10 | **F3 (mobile)** Pantalla de registro | 🔴 | `RegisterScreen`, `RegisterViewModel`, `LoginScreen:399` | Media |
| 11 | **F6 (mobile)** Pausa manual + auto-pausa por FC | 🔴 | `PauseSessionUseCase`, `SessionViewModel`, `SessionScreen` | Alta |
| 12 | **M1** Validador sin duplicados + eliminar R4 | 🟡 | `XpSanityValidator.kt` | Fácil |
| 13 | **M2** Relajar R3 (sample_count) | 🟡 | `XpSanityValidator.kt` | Fácil |
| 14 | **M4** `version` real + header | 🟡 | `FormulaService.kt`, `XpController.kt` | Fácil |
| 15 | **M3** Stats oficiales desde BD | 🟡 | `SyncBatchService.kt`, stats services | Media |

> **F6 es el más complejo** (toca estados de UI, use cases, buffer de muestras y lógica de timer). Dejarlo para el final, cuando el resto del móvil esté estable.

---

## 6. Verificación final (checklist)

Tras aplicar los fixes:

- [ ] `:shared` 1.0.6 resuelto en backend, mobile y wear (`libs.versions.toml`); tag `v1.0.6` verde en JitPack.
- [ ] `ace-backend`: `./gradlew compileKotlin` pasa. Arranca con `ddl-auto: validate` (verifica C4).
- [ ] `ace-mobile`: `./gradlew :app:assembleDebug` pasa.
- [ ] `ace-wear`: `./gradlew :app:assembleDebug` pasa (sin cambios esperados — la pausa es solo móvil).
- [ ] `GET /api/profile` y `PUT /api/profile` responden con JWT.
- [ ] Desde el móvil, editar perfil y guardar `cityId` → el ranking municipal deja de decir "No tienes ciudad configurada".
- [ ] Botón "CREAR CUENTA" abre la pantalla de registro y un registro exitoso navega a Home.
- [ ] Login desde el dispositivo B invalida el refresh token del dispositivo A (próximo refresh del A → logout).
- [ ] `GET /api/xp/formulas` devuelve RUNNING, CYCLING y WALKING.
- [ ] En una sesión, botón Pausar congela el cronómetro; Reanudar lo continúa sin perder el bloque OPEN.
- [ ] En una sesión activa, FC < 110 por >30s pausa automáticamente; FC ≥ 110 reanuda.
- [ ] Una pausa manual NO se reanuda sola por FC alta.
- [ ] XP móvil = XP backend para el mismo bloque (sin `bonusMultiplier`).
- [ ] Tras sync con bloques de horas distintas, `StreakStateDto` refleja el día del bloque más reciente.
- [ ] `POST /api/exercise/batch` acepta bloques de 60s y rechaza los < 10s (frontera consistente).

---

## 7. Apéndice — Referencias

- **Apéndice S1 (Captura):** `CONTEXT/ACE_APPENDIX_S1_Capture_Sensor.md` — paths, formato HR, timeout desconexión 5s.
- **Apéndice S2 (Sesión):** `CONTEXT/ACE_APPENDIX_S2_Session.md` — estados ACTIVE/PAUSED/COMPLETED/ABORTED, regla de bloque OPEN congelado al pausar (§3.3).
- **Apéndice S4 (Auth):** `CONTEXT/ACE_APPENDIX_S4_Auth.md` + `CONTEXT/reporte_estado_auth.md` — JWT híbrida, rotación, race conditions.
- **Apéndice S5 (XP):** `CONTEXT/ACE_APPENDIX_S5_XP.md` — fórmula RUNNING, R1-R5, header `X-Formula-Version`.
- **Apéndice S6 (Ranking):** `CONTEXT/ACE_APPENDIX_S6_Ranking.md` — recálculo batch horario, cache 1h, invalidar en `rank_changed`.
- **Apéndice S7 (Streaks):** `CONTEXT/ACE_APPENDIX_S7_Streaks.md` — evaluación por día con el bloque más reciente.
- **Apéndice S10 (Perfil/Stats):** `CONTEXT/ACE_APPENDIX_S10_Profile_Stats.md` — acumulación, reconciliación, corrección.
- **Deuda previa:** `CONTEXT/DEUDA_TECNICA_S3_S5_v1.md` (C1/C2/M5 cerrados por decisión del equipo; C3/C4/C5 retomados aquí).
- **Arquitectura:** `CONTEXT/ACE_SYSTEMS_ARCHITECTURE_v0.3.md` — responsabilidades por módulo, `:shared` contrato.
- **`:shared` constantes:** `com.ace.shared.constants.{SyncConstants, XpConstants, RankingConstants, AuthConstants, ApiEndpoints}`.

---

*Documento de deuda técnica generado por auditoría integral del ecosistema A.C.E (backend + mobile + wear + shared). Versión 1.0 — 2026-06-24. Delegable: cada ítem es ejecutable sin contexto adicional. Cualquier fix aplicado debe marcarse como `✅ Hecho` con fecha y commit.*
