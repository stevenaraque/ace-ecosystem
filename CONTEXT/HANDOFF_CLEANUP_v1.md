# A.C.E — Handoff: Limpieza y Reorganización del Ecosistema

> ⛔ **SUPERSEDED — 2026-06-25.** Las tareas de limpieza pendientes de este handoff (Bloques 2.1–2.4, 3, 4, 5) se re-auditaron contra el código. Estado real: Bloque 3 (cierre de bloque) y Bloque 4 (userId) **ya están hechos**; los Bloques 2.1–2.4 (limpieza de configs vacíos, jar huérfano, tests vacíos) y Bloque 5 (WearDataListenerService) se consolidaron en **`CONTEXT/DEUDA_TECNICA_v2.md`** como ítems D1–D6. Este archivo se retiene solo como historial.

> **Versión:** 1.2
> **Fecha:** 2026-06-19
> **Tipo:** Tareas de refactor + limpieza + documentación
> **Origen:** Análisis de coherencia realizado el 2026-06-19 entre el código real y los planes de `CONTEXT/`.
> **Auditoría previa:** Este documento se generó tras indexar los 3 proyectos (`ace-backend`, `ace-mobile`, `ace-wear`) y cotejarlos con los planes v4.1/v4.2 y los Apéndices S1–S10.
> **v1.1 → v1.2:** Bloque 6 (wear) marcado COMPLETADO tras pull de un compañero. Decisión final: `:shared` **hibrido** en wear (no unificar).

---

## 0. Cómo usar este documento

Este es un documento **delegable**. Cada tarea tiene:
- **Archivo(s)** afectados con rutas exactas.
- **Qué hay ahora** (estado real verificado leyendo el código).
- **Qué debe quedar** (objetivo).
- **Notas del humano** (decisiones ya tomadas por el equipo, no negociables).

**Reglas de ejecución:**
1. Ejecuta las tareas en orden (están ordenadas por dependencia).
2. Antes de cada bloque, lee los archivos citados para confirmar que el estado coincide.
3. **No toques** los Apéndices S1–S10, la Arquitectura v0.3, ni la Guía de Conexión v1.0 — están al día.
4. Tras cada bloque, deja el proyecto en estado compilable.
5. Al terminar, actualiza la cabecera de cada plan modificado (versión + fecha + nota de cambio).

**Decisiones del equipo ya tomadas (NO cambiar):**
- ✅ Backend: estructura **por-feature** es la deseada.
- ✅ Credenciales hardcodeadas en `application.yml` **se quedan** (efectos prácticos del proyecto académico). No mover a `.env`.
- ✅ **Dockerfile se CONSERVA** — se usa para despliegue en Render. NO borrar.
- ✅ **Wear consume `:shared` en modo HIBRIDO** (decisión final del equipo): `WearMessageClient` usa `DataLayerPaths` de `:shared`; `WearDataClient` usa `WearDataLayerPaths` local. **No unificar** — la sincronización de paths entre ambos es manual e intencional.
- ✅ Los archivos `nul` **no se pueden eliminar** (residuo de una sesión previa con cuota agotada). No insistir; ya están en `.gitignore`.
- ✅ El `reporte_estado_auth.md` ya fue corregido y consolidado en `CONTEXT/`.

---

## BLOQUE 1 — ✅ COMPLETADO: Migración de Auth a estructura por-feature

> **Estado:** Ya ejecutado. Los archivos están migrados y los del layout plano están borrados del filesystem.
> **Git status:** Los archivos viejos aparecen como `D` (deleted) en el index. La carpeta `auth/` aparece como `??` (untracked). Hay que hacer `git add` de la carpeta nueva y `git rm` de los archivos viejos, o simplemente `git add -A` desde `ace-backend/`.

### Lo que se hizo
- Auth migrado a `auth/controller/`, `auth/model/`, `auth/repository/`, `auth/service/` con packages correctos (`sena.adso.ace_backend.auth.*`).
- `JwtAuthenticationFilter` se quedó en `security/` (infraestructura transversal).
- `SecurityConfig` se quedó en `config/`.
- Layout plano eliminado (`controller/`, `service/`, `repository/`, `domain/`, `dto/`, `exception/`, `shared/`, `scheduler/`).
- `Application.kt` duplicado eliminado.
- `CustomUserDetailsService.kt` vacío eliminado.

