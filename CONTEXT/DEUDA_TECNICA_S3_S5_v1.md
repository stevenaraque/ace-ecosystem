# A.C.E — Deuda Técnica: S3 (Sync) + S5 (XP)

> **Versión:** 1.0
> **Fecha:** 2026-06-19
> **Tipo:** Deuda técnica detectada tras auditoría de los Hitos 1 y 2
> **Origen:** Análisis del código real (commits `26d700d` S3, `887c4df`/`b75dd64`/`53e6e4e` S5) cotejado contra los Apéndices S3 y S5, los DTOs de `:shared` 1.0.5 y el seed de la BD.
> **Bloquea:** Hito 3 (S6 Ranking + S7 Streaks) — el ranking y las rachas leen de `xp_transactions`, que hoy se llena con datos incorrectos.

---

## 0. Cómo leer este documento

Cada ítem de deuda tiene:
- **ID** (`C#` = crítico, `M#` = menor).
- **Severidad** y **bloquea a**.
- **Archivo(s)** con ruta exacta y línea(s).
- **Qué hay ahora** (estado real verificado en código).
- **Qué dice el apéndice / `:shared`**.
- **Acción correctiva** concreta.
- **Estado** (`⬜ Pendiente` / `✅ Hecho`).

**Reglas:**
- No marcar un ítem como hecho sin verificar leyendo el archivo después del cambio.
- Cada ítem crítico debe dejar el proyecto compilable.
- El orden de ejecución recomendado está en el §"Plan de acción".

---

## 1. Resumen ejecutivo

| Sistema | Avance real | Veredicto |
|---|---|---|
| **S3 Sync (Hito 1)** | ✅ ~90% implementado | Funciona end-to-end con bugs |
| **S5 XP (Hito 2)** | 🟡 ~70% implementado | **Bug crítico bloquea todo** |
| **S7 Streaks (parcial)** | 🟡 ~40% (solo backend) | `StreakEvaluationService` existe y se invoca, con bug menor |

`ace-shared` está en **1.0.5** local con DTOs completos y alineados a los apéndices. Hay que bumpar `ace-backend` y `ace-mobile` de 1.0.4 → 1.0.5.

**Conclusión:** antes de tocar S6/S7 (Hito 3) hay que cerrar **5 ítems críticos** (C1–C5). Sin esos fixes, el ranking leerá datos corruptos.

---

## 2. Deuda CRÍTICA (bloquea Hito 3)

### 🔴 C1 — `BuildExerciseBlockUseCase` tiene constantes de duración corruptas

- **Severidad:** CRÍTICA
- **Bloquea a:** Hito 3 (todo bloque será rechazado o inválido)
- **Archivo:** `ace-mobile/app/src/main/kotlin/com/ace/mobile/domain/usecase/wear/BuildExerciseBlockUseCase.kt:23-24`
- **Estado:** ⬜ Pendiente

**Qué hay ahora:**
```kotlin
private const val BLOCK_DURATION_SECONDS = 20        // 5 minutos  ← MENTIRA
private const val BLOCK_DURATION_TOLERANCE_PERCENT = 300 // ±10%       ← MENTIRA
```

Los comentarios dicen "5 minutos" y "±10%" pero los valores reales son **20 segundos** y **300%**. El cálculo (líneas 57-60) produce:
```kotlin
val minDuration = 20 - (20 * 300 / 100) = -40   // siempre se cumple
val maxDuration = 20 + (20 * 300 / 100) = 80    // 80s máximo
```

→ El móvil cierra bloques de cualquier duración entre ~0s y ~80s. Como el backend valida 270–330s (ver C2), **todos los bloques del móvil serán rechazados por R2**.

**Qué dice el apéndice / `:shared`:**
- Apéndice S5 §4.3 R2: `duration_seconds` entre **270 y 330** (±10% de 300).
- `com.ace.shared.constants.SyncConstants`:
  - `BLOCK_DURATION_SECONDS = 300`
  - `BLOCK_DURATION_TOLERANCE_PERCENT = 10`
  - `BLOCK_MIN_DURATION = 270`, `BLOCK_MAX_DURATION = 330`

