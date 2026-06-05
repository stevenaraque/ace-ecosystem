# A.C.E — Implementation Plan: Shared Module (`:shared`)

> **Estado:** Coherente con Apéndices Aprobados S1-S10 y Arquitectura v0.2  
> **Versión:** 4.0 (Actualizado a Kotlin 2.2 + kotlinx-serialization + Gson)  
> **Fecha:** Junio 2026  
> **Stack:** Kotlin 2.2.21 · Gradle 8.10+ (Kotlin DSL) · kotlinx-serialization 1.8.0 · Gson 2.11.0 · JUnit 5.11.0  
> **Depende de:** Apéndices S1, S2, S3, S4, S5, S6, S7, S8, S9, S10  
> **Consumido por:** `ace-backend`, `ace-mobile` (ace-wear NO consume `:shared` en MVP)

---

## 1. Visión y Alcance

El módulo `:shared` es el **contrato inmutable** entre backend y mobile. Contiene únicamente:

1. **DTOs** (Data Transfer Objects) serializables que viajan por la red.
2. **Enums** compartidos (`SportType`, `BlockStatus`, `SessionStatus`).
3. **Constantes** de contrato (paths, versiones de schema, límites).
4. **Utilidades** de serialización/deserialización (Gson + kotlinx-serialization adapters).

**Principio rector:** *Si un campo cambia en `:shared`, ambos lados (backend y mobile) deben romper compilación antes de romper producción.*

**Regla de oro:** `:shared` es **puro Kotlin**, sin dependencias de Android ni Spring. Cualquier framework-specific adapter vive en el módulo consumidor.

---

## 2. Dependencias Gradle (`build.gradle.kts`)

### 2.1. `build.gradle.kts` (Kotlin DSL)

```kotlin
plugins {
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.serialization") version "2.2.21"
    `maven-publish`
    `java-library`
}

group = "com.ace"
version = "1.0.0"

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

// ─── Kotlin 2.2+ compilerOptions DSL ───
kotlin {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xjsr305=strict",
            "-Xannotation-default-target=param-property"
        )
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

// ─── GitHub Packages Publishing ───
publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/tu-org/ace-shared")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
```

### 2.2. Notas críticas sobre la actualización v3.0 → v4.0

| Aspecto | Pre-v4.0 (plan v3.0) | v4.0 (actual) | Justificación |
|---------|---------------------|---------------|---------------|
| **Kotlin** | 2.1.0 | **2.2.21** | Coherente con backend (baseline Spring Boot 4.0). Mobile/wear usan versiones built-in de AGP 9.0, pero `:shared` es JVM puro y puede usar 2.2.21. |
| **Moshi** | 1.15.1 + kapt | **Eliminado** | Reemplazado por Gson + kotlinx-serialization para coherencia con mobile/backend. |
| **Gson** | No usado | **2.11.0** | Coherente con mobile (`retrofit-converter-gson`) y backend (usado implícitamente). |
| **kotlinx-serialization** | No usado | **1.8.0** | Coherente con backend (`spring-boot-starter-kotlin-serialization`). |
| **Plugin Kotlin JVM** | `kotlin("jvm")` version "2.1.0" | `kotlin("jvm")` version "2.2.21" | Actualizado a versión coherente con backend. |
| **Plugin Kotlin Serialization** | No usado | **`kotlin("plugin.serialization")` version "2.2.21"** | Necesario para kotlinx-serialization. |
| **kapt** | `kotlin("kapt")` | **Eliminado** | No se usa kapt en `:shared` con Gson/kotlinx-serialization. |
| **`kotlinOptions { jvmTarget }`** | `"21"` | **Eliminado** | Deprecado en Kotlin 2.2. Se usa `compilerOptions` DSL. |
| **`compilerOptions` DSL** | No existía | **`kotlin { compilerOptions { ... } }`** | Nuevo DSL obligatorio en Kotlin 2.2+. |
| **Java** | 21 | **21** (mantenido) | Compatible con todos los módulos. |

---

## 3. Estructura de Carpetas