### Lo que resta por verificar
- [ ] Confirmar que los tests (`src/test/.../service/auth/*`) compilaban con los imports actualizados (fueron actualizados parcialmente — verificar).
- [ ] Confirmar que `config/SecurityConfig.kt` importa `auth.repository.UserRepository` y `security.JwtAuthenticationFilter` correctamente.

---

## BLOQUE 2 — Backend: limpieza de artefactos muertos

> **NOTA:** El Dockerfile NO se toca (se usa para Render). Ver Bloque 2.5 para mejoras del Dockerfile.

### 2.1 Eliminar artefactos muertos
- `ace-backend/libs/ace-shared-1.0.0.jar` → **borrar** (jar huérfano, 420 bytes, no referenciado en el build — se usa JitPack 1.0.4).
- `ace-backend/nul` → **NO borrar** (no se puede).
- `nul` (raíz del repo) → **NO borrar**.

### 2.2 Verificar que no existen docker-compose
- Buscar `docker-compose*.yml` en `ace-backend/` y raíz → **borrar** si existen (no se usan, solo el Dockerfile).

### 2.3 Limpieza de configs vacíos
Los siguientes archivos en `config/` están vacíos (0 bytes) y no aportan valor:
- `config/DatabaseConfig.kt`
- `config/JacksonConfig.kt`
- `config/JwtConfig.kt`
- `config/SchedulingConfig.kt`
- `config/WebConfig.kt`

**Acción:** borrarlos. Solo conservar `config/SecurityConfig.kt` (funcional).

### 2.4 Tests vacíos del backend
- `AceBackendApplicationTests.kt` → vacío (sin `@Test`). Borrar o añadir un test trivial `@Test fun contextLoads()`.
- `integration/SupabaseAuthIntegrationTest.kt` → vacío. Borrar.

### 2.5 ✅ COMPLETADO: Mejoras del Dockerfile

> **Estado:** Ya ejecutado. Cambios aplicados al `ace-backend/Dockerfile`.

**Lo que se hizo:**
1. **Eliminada la descarga manual de Gradle 8.14.** Ahora usa el **Gradle Wrapper del proyecto** (`gradlew` + `gradle/wrapper/`), que respeta la versión 9.5.1 del `gradle-wrapper.properties`. Builds reproducibles con local.
2. **`gradlew` commiteado en git** con permiso `+x` (`100755`). Se añadió una excepción en `.gitignore` (`!ace-backend/gradlew` y `!ace-backend/gradlew.bat`) para que el wrapper del backend llegue a Render, mientras mobile/wear siguen ignorados (Android Studio los regenera).
3. **Runtime cambiado de Alpine a jammy (Debian):** mejor resolución DNS con Supabase/JitPack en entornos cloud.
4. **Añadido `SPRING_PROFILES_ACTIVE=prod`** con el nuevo `application-prod.yml`.
5. **Tuning de JVM para contenedores:** `-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -XX:+UseContainerSupport`.
6. **Precaché de dependencias:** capa separada de COPY de build files + `dependencies || true` antes de copiar src.

### 2.6 ✅ COMPLETADO: `application-prod.yml` creado

> **Estado:** Ya creado en `ace-backend/src/main/resources/application-prod.yml`.

**Contenido clave:**
- Mismas credenciales de Supabase (decisión del equipo).
- `hikari.maximum-pool-size: 3` (optimizado para Render free/starter con RAM limitada).
- `ddl-auto: validate` (Flyway maneja schema).
- Logging en WARN/INFO (no DEBUG como en default).
- Actuator solo expone `health` e `info` (`show-details: never`).
- `PORT` por variable de entorno (Render la inyecta).

---

## BLOQUE 3 — Mobile: completar el cierre de bloque en `ExerciseSyncService`

### Contexto
`ace-mobile/app/src/main/kotlin/com/ace/mobile/service/ExerciseSyncService.kt`, método `checkBlockClosure()`, líneas **354–356**, hay 3 TODOs:

```kotlin
// TODO: Guardar bloque en SQLite usando BlockDao
// TODO: Calcular XP usando CalculateBlockXpUseCase
// TODO: Actualizar estadisticas usando AccumulateStatsUseCase
```

El bloque se cierra (se vacía el buffer y se loguea) pero los datos se **descartan**. Esta es la cadena crítica rota del ecosistema.