**Acción:**
1. Eliminar las constantes locales `BLOCK_DURATION_SECONDS` y `BLOCK_DURATION_TOLERANCE_PERCENT`.
2. Importar y usar `SyncConstants.BLOCK_MIN_DURATION` y `SyncConstants.BLOCK_MAX_DURATION` en la comparación de líneas 57-64.
3. Borrar los comentarios engañosos ("5 minutos", "±10%").

**Notas:** El commit `53e6e4e "arreglando el limite de tiempo de los bloques"` parece haber introducido (o empeorado) este bug. Revisar si además de `BuildExerciseBlockUseCase` existe otro cierre de bloque en `ExerciseSyncService` con `BLOCK_DURATION_MS = 30000L` que también haya que alinear.

---

### 🔴 C2 — Backend valida duración 30–330s en vez de 270–330s

- **Severidad:** CRÍTICA
- **Bloquea a:** Hito 3
- **Archivo:** `ace-backend/src/main/kotlin/sena/adso/ace_backend/xp/service/XpSanityValidator.kt:93-94`
- **Estado:** ⬜ Pendiente

**Qué hay ahora:**
```kotlin
const val MIN_DURATION = 30      // ← debería ser 270
const val MAX_DURATION = 330
```

R2 permite bloques de 30s, absurdo fisiológicamente. Se solapa con C1: el móvil manda bloques de 20-80s y el backend los acepta hoy, pero **ambos se desvían del apéndice S5**.

**Qué dice el apéndice / `:shared`:**
- Apéndice S5 §4.3 R2: 270–330s.
- `SyncConstants.BLOCK_MIN_DURATION = 270`, `BLOCK_MAX_DURATION = 330`.

**Acción:**
1. Eliminar `MIN_DURATION` y `MAX_DURATION` del `companion object`.
2. Usar `SyncConstants.BLOCK_MIN_DURATION` y `SyncConstants.BLOCK_MAX_DURATION` en la R2 (líneas 45-48).
3. Mientras estés en el archivo, aplicar también M1 (eliminar el resto de constantes duplicadas).

---

### 🔴 C3 — `bonusMultiplier` no existe en el apéndice ni en `:shared`

- **Severidad:** CRÍTICA
- **Bloquea a:** Hito 3 (XP inconsistente móvil ↔ backend)
- **Archivos:**
  - `ace-backend/.../xp/model/XpFormula.kt:26` → campo `bonusMultiplier: Double`
  - `ace-backend/.../xp/service/XpSanityValidator.kt:86` → `minutes * formula.xpPerMinute * formula.bonusMultiplier`
  - `ace-backend/.../db/migration/V5__align_entities.sql:11` → `ADD COLUMN bonus_multiplier ... DEFAULT 1.0`
- **Estado:** ⬜ Pendiente

**Qué hay ahora:**
El backend calcula XP teórica multiplicando por `bonusMultiplier`:
```kotlin
val baseXp = (minutes * formula.xpPerMinute * formula.bonusMultiplier).toInt()
```

Pero `XpFormulaDto` de `:shared` **no tiene** `bonusMultiplier` (solo `minBpm, xpPerMinute, maxXpPerBlock, version`). Y el móvil (`CalculateBlockXpUseCase`, `BuildExerciseBlockUseCase`) **no lo usa**.

**Consecuencia:** El móvil reportará XP sin el multiplicador; el backend calculará un máximo teórico distinto → los bloques válidos pueden fallar R4 (consistencia de XP con fórmula teórica). **XP inconsistente entre móvil y backend.**

**Qué dice el apéndice / `:shared`:**
- Apéndice S5 §3.3 (fórmula RUNNING): `(duration_seconds / 60) * xp_per_minute`. **Sin multiplicador.**
- `XpFormulaDto` no lo define.

