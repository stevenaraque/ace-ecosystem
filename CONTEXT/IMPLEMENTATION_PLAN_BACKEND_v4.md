# A.C.E — Implementation Plan: Backend (`ace-backend`)

> **Estado:** Coherente con Apéndices Aprobados S1-S10 y Arquitectura v0.2  
> **Versión:** 4.0 (Actualizado a Spring Boot 4.0 + Kotlin 2.2 + compilerOptions DSL)  
> **Fecha:** Junio 2026  
> **Stack:** Spring Boot 4.0.6 · Kotlin 2.2.21 · Gradle 8.10+ (Kotlin DSL) · PostgreSQL 16 · Flyway 10.15 · kotlinx-serialization  
> **Depende de:** `:shared` JAR v1.0.0+  
> **Responsables:** Reinaldo, Santiago (Backend)

---

## 1. Visión y Alcance (Coherente con Arquitectura §1)

El backend es la **única fuente de verdad** para:

1. **Validación** de identidad (JWT híbrido, S4).
2. **Validación** de sanidad de bloques y XP (NO recálculo, S5).
3. **Persistencia** inmutable de transacciones XP y bloques (S3, S5).
4. **Evaluación** de rachas (S7) y rangos (S5).
5. **Recálculo batch** de ranking global y municipal (S6).
6. **Corrección** de estadísticas cuando detecta inconsistencias (S10).

**Principio rector:** *El reloj captura, el móvil calcula y transporta, el backend valida y decide.* (Arquitectura §1)

**Regla de oro:** El backend **NUNCA** recalcula XP desde cero. Solo valida que la XP reportada por el móvil sea consistente con las métricas del bloque (S5 §4.1).

---

## 2. Dependencias Gradle (`build.gradle.kts`)

### 2.1. `build.gradle.kts` (Kotlin DSL)

```kotlin
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("org.springframework.boot") version "4.0.6"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.spring") version "2.2.21"
    kotlin("plugin.jpa") version "2.2.21"
    kotlin("plugin.serialization") version "2.2.21"
    id("org.flywaydb.flyway") version "10.15.0"
    `maven-publish`
}

group = "sena.adso"
version = "1.0.0"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenCentral()
    maven { url = uri("https://repo.spring.io/milestone") }
    // GitHub Packages para :shared
    maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/tu-org/ace-shared")
        credentials {
            username = System.getenv("GITHUB_ACTOR")
            password = System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    // ─── Módulo :shared ───
    implementation("com.ace:shared:1.0.0")

    // ─── Spring Boot Starters ───
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-kotlin-serialization")

    // ─── Kotlin ───
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.9.0")

    // ─── JWT ───
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

    // ─── PostgreSQL ───
    runtimeOnly("org.postgresql:postgresql:42.7.3")

    // ─── Flyway (Migraciones) ───
    implementation("org.flywaydb:flyway-core:10.15.0")
    runtimeOnly("org.flywaydb:flyway-database-postgresql:10.15.0")

    // ─── BCrypt (Hash de contraseñas) ───
    implementation("org.mindrot:jbcrypt:0.4")

    // ─── Logging ───
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")

    // ─── dotenv ───
    runtimeOnly("me.paulschwarz:spring-dotenv:4.0.0")

    // ─── Testing ───
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("io.mockk:mockk:1.13.12")
    testImplementation("org.testcontainers:junit-jupiter:1.19.8")
    testImplementation("org.testcontainers:postgresql:1.19.8")
    testImplementation("org.assertj:assertj-core:3.26.3")
}

// ✅ compilerOptions DSL (Kotlin 2.2+ — reemplaza kotlinOptions deprecado)
kotlin {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xjsr305=strict",
            "-Xannotation-default-target=param-property"
        )
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// ─── Flyway Config ───
flyway {
    url = "jdbc:postgresql://localhost:5432/ace_db"
    user = "ace_user"
    password = "ace_password"
    locations = arrayOf("filesystem:src/main/resources/db/migration")
}
```

### 2.2. Notas críticas sobre Spring Boot 4.0 + Kotlin 2.2

