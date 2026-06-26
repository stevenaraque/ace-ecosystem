# A.C.E — Deuda Técnica (Registro Único)

> **Versión:** 2.0
> **Fecha:** 2026-06-25
> **Tipo:** Registro único de deuda técnica del ecosistema (`ace-backend`, `ace-mobile`, `ace-wear`, `:shared`)
> **Origen:** Auditoría integral del código real contrastada contra los Apéndices S1–S10. Verificación `file:line` por ítem.
> **Reemplaza a:** `DEUDA_TECNICA_S3_S5_v1.md` (2026-06-19), `DEUDA_TECNICA_S6_S10_v1.md` (2026-06-24) y `HANDOFF_CLEANUP_v1.md` (2026-06-19), que quedan **SUPERSEDED** (retenidos solo como historial).

---

## 0. Cómo leer este documento

Cada ítem tiene:
- **ID** (`F#` = feature, `C#` = deuda crítica, `M#` = deuda menor, `D#` = deuda nueva detectada en esta auditoría).
- **Estado** (`✅ Hecho` / `⬜ Pendiente` / `⚪ Fuera de alcance`), con evidencia `file:line`.
- **Módulo** afectado.

**Lectura clave:** la auditoría de 2026-06-25 encontró que **la gran mayoría de la deuda documentada en las versiones v1 ya estaba implementada en el código**. Los documentos v1 marcaban los ítems como `⬜ Pendiente`, pero el código los resuelve (con comentarios `C3 FIX`, `C5 FIX`, `M1`, `M2`, `M4` que lo confirman). Este documento corrige el desfase.

**Decisiones del equipo (NO cambiar):**
- ✅ JWT único por dispositivo = desalojo en login/register (F7).
- ✅ Auto-pausa por FC solo en el móvil; el reloj sigue midiendo (F6).
- ✅ Bloque de **60s** (`BLOCK_DURATION_SECONDS = 60`, `MIN_DURATION = 10`) como decisión consciente. **No revertir a 270–330.** Las deudas C1/C2/M5 quedan cerradas por decisión del equipo.
- ✅ Ruta de sync unificada a `/api/exercise/batch`.
- ✅ Credenciales hardcodeadas en `application.yml` (proyecto académico).
- ✅ `:shared` híbrido en wear: `WearMessageClient` usa `DataLayerPaths` de `:shared`; `WearDataClient` usa `WearDataLayerPaths` local. No unificar.

---

## 1. Resumen ejecutivo (estado real a 2026-06-25)

| Sistema | Avance real | Veredicto |
|---|---|---|
| **S1 Captura** (wear+mobile) | ✅ ~100% | Health Services + DataClient + `WearDataSource`/`WearDataListenerService` funcionales |
| **S2 Sesión** (mobile+wear) | ✅ ~95% | Captura, cierre de bloque, persistencia, **pausa manual + auto-pausa por FC** implementadas |
| **S3 Sync** | ✅ ~95% | Batch, idempotencia, backoff, estados PENDING/SYNCED/ERROR |
| **S4 Auth / JWT** | ✅ ~100% | Login/register/refresh + **desalojo en login (JWT único)** |
| **S5 XP** | ✅ ~95% | Cálculo móvil, validación backend, sin `bonusMultiplier`, `XpTransaction` con mapeo correcto |
| **S6 Ranking** | ✅ ~90% | Recálculo batch horario + rango; municipal funcional vía `cityId`; auto-refresh UI |
| **S7 Streaks** | ✅ ~95% | Evaluación con último bloque aceptado |
| **S8 Notificaciones** | 🟡 ~60% | Foreground service; recordatorio/error pendientes de verificar |
| **S9 Historial** | ✅ ~90% | FIFO 5 sesiones local |
| **S10 Perfil/Stats** | 🟡 ~80% | Editar perfil + reconciliación; **M3 pendiente** (stats no recalculadas de BD) |
| **Crear cuenta (UI)** | ✅ ~100% | `RegisterScreen` + wiring completo |
| **Fórmulas deportes** | ✅ ~100% | RUNNING/CYCLING/WALKING con seed + selector en móvil |

**Conclusión:** el ecosistema está **~85–90% implementado**. La deuda técnica real pendiente es **mínima** (1 ítem crítico M3 + limpieza de código muerto). El resto del esfuerzo es pulido, verificación y S8.

---

## 2. Tabla maestra de ítems

### Features (F1–F7) — TODAS implementadas