**Acción (recomendada: eliminar):**
1. Crear migración `V6__drop_bonus_multiplier.sql`: `ALTER TABLE xp_formulas DROP COLUMN IF EXISTS bonus_multiplier;`
2. Eliminar `bonusMultiplier` de `XpFormula.kt`.
3. En `XpSanityValidator.calculateMaxTheoreticalXp` (línea 86): quitar `* formula.bonusMultiplier`.
4. (Opcional) Si en el futuro se quiere un multiplicador, añadirlo a `XpFormulaDto` en `:shared` primero y luego propagar.

> **Alternativa (no recomendada para MVP):** añadir `bonusMultiplier` a `XpFormulaDto` en `:shared` 1.0.6, propagar al móvil y al cálculo. Más trabajo, más superficie de bug. El apéndice no lo contempla.

---

### 🔴 C4 — `XpTransaction` diverge de la BD (nombre y tipo)

- **Severidad:** CRÍTICA
- **Bloquea a:** Hito 3 (Hibernate `ddl-auto: validate` puede fallar al arranque)
- **Archivo:** `ace-backend/.../xp/model/XpTransaction.kt:25,28`
- **Estado:** ⬜ Pendiente

**Qué hay ahora:**
```kotlin
@Column(nullable = false)
val xpAmount: Int,              // ← la BD la llama `amount`

@Column(nullable = false)
val balanceAfter: Int,          // ← la BD es `balance_after BIGINT`
```

**Qué dice la BD (migración V2):**
```sql
amount INT NOT NULL,
balance_after BIGINT NOT NULL,
```

**Problemas:**
1. `xpAmount` no tiene `@Column(name = "amount")` → Hibernate mapea por convención a `xp_amount`, que **no existe** en la BD → fail de `validate`.
2. `balanceAfter: Int` pero la BD es `BIGINT` → si un usuario supera ~2.1 mil millones de XP (improbable pero posible a largo plazo) hay overflow. Y conceptualmente debe ser `Long`.

**Acción:**
1. Renombrar mapeo: `@Column(name = "amount", nullable = false) val xpAmount: Int`.
2. Cambiar tipo: `val balanceAfter: Long`.
3. Actualizar `XpTransactionService.recordXpTransaction` y `getCurrentBalance` (que ya retorna Int y debe retornar Long) y todos los `.toLong()` que se hicieron para compensar en `SyncBatchService` y `XpSanityValidator`.

---

### 🔴 C5 — Streak se evalúa con el primer bloque aceptado, no el último

- **Severidad:** CRÍTICA
- **Bloquea a:** Hito 3 (racha mal calculada)
- **Archivo:** `ace-backend/.../exercise/service/SyncBatchService.kt:83`
- **Estado:** ⬜ Pendiente

**Qué hay ahora:**
```kotlin
val lastAcceptedBlock = request.blocks.find { it.blockId in acceptedBlocks }
```

`find` devuelve el **primero** de la lista que cumpla. Para la racha importa el `timestamp_start` **más reciente**, no el primero.

**Qué dice el apéndice S7 §2.3:**
La evaluación compara `block_date` (truncado a día) con `last_exercise_date`. Si un batch trae bloques de distintas horas del mismo día, todos dan el mismo resultado, pero si por alguna razón hay bloques de días distintos (retrasados/offline), **el último** es el correcto para decidir si la racha sube o se rompe.

**Acción:**
```kotlin
val lastAcceptedBlock = request.blocks
    .filter { it.blockId in acceptedBlocks }
    .maxByOrNull { it.timestampStart }
```

---

## 3. Deuda MENOR (no bloquea Hito 3 pero conviene corregir)

### 🟡 M1 — `XpSanityValidator` duplica constantes que ya están en `:shared`

- **Archivo:** `ace-backend/.../xp/service/XpSanityValidator.kt:90-98`
- **Estado:** ⬜ Pendiente

**Qué hay ahora:**
```kotlin
companion object {
    const val MIN_BPM = 30.0
    const val MAX_BPM = 250.0
    const val MIN_DURATION = 30        // (ver C2)
    const val MAX_DURATION = 330
    const val SAMPLE_TOLERANCE_PERCENT = 20
    const val MAX_XP_PER_BLOCK = 50    // ← contradice el seed (max=30 para RUNNING)
    const val XP_TOLERANCE = 5
}
```

