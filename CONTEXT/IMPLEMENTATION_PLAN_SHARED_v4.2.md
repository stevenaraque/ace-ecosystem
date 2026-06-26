# A.C.E — Implementation Plan: Shared Module (`:shared`)

> **Estado:** ✅ **Implementado y Publicado (v1.0.9)**
> **Versión:** 4.4 (Actualizado: `:shared` v1.0.9; DTOs de perfil/stats añadidos post-1.0.4; `XpFormulaDto` sin `bonusMultiplier`)
> **Fecha:** 2026-06-25
> **Stack:** Kotlin 2.2.21 · Gradle 8.10+ (Kotlin DSL) · kotlinx-serialization 1.8.0 · Gson 2.11.0 · JUnit 5.11.0
> **Publicado en:** [JitPack](https://jitpack.io/#reinaldojperalta/ace-shared)
> **Depende de:** Apéndices S1, S2, S3, S4, S5, S6, S7, S8, S9, S10 y Arquitectura A.C.E v0.4
> **Consumido por:** `ace-backend`, `ace-mobile` (vía DTOs + `DataLayerPaths`), y `ace-wear` (híbrido: enums/constants, sin DTOs)

---

## 1. Visión y Alcance

El módulo `:shared` es el **contrato inmutable** entre backend y mobile. Contiene únicamente:

1. **DTOs** (Data Transfer Objects) serializables que viajan por la red.
2. **Enums** compartidos (`SportType`, `BlockStatus`, `SessionStatus`).
3. **Constantes** de contrato (paths, versiones de schema, límites).
4. **Utilidades** de serialización/deserialización (Gson + kotlinx-serialization adapters).

**Principio rector:** *Si un campo cambia en `:shared`, ambos lados (backend y mobile) deben romper compilación antes de romper producción.*

**Regla de oro:** `:shared` es **puro Kotlin**, sin dependencias de Android ni Spring. Cualquier framework-specific adapter vive en el módulo consumidor.

**Nota sobre JitPack:** El artifact se publica automáticamente vía [JitPack](https://jitpack.io). El group Maven se resuelve como `com.github.reinaldojperalta` (sobrescrito por JitPack). Los consumidores deben usar esta coordenada exacta.

**Estado actual:** `:shared` v1.0.9 está **implementado, probado y publicado** en JitPack (tags llegan a `v1.0.9`). Repo separado: `reinaldojperalta/ace-shared`. Consumido por backend (inline en `build.gradle.kts:29`) y mobile (`libs.versions.toml:25`). Ver §8 para detalles de consumo.

---

## 2. Dependencias Gradle (`build.gradle.kts`)

### 2.1. `build.gradle.kts` (Kotlin DSL)

```kotlin
plugins {
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.serialization") version "2.2.21"
    `java-library`
    `maven-publish`
}

group = "com.ace"
version = "1.0.9"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
    withSourcesJar()
    withJavadocJar()
}

repositories {
    mavenCentral()
    google()
}

dependencies {
    // ─── Kotlin Standard Library ───
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.2.21")

    // ─── JSON Serialization: Gson (coherente con mobile/backend) ───
    implementation("com.google.code.gson:gson:2.11.0")

    // ─── JSON Serialization: kotlinx-serialization (coherente con backend) ───
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")

    // ─── Testing ───
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.0")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.11.0")
    testImplementation("org.assertj:assertj-core:3.26.3")
    testImplementation("io.mockk:mockk:1.13.12")
    testImplementation("org.jetbrains.kotlin:kotlin-test:2.2.21")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:2.2.21")
}

tasks.test {
    useJUnitPlatform()
}

// ✅ Kotlin 2.2+ compilerOptions DSL — reemplaza kotlinOptions deprecado
kotlin {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xjsr305=strict",
            "-Xannotation-default-target=param-property"
        )
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

// ─── Maven Publish (requerido por JitPack) ───
publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            groupId = group.toString()
            artifactId = "shared"
            version = version.toString()
        }
    }
}
```

### 2.2. Notas críticas sobre la actualización v3.0 → v4.0 → v4.1 → v4.2 → v4.3

| Aspecto | Pre-v4.0 | v4.0 | v4.1 | v4.2 | v4.3 (Actual) | Justificación |
|---------|---------|------|------|------|---------------|---------------|
| **Publicación** | GitHub | GitHub | **JitPack** | **JitPack** | **JitPack (activo)** | JitPack no requiere configuración de tokens PAT. |
| **Group Maven** | `com.ace` | `com.ace` | **`com.github.reinaldojperalta`** (sobrescrito por JitPack) | **`com.github.reinaldojperalta`** | JitPack sobrescribe el group. Los consumidores DEBEN usar esta coordenada. |
| **Repo** | Monorepo `ace-ecosystem` | Monorepo `ace-ecosystem` | **Repo separado** `reinaldojperalta/ace-shared` | **Repo separado (activo)** | Monorepo mixto (JVM + Android) confunde a JitPack. Repo separado = compilación limpia. |
| **Kotlin** | 2.1.0 | **2.2.21** | 2.2.21 | **2.2.21** | Coherente con backend (baseline Spring Boot 4.0). |
| **Moshi** | 1.15.1 + kapt | **Eliminado** | Eliminado | **Eliminado** | Reemplazado por Gson + kotlinx-serialization. |
| **Gson** | No usado | **2.11.0** | 2.11.0 | **2.11.0** | Coherente con mobile (`retrofit-converter-gson`). |
| **kotlinx-serialization** | No usado | **1.8.0** | 1.8.0 | **1.8.0** | Coherente con backend (`spring-boot-starter-kotlinx-serialization-json`). |
| **Plugin Kotlin JVM** | `kotlin("jvm")` version "2.1.0" | `kotlin("jvm")` version "2.2.21" | 2.2.21 | **2.2.21** | Actualizado a versión coherente con backend. |
| **Plugin Kotlin Serialization** | No usado | **`kotlin("plugin.serialization")`** | `kotlin("plugin.serialization")` | **`kotlin("plugin.serialization")`** | Necesario para kotlinx-serialization. |
| **kapt** | `kotlin("kapt")` | **Eliminado** | Eliminado | **Eliminado** | No se usa kapt con Gson/kotlinx-serialization. |
| **`kotlinOptions { jvmTarget }`** | `"21"` | **Eliminado** | Eliminado | **Eliminado** | Deprecado en Kotlin 2.2. Se usa `compilerOptions` DSL. |
| **`compilerOptions` DSL** | No existía | **`kotlin { compilerOptions { ... } }`** | `kotlin { compilerOptions { ... } }` | **`kotlin { compilerOptions { ... } }`** | Nuevo DSL obligatorio en Kotlin 2.2+. |
| **Java** | 21 | **21** (mantenido) | 21 | **21** | Compatible con todos los módulos. |
| **`failOnNoDiscoveredTests`** | No usado | Usado en v4.0 erróneamente | **Eliminado** | **Eliminado** | Propiedad inválida en Gradle Kotlin DSL puro. Causaba fallo en JitPack. |
| **Tests** | — | — | — | **17 tests pasando** | Cobertura de DTOs, serialización y constantes. |
| **Archivos** | — | — | — | **40 archivos** | 12 DTOs, 4 enums, 8 constantes, 4 utilidades de serialización, tests. |

---

## 3. Estructura de Carpetas

```
ace-shared/                       <-- Repo separado: reinaldojperalta/ace-shared
├── build.gradle.kts              # Kotlin DSL, Kotlin 2.2.21, Gson, kotlinx-serialization, JUnit 5
├── settings.gradle.kts           # rootProject.name = "ace-shared"
├── gradle/
│   └── wrapper/
│
├── src/main/kotlin/com/ace/shared/
│   ├── dto/                      # DTOs inmutables (data classes, @Serializable)
│   │   ├── ExerciseBlockDto.kt
│   │   ├── ExerciseSessionDto.kt
│   │   ├── SyncBatchRequestDto.kt
│   │   ├── SyncBatchResponseDto.kt          # añadido post-1.0.4
│   │   ├── RejectedBlockDto.kt              # añadido post-1.0.4
│   │   ├── ClientStatsDto.kt
│   │   ├── OfficialStatsDto.kt              # añadido post-1.0.4
│   │   ├── XpFormulaDto.kt
│   │   ├── XpAwardedResponseDto.kt
│   │   ├── RankingEntryDto.kt
│   │   ├── RankingResponseDto.kt
│   │   ├── StreakStateDto.kt
│   │   ├── AuthRequestDto.kt
│   │   ├── AuthResponseDto.kt
│   │   ├── RefreshTokenRequestDto.kt
│   │   ├── UserProfileDto.kt                # añadido en 1.0.6 (F1/F2)
│   │   ├── UpdateProfileRequestDto.kt       # añadido en 1.0.6 (F1/F2)
│   │   ├── StatsResponseDto.kt             # añadido post-1.0.4 (S10)
│   │   ├── StatsReconcileRequestDto.kt     # añadido post-1.0.4 (S10)
│   │   └── StatsReconcileResponseDto.kt    # añadido post-1.0.4 (S10)
│   │
│   ├── enums/
│   │   ├── SportType.kt          # RUNNING, CYCLING, WALKING (3 valores reales)
│   │   ├── SessionStatus.kt
│   │   ├── BlockStatus.kt
│   │   └── NotificationChannelId.kt  # enum con propiedad channelId (3 canales)
│   │
│   ├── constants/
│   │   ├── DataLayerPaths.kt
│   │   ├── ApiEndpoints.kt
│   │   ├── SyncConstants.kt
│   │   ├── AuthConstants.kt
│   │   ├── XpConstants.kt
│   │   ├── RankingConstants.kt
│   │   ├── StreakConstants.kt
│   │   └── HistoryConstants.kt
│   │
│   └── serialization/
│       ├── GsonFactory.kt
│       ├── InstantAdapter.kt
│       ├── UuidAdapter.kt
│       ├── SportTypeAdapter.kt            # añadido post-1.0.4
│       └── KotlinxSerializationConfig.kt
│
└── src/test/kotlin/com/ace/shared/
    ├── dto/ExerciseBlockDtoTest.kt
    ├── dto/SyncBatchRequestDtoTest.kt
    ├── serialization/InstantAdapterTest.kt
    └── constants/ConstantsValidationTest.kt
```

---

## 4. DTOs Detallados (Coherentes con Apéndices)

### 4.1 ExerciseBlockDto (S2, S3, S5)

```kotlin
@Serializable
data class ExerciseBlockDto(
    val blockId: String,
    val sessionId: String,
    val userId: String,
    val deviceId: String,
    val sportType: SportType,
    val timestampStart: Long,
    val timestampEnd: Long,
    val durationSeconds: Int,
    val avgBpm: Double,
    val maxBpm: Double,
    val minBpm: Double,
    val sampleCount: Int,
    val xpCalculated: Int,
    val schemaVersion: Int = 1
)
```

### 4.2 ExerciseSessionDto (S2)

```kotlin
@Serializable
data class ExerciseSessionDto(
    val sessionId: String,
    val userId: String,
    val deviceId: String,
    val status: SessionStatus,
    val sportType: SportType,
    val timestampStart: Long,
    val timestampEnd: Long? = null,
    val totalBlocks: Int = 0,
    val totalXp: Int = 0,
    val schemaVersion: Int = 1
)
```

### 4.3 ClientStatsDto (S10)

```kotlin
@Serializable
data class ClientStatsDto(
    val userId: String,
    val totalXp: Int,
    val totalSessions: Int,
    val totalDurationSeconds: Long,
    val avgBpmAllTime: Double,
    val lastSyncAt: Long? = null,
    val schemaVersion: Int = 1
)
```

### 4.4 XpFormulaDto (S5)

```kotlin
@Serializable
data class XpFormulaDto(
    val sportType: SportType,
    val minBpm: Int,
    val xpPerMinute: Double,
    val maxXpPerBlock: Int,
    val version: Int = 1,
    val effectiveSince: Long,
    val schemaVersion: Int = 1
)
```

> **Nota (C3 fix):** `bonusMultiplier` fue **eliminado** del contrato (no existía en el apéndice S5 y causaba desync móvil↔backend). La fórmula real es `minutes * xpPerMinute`, limitado por `maxXpPerBlock`.

### 4.5 XpAwardedResponseDto (S5)

```kotlin
@Serializable
data class XpAwardedResponseDto(
    val blockId: String,
    val xpAccepted: Int,
    val xpRejected: Int = 0,
    val newTotalXp: Int,
    val rankChanged: Boolean,
    val newRankId: String? = null,
    val balanceAfter: Int,
    val schemaVersion: Int = 1
)
```

### 4.6 RankingEntryDto (S6)

```kotlin
@Serializable
data class RankingEntryDto(
    val position: Int,
    val userId: String,
    val username: String,
    val totalXp: Int,
    val cityId: String? = null,
    val schemaVersion: Int = 1
)
```

### 4.7 RankingResponseDto (S6)

```kotlin
@Serializable
data class RankingResponseDto(
    val scope: String, // "global" o "municipal"
    val userPosition: RankingEntryDto,
    val topEntries: List<RankingEntryDto>,
    val totalParticipants: Int,
    val cachedAt: Long,
    val schemaVersion: Int = 1
)
```

### 4.8 StreakStateDto (S7)

```kotlin
@Serializable
data class StreakStateDto(
    val userId: String,
    val currentStreak: Int,
    val bestStreak: Int,
    val lastExerciseDate: Long? = null,
    val streakBrokenAt: Long? = null,
    val schemaVersion: Int = 1
)
```

### 4.9 AuthRequestDto (S4)

```kotlin
@Serializable
data class AuthRequestDto(
    val email: String,
    val password: String,
    val deviceId: String,
    val schemaVersion: Int = 1
)
```

### 4.10 AuthResponseDto (S4)

```kotlin
@Serializable
data class AuthResponseDto(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Int, // segundos
    val tokenType: String = "Bearer",
    val userId: String,
    val schemaVersion: Int = 1
)
```

### 4.11 RefreshTokenRequestDto (S4)

```kotlin
@Serializable
data class RefreshTokenRequestDto(
    val refreshToken: String,
    val deviceId: String,
    val schemaVersion: Int = 1
)
```

### 4.12 SyncBatchRequestDto (S3)

```kotlin
@Serializable
data class SyncBatchRequestDto(
    val deviceId: String,
    val sessionId: String,
    val blocks: List<ExerciseBlockDto>,
    val clientStats: ClientStatsDto,
    val sentAt: Long,
    val schemaVersion: Int = 1
)
```

---

## 5. Enums Detallados

### 5.1 SportType

```kotlin
enum class SportType {
    RUNNING,
    CYCLING,
    WALKING
}
```

> **Nota:** solo 3 deportes en el MVP (con fórmula/seed en la BD). El plan v4.2 listaba 8 valores idealizados; el código real y el seed (V10) manejan 3. Añadir más requiere DTO + seed + fórmula coordinados.

### 5.2 SessionStatus

```kotlin
enum class SessionStatus {
    ACTIVE,
    PAUSED,
    COMPLETED,
    ABORTED
}
```

### 5.3 BlockStatus

```kotlin
enum class BlockStatus {
    PENDING,
    SYNCING,
    SYNCED,
    ERROR
}
```

### 5.4 NotificationChannelId

```kotlin
enum class NotificationChannelId {
    SYNC_STATUS,
    STREAK_REMINDER,
    SESSION_ACTIVE,
    RANKING_UPDATE,
    GENERAL
}
```

---

## 6. Constantes de Contrato

### 6.1 DataLayerPaths (S1)

```kotlin
object DataLayerPaths {
    const val HEART_RATE = "/ace/health/heart_rate"
    const val SESSION_STATUS = "/ace/session/%s/status"
    const val START_COMMAND = "START"
    const val STOP_COMMAND = "STOP"
}
```

### 6.2 ApiEndpoints

```kotlin
object ApiEndpoints {
    const val BASE_API = "/api/v1"
    const val AUTH_LOGIN = "$BASE_API/auth/login"
    const val AUTH_REFRESH = "$BASE_API/auth/refresh"
    const val SYNC_BATCH = "$BASE_API/sync/batch"
    const val XP_FORMULAS = "$BASE_API/xp/formulas"
    const val RANKING_GLOBAL = "$BASE_API/ranking/global"
    const val RANKING_MUNICIPAL = "$BASE_API/ranking/municipal/{cityId}"
    const val USER_PROFILE = "$BASE_API/users/me"
}
```

### 6.3 SyncConstants (S3)

```kotlin
object SyncConstants {
    const val BATCH_MAX_SIZE = 20
    const val RETRY_MAX_ATTEMPTS = 5
    const val RETRY_BACKOFF_MINUTES = 15L
    const val RETRY_EXPONENTIAL_BASE = 2.0
}
```

### 6.4 AuthConstants (S4)

```kotlin
object AuthConstants {
    const val ACCESS_TOKEN_EXPIRY_MINUTES = 15
    const val REFRESH_TOKEN_EXPIRY_DAYS = 7
    const val BEARER_PREFIX = "Bearer "
    const val REFRESH_REUSED_CODE = "REFRESH_REUSED"
    const val DEVICE_ID_HEADER = "X-Device-Id"
}
```

### 6.5 XpConstants (S5)

```kotlin
object XpConstants {
    const val BLOCK_DURATION_SECONDS = 300 // 5 minutos
    const val BLOCK_DURATION_TOLERANCE_PERCENT = 10
    const val MIN_BPM_DEFAULT = 80
    const val SCHEMA_VERSION = 1
}
```

### 6.6 RankingConstants (S6)

```kotlin
object RankingConstants {
    const val CACHE_TTL_MINUTES = 60
    const val TOP_ENTRIES_LIMIT = 10
    const val RANKING_RECALC_CRON = "0 0 * * * *" // cada hora
}
```

### 6.7 StreakConstants (S7)

```kotlin
object StreakConstants {
    const val CHECK_HOUR = 20 // 8 PM
    const val CHECK_MINUTE = 0
    const val MIN_BLOCKS_FOR_STREAK = 1
}
```

### 6.8 HistoryConstants (S9)

```kotlin
object HistoryConstants {
    const val MAX_LOCAL_SESSIONS = 5
}
```

---

## 7. Serialización (Gson + kotlinx-serialization)

### 7.1 GsonFactory

```kotlin
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.time.Instant
import java.util.UUID

object GsonFactory {
    fun create(): Gson = GsonBuilder()
        .registerTypeAdapter(Instant::class.java, InstantAdapter())
        .registerTypeAdapter(UUID::class.java, UuidAdapter())
        .serializeNulls()
        .setPrettyPrinting()
        .create()
}
```

### 7.2 InstantAdapter

```kotlin
import com.google.gson.*
import java.lang.reflect.Type
import java.time.Instant

class InstantAdapter : JsonSerializer<Instant>, JsonDeserializer<Instant> {
    override fun serialize(src: Instant, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        return JsonPrimitive(src.toEpochMilli())
    }

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Instant {
        return Instant.ofEpochMilli(json.asLong)
    }
}
```

### 7.3 UuidAdapter

```kotlin
import com.google.gson.*
import java.lang.reflect.Type
import java.util.UUID

class UuidAdapter : JsonSerializer<UUID>, JsonDeserializer<UUID> {
    override fun serialize(src: UUID, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        return JsonPrimitive(src.toString())
    }

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): UUID {
        return UUID.fromString(json.asString)
    }
}
```

### 7.4 KotlinxSerializationConfig

```kotlin
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule

object KotlinxSerializationConfig {
    val json: Json = Json {
        prettyPrint = true
        isLenient = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
}
```

---

## 8. Versionado y Publicación (JitPack)

### 8.1 Versionado semántico

- `:shared` sigue versionado semántico: `MAJOR.MINOR.PATCH`.
- **Breaking change** (campo obligatorio nuevo, renombre, tipo cambiado) → sube `MAJOR`.
- **Adición opcional** (campo nuevo con default) → sube `MINOR`.
- **Fix** (corrección de adapter) → sube `PATCH`.

### 8.2 Publicación vía JitPack (Proceso validado)

1. Hacer cambios en `:shared`.
2. Commitear y pushear a `main`.
3. Crear tag: `git tag -a v1.0.1 -m "Release 1.0.1"`.
4. Push del tag: `git push origin v1.0.1`.
5. JitPack detecta el tag automáticamente, compila y publica.
6. Verificar estado en: `https://jitpack.io/#reinaldojperalta/ace-shared`

**Estado actual:** v1.0.9 está publicado y disponible. Los consumidores (`ace-backend`, `ace-mobile`) usan esta versión. **`ace-wear`** consume `:shared` en modo **híbrido** (enums/constantes, sin DTOs).

### 8.3 Coordenada Maven para Consumidores

```kotlin
// Backend (ace-backend/build.gradle.kts, línea 29 — inline)
repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("com.github.reinaldojperalta:ace-shared:1.0.9")
}
```

```kotlin
// Mobile (ace-mobile/app/build.gradle.kts) — usa version catalog
repositories {
    google()
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation(libs.ace.shared)  // apunta a ace-shared = "1.0.9" en libs.versions.toml
}
```

```kotlin
// Wear (ace-wear/app/build.gradle.kts) — `:shared` HÍBRIDO
// implementation(libs.ace.shared) declarado; se usan enums/constantes (DataLayerPaths),
// pero NO se serializan DTOs (paths locales en WearDataClient).
```

**⚠️ IMPORTANTE:** JitPack sobrescribe el `group` definido en `build.gradle.kts` (`com.ace`) por `com.github.reinaldojperalta`. Los consumidores DEBEN usar `com.github.reinaldojperalta:ace-shared`.

### 8.4 Proceso de cambio

1. Proponer cambio en `reinaldojperalta/ace-shared` con PR.
2. Revisión por Reinaldo (backend) y Steven (mobile).
3. Si es breaking change, ambos equipos deben actualizar antes de mergear.
4. Crear tag y push. JitPack compila automáticamente.
5. Ambos equipos actualizan versión en `build.gradle.kts` y adaptan código.

---

## 9. Checklist de Coherencia con Apéndices

| Apéndice | DTO/Enum/Constante | ¿Cubierto? | Notas |
|----------|-------------------|------------|-------|
| S1 (Captura) | `DataLayerPaths` | ✅ | Paths de DataClient documentados. |
| S2 (Sesión) | `ExerciseSessionDto`, `ExerciseBlockDto`, `SessionStatus` | ✅ | Estados y schema de bloque definidos. |
| S3 (Sync) | `SyncBatchRequestDto`, `BlockStatus`, `SyncConstants` | ✅ | Batch max 20, estados PENDING→SYNCED. |
| S4 (Auth) | `AuthRequestDto`, `AuthResponseDto`, `RefreshTokenRequestDto`, `AuthConstants` | ✅ | JWT híbrido, rotación, race conditions. |
| S5 (XP) | `XpFormulaDto`, `XpAwardedResponseDto`, `XpConstants` | ✅ | APK calcula, backend valida. |
| S6 (Ranking) | `RankingResponseDto`, `RankingEntryDto`, `RankingConstants` | ✅ | Cache 1h, top 10. |
| S7 (Racha) | `StreakStateDto`, `StreakConstants` | ✅ | 8 PM check, backend controla. |
| S8 (Notificaciones) | `NotificationChannelId` | ✅ | Canales locales definidos. |
| S9 (Historial) | `HistoryConstants` | ✅ | MAX_LOCAL_SESSIONS = 5. |
| S10 (Estadísticas) | `ClientStatsDto` | ✅ | Stats procesadas, sincronización APK↔Backend. |

---

## 10. Nota sobre Coherencia de Kotlin en el Ecosistema A.C.E

| Módulo | Kotlin | ¿Por qué? | Coherencia |
|--------|--------|-----------|------------|
| **Backend** | 2.2.21 | Baseline obligatorio de Spring Boot 4.0 | Mismo lenguaje, versión adaptada a plataforma |
| **Mobile** | 2.2.21 | Configurado en `libs.versions.toml` | Mismo lenguaje, versión adaptada a plataforma |
| **Wear** | 2.0.21 | Built-in en AGP 9.0.1 | Mismo lenguaje, versión adaptada a plataforma |
| **:shared** | 2.2.21 | JVM puro, coherente con backend | Mismo lenguaje, versión adaptada a plataforma |

**No intentar unificar a una sola versión de Kotlin.** Cada plataforma tiene su propio toolchain y baseline. `:shared` es JVM puro y puede usar la versión más reciente (2.2.21) sin estar limitado por AGP 9.0. La coherencia se mantiene a nivel de:
- **Lenguaje:** Kotlin en los cuatro módulos
- **JVM Target:** 21 en los cuatro módulos
- **Serialización:** Gson + kotlinx-serialization en backend, mobile y :shared
- **Coroutines:** 1.9.0 en los tres módulos activos

---

## 11. Nota sobre Migración desde Monorepo (v4.0 → v4.1) — Completada

La migración desde monorepo a repo separado con JitPack **ya está completada**. Los pasos históricos fueron:

1. ✅ Crear repo separado `reinaldojperalta/ace-shared`.
2. ✅ Copiar `src/`, `build.gradle.kts`, `settings.gradle.kts`, `gradle/wrapper/`.
3. ✅ Eliminar `failOnNoDiscoveredTests = false` de `tasks.test`.
4. ✅ Eliminar bloque `publishing.repositories` (GitHub Packages) — JitPack no lo requiere.
5. ✅ Crear tag `v1.0.0` y push.
6. ✅ Actualizar `ace-backend` y `ace-mobile` para usar `com.github.reinaldojperalta:ace-shared`.
7. ✅ Eliminar `ace-shared/` del monorepo original.

**Estado actual:** `:shared` vive únicamente en `reinaldojperalta/ace-shared`. No existe en el monorepo original.

---

## 12. Estado de Implementación y Próximos Pasos

| Módulo | Estado | Versión `:shared` | Notas |
|--------|--------|---------|-------|
| **:shared** | ✅ **IMPLEMENTADO** | 1.0.9 | Publicada en JitPack. Repo separado: `reinaldojperalta/ace-shared`. DTOs de perfil/stats añadidos post-1.0.4. |
| **ace-backend** | 🟢 En desarrollo (~85%) | 1.0.9 | S1–S10 funcionales; S4 completo; deuda M3 (OfficialStatsDto). |
| **ace-mobile** | 🟢 En desarrollo (~85%) | 1.0.9 | S1–S10 + F1–F7 completos. |
| **ace-wear** | 🟢 HU-06 completada | híbrido | Captura + transmisión + UI completas. `:shared` híbrido (enums/constants). |

### Próximos pasos recomendados
1. **:shared:** Mantener estable en v1.0.9. No breaking changes sin coordinación previa (PR revisado por Reinaldo + Steven).
2. **Deuda (cuando se toque `:shared`):** alinear `SyncConstants.BLOCK_DURATION_*` con la decisión de equipo (bloques de 60s) — ver `DEUDA_TECNICA_v2.md` D8. El `build.gradle.kts` local debe llevar `version = "1.0.9"` (coincidir con el tag publicado).

---

*Documento coherente con Apéndices S1-S10 y Arquitectura A.C.E v0.3. Cualquier divergencia debe ser reportada como bug de coherencia.*