### 3.1 Qué ya existe (no hay que crear)
- `BlockDao` (`dao/BlockDao.kt`): `insert(block)`, `update(block)`, `getBlocksBySession(sessionId)`, `updateBlockStatus(blockId, status)`.
- `LocalBlockEntity` (`entity/LocalBlockEntity.kt`): campos `blockId, sessionId, userId, timestampStart, timestampEnd, durationSeconds, avgBpm, maxBpm, minBpm, sampleCount, sportType, xpCalculated, status`.
- `DatabaseModule` ya provee `BlockDao` vía Hilt.
- `BuildExerciseBlockUseCase` (inyectado en el servicio, línea 74).

### 3.2 Qué falta implementar

**Paso A — Persistir el bloque en Room.** Dentro del `sessionScope?.launch(Dispatchers.IO)` en `checkBlockClosure()`:
1. Calcular `avgBpm`, `maxBpm`, `minBpm`, `sampleCount` a partir de `samplesForBlock` (ya están en el log, reusarlos).
2. Generar `blockId` (UUIDv4) — verificar si `BuildExerciseBlockUseCase` ya lo hace; si no, usar `java.util.UUID.randomUUID().toString()`.
3. Construir `LocalBlockEntity(...)` con `status = BlockStatus.PENDING`.
4. Llamar `blockDao.insert(entity)`.

> **Pendiente del humano:** decidir si el bloque se construye con `BuildExerciseBlockUseCase` (que ya está inyectado y devuelve un DTO/objeto de dominio) y luego se mapea a `LocalBlockEntity`, o se construye la entity directo. **Recomendado:** usar el use case para no duplicar la lógica de validación de duración (270–330s), y mapear su salida a la entity.

**Paso B — Calcular XP.** `CalculateBlockXpUseCase` existe pero está **vacío** (0 bytes). Opciones:
- Implementar el cálculo básico de XP para RUNNING (único deporte del MVP) según Apéndice S5.
- O dejar XP como `null` por ahora y solo persistir el bloque (mínimo viable para desbloquear S3).
- **Recomendado para este handoff:** dejar `xpCalculated = null` y marcar con un TODO claro que remita al Apéndice S5, para no mezclar alcance. El objetivo de este handoff es **persistir el bloque**, no implementar XP.

**Paso C — Stats.** `AccumulateStatsUseCase` también vacío. **Dejar fuera de este handoff** (mismo criterio: un TODO que remita a S10).

### 3.3 Inyección de `BlockDao` en el servicio

`ExerciseSyncService` es un `@AndroidEntryPoint` Service. Añadir al bloque de `@Inject` existente (líneas 70–74):
```kotlin
@Inject
lateinit var blockDao: BlockDao
```
Confirmar que `DatabaseModule` provee `BlockDao` (según la auditoría, sí lo hace).

### 3.4 Bloque final al detener la sesión

`stopSessionInternal()` (líneas 302–313) también cierra el buffer final pero **no persiste**. Aplicar la misma lógica del Paso A ahí: si `_buffer` no está vacío al detener, persistir como un bloque final antes de emitir el `SessionStoppedEvent`.

### 3.5 Mapeo de tipos

`HeartRateSample` (dominio, `domain/model/HeartRateSample.kt`) tiene `bpm: Double, timestamp: Long`. Para los cálculos: `samples.map { it.bpm }.average()` para avg, `.maxOrNull()`, `.minOrNull()`, `.size` para count.

### 3.6 Verificación
- La app compila.
- Al iniciar una sesión y dejar correr >30s (BLOCK_DURATION_MS), un bloque se persiste en Room (verificable con Database Inspector o un log que confirme el `blockId` insertado).
- Al detener la sesión, el bloque final también se persiste.

---

## BLOQUE 4 — Mobile: completar el `userId` en `MainActivity`

### Contexto
`ace-mobile/app/src/main/kotlin/com/ace/mobile/presentation/MainActivity.kt`, líneas **59–64**:

```kotlin
composable("session_screen_route") {
    // TODO: Reemplazar "user-123" con el userId real del usuario logueado
    SessionScreen(
        userId = "user-123" // Temporal hasta integrar auth
    )
}
```

El auth **ya está implementado** (S4 completo), así que el TODO es absurdo.

### 4.1 De dónde sacar el `userId` real
- `LocalUserEntity` (`entity/LocalUserEntity.kt`) tiene `userId: String` (PK), `accessToken`, `refreshToken`, etc.
- `UserDao` expone el usuario actual (es una tabla de 1 registro).
- `AuthRepository` ya gestiona el login y persiste el usuario en Room.