| Aspecto | Pre-Spring Boot 4.0 (plan v3.0) | Spring Boot 4.0 (plan v4.0) | Justificación |
|---------|-------------------------------|-------------------------------|---------------|
| **Spring Boot** | 3.3.2 | **4.0.6** | Última estable (abril 2026). Soporte hasta dic 2026. |
| **Kotlin** | 2.1.0 | **2.2.21** | **Baseline obligatorio** de Spring Boot 4.0. No se puede usar 2.1.x. |
| **Plugin Kotlin JVM** | `kotlin("jvm")` | `kotlin("jvm")` (mantenido) | Versión actualizada a 2.2.21. |
| **Plugin Kotlin Spring** | `kotlin("plugin.spring")` | `kotlin("plugin.spring")` (mantenido) | Versión actualizada a 2.2.21. |
| **Plugin Kotlin JPA** | `kotlin("plugin.jpa")` | `kotlin("plugin.jpa")` (mantenido) | Versión actualizada a 2.2.21. |
| **Plugin Kotlin Serialization** | No usado | **`kotlin("plugin.serialization")`** | Soporte nativo de kotlinx-serialization en Spring Boot 4.0. |
| **Spring Dependency Management** | 1.1.6 | **1.1.7** | Última estable (diciembre 2024). |
| **Moshi** | 1.15.1 | **Eliminado** | Reemplazado por Gson + kotlinx-serialization (coherente con mobile). |
| **Gson** | No usado | **Usado** | Coherente con mobile (`retrofit-converter-gson`). |
| **kotlinx-serialization** | No usado | **`spring-boot-starter-kotlin-serialization`** | Starter nativo de Spring Boot 4.0. |
| **`kotlinOptions { jvmTarget }`** | `"21"` | **Eliminado** | Deprecado en Kotlin 2.2. Se usa `compilerOptions` DSL. |
| **`compilerOptions` DSL** | No existía | **`kotlin { compilerOptions { ... } }`** | Nuevo DSL obligatorio en Kotlin 2.2+. |
| **`JvmTarget.JVM_21`** | No usado | **`jvmTarget.set(JvmTarget.JVM_21)`** | API tipada para especificar target JVM. |
| **Java** | 21 | **21** (mantenido) | Compatible con Spring Boot 4.0. |
| **Flyway** | 10.15.0 | **10.15.0** (mantenido) | Conservado como requerido. |

---

## 3. Arquitectura y Patrones (Coherente con Arquitectura §2.1)

| Patrón | Dónde vive | Justificación | Coherencia con Apéndice |
|--------|-----------|---------------|------------------------|
| **DTO** | Consumido vía `:shared` | Contrato inmutable. Breaking change rompe compilación en ambos lados. | S3, S5, S10 |
| **Repository** | `repository/` | Abstrae PostgreSQL. Cambio de DB sin tocar servicios. | S3, S6, S10 |
| **Strategy** | `service/validation/` | Cada deporte tiene su propio **validador** de XP (no calculador). | S5 §4.3 |
| **Singleton** | `JwtService`, configuraciones Spring | Tokens sin side-effects. Spring gestiona ciclo de vida. | S4 §2.2 |
| **Observer** | `scheduler/` + EventListeners | Ranking recalculado por job, no on-write. | S6 §2.1 |
| **Layered Architecture** | Controller → Service → Repository → Domain | Controllers solo deserializan y delegan. | Todos |

---

## 4. Estructura de Carpetas