**Qué dice `:shared`:** `XpConstants.MIN_BPM_PHYSIOLOGICAL = 30`, `MAX_BPM_PHYSIOLOGICAL = 250`.

**Problemas:**
1. Duplicación: `MIN_BPM`/`MAX_BPM` ya están en `XpConstants`.
2. `MAX_XP_PER_BLOCK = 50` es arbitrario y **contradice el seed** (V4: `max_xp_per_block=30` para RUNNING). R4 rechazará bloques que R5 (validación contra fórmula) ya cubre.

**Acción:**
1. Importar `XpConstants` y reemplazar `MIN_BPM`/`MAX_BPM`.
2. **Eliminar R4** (líneas 59-63). R5 (validación contra fórmula) ya valida el techo correctamente usando `formula.maxXpPerBlock`.
3. Eliminar `MAX_XP_PER_BLOCK` y `XP_TOLERANCE` (este último quedaría solo si R5 se queda; revisar).

---

### 🟡 M2 — R3 (sample_count) demasiado estricta para Bluetooth intermitente

- **Archivo:** `ace-backend/.../xp/service/XpSanityValidator.kt:50-57`
- **Estado:** ⬜ Pendiente

**Qué hay ahora:** exige `sample_count ≈ duration_seconds ± 20%` (densidad 1Hz estricta).

**Problema:** el apéndice S5 dice "coherente" sin fijar 1Hz. El reloj captura a ~1Hz pero con desconexiones Bluetooth (que el propio diseño contempla: timeout 5s), el móvil puede tener menos muestras. **Esto rechazará bloques válidos en condiciones reales.**

**Acción:** relajar a densidad ≥ 0.5Hz:
```kotlin
val minExpectedSamples = block.durationSeconds / 2
if (block.sampleCount < minExpectedSamples) {
    return ValidationResult.Invalid(block.blockId, "sampleCount too low: ...")
}
```
O eliminar la regla en MVP y dejar solo R1, R2, R5.

---

### 🟡 M3 — `OfficialStatsDto` no es realmente oficial

- **Archivo:** `ace-backend/.../exercise/service/SyncBatchService.kt:104-116`
- **Estado:** ⬜ Pendiente (se cierra en Hito 4)

**Qué hay ahora:** el backend copia `totalSessions` y `avgBpmAllTime` del `clientStats` del móvil. Solo `totalXp` y `totalDurationSeconds` se calculan de bloques aceptados. **No hay recálculo desde la BD.**

**Qué dice el apéndice S10 §4.2:** el backend debe recalcular desde `xp_transactions` y bloques SYNCED, y aplicar corrección si difiere (>10 XP → notificación).

**Acción (en Hito 4):** implementar `StatsValidationService` que lea de la BD, no que confíe en `clientStats`. Marcar como TODO ahora.

---

### 🟡 M4 — `XpFormulaDto.version` siempre devuelve 1

- **Archivos:**
  - `ace-backend/.../xp/service/FormulaService.kt:24` → `version = 1` hardcoded
  - `ace-backend/.../xp/controller/XpController.kt` → no añade header `X-Formula-Version`
- **Estado:** ⬜ Pendiente

**Qué hay ahora:** el backend siempre devuelve `version = 1` sin leer `formula.version` de la BD. El móvil no puede detectar fórmula desactualizada.

**Qué dice el apéndice S5 §2.3 y `:shared`:**
- Header `X-Formula-Version` en respuestas para que el móvil detecte versión vieja.
- `XpConstants.FORMULA_VERSION_HEADER = "X-Formula-Version"`.

**Acción:**
1. En `FormulaService.getActiveFormulas()`: leer `formula.version` de la BD (campo ya existe en `XpFormula`? → **verificar**, si no existe añadirlo).
2. En `XpController.getActiveFormulas()`: calcular la `max(version)` de las fórmulas y añadir `response.header(XpConstants.FORMULA_VERSION_HEADER, maxVersion.toString())`.