### 4.2 Implementación recomendada
1. Añadir al `SessionViewModel` (o a un `ProfileViewModel` ya existente) un `StateFlow<String?>` que exponga el `userId` del usuario logueado, leído de `UserDao`.
2. En la `composable("session_screen_route")`, leer ese `userId` y pasarlo a `SessionScreen`. Si es `null` (no logueado), redirigir a login o lanzar error.
3. Borrar el TODO y el `"user-123"`.

**Alternativa más simple (si no se quiere tocar ViewModels):** usar `hiltViewModel()` con un `AuthViewModel`/`SessionViewModel` que ya tenga el `userId` inyectado, y leerlo del estado.

### 4.3 Verificación
- Tras login exitoso, navegar a sesión → el `userId` pasado al servicio es el real (verificar en el log del `ExerciseSyncService`: `INICIANDO SESION: <uuid>` y `EXTRA_USER_ID`).

---

## BLOQUE 5 — Mobile: limpiar código muerto

### 5.1 `WearDataListenerService` inerte
`ace-mobile/app/src/main/kotlin/com/ace/mobile/service/WearDataListenerService.kt` está completo pero **comentado en el Manifest** (líneas ~60–90). Duplica la funcionalidad de `WearDataSource` + `ExerciseSyncService`.

- **Decisión:** como la captura real de HR ya pasa por `WearDataSource` (DataClient) dentro del foreground service, este listener es redundante.
- **Acción recomendada:** **borrar** el archivo `.kt` y su bloque comentado del Manifest. Si el humano quiere conservarlo como referencia, dejar el archivo pero quitar el bloque del Manifest y añadir un KDoc "NO USAR — ver WearDataSource".

### 5.2 `SessionRepositoryImpl` placeholder en RAM
`data/repository/SessionRepositoryImpl.kt:19` tiene un TODO pidiendo reemplazar por Room, pese a que los DAOs ya existen.

- Este es **alcance del Bloque 3/4**, no de limpieza. Documentar en el plan que queda pendiente pero **no tocar en este handoff** salvo que el humano lo pida.

### 5.3 Tests vacíos
- `app/src/test/.../CalculateBlockXpUseCaseTest.kt` → clase vacía.
- `app/src/test/.../BuildExerciseBlockUseCaseTest.kt` → clase vacía.
- **Acción:** dejarlos (son placeholders válidos para cuando se implementen esos use cases). No borrar.

---

## BLOQUE 6 — ✅ COMPLETADO: Wear — interacción UseCases + decisión `:shared` hibrido

> **Estado:** Resuelto por un compañero del equipo (merge `e356683`). Se verificó tras `pull` el 2026-06-19.
> Solo resta **documentación** del plan (§6.5).

### 6.1 Lo que se hizo (verificado en código)

**Arquitectura Clean completada — el ViewModel ya no habla directo con el repositorio:**

1. **`StartExerciseUseCase`** revive con 2 métodos:
   - `operator fun invoke()` → inicializa el repositorio (llamado desde `SessionViewModel.initialize()`).
   - `fun startSession(sessionId: String)` → activa el sensor de FC.
2. **`StopExerciseUseCase`** gana 2 métodos:
   - `operator fun invoke(sessionId: String)` → detiene sesión (sensor + notifica al móvil).
   - `fun dispose()` → limpia recursos al cerrar la app.
3. **`SessionViewModel`** ya **NO inyecta `WearHealthRepository`**:
   - `initialize()` → `startExerciseUseCase()`.
   - `handleStopFromMobile(sessionId)` → `stopExerciseUseCase(sessionId)` + `stopSessionInternal()` (solo UI).
   - `onStopButtonClicked()` → `stopExerciseUseCase(sessionId)` + `stopSessionInternal()`.
   - `dispose()` → `stopExerciseUseCase.dispose()`.
4. **`WearHealthRepository`** perdió la responsabilidad de escuchar comandos:
   - Eliminó la suscripción a `wearMessageClient.commands`.
   - Eliminó `onStartCommand()` / `onStopCommand()` privados.
   - Expone `startSession(sessionId)` y `stopSession(sessionId)` públicos (llamados por los use cases).
   - `initialize()` ahora solo escucha muestras de FC para enviar al móvil.