```
ace-backend/
├── build.gradle.kts                      # Kotlin DSL, Spring Boot 4.0.6, Kotlin 2.2.21, compilerOptions DSL
├── settings.gradle.kts
├── gradle/wrapper/
│
├── src/main/kotlin/com/ace/backend/
│   ├── AceBackendApplication.kt
│   │
│   ├── config/
│   │   ├── SecurityConfig.kt             # CORS, CSRF deshabilitado (API stateless)
│   │   ├── JwtConfig.kt                  # Secret, issuer, TTL access/refresh
│   │   └── WebConfig.kt                  # Timezone UTC, logging
│   │
│   ├── domain/                             # ENTIDADES JPA PURAS — sin anotaciones de servicio
│   │   ├── user/
│   │   │   ├── User.kt                     # id, email, password_hash, role, created_at
│   │   │   ├── UserProfile.kt              # nickname, weight, birth_date, city_id (FK)
│   │   │   └── UserStreak.kt               # current_streak, best_streak, last_exercise_date (S7)
│   │   ├── exercise/
│   │   │   ├── ExerciseSession.kt          # id, user_id, sport_type, started_at, ended_at, status
│   │   │   ├── ExerciseBlock.kt            # id (UUID del móvil), session_id, device_id, metrics_jsonb, xp_awarded
│   │   │   └── SportType.kt                # Enum interno (mapea desde :shared para JPA)
│   │   ├── gamification/
│   │   │   ├── UserRank.kt                 # user_id, current_rank, total_xp, updated_at
│   │   │   ├── RankCatalog.kt              # BRONZE=0, SILVER=100, GOLD=250... (seed inicial)
│   │   │   └── XpTransaction.kt            # id, user_id, block_id, amount, balance_after, reason, timestamp (S5 §5.2)
│   │   ├── ranking/
│   │   │   ├── RankingGlobal.kt            # user_id, position (indexed), total_xp, updated_at (S6 §3.2)
│   │   │   └── RankingMunicipal.kt         # user_id, city_id, position (indexed), total_xp, updated_at
│   │   ├── auth/
│   │   │   └── RefreshToken.kt             # token_hash, user_id, device_id, expires_at, revoked_at, replaced_by (S4 §2.3)
│   │   └── audit/                          # TABLAS VACÍAS EN MVP — listas para máximo
│   │       ├── SuspicionAudit.kt
│   │       └── SessionGpsPoint.kt
│   │
│   ├── repository/                         # Spring Data JPA — interfaces, cero lógica
│   │   ├── UserRepository.kt
│   │   ├── ExerciseBlockRepository.kt
│   │   ├── ExerciseSessionRepository.kt
│   │   ├── RefreshTokenRepository.kt
│   │   ├── XpTransactionRepository.kt
│   │   ├── UserStreakRepository.kt
│   │   ├── UserRankRepository.kt
│   │   ├── RankingGlobalRepository.kt
│   │   └── RankingMunicipalRepository.kt
│   │
│   ├── service/                            # LÓGICA DE NEGOCIO — donde viven las reglas
│   │   ├── auth/
│   │   │   ├── AuthService.kt              # Login, registro, hash BCrypt
│   │   │   ├── JwtService.kt               # Generar/validar access token (stateless, Singleton)
│   │   │   └── RefreshTokenService.kt      # Crear, validar, revocar con SELECT FOR UPDATE (S4 §6.3)
│   │   ├── exercise/
│   │   │   ├── BlockProcessingService.kt     # Recibe batch, valida tamaño ≤ 20, persiste con ON CONFLICT DO NOTHING (S3 §4.5)
│   │   │   └── SessionValidationService.kt   # Valida 1 ACTIVE por usuario, aborta anterior si es nueva (S2 §2.4)
│   │   ├── gamification/
│   │   │   ├── XpValidationService.kt      # VALIDA sanidad de XP (NO calcula). Strategy por deporte. (S5 §4)
│   │   │   ├── RunningXpValidator.kt         # Reglas de validación para RUNNING (S5 §4.3)
│   │   │   ├── CyclingXpValidator.kt         # Stub para futuro deportes
│   │   │   ├── RankService.kt                # Evalúa si usuario sube de rango (S5 §7)
│   │   │   └── XpFormulaService.kt          # Expone GET /api/xp/formulas con X-Formula-Version (S5 §2.3)
│   │   ├── ranking/
│   │   │   └── RankingService.kt             # Recalcula tablas materializadas cada 1h (S6 §2)
│   │   ├── streak/
│   │   │   └── StreakEvaluationService.kt      # Evalúa racha al validar bloque (S7 §2)
│   │   └── stats/
│   │       └── StatsValidationService.kt     # Valida consistencia client_stats vs bloques (S10 §4)
│   │
│   ├── controller/                         # REST Controllers — solo delegan, deserializan DTOs de :shared
│   │   ├── AuthController.kt               # POST /api/auth/login, /refresh, /register, /logout
│   │   ├── ExerciseController.kt           # POST /api/exercise/blocks (batch ≤ 20)
│   │   ├── RankingController.kt            # GET /api/ranking/global, /municipal/{cityId}
│   │   ├── UserController.kt               # GET /api/user/profile, /stats
│   │   └── XpFormulaController.kt          # GET /api/xp/formulas (S5 §2.3)
│   │
│   ├── dto/                                # NO USAR — todos los DTOs vienen de :shared
│   │   └── [vacío — importar desde com.ace.shared.dto.*]
│   │
│   ├── security/
│   │   ├── JwtAuthenticationFilter.kt      # Valida access token en cada request (stateless)
│   │   └── CustomUserDetailsService.kt       # Carga User desde DB para Spring Security
│   │
│   ├── exception/
│   │   └── GlobalExceptionHandler.kt         # @ControllerAdvice: 401, 403, 409, 422, 429
│   │
│   └── scheduler/
│       ├── RankingRecalculationJob.kt        # @Scheduled(cron = "0 0 * * * *") — cada hora (S6 §2.2)
│       └── TokenCleanupJob.kt              # Elimina refresh tokens expirados hace > 30 días (S4 §7.3)
│
├── src/main/resources/
│   ├── application.yml
│   ├── application-dev.yml
│   ├── application-prod.yml
│   └── db/migration/
│       ├── V1__init_schema.sql             # Todas las tablas (Opción B), constraints mínimas
│       ├── V2__seed_rank_catalog.sql       # Bronce, Plata, Oro...
│       ├── V3__seed_city_catalog.sql       # Bogotá, Medellín, Cali...
│       └── V4__add_indexes.sql             # Índices en ranking.position, xp_transactions.user_id, etc.
│
├── src/test/
│   ├── unit/                               # Tests aislados: XpValidator, JwtService, StreakEvaluation
│   └── integration/                        # TestContainers PostgreSQL
│       ├── AuthIntegrationTest.kt          # JWT refresh, rotación, race condition
│       ├── ExerciseBlockIntegrationTest.kt   # Batch, idempotencia, validación 422
│       └── StreakIntegrationTest.kt          # Evaluación de racha con bloques atrasados
│
└── docker-compose.yml                      # PostgreSQL 16 + pgAdmin
```