| ID | Descripción | Módulo | Estado | Evidencia |
|---|---|---|---|---|
| **F1** | Editar perfil (backend) | backend | ✅ Hecho | `user/controller/ProfileController.kt` (GET/PUT `/api/profile`), `user/service/ProfileService.kt` completo con lazy-init |
| **F2** | Editar perfil (mobile) | mobile | ✅ Hecho | `feature/profile/data/UserApi.kt`, `UserRepository.kt`; `ProfileScreen.kt` con form editable + Save; `UserRepository.kt:29,55` llama `setCityId` |
| **F3** | Crear cuenta (mobile) | mobile | ✅ Hecho | `feature/auth/presentation/RegisterScreen.kt` + `RegisterViewModel.kt`; `LoginScreen.kt:309` cableado; `MainActivity.kt:67` ruta `register_screen_route` |
| **F4** | Ranking municipal + auto-refresh | mobile | ✅ Hecho | `RankingViewModel.kt:76` `refresh()`, `:44` reacciona a `invalidateSignal`, `:87` `onScreenVisible()`; `RankingScreen.kt:33` ON_RESUME + botón refresh |
| **F5** | Fórmulas deportes + selector | backend+mobile | ✅ Hecho | `V10__backend_deuda_tecnica_s3_s10.sql` siembra CYCLING/WALKING; `SessionScreen.kt:143` selector `SportType.entries` |
| **F6** | Pausa manual + auto-pausa por FC | mobile | ✅ Hecho | `PauseSessionUseCase.kt`/`ResumeSessionUseCase.kt` implementados; `SessionUiState.kt:9` `Paused(isAutoPaused)`; `SessionViewModel.kt:31` `PAUSE_BPM_THRESHOLD=110`, `:198` watcher FC |
| **F7** | JWT único (desalojo en login) | backend | ✅ Hecho | `auth/service/AuthService.kt:35` (login), `:68` (register) llaman `revokeAllUserTokens` |

### Deuda crítica heredada (C1–C5) — TODA resuelta o cerrada

| ID | Descripción | Módulo | Estado | Evidencia |
|---|---|---|---|---|
| **C1** | Constantes duración corruptas en móvil | mobile | ✅ Cerrada (decisión equipo) | Bloques de 60s; `MIN_DURATION=10`. No revertir a 270–330 |
| **C2** | Backend valida 30–330s | backend | ✅ Cerrada (decisión equipo) | `XpSanityValidator.kt:85` `MIN_DURATION=10`, `:86` `MAX_DURATION=330` |
| **C3** | `bonusMultiplier` no en apéndice/`:shared` | backend | ✅ Hecho | `XpFormula.kt` sin `bonusMultiplier`; `XpSanityValidator.kt:78` comentario "C3 FIX: eliminado bonusMultiplier"; V10 lo dropea |
| **C4** | `XpTransaction` diverge de BD (nombre+tipo) | backend | ✅ Hecho | `XpTransaction.kt:24` `@Column(name="xp_amount")`, `:27` `@Column(name="balance_after")`, `:28` `balanceAfter: Long` |
| **C5** | Streak con primer bloque, no último | backend | ✅ Hecho | `SyncBatchService.kt:82-84` `.maxByOrNull { it.timestampStart }` con comentario "C5 FIX" |

### Deuda menor (M1–M7) — Casi toda resuelta

| ID | Descripción | Módulo | Estado | Evidencia |
|---|---|---|---|---|
| **M1** | Validador duplica constantes + R4 redundante | backend | ✅ Hecho | `XpSanityValidator.kt:53` "R4 ELIMINADO"; usa `XpConstants`; sin `MAX_XP_PER_BLOCK` |
| **M2** | R3 (sample_count) demasiado estricta | backend | ✅ Hecho | `XpSanityValidator.kt:47` relajado a `≥0.5Hz` (`durationSeconds / 2`) |
| **M3** | `OfficialStatsDto` no recalcula de BD | backend | ⬜ **Pendiente** | `SyncBatchService.kt:131,134` copia `totalSessions`/`avgBpmAllTime` de `clientStats` |
| **M4** | `XpFormulaDto.version` siempre 1 | backend | ✅ Hecho | `XpFormula.kt:32` campo `version`; `FormulaService.kt` retorna maxVersion; `XpController.kt` añade header `X-Formula-Version` |
| **M5** | Ruta endpoint diverge | backend+mobile | ✅ Hecho | Unificada a `/api/exercise/batch` |
| **M6** | Bump `:shared` | todos | ✅ Hecho | Backend y mobile consumen `:shared` **1.0.9** (backend `build.gradle.kts:29`, mobile `libs.versions.toml:25`) |
| **M7** | `XpController` sin userId | backend | ⚪ Documentada | No bloqueante (fórmulas globales en MVP) |

### Deuda nueva detectada en esta auditoría (D1–D6)