5. **Registro único del listener de `MessageClient`:** `WearApplication.onCreate()` es el ÚNICO lugar que llama a `wearMessageClient.startListening()` (comentario actualizado: *"UNICO lugar donde se registra el listener"*).

### 6.2 Decisión `:shared` — HIBRIDO (NO negociable)

El consumo de `:shared` en wear quedó **hibrido** y así se mantiene:

| Archivo | Fuente de paths | Estado |
|---|---|---|
| `app/build.gradle.kts:55` | — | `implementation(libs.ace.shared)` ✅ declarado |
| `gradle/libs.versions.toml` | — | `ace-shared = "1.0.4"` ✅ |
| `WearMessageClient.kt:6` | `com.ace.shared.constants.DataLayerPaths` | Importa de `:shared` ✅ |
| `WearDataClient.kt:24` | `WearDataLayerPaths` (objeto LOCAL) | Define sus propios paths como strings literales |

**Regla de coherencia hibrida:** los valores en `WearDataLayerPaths` (local) **deben coincidir manualmente** con `com.ace.shared.constants.DataLayerPaths` (del módulo `:shared`). Si alguien cambia un path en `:shared`, debe actualizar `WearDataClient.WearDataLayerPaths` en paralelo. Es un punto de desync intencional y aceptado.

> **No unificar.** Esta es la decisión final del equipo. No eliminar `WearDataLayerPaths` ni reemplazarlo por `DataLayerPaths`.

### 6.3 Registro único del listener

Decisión tomada por el compañero (difiere de la recomendación original del handoff, pero es **igual de válida**): el listener se registra **solo en `WearApplication.onCreate()`**, no en el repositorio. Hay un único registro, con dueño claro. No cambiar.

### 6.4 Checklist del Bloque 6 — estado final

| Tarea | Estado |
|---|---|
| `StartExerciseUseCase` vivo con `invoke()` + `startSession()` | ✅ Hecho |
| `StopExerciseUseCase` con `invoke(sessionId)` + `dispose()` | ✅ Hecho |
| `SessionViewModel` no habla directo con repositorio | ✅ Hecho |
| `WearHealthRepository` solo orquesta sensor (sin escuchar comandos) | ✅ Hecho |
| Registro único de `MessageClient` listener (en `WearApplication`) | ✅ Hecho |
| `ace-shared` bumped a 1.0.4 en `libs.versions.toml` | ✅ Hecho |
| `:shared` declarado en `build.gradle.kts` | ✅ Hecho |
| Unificar paths (eliminar hibrido) | ❌ **NO** — decisión: queda hibrido |

### 6.5 Pendiente: solo documentación del plan

Actualizar `CONTEXT/IMPLEMENTATION_PLAN_WEAROS_v4.2.md`:
- Cabecera: bump a **v4.4**, fecha 2026-06-19. Nota: *"Arquitectura Clean completada (ViewModel→UseCase→Repository); `:shared` hibrido (MessageClient usa `DataLayerPaths`, DataClient usa `WearDataLayerPaths` local)."*
- §2 snippet de dependencias: confirmar `implementation(libs.ace.shared)` y `legacy-kapt` con `kapt(libs.hilt.compiler)`.
- §5 tabla de paths: dejar las DOS fuentes documentadas (hibrido) con la regla de coherencia del §6.2.
- §8 tabla de decisiones:
  - Fila "`:shared` en wear" → "**Hibrido** (v4.4): MessageClient consume `DataLayerPaths` de `:shared`; DataClient usa `WearDataLayerPaths` local. Sincronización manual.".
  - Fila "Registro listener" → "Único registro en `WearApplication.onCreate()`".
- Añadir sección "v4.2 → v4.4" en el historial de cambios explicando:
  1. Arquitectura Clean completada (use cases vivos).
  2. `:shared` hibrido (decisión del equipo).

---

## BLOQUE 7 — Documentación: actualizar planes y README

### 7.1 `README.md` (raíz)
Reescribir según la realidad:
- Estructura: 3 subproyectos locales (`ace-backend`, `ace-mobile`, `ace-wear`) + `:shared` externo (`github.com/reinaldojperalta/ace-shared`) vía JitPack.
- **Eliminar** la sección "Setup Rápido" que manda a `docker-compose up -d` y `cd ace-shared && ./gradlew build publishToMavenLocal` (no existen).
- Backend: PostgreSQL en **Supabase** remoto (puerto 8080). Dockerfile para despliegue en Render. Quitar toda mención a `docker-compose`.
- Tabla de responsables: mantener.
- Reglas de oro: "cambios en `:shared` = nuevo tag + bump de versión en backend y mobile".