---

## 5. Flujos de Datos Conceptuales (Coherentes con Apéndices)

### 5.1 Autenticación JWT Híbrida (S4)

```kotlin
// Login → AuthService valida BCrypt → emite access_token (15 min) + refresh_token (7 días)
// → persiste hash de refresh en PostgreSQL con token_id UUID
// → responde AuthResponseDto (de :shared)

// Refresh:
// 1. Mobile envía POST /api/auth/refresh con RefreshTokenRequestDto
// 2. RefreshTokenService ejecuta SELECT FOR UPDATE (S4 §6.3):
//    BEGIN;
//    SELECT * FROM refresh_tokens WHERE token_hash = ? FOR UPDATE;
//    -- Verificar no revocado, no expirado, device_id coincide
//    UPDATE refresh_tokens SET revoked_at = now(), replaced_by = ? WHERE id = ?;
//    INSERT INTO refresh_tokens (token_hash, user_id, device_id, expires_at) VALUES (?, ?, ?, ?);
//    COMMIT;
// 3. Si segundo refresh concurrente: ve token revocado → 401 REFRESH_REUSED
// 4. Emite nuevo par access + refresh
// 5. Mobile guarda nuevos tokens

// Logout:
// 1. POST /api/auth/logout con refresh token
// 2. Marca refresh como revocado (revoked_at = now())
// 3. Access token sigue válido hasta expirar (15 min), aceptable en MVP
```

### 5.2 Recepción y Validación de Bloques (S3, S5)