```
ace-shared/
├── build.gradle.kts                      # Kotlin DSL, Kotlin 2.2.21, Gson, kotlinx-serialization, JUnit 5
├── settings.gradle.kts
├── gradle/wrapper/
│
├── src/main/kotlin/com/ace/shared/
│   ├── dto/                              # DTOs inmutables (data classes)
│   │   ├── ExerciseBlockDto.kt           # Bloque de ejercicio (payload principal)
│   │   ├── ExerciseSessionDto.kt         # Sesión (solo para referencia, no viaja en batch)
│   │   ├── ClientStatsDto.kt             # Estadísticas del móvil (S10)
│   │   ├── XpFormulaDto.kt               # Fórmula de XP (S5)
│   │   ├── XpAwardedResponseDto.kt       # Respuesta de XP aceptada (S5)
│   │   ├── RankingEntryDto.kt            # Entrada de ranking (S6)
│   │   ├── RankingResponseDto.kt         # Respuesta de ranking (S6)
│   │   ├── StreakStateDto.kt             # Estado de racha (S7)
│   │   ├── AuthRequestDto.kt             # Login/Register (S4)
│   │   ├── AuthResponseDto.kt            # Tokens (S4)
│   │   ├── RefreshTokenRequestDto.kt     # Renovación (S4)
│   │   └── SyncBatchRequestDto.kt        # Batch de bloques + stats (S3, S10)
│   │
│   ├── enums/
│   │   ├── SportType.kt                  # RUNNING, CYCLING, WALKING...
│   │   ├── SessionStatus.kt              # ACTIVE, PAUSED, COMPLETED, ABORTED (S2)
│   │   ├── BlockStatus.kt                # PENDING, SYNCING, SYNCED, ERROR (S3)
│   │   └── NotificationChannelId.kt      # ace_session_active, ace_streak_reminder, ace_sync_error (S8)
│   │
│   ├── constants/
│   │   ├── DataLayerPaths.kt             # Paths del Wear OS Data Layer (S1)
│   │   ├── ApiEndpoints.kt               # Rutas de API REST (S3, S4, S5, S6)
│   │   ├── SyncConstants.kt              # BATCH_MAX_SIZE = 20, BACKOFF_DELAYS (S3)
│   │   ├── AuthConstants.kt              # ACCESS_TTL_MINUTES = 15, REFRESH_TTL_DAYS = 7 (S4)
│   │   ├── XpConstants.kt                # BLOCK_DURATION_SECONDS = 300, TOLERANCE_PERCENT = 10 (S2, S5)
│   │   ├── RankingConstants.kt           # CACHE_TTL_HOURS = 1, TOP_LIMIT = 100 (S6)
│   │   ├── StreakConstants.kt            # CHECK_HOUR = 20, CHECK_MINUTE = 0 (S7, S8)
│   │   └── HistoryConstants.kt           # MAX_LOCAL_SESSIONS = 5 (S9)
│   │
│   └── serialization/
│       ├── GsonFactory.kt                # Configuración central de Gson
│       ├── InstantAdapter.kt             # Epoch millis ↔ Instant (NO ISO-8601 strings)
│       ├── UuidAdapter.kt                # UUID ↔ String
│       └── KotlinxSerializationConfig.kt # Configuración de kotlinx-serialization
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
@KeepForGson  // Anotación personalizada si necesitas compatibilidad dual
data class ExerciseBlockDto(
    val blockId: String,              // UUID generado por móvil al cerrar bloque (S2 §3.1)
    val sessionId: String,            // UUID generado por móvil al crear sesión (S2 §2.1)
    val userId: String,               // Del JWT o cache local
    val deviceId: String,             // UUID de instalación del móvil (S4 §3.3)
    val sportType: SportType,           // Enum compartido
    val timestampStart: Long,           // Epoch millis, primer heartbeat del bloque (S2 §3.1)
    val timestampEnd: Long,             // Epoch millis, último heartbeat del bloque
    val durationSeconds: Int,           // timestampEnd - timestampStart (S2 §4.2)
    val avgBpm: Double,                 // Promedio aritmético de muestras (S2 §4.2)
    val maxBpm: Double,                 // Valor máximo
    val minBpm: Double,                 // Valor mínimo
    val sampleCount: Int,               // Cantidad de muestras recibidas
    val xpCalculated: Int,              // Calculado por móvil, validado por backend (S5 §3.3)
    val schemaVersion: Int = 1          // Versión del DTO (actual = 1)
)
```

**Invariantes del DTO:**
- `blockId` es UUIDv4 generado por el **móvil** al cerrar el bloque. El backend NUNCA lo regenera.
- `timestampStart` es la fuente de verdad temporal. El backend no reinterpreta.
- `durationSeconds` debe estar entre 270 y 330 (±10% de 300). Fuera de rango = rechazo `422` (S5 §4.3).
- `xpCalculated` debe ser ≤ `maxXpPerBlock` definido en la fórmula del deporte.
- `schemaVersion` permite evolución del contrato sin breaking changes.

### 4.2 SyncBatchRequestDto (S3, S10)