### 7.2 `CONTEXT/IMPLEMENTATION_PLAN_BACKEND_v4.1.md`
- Cabecera: bump a **v4.3**, fecha 2026-06-19, nota "Migración a estructura por-feature + Dockerfile mejorado + gradlew tracked + application-prod.yml".
- §4 estructura de carpetas: reescribir para reflejar **solo** el layout por-feature (`auth/`, `exercise/`, `ranking/`, `streak/`, `user/`, `xp/`, más `config/`, `security/`). Eliminar el layout plano.
- Añadir §"Estado real por sistema": S4 ✅ implementado (en `auth/`), S1,S2,S3,S5,S6,S7,S9,S10 ❌ (solo esquema SQL en Flyway V1–V4).
- Añadir nota: Dockerfile usa `gradlew` (wrapper 9.5.1), `SPRING_PROFILES_ACTIVE=prod`, `application-prod.yml`, runtime jammy.
- Eliminar cualquier mención a `docker-compose`.
- **No tocar** la sección de credenciales (el humano decidió que se quedan).

### 7.3 `CONTEXT/IMPLEMENTATION_PLAN_MOBILE_v4.1.md`
- Cabecera: bump a **v4.3**, fecha 2026-06-19.
- Correcciones de stack (verificadas contra `libs.versions.toml`):
  - Kotlin **2.2.21** (no 2.1.20).
  - Usa **`legacy.kapt`**, NO KSP (corregir tabla §2.4 y todas las menciones).
  - `ace-shared:1.0.4` (no 1.0.0 en snippets).
  - Snippet `app/build.gradle.kts`: añadir `alias(libs.plugins.legacy.kapt)` y `kapt(libs.hilt.compiler)`.
- Añadir §"Estado real por sistema": S1 ✅, S4 ✅, S2 🟡 (live funciona pero cierre de bloque sin persistir — referenciar Bloque 3 del handoff), S3/S5/S6/S7/S9/S10 ❌ vacíos.
- Documentar la cadena rota: `ExerciseSyncService.kt:354-356` (persistencia de bloque pendiente).
- Documentar que tokens van en **Room plano** (`LocalUserEntity`), no EncryptedSharedPreferences.

### 7.4 `CONTEXT/IMPLEMENTATION_PLAN_SHARED_v4.2.md`
- Renombrar archivo a `IMPLEMENTATION_PLAN_SHARED_v4.3.md` (la cabecera interna ya dice v4.3).
- Añadir nota: "Consumido por `ace-backend`, `ace-mobile` (vía `DataLayerPaths` + DTOs) Y `ace-wear` (hibrido: `WearMessageClient` usa `DataLayerPaths`; `WearDataClient` tiene su propio `WearDataLayerPaths` local sincronizado manualmente). Versión 1.0.4."

### 7.5 `CONTEXT/BD-BACKEND.md`
- Verificar que lista las migraciones reales: `V1__init.sql`, `V2__xp_transactions.sql`, `V3__ranking_materialized.sql`, `V4__seed_data.sql`.
- Si solo describe schema teórico, añadir referencia a los archivos Flyway reales en `db/migration/`.

### 7.6 `reporte_estado_auth.md`
- Ya consolidado en `CONTEXT/` por el humano. **No tocar** el de `CONTEXT/`.
- Verificar que el de `ace-backend/reporte_estado_auth.md` fue borrado o redirige al de `CONTEXT/`.

---

## BLOQUE 8 — Checklist final de verificación

Tras ejecutar todos los bloques:

- [x] **Backend Bloque 1:** Auth migrado a `auth/` por-feature. Layout plano eliminado.
- [x] **Backend Bloque 2.5:** Dockerfile usa `gradlew` (no descarga manual). Perfil `prod` activo. JVM tuneada. Runtime jammy.
- [x] **Backend Bloque 2.6:** `application-prod.yml` creado.
- [ ] **Backend Bloque 2.1-2.4:** Borrar `libs/ace-shared-1.0.0.jar`, configs vacíos (`DatabaseConfig`, `JacksonConfig`, `JwtConfig`, `SchedulingConfig`, `WebConfig`), tests vacíos. `POST /api/auth/login` funciona tras limpieza.
- [ ] **Mobile Bloque 3:** Un bloque se persiste en Room tras 30s de sesión (`ExerciseSyncService.kt:354-356`).
- [ ] **Mobile Bloque 4:** El `userId` real viaja al servicio (sin `"user-123"`).
- [ ] **Mobile Bloque 5:** `WearDataListenerService` eliminado o marcado NO USAR.
- [x] **Wear Bloque 6:** Un solo registro de `MessageClient` listener (en `WearApplication`). `StartExerciseUseCase` vivo. `StopExerciseUseCase` con `sessionId`. `:shared` hibrido (MessageClient usa `DataLayerPaths`, DataClient usa `WearDataLayerPaths` local). **Decisión final del equipo: NO unificar.**
- [ ] **Docs Bloque 7:** README sin `docker-compose` ni `ace-shared/` local. Planes bumped a v4.3/v4.4 con estado real. Plan de Wear documenta arquitectura Clean + `:shared` hibrido. Plan de backend documenta Dockerfile mejorado.
- [ ] **Residuales:** archivos `nul` dejados en paz (en `.gitignore`). `gradlew` del backend commiteado con `+x`.

---

## Apéndice — Estructura final del backend (estado actual verificado)

```
ace-backend/src/main/kotlin/sena/adso/ace_backend/
├── AceBackendApplication.kt
├── config/
│   ├── SecurityConfig.kt               ← funcional
│   ├── DatabaseConfig.kt               ← VACÍO (borrar en Bloque 2.3)
│   ├── JacksonConfig.kt                ← VACÍO (borrar en Bloque 2.3)
│   ├── JwtConfig.kt                    ← VACÍO (borrar en Bloque 2.3)
│   ├── SchedulingConfig.kt             ← VACÍO (borrar en Bloque 2.3)
│   └── WebConfig.kt                    ← VACÍO (borrar en Bloque 2.3)
├── security/
│   └── JwtAuthenticationFilter.kt       ← funcional
├── auth/                                ← ✅ MIGRADO (Bloque 1)
│   ├── controller/AuthController.kt
│   ├── model/{User, RefreshToken}.kt
│   ├── repository/{User, RefreshToken}Repository.kt
│   └── service/{Auth, Jwt, RefreshToken}Service.kt
├── exercise/   (vacío, esqueleto)
├── ranking/    (vacío, esqueleto)
├── streak/     (vacío, esqueleto)
├── user/       (vacío, esqueleto)
└── xp/         (vacío, esqueleto)
```

---

## Apéndice — Archivos nuevos/modificados relevantes

| Archivo | Estado | Qué hace |
|---|---|---|
| `ace-backend/Dockerfile` | ✅ Modificado (sesión 2026-06-19) | Usa `gradlew` en vez de descargar Gradle 8.14 manualmente. Runtime jammy. Perfil prod. JVM tuning. |
| `ace-backend/src/main/resources/application-prod.yml` | ✅ Creado (sesión 2026-06-19) | Perfil de producción para Render (pool-size 3, logging WARN/INFO, actuator sin detalles). |
| `ace-backend/gradlew` | ✅ Commiteado (staged) | Script del Gradle Wrapper con permiso `+x` (`100755`). Necesario para que el Dockerfile funcione en Render. |
| `ace-backend/gradlew.bat` | ✅ Commiteado (staged) | Versión Windows del wrapper (no se usa en Docker pero se trackea para consistencia). |
| `.gitignore` (raíz) | ✅ Modificado | Excepciones `!ace-backend/gradlew` y `!ace-backend/gradlew.bat` para que lleguen a Render sin ignorar mobile/wear. |
| `ace-wear/**` (varios) | ✅ Modificado por un compañero (merge `e356683`) | Bloque 6: arquitectura Clean completada (use cases vivos, ViewModel sin tocar repositorio directo), registro único de listener en `WearApplication`, `:shared` hibrido. |
| `CONTEXT/HANDOFF_CLEANUP_v1.md` | ✅ Creado → v1.2 | Este documento. |

---

*Documento generado por análisis de coherencia del ecosistema A.C.E. Versión 1.2 — marca Bloques 1, 2.5, 2.6 y 6 como completados. Pendientes: Bloque 2.1-2.4 (limpieza backend), Bloque 3 (cierre de bloque mobile), Bloque 4 (userId), Bloque 5 (código muerto mobile), Bloque 7 (documentación).*