```kotlin
// POST /api/exercise/blocks
// Body: SyncBatchRequestDto (de :shared)
//
// 1. JwtAuthenticationFilter valida access token (stateless)
// 2. ExerciseController deserializa SyncBatchRequestDto
// 3. BlockProcessingService:
//    a. Valida batch size ≤ 20 (SyncConstants.BATCH_MAX_SIZE). Si > 20 → 400 Bad Request.
//    b. Para cada ExerciseBlockDto:
//       - Valida schemaVersion (actual = 1). Si no coincide → 422.
//       - Valida duración: 270 ≤ duration_seconds ≤ 330. Si no → 422.
//       - Valida avg_bpm: 30 ≤ avg_bpm ≤ 250. Si no → 422.
//       - Valida sample_count coherente con duration. Si no → 422.
//       - Delega a XpValidationService (Strategy por sport_type):
//         * RunningXpValidator: verifica que xp_calculated ≤ (duration/60) * xp_per_minute
//         * Verifica que xp_calculated ≤ max_xp_per_block
//         * Si xp_calculated > fórmula actual → 422 (posible trampa)
//         * Si xp_calculated < fórmula actual → ACEPTA (móvil tenía fórmula vieja)
//       - Si pasa validación: inserta exercise_blocks con ON CONFLICT (id) DO NOTHING (idempotencia)
//       - Inserta xp_transaction con amount = xp_calculated, balance_after recalculado (S5 §5.2)
//       - Evalúa racha: StreakEvaluationService compara timestamp_start con last_exercise_date (S7 §2.3)
//       - Evalúa rango: RankService verifica si total_xp cruza umbral (S5 §7)
//    c. Valida consistencia de client_stats (S10 §4.2):
//       - total_xp reportado ≤ suma de amount en xp_transactions + bloques en vuelo
//       - total_sessions reportado ≤ sesiones distintas en bloques SYNCED
//       - Si discrepancia > margen: responde official_stats con corrección
// 4. Responde lista de XpAwardedResponseDto (uno por bloque):
//    - xpAccepted, newTotalXp, rankChanged, currentStreak, lastExerciseDate, bestStreak, correctionApplied
```

### 5.3 Recálculo de Ranking (S6)

```kotlin
// @Scheduled(cron = "0 0 * * * *") — cada hora
// RankingService:
// 1. Consulta xp_transactions, agrupa por user_id, lee balance_after más reciente (O(1) por usuario)
// 2. Ordena por total_xp descendente
// 3. Asigna position = 1, 2, 3... (sin gaps, empates por user_id determinista)
// 4. TRUNCATE ranking_global (o marca obsoletas)
// 5. INSERT nuevas posiciones
// 6. Repite filtrando por city_id para ranking_municipal
// 7. COMMIT
//
// Nota: Durante la transacción, lecturas ven datos parcialmente actualizados. Aceptable porque ranking es eventualmente consistente (S6 §3.3).
```

### 5.4 Evaluación de Racha (S7)

```kotlin
// StreakEvaluationService (ejecutado dentro de la misma transacción que inserta bloque):
//
// Entrada: timestamp_start del bloque (truncado a fecha)
// Lógica:
// 1. Lee last_exercise_date del usuario
// 2. Compara:
//    - Si block_date == last_exercise_date → no cambia (S7 §2.3, escenario "Mismo día")
//    - Si block_date == last_exercise_date + 1 día → current_streak += 1 (escenario "Día siguiente")
//    - Si block_date > last_exercise_date + 1 día → current_streak = 1 (escenario "Hueco > 1 día")
//    - Si last_exercise_date IS NULL → current_streak = 1, best_streak = 1 (escenario "Primera vez")
// 3. Si current_streak > best_streak → actualiza best_streak
// 4. Actualiza last_exercise_date = block_date
// 5. Incluye current_streak, best_streak, last_exercise_date en respuesta de sync
//
// Invariantes:
// - Usa SIEMPRE timestamp_start del bloque, NUNCA server_received_at (S7 §2.5)
// - NUNCA decrementa current_streak directamente. Solo incrementa o resetea a 1.
// - Un bloque rechazado (422) NO dispara evaluación (S7 §2.5)
```

---

## 6. Decisiones Técnicas y Trade-offs (Coherentes con Arquitectura §14)