---

### 🟡 M5 — Ruta del endpoint diverge entre `ApiEndpoints` y el controller

- **Archivos:**
  - `ace-backend/.../exercise/controller/ExerciseController.kt:18,30` → `@RequestMapping("/api/exercise")` + `@PostMapping("/batch")` = `/api/exercise/batch`
  - `ace-shared/.../constants/ApiEndpoints.kt` → `EXERCISE_BLOCKS = "/api/exercise/blocks"`
  - `ace-mobile/.../data/remote/api/ExerciseApi.kt` → (verificar qué ruta usa)
- **Estado:** ⬜ Pendiente (verificar)

**Qué hay ahora:** el controller expone `/api/exercise/batch`, pero `ApiEndpoints.EXERCISE_BLOCKS` dice `/api/exercise/blocks`. Si el `ExerciseApi` del móvil usa `ApiEndpoints.EXERCISE_BLOCKS`, el sync **fallará con 404**.

**Acción:**
1. Verificar `ExerciseApi.kt` del móvil: ¿usa `/api/exercise/blocks` o `/api/exercise/batch`?
2. Unificar: recomiendo cambiar el controller a `@PostMapping("/blocks")` (ruta plural REST, coherente con `ApiEndpoints`).
3. O cambiar `ApiEndpoints.EXERCISE_BLOCKS` a `/api/exercise/batch`. Lo importante es que **ambos lados coincidan**.

---

### 🟡 M6 — Bump `ace-shared` 1.0.4 → 1.0.5

- **Archivos:**
  - `ace-backend/gradle/libs.versions.toml`
  - `ace-mobile/gradle/libs.versions.toml`
- **Estado:** ⬜ Pendiente

`ace-shared` local está en 1.0.5 con los DTOs S3/S5/S6/S7 ya creados (`SyncBatchRequestDto`, `ExerciseBlockDto`, `XpFormulaDto`, `ClientStatsDto`, `OfficialStatsDto`, `StreakStateDto`, `RankingResponseDto`, `XpAwardedResponseDto`, `RejectedBlockDto`). Pero los `libs.versions.toml` de backend y mobile pueden seguir en 1.0.4 (verificar).