```kotlin
@Serializable
data class SyncBatchRequestDto(
    val blocks: List<ExerciseBlockDto>,     // Máximo 20 bloques (S3 §3.2)
    val clientStats: ClientStatsDto,        // Estadísticas locales del móvil (S10 §3.2)
    val clientTimestamp: Long,              // Epoch millis del dispositivo (para detección de drift)
    val deviceId: String                    // Identificador de instalación
)
```

**Invariantes:**
- `blocks` nunca vacío. Máximo 20 elementos.
- `clientStats` incluido en **cada batch** (S10 §3.5). No petición separada.
- Cada bloque debe tener `xpCalculated` definido.

### 4.3 ClientStatsDto (S10)

```kotlin
@Serializable
data class ClientStatsDto(
    val totalXp: Long,                  // Suma de xpCalculated de bloques locales
    val totalSessions: Int,               // Sesiones completadas
    val totalBlocks: Int,                 // Bloques cerrados
    val totalDurationSeconds: Long,       // Tiempo total de ejercicio
    val avgBpmAllTime: Double,            // Promedio ponderado por sampleCount (S10 §2.4)
    val lastUpdated: Long                 // Epoch millis de última actualización local
)
```

**Nota sobre `avgBpmAllTime`:** El backend valida que el valor reportado sea coherente con los bloques recibidos, pero no recalcula desde cero (S10 §4.2).

### 4.4 XpFormulaDto (S5)

```kotlin
@Serializable
data class XpFormulaDto(
    val sportType: SportType,
    val minBpm: Int,                    // Umbral mínimo para generar XP
    val xpPerMinute: Int,               // Multiplicador base
    val maxXpPerBlock: Int,               // Techo anti-trampa
    val version: Int                      // Versión de la fórmula para invalidar cache
)
```

### 4.5 XpAwardedResponseDto (S5)

```kotlin
@Serializable
data class XpAwardedResponseDto(
    val blockId: String,
    val xpAccepted: Int,                // XP que el backend validó y persistió
    val newTotalXp: Long,                 // Total acumulado del usuario (balance_after)
    val rankChanged: Boolean,               // Si cambió posición en ranking (S6)
    val currentStreak: Int,               // Racha actual (S7)
    val lastExerciseDate: String,           // Fecha del último bloque validado (S7) - ISO-8601 date
    val bestStreak: Int,                  // Racha máxima histórica (S7)
    val correctionApplied: Boolean        // Si se aplicó corrección de stats (S10)
)
```

### 4.6 AuthResponseDto (S4)

```kotlin
@Serializable
data class AuthResponseDto(
    val accessToken: String,            // JWT firmado, 15 minutos
    val refreshToken: String,           // Token stateful, 7 días
    val expiresIn: Long,                // Segundos hasta expiración del access
    val tokenType: String,              // "Bearer"
    val deviceId: String                // Confirmado por backend
)
```

### 4.7 RankingResponseDto (S6)

```kotlin
@Serializable
data class RankingResponseDto(
    val myPosition: Int,
    val myTotalXp: Long,
    val top: List<RankingEntryDto>,     // Top 100 (o 10 para cache móvil)
    val lastUpdated: String,              // ISO-8601 del último recálculo
    val type: String                      // "GLOBAL" o "MUNICIPAL_{cityId}"
)

@Serializable
data class RankingEntryDto(
    val position: Int,
    val userId: String,
    val totalXp: Long,
    val username: String
)
```

### 4.8 StreakStateDto (S7)

```kotlin
@Serializable
data class StreakStateDto(
    val currentStreak: Int,
    val bestStreak: Int,
    val lastExerciseDate: String          // ISO-8601 date (sin hora)
)
```

---

## 5. Enums Detallados

### 5.1 SportType

```kotlin
enum class SportType {
    RUNNING,
    CYCLING,
    WALKING;
    // MVP: solo RUNNING tiene fórmula activa. Los demás son stubs para OCP.
}
```

### 5.2 SessionStatus (S2 §2.2)

```kotlin
enum class SessionStatus {
    ACTIVE,     // Sesión en curso
    PAUSED,     // Sesión pausada (no genera bloques nuevos)
    COMPLETED,  // Cerrada normalmente
    ABORTED;    // Cerrada forzosamente (nueva sesión o error)
}
```

### 5.3 BlockStatus (S3 §2.3)

```kotlin
enum class BlockStatus {
    PENDING,    // Listo para sync
    SYNCING,    // En vuelo
    SYNCED,     // Confirmado por backend
    ERROR;      // Rechazado (422) o reintentos agotados
}
```