| Decisión | Valor | Justificación | Trade-off | Apéndice |
|----------|-------|---------------|-----------|----------|
| **Spring Boot 4.0.6** | Última estable | Features de seguridad y performance. Soporte hasta dic 2026. | — | — |
| **Kotlin 2.2.21** | Baseline de Spring Boot 4.0 | Corrutinas, null-safety. Unificación con mobile/wear (mismo lenguaje, versión adaptada a plataforma). | Mobile/wear usan 2.1.20/2.0.21 por AGP 9.0 built-in. Backend requiere 2.2.21. | — |
| **Gradle Kotlin DSL** | Tipado en scripts | Mejor que Groovy para detección temprana de errores. Coherente con mobile/wear. | Migración desde Groovy requiere ajuste. | — |
| **compilerOptions DSL** | `kotlin { compilerOptions { ... } }` | Reemplaza `kotlinOptions` deprecado en Kotlin 2.2+. | Necesita `import org.jetbrains.kotlin.gradle.dsl.JvmTarget`. | — |
| **JWT Híbrido** | Access 15min + Refresh 7días | Revocación inmediata, rotación reduce ventana de ataque. | Complejidad en mobile (refresh automático). | S4 §2.2 |
| **XpValidation (NO cálculo)** | Strategy por deporte | El backend valida, no recalcula. OCP para nuevos deportes. | Solo 1 implementación activa en MVP. | S5 §4.1 |
| **PostgreSQL + JSONB** | Métricas flexibles | Relaciones fuertes + flexibilidad para métricas de sensor. | Escritura JSONB más lenta que columnas planas. | S3 §4.2 |
| **Flyway 10.15** | Migraciones versionadas | Reproducibilidad entre dev/staging/prod. | Equipo debe aprender SQL de migración. | — |
| **Esquema Opción B** | Tablas máximo desde día 1 | Migraciones futuras más simples. | Esquema inicial más grande (pero vacío). | — |
| **Ranking cada hora** | Job @Scheduled | Reduce carga de escritura. Ranking no necesita ser exacto al segundo. | Delay de hasta 1h en reflejar bloque nuevo. | S6 §2.2 |
| **ON CONFLICT DO NOTHING** | Idempotencia por block_id | Reenvío seguro sin duplicar. | Requiere block_id UUID del móvil. | S3 §4.5 |
| **balance_after** | Campo en xp_transactions | Consulta O(1) del total. No requiere SUM() en toda la tabla. | Ligera redundancia, pero acelera ranking. | S5 §5.3 |
| **SELECT FOR UPDATE** | Transacción atómica refresh | Previene race condition en rotación de refresh tokens. | Bloquea fila durante transacción. | S4 §6.3 |
| **UserStreak en Fase Mínima** | Sí | Coherente con mobile que implementa CheckStreakWorker en fase mínima. | Añade complejidad inicial. | S7 §1 |
| **jjwt 0.12.6** | Sí | Librería estándar JWT para JVM. Compatible con Spring Security. | API cambió significativamente desde 0.11. | S4 |
| **jbcrypt 0.4** | Sí | Hash BCrypt estándar. Simple y probado. | No tiene actualizaciones recientes, pero es estable. | S4 |
| **TestContainers** | Sí | PostgreSQL real en tests de integración. | Requiere Docker en CI/CD. | — |
| **kotlinx-serialization** | Sí | Soporte nativo en Spring Boot 4.0. Reemplaza Moshi. | Si :shared usa Moshi, requiere migración de DTOs. | — |
| **Gson** | Sí | Coherente con mobile (`retrofit-converter-gson`). | Si backend necesita comportamientos específicos de Moshi, requiere ajuste. | — |

---

## 7. Contratos de API con Mobile (Coherentes con :shared)

### 7.1 Endpoints y DTOs

| Endpoint | Método | Request DTO (:shared) | Response DTO (:shared) | Apéndice |
|----------|--------|----------------------|------------------------|----------|
| `/api/auth/login` | POST | `AuthRequestDto` | `AuthResponseDto` | S4 §2 |
| `/api/auth/register` | POST | `AuthRequestDto` | `AuthResponseDto` | S4 §2 |
| `/api/auth/refresh` | POST | `RefreshTokenRequestDto` | `AuthResponseDto` | S4 §5.3 |
| `/api/auth/logout` | POST | `RefreshTokenRequestDto` | void | S4 §7.1 |
| `/api/exercise/blocks` | POST | `SyncBatchRequestDto` | `List<XpAwardedResponseDto>` | S3, S5, S10 |
| `/api/ranking/global` | GET | — | `RankingResponseDto` | S6 §4.2 |
| `/api/ranking/municipal/{cityId}` | GET | — | `RankingResponseDto` | S6 §4.2 |
| `/api/xp/formulas` | GET | — | `List<XpFormulaDto>` + header `X-Formula-Version` | S5 §2.3 |
| `/api/user/profile` | GET | — | `UserProfile` (entidad JPA, no DTO compartido) | S10 |
| `/api/user/stats` | GET | — | `official_stats` (en XpAwardedResponseDto) | S10 §3.3 |