**Acción:**
1. Confirmar la versión actual en ambos `libs.versions.toml`.
2. Subir a `1.0.5`.
3. Asegurarse de que el tag `v1.0.5` está pusheado a GitHub y JitPack lo compiló en verde (https://jitpack.io/#reinaldojperalta/ace-shared).

---

### 🟡 M7 — `XpController.getActiveFormulas()` no declara `userId`

- **Archivo:** `ace-backend/.../xp/controller/XpController.kt:24`
- **Estado:** ⬜ Pendiente (verificar)

El método no recibe `@AuthenticationPrincipal`. El apéndice S5 exige JWT. El `SecurityConfig` protege la ruta implícitamente, pero el filtro setea `userId` en el `SecurityContext`; si el método no lo lee, no hay forma de personalizar fórmulas por usuario en el futuro. Para MVP no es bloqueante (fórmulas globales), pero documentar.

---

## 4. Lo que está ALINEADO (no tocar)

- **`SyncBatchRequestDto` / `SyncBatchResponseDto`** (`:shared` 1.0.5) coinciden con el uso real en backend y mobile (`xpDetails`, `streakState`, `officialStats`).
- **Idempotencia** en `BlockPersistenceService.persistIfNotExists` (con `existsByBlockId`) ✅.
- **Batch ≤ 20** validado en `ExerciseController` (usando `SyncConstants.BATCH_MAX_SIZE`) ✅.
- **Estados PENDING/SYNCING/SYNCED/ERROR** en `SendPendingBlocksUseCase` ✅.
- **Backoff exponencial** en `SyncBlockWorker` (usando `SyncConstants.RETRY_DELAY_INITIAL_MS`) ✅.
- **Auth con `userId` del JWT** verificado en `ExerciseController` (coincidencia JWT ↔ bloques) ✅.
- **`XpAwardedResponseDto`** con detalle por bloque ✅.
- **`RejectedBlockDto`** con `reason` y `ruleViolated` ✅.

---

## 5. Plan de acción recomendado

Orden por dependencia. **C1–C5 son pre-requisito del Hito 3.**

| Orden | Fix | Severidad | Archivo principal | Dificultad |
|---|---|---|---|---|
| 1 | **M6** Bump `:shared` 1.0.5 en backend + mobile | 🟡 | `libs.versions.toml` x2 | Trivial |
| 2 | **C1** Constantes duración corruptas | 🔴 | `BuildExerciseBlockUseCase.kt` | Fácil |
| 3 | **C2 + M1** Validador backend (270-330, sin duplicados) | 🔴 | `XpSanityValidator.kt` | Fácil |
| 4 | **C3** Eliminar `bonusMultiplier` | 🔴 | `XpFormula.kt`, `XpSanityValidator.kt`, V6 | Media (migración) |
| 5 | **C4** `XpTransaction` nombre + tipo | 🔴 | `XpTransaction.kt`, `XpTransactionService.kt` | Media |
| 6 | **C5** Streak con último bloque | 🔴 | `SyncBatchService.kt:83` | Trivial |
| 7 | **M5** Unificar ruta endpoint | 🟡 | `ExerciseController.kt` + `ExerciseApi.kt` | Trivial |
| 8 | **M2** Relajar R3 (sample_count) | 🟡 | `XpSanityValidator.kt` | Fácil |
| 9 | **M4** `version` real + header | 🟡 | `FormulaService.kt`, `XpController.kt` | Fácil |

Una vez cerrados los 6 primeros, el Hito 3 (S6 Ranking + S7 Streaks) tiene base limpia.

---

## 6. Verificación final (checklist)

Tras aplicar los fixes:

- [ ] `ace-backend`: `./gradlew compileKotlin` pasa sin errores.
- [ ] `ace-mobile`: `./gradlew :app:assembleDebug` pasa sin errores.
- [ ] Backend arranca con `ddl-auto: validate` sin errores de mapeo (verifica C4).
- [ ] `GET /api/xp/formulas` devuelve `version` real y header `X-Formula-Version`.
- [ ] `POST /api/exercise/batch` (o `/blocks`) acepta un bloque de 300s ±10% y rechaza uno de 30s.
- [ ] Un bloque generado por el móvil (usando `BuildExerciseBlockUseCase`) pasa todas las R1-R5 del backend.
- [ ] XP móvil = XP backend para el mismo bloque (sin `bonusMultiplier`).
- [ ] Tras sync con bloques de horas distintas, `StreakStateDto` refleja el día del bloque más reciente.
- [ ] `ace-shared` 1.0.5 resuelto en ambos `libs.versions.toml`.

---

## 7. Apéndice — Referencias

- **Apéndice S3 (Sync):** `CONTEXT/ACE_APPENDIX_S3_Sync.md` — batch ≤20, idempotencia, estados, backoff.
- **Apéndice S5 (XP):** `CONTEXT/ACE_APPENDIX_S5_XP.md` — fórmula RUNNING, reglas R1-R5, header `X-Formula-Version`.
- **Apéndice S7 (Streaks):** `CONTEXT/ACE_APPENDIX_S7_Streaks.md` — evaluación por día.
- **`:shared` constantes:** `com.ace.shared.constants.SyncConstants`, `XpConstants`, `ApiEndpoints`.
- **Plan Shared:** `CONTEXT/IMPLEMENTATION_PLAN_SHARED_v4.2.md`.
- **Manual JitPack:** `CONTEXT/MANUAL_JITPACK_EQUIPO.md`.

---

*Documento de deuda técnica generado por auditoría del ecosistema A.C.E. Versión 1.0 — 2026-06-19. Cualquier fix aplicado debe marcarse como `✅ Hecho` con la fecha y commit correspondiente.*