### 5.4 NotificationChannelId (S8 §5.2)

```kotlin
enum class NotificationChannelId(
    val channelId: String,
    val importance: Importance
) {
    ACE_SESSION_ACTIVE("ace_session_active", Importance.LOW),
    ACE_STREAK_REMINDER("ace_streak_reminder", Importance.HIGH),
    ACE_SYNC_ERROR("ace_sync_error", Importance.HIGH);

    enum class Importance { LOW, HIGH }
}
```

---

## 6. Constantes de Contrato

### 6.1 DataLayerPaths (S1 §2.2)

```kotlin
object DataLayerPaths {
    const val HEART_RATE = "/ace/health/heart_rate"
    const val SESSION_STATUS = "/ace/session/{sessionId}/status"
    const val COMMAND = "/ace/command"
}
```

**Nota:** El path es `/ace/health/heart_rate` (con `health/`), no `/ace/heart_rate`.

### 6.2 SyncConstants (S3)

```kotlin
object SyncConstants {
    const val BATCH_MAX_SIZE = 20
    const val MAX_RETRIES = 5
    val BACKOFF_DELAYS_MINUTES = listOf(15L, 30L, 60L, 120L, 240L)
    const val BLOCK_DURATION_SECONDS = 300
    const val BLOCK_DURATION_TOLERANCE_PERCENT = 10
}
```

### 6.3 AuthConstants (S4)

```kotlin
object AuthConstants {
    const val ACCESS_TOKEN_TTL_MINUTES = 15
    const val REFRESH_TOKEN_TTL_DAYS = 7
    const val AUTH_HEADER = "Authorization"
    const val BEARER_PREFIX = "Bearer "
    const val REFRESH_REUSED_ERROR = "REFRESH_REUSED"
    const val REFRESH_REVOKED_ERROR = "REFRESH_REVOKED"
}
```

### 6.4 XpConstants (S5)

```kotlin
object XpConstants {
    const val FORMULA_VERSION_HEADER = 1
    const val FORMULA_VERSION_HEADER_NAME = "X-Formula-Version"
    const val DEFAULT_MIN_BPM = 80
    const val DEFAULT_XP_PER_MINUTE = 2
    const val DEFAULT_MAX_XP_PER_BLOCK = 30
}
```

### 6.5 RankingConstants (S6)

```kotlin
object RankingConstants {
    const val RECALCULATION_INTERVAL_HOURS = 1
    const val TOP_GLOBAL_LIMIT = 100
    const val TOP_MUNICIPAL_LIMIT = 100
    const val MOBILE_CACHE_TOP_LIMIT = 10
    const val MOBILE_CACHE_TTL_HOURS = 1
}
```

### 6.6 StreakConstants (S7, S8)

```kotlin
object StreakConstants {
    const val CHECK_HOUR = 20      // 8 PM
    const val CHECK_MINUTE = 0
    const val STREAK_NOTIFICATION_TITLE = "🔥 Tu racha está en peligro"
    const val STREAK_NOTIFICATION_BODY = "No has entrenado hoy. ¡Sal a correr para mantener tu racha de %d días!"
}
```

---

## 7. Serialización (Gson + kotlinx-serialization)

### 7.1 Reglas de serialización

- **Timestamps:** Todos los timestamps internos se serializan como **epoch millis (Long)**. NO como ISO-8601 strings.
- **UUIDs:** Serializados como strings.
- **Enums:** Serializados como strings en UPPER_SNAKE_CASE.
- **Instantes:** Usar `InstantAdapter` que convierte `Instant` ↔ `Long` (epoch millis).

### 7.2 GsonFactory

```kotlin
object GsonFactory {
    fun create(): Gson = GsonBuilder()
        .registerTypeAdapter(Instant::class.java, InstantAdapter())
        .registerTypeAdapter(UUID::class.java, UuidAdapter())
        .registerTypeAdapter(SportType::class.java, SportTypeAdapter())
        .create()
}
```

### 7.3 KotlinxSerializationConfig

```kotlin
object KotlinxSerializationConfig {
    val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        isLenient = false
    }
}
```

### 7.4 InstantAdapter (Gson)

```kotlin
class InstantAdapter : TypeAdapter<Instant>() {
    override fun write(out: JsonWriter, value: Instant) {
        out.value(value.toEpochMilli())
    }
    override fun read(`in`: JsonReader): Instant {
        return Instant.ofEpochMilli(`in`.nextLong())
    }
}
```

### 7.5 UuidAdapter (Gson)