### 7.2 Códigos de Error Específicos (Coherentes con Apéndices)

| Código | Body | Cuándo | Acción del Mobile |
|--------|------|--------|-------------------|
| `201 Created` | `List<XpAwardedResponseDto>` | Batch aceptado | Marcar SYNCED, actualizar caches |
| `400 Bad Request` | `{"error": "BATCH_SIZE_EXCEEDED"}` | Batch > 20 bloques | Dividir batch |
| `401 Unauthorized` | `{"error": "TOKEN_EXPIRED"}` | Access token expirado | Disparar refresh |
| `401 Unauthorized` | `{"error": "REFRESH_REUSED"}` | Refresh token ya usado | Limpiar tokens, forzar login |
| `401 Unauthorized` | `{"error": "REFRESH_REVOKED"}` | Refresh revocado remotamente | Limpiar tokens, forzar login |
| `422 Unprocessable` | `{"error": "BLOCK_VALIDATION_FAILED", "details": [...]}` | XP inconsistente, duración inválida | Marcar ERROR, revertir XP local |
| `429 Too Many Requests` | `{"error": "RATE_LIMITED"}` | Rate limit | Backoff exponencial |

---

## 8. Roadmap: Fase Mínima (Coherente con Apéndices)

### Fase Mínima (Semanas 1-4) — TODOS los sistemas S1-S10 presentes

- [ ] Esqueleto Spring Boot 4.0.6 + Kotlin 2.2.21 + compilerOptions DSL + :shared JAR
- [ ] PostgreSQL local con Docker Compose
- [ ] Flyway V1: todas las tablas (Opción B), V2: seeds rangos/ciudades, V3: índices
- [ ] **S4 Auth:** Registro/login, JWT híbrido (access 15min / refresh 7días), tabla `refresh_tokens` con **SELECT FOR UPDATE** en rotación
- [ ] **S4 Auth:** Endpoint `/api/auth/refresh` con rotación atómica y detección de **REFRESH_REUSED**
- [ ] **S3 Sync:** Endpoint `POST /api/exercise/blocks` con validación de **batch size ≤ 20**
- [ ] **S3 Sync:** Idempotencia con `ON CONFLICT (id) DO NOTHING` (block_id del móvil)
- [ ] **S5 XP:** Endpoint `GET /api/xp/formulas` con header `X-Formula-Version`
- [ ] **S5 XP:** `XpValidationService` con `RunningXpValidator` (valida, NO calcula)
- [ ] **S5 XP:** Validación de sanidad: duración 270-330s, avg_bpm 30-250, sample_count coherente, xp ≤ fórmula
- [ ] **S5 XP:** Tabla `xp_transactions` append-only con `balance_after`
- [ ] **S7 Streaks:** `UserStreak` tabla + `StreakEvaluationService` (evalúa en misma transacción que bloque)
- [ ] **S7 Streaks:** Respuesta de sync incluye `current_streak`, `best_streak`, `last_exercise_date`
- [ ] **S6 Ranking:** Tablas `ranking_global` y `ranking_municipal` con **índice en position**
- [ ] **S6 Ranking:** Job `@Scheduled` cada hora para recálculo batch
- [ ] **S6 Ranking:** Endpoints `GET /api/ranking/global` y `/municipal/{cityId}` con top 100
- [ ] **S10 Stats:** Validación de consistencia `client_stats` vs bloques recibidos
- [ ] **S10 Stats:** Respuesta con `official_stats` y `correction_applied`
- [ ] **S2 Session:** Validación de 1 ACTIVE por usuario, aborta anterior si llega nueva
- [ ] **S8 Notif:** NO FCM (backend no envía push, todo local en mobile)
- [ ] **S9 History:** NO expone endpoint de últimas 5 sesiones (backend tiene todas en `exercise_sessions`)
- [ ] Tablas de auditoría (`SuspicionAudit`, `SessionGpsPoint`) creadas pero **sin lógica de escritura**
- [ ] **compilerOptions DSL:** Verificar que build es successful con `kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_21) } }`
- [ ] **kotlinx-serialization:** Verificar que DTOs de :shared se serializan correctamente con Gson/kotlinx-serialization