| ID | Descripción | Módulo | Estado | Acción |
|---|---|---|---|---|
| **D1** | `SessionRepositoryImpl.kt` placeholder en RAM (DI-bound, sin consumidores) | mobile | ⬜ Pendiente | Eliminar impl + interfaz + binding DI. Los ViewModels usan `BlockRepository` |
| **D2** | 5 stubs Kotlin de 0 bytes (código muerto) | mobile | ⬜ Pendiente | `LoginUseCase`, `RefreshTokenUseCase`, `ExerciseRepository`, `AccumulateStatsUseCase`, `WearSyncRepository`, `DataStoreModule` (6 archivos) |
| **D3** | 4 configs backend a 0 bytes | backend | ⬜ Pendiente | `DatabaseConfig`, `JacksonConfig`, `JwtConfig`, `WebConfig`. ⚠️ `SchedulingConfig` **NO borrar** (tiene `@EnableScheduling`, crítico para S6) |
| **D4** | 2 tests backend vacíos | backend | ⬜ Pendiente | `AceBackendApplicationTests.kt`, `SupabaseAuthIntegrationTest.kt` |
| **D5** | `version="1.0.4"` stale en `build.gradle.kts` de `:shared` | shared | ⬜ Pendiente | Bump a `1.0.9` (tags llegan a 1.0.9) |
| **D6** | `WearDataListenerService.kt` activo en Manifest | mobile | ⚪ Requiere verificación manual | Está `enabled="true"`. El HANDOFF decía "comentado" pero **no lo está**. Verificar si duplica `WearDataSource` antes de eliminar |
| **D7** | Botón "¿Olvidaste tu contraseña?" con `onClick={}` | mobile | ⚪ Fuera de alcance | `LoginScreen.kt:248`. Feature nueva (recuperar contraseña) |
| **D8** | `ace-shared` `SyncConstants.BLOCK_DURATION_*` | shared | ⚪ Documentada | El plan v4.2 dice `BLOCK_DURATION_SECONDS=300`; código tiene 60 y la matemática de MIN/MAX produce 0..306. No se usa en mobile/backend (decisión equipo 60s), pero el contrato está desalineado con el texto. Revisar cuando se toque `:shared` |

---

## 3. Plan de acción (lo único que falta)

Ordenado por dependencia y dificultad.

| Orden | Ítem | Severidad | Dificultad | Estado |
|---|---|---|---|---|
| 1 | **D5** Bump `:shared` 1.0.4 → 1.0.9 en `build.gradle.kts` | 🟡 | Trivial | ⬜ |
| 2 | **D2** Eliminar 6 stubs de 0 bytes del mobile | 🟡 | Trivial | ⬜ |
| 3 | **D1** Eliminar `SessionRepositoryImpl` + interfaz + DI binding | 🟡 | Fácil | ⬜ |
| 4 | **D3** Eliminar 4 configs vacíos del backend (NO `SchedulingConfig`) | 🟡 | Trivial | ⬜ |
| 5 | **D4** Eliminar/rellenar 2 tests vacíos del backend | 🟡 | Trivial | ⬜ |
| 6 | **M3** `OfficialStatsDto` recalculado de BD (S10 reconciliación) | 🟡 | Media | ⬜ |
| 7 | **D6** Verificar si `WearDataListenerService` duplica `WearDataSource` | 🟡 | Investigación | ⬜ |
| 8 | **D8** Alinear `SyncConstants.BLOCK_*` en `:shared` con la decisión 60s | 🟢 | Trivial | ⬜ |
| 9 | **D7** Pantalla "recuperar contraseña" (feature nueva) | 🟢 | Media | ⬜ |

> Los ítems 1–5 son **código muerto/stale** (limpieza, sin cambio de comportamiento). El 6 es la única deuda funcional real. El 7 requiere investigación. 8–9 son opcionales.

---

## 4. Verificación de coherencia (checklist)

- [ ] `:shared` 1.0.9 resuelto en backend (`build.gradle.kts:29`) y mobile (`libs.versions.toml:25`). ✅ Confirmado en auditoría.
- [ ] `bonusMultiplier` no aparece en `XpFormula.kt` ni en `XpSanityValidator.kt`. ✅ Confirmado.
- [ ] `XpTransaction` arranca con `ddl-auto: validate` (`@Column(name="xp_amount")`, `balance_after` BIGINT). ✅ Confirmado.
- [ ] `AuthService.login`/`register` llaman `revokeAllUserTokens`. ✅ Confirmado.
- [ ] `SyncBatchService` usa `.maxByOrNull { it.timestampStart }` para la racha. ✅ Confirmado.
- [ ] Stubs de 0 bytes y configs vacíos eliminados tras la limpieza. ⬜ Pendiente (Fase B).
- [ ] `OfficialStatsDto` calcula de `user_stats`/`xp_transactions` (M3). ⬜ Pendiente.
- [ ] `WearDataListenerService`: confirmar rol real vs `WearDataSource`. ⬜ Pendiente.

---

## 5. Apéndice — Referencias

- **Apéndices S1–S10:** `CONTEXT/ACE_APPENDIX_S1...S10_*.md` (contratos aprobados, fuente de verdad conceptual).
- **Arquitectura:** `CONTEXT/ACE_SYSTEMS_ARCHITECTURE_v0.4.md`.
- **Planes:** `CONTEXT/IMPLEMENTATION_PLAN_{BACKEND,MOBILE,WEAROS,SHARED}_v*.md`.
- **Historial de deuda (SUPERSEDED):** `DEUDA_TECNICA_S3_S5_v1.md`, `DEUDA_TECNICA_S6_S10_v1.md`, `HANDOFF_CLEANUP_v1.md`.

---

*Documento único de deuda técnica del ecosistema A.C.E. Versión 2.0 — 2026-06-25. Generado tras auditoría de código. Los ítems cerrados se conservan con evidencia para trazabilidad; los pendientes son los únicos que requieren acción.*