```kotlin
class UuidAdapter : TypeAdapter<UUID>() {
    override fun write(out: JsonWriter, value: UUID) {
        out.value(value.toString())
    }
    override fun read(`in`: JsonReader): UUID {
        return UUID.fromString(`in`.nextString())
    }
}
```

### 7.6 SportTypeAdapter (Gson)

```kotlin
class SportTypeAdapter : TypeAdapter<SportType>() {
    override fun write(out: JsonWriter, value: SportType) {
        out.value(value.name)
    }
    override fun read(`in`: JsonReader): SportType {
        return SportType.valueOf(`in`.nextString())
    }
}
```

---

## 8. Versionado y Publicación

### 8.1 Versionado semántico

- `:shared` sigue versionado semántico: `MAJOR.MINOR.PATCH`.
- **Breaking change** (campo obligatorio nuevo, renombre, tipo cambiado) → sube `MAJOR`.
- **Adición opcional** (campo nuevo con default) → sube `MINOR`.
- **Fix** (corrección de adapter) → sube `PATCH`.

### 8.2 Publicación

- Publicado en **GitHub Packages** como JAR.
- Backend y mobile declaran la dependencia en `build.gradle.kts`.
- Nunca se modifica `:shared` dentro del módulo consumidor.

### 8.3 Proceso de cambio

1. Proponer cambio en `:shared` con PR.
2. Revisión por Reinaldo (backend) y Steven (mobile).
3. Si es breaking change, ambos equipos deben actualizar antes de mergear.
4. Publicar nueva versión en GitHub Packages.
5. Ambos equipos actualizan dependencia y adaptan código.

---

## 9. Checklist de Coherencia con Apéndices

| Apéndice | DTO/Enum/Constante | Verificación |
|----------|---------------------|--------------|
| S1 | `DataLayerPaths.HEART_RATE` | Path = `/ace/health/heart_rate` ✓ |
| S2 | `ExerciseBlockDto`, `SessionStatus` | `blockId` UUID móvil, `timestampStart` fuente de verdad ✓ |
| S3 | `SyncBatchRequestDto`, `BlockStatus`, `SyncConstants` | Batch ≤ 20, backoff exponencial, 5 reintentos ✓ |
| S4 | `AuthResponseDto`, `AuthConstants` | Access 15min, Refresh 7días, rotación ✓ |
| S5 | `XpFormulaDto`, `XpAwardedResponseDto`, `XpConstants` | Fórmulas cacheadas, validación backend, `maxXpPerBlock` ✓ |
| S6 | `RankingResponseDto`, `RankingEntryDto`, `RankingConstants` | Recálculo 1h, top 100, cache 1h ✓ |
| S7 | `StreakStateDto`, `StreakConstants` | Evaluación backend, notificación 20:00 local ✓ |
| S8 | `NotificationChannelId` | 3 canales: session_active, streak_reminder, sync_error ✓ |
| S9 | `HistoryConstants.MAX_LOCAL_SESSIONS` | 5 sesiones, sin discriminar categoría ✓ |
| S10 | `ClientStatsDto` | En cada batch, promedio ponderado, corrección silenciosa < 10 XP ✓ |

---

## 10. Nota sobre Coherencia de Kotlin en el Ecosistema A.C.E

| Módulo | Kotlin | ¿Por qué? | Coherencia |
|--------|--------|-----------|------------|
| **Backend** | 2.2.21 | Baseline obligatorio de Spring Boot 4.0 | Mismo lenguaje, versión adaptada a plataforma |
| **Mobile** | 2.1.20 | Built-in en AGP 9.0.1 | Mismo lenguaje, versión adaptada a plataforma |
| **Wear** | 2.0.21 | Built-in en AGP 9.0.1 | Mismo lenguaje, versión adaptada a plataforma |
| **:shared** | 2.2.21 | JVM puro, coherente con backend | Mismo lenguaje, versión adaptada a plataforma |

**No intentar unificar a una sola versión de Kotlin.** Cada plataforma tiene su propio toolchain y baseline. `:shared` es JVM puro y puede usar la versión más reciente (2.2.21) sin estar limitado por AGP 9.0. La coherencia se mantiene a nivel de:
- **Lenguaje:** Kotlin en los cuatro módulos
- **JVM Target:** 21 en los cuatro módulos
- **Serialización:** Gson + kotlinx-serialization en backend, mobile y :shared
- **Coroutines:** 1.9.0 en backend, mobile y wear

---

*Documento coherente con Apéndices S1-S10 y Arquitectura A.C.E v0.2. Cualquier divergencia debe ser reportada como bug de coherencia.*