### Fase de Transición (Semanas 5-8)
- [ ] Implementar `CyclingXpValidator` y `WalkingXpValidator` (stubs con validación básica)
- [ ] Anti-trampa mínima: validar que `timestampEnd - timestampStart ≈ 300s` (ya en fase mínima)
- [ ] Notificación backend (estructura para FCM push en futuro)
- [ ] Dashboard web administrativo (estructura básica)

### Fase Máxima (Semanas 9-12+)
- [ ] Anti-trampa completa: análisis de patrones, detección de velocidad imposible
- [ ] Ranking local (barrio) y filtros avanzados
- [ ] Eventos y desafíos temporales con multiplicadores de XP
- [ ] Migración a cloud (AWS/GCP) con RDS PostgreSQL

---

## 9. Checklist de Integración con Mobile (Coherente con Apéndices)

- [ ] Endpoint `POST /api/exercise/blocks` responde `201` con batch de prueba desde Postman
- [ ] `XpAwardedResponseDto` incluye: `xpAccepted`, `newTotalXp`, `rankChanged`, `currentStreak`, `lastExerciseDate`, `bestStreak`, `correctionApplied`
- [ ] Job de ranking actualiza posiciones dentro de 1 hora tras recibir bloque
- [ ] Refresh token rota correctamente: un refresh usado una vez no puede reusarse → `401 REFRESH_REUSED`
- [ ] `SELECT FOR UPDATE` en rotación de refresh token (verificar en logs de PostgreSQL)
- [ ] Índice en `ranking_global.position` y `ranking_municipal.position` (verificar con `EXPLAIN`)
- [ ] Validación de batch size: batch de 21 bloques debe devolver `400 BATCH_SIZE_EXCEEDED`
- [ ] `XpValidationService` rechaza bloque con `xpCalculated = 999` para 5min a 80bpm → `422`
- [ ] `XpValidationService` **acepta** bloque con `xpCalculated = 8` cuando fórmula dice 10 (móvil con fórmula vieja)
- [ ] Streak evaluation: bloque con `timestamp_start` de ayer no afecta racha de hoy
- [ ] `client_stats` en batch: backend valida coherencia y responde `official_stats`
- [ ] `:shared` JAR consumido correctamente (no DTOs duplicados en backend)
- [ ] **compilerOptions DSL:** build successful sin errores de `kotlinOptions` deprecado
- [ ] **kotlinx-serialization:** DTOs de :shared se deserializan correctamente sin Moshi

---

## 10. Nota sobre Coherencia de Kotlin en el Ecosistema A.C.E

| Módulo | Kotlin | ¿Por qué? | Coherencia |
|--------|--------|-----------|------------|
| **Backend** | 2.2.21 | Baseline obligatorio de Spring Boot 4.0 | Mismo lenguaje, versión adaptada a plataforma |
| **Mobile** | 2.1.20 | Built-in en AGP 9.0.1 | Mismo lenguaje, versión adaptada a plataforma |
| **Wear** | 2.0.21 | Built-in en AGP 9.0.1 | Mismo lenguaje, versión adaptada a plataforma |

**No intentar unificar a una sola versión de Kotlin.** Cada plataforma tiene su propio toolchain y baseline. La coherencia se mantiene a nivel de:
- **Lenguaje:** Kotlin en los tres módulos
- **JVM Target:** 21 en los tres módulos
- **Serialización:** Gson/kotlinx-serialization en backend y mobile
- **Coroutines:** 1.9.0 en los tres módulos

---

*Documento coherente con Apéndices S1-S10 y Arquitectura A.C.E v0.2. Cualquier divergencia debe ser reportada como bug de coherencia.*
