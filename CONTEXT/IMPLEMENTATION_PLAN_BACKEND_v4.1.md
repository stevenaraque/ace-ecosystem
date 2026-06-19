# A.C.E — Implementation Plan: Backend (`ace-backend`)

> **Estado:** Coherente con Apéndices Aprobados S1-S10 y Arquitectura v0.3  
> **Versión:** 4.3 (Migración a estructura por-feature + Dockerfile mejorado + gradlew tracked + application-prod.yml)  
> **Fecha:** 2026-06-19  
> **Stack:** Spring Boot 4.0.6 · Kotlin 2.2.21 · Gradle 8.10+ (Kotlin DSL) · PostgreSQL 16 · Flyway 10.15 · kotlinx-serialization  
> **Depende de:** `com.github.reinaldojperalta:ace-shared` v1.0.4 (JitPack)  
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

**Nota sobre :shared:** El backend consume el módulo `:shared` vía **JitPack** como artifact Maven externo. No se incluye como `project(":shared")` ni como JAR local en `libs/`. La coordenada exacta es `com.github.reinaldojperalta:ace-shared`.

**Despliegue y Entorno:** El proyecto cuenta con un `Dockerfile` optimizado que utiliza `gradlew` (wrapper 9.5.1), activa `SPRING_PROFILES_ACTIVE=prod` cargando `application-prod.yml` y usa runtime `jammy` (Debian) para despliegue en plataformas como Render.

### Estado Real por Sistema (a fecha 2026-06-19)
- **S4 (Auth):** ✅ Totalmente implementado en `auth/`.
- **S1, S2, S3, S5, S6, S7, S9, S10:** ❌ No implementados (esquema SQL presente en Flyway V1–V4, pero esqueletos vacíos en código).

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
    // ✅ JitPack para :shared (repo separado reinaldojperalta/ace-shared)
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    // ─── :shared vía JitPack ───
    // Coordenada exacta: JitPack sobrescribe group a com.github.reinaldojperalta
    implementation("com.github.reinaldojperalta:ace-shared:1.0.0")

    // ─── Spring Boot Starters ───
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-kotlinx-serialization-json")

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
    testImplementation("com.h2database:h2")
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
```

### 2.2. Notas críticas sobre Spring Boot 4.0 + Kotlin 2.2 + JitPack

| Aspecto | Pre-Spring Boot 4.0 (plan v3.0) | Spring Boot 4.0 (plan v4.0) | v4.1 (JitPack) | Justificación |
|---------|-------------------------------|-------------------------------|----------------|---------------|
| **:shared** | `project(":shared")` | `files("libs/ace-shared.jar")` | **JitPack `com.github.reinaldojperalta:ace-shared`** | Repo separado, compilación automática, sin fricción de JAR manual. |
| **Repositorios** | mavenCentral + GitHub Packages | mavenCentral + flatDir | **mavenCentral + JitPack** | JitPack no requiere PAT para consumir. |
| **Spring Boot** | 3.3.2 | **4.0.6** | 4.0.6 | Última estable (abril 2026). Soporte hasta dic 2026. |
| **Kotlin** | 2.1.0 | **2.2.21** | 2.2.21 | Baseline obligatorio de Spring Boot 4.0. |
| **Plugin Kotlin JVM** | `kotlin("jvm")` | `kotlin("jvm")` (mantenido) | `kotlin("jvm")` | Versión actualizada a 2.2.21. |
| **Plugin Kotlin Spring** | `kotlin("plugin.spring")` | `kotlin("plugin.spring")` (mantenido) | `kotlin("plugin.spring")` | Versión actualizada a 2.2.21. |
| **Plugin Kotlin JPA** | `kotlin("plugin.jpa")` | `kotlin("plugin.jpa")` (mantenido) | `kotlin("plugin.jpa")` | Versión actualizada a 2.2.21. |
| **Plugin Kotlin Serialization** | No usado | **`kotlin("plugin.serialization")`** | `kotlin("plugin.serialization")` | Soporte nativo de kotlinx-serialization en Spring Boot 4.0. |
| **Spring Dependency Management** | 1.1.6 | **1.1.7** | 1.1.7 | Última estable (diciembre 2024). |
| **Moshi** | 1.15.1 | **Eliminado** | Eliminado | Reemplazado por Gson + kotlinx-serialization. |
| **Gson** | No usado | **Usado** | Usado | Coherente con mobile (`retrofit-converter-gson`). |
| **kotlinx-serialization** | No usado | **`spring-boot-starter-kotlinx-serialization-json`** | `spring-boot-starter-kotlinx-serialization-json` | Starter nativo de Spring Boot 4.0. |
| **`kotlinOptions { jvmTarget }`** | `"21"` | **Eliminado** | Eliminado | Deprecado en Kotlin 2.2. Se usa `compilerOptions` DSL. |
| **`compilerOptions` DSL** | No existía | **`kotlin { compilerOptions { ... } }`** | `kotlin { compilerOptions { ... } }` | Nuevo DSL obligatorio en Kotlin 2.2+. |
| **`JvmTarget.JVM_21`** | No usado | **`jvmTarget.set(JvmTarget.JVM_21)`** | `jvmTarget.set(JvmTarget.JVM_21)` | API tipada para especificar target JVM. |
| **Java** | 21 | **21** (mantenido) | 21 | Compatible con Spring Boot 4.0. |
| **Flyway** | 10.15.0 | **10.15.0** (mantenido) | 10.15.0 | Conservado como requerido. |
| **H2** | No usado | No usado | **Añadido en tests** | Para tests unitarios rápidos sin Docker. Opcional. |

---

## 3. Arquitectura y Patrones (Coherente con Arquitectura §2.1)

[Idéntico a v4.0]

---

## 4. Estructura de Carpetas

```
ace-backend/
├── build.gradle.kts
├── settings.gradle.kts
├── gradlew               # Trackeado para CI/CD (Render)
├── Dockerfile            # Configurado para jammy, perfil prod, y JVM tuning
├── src/main/kotlin/sena/adso/ace_backend/
│   ├── AceBackendApplication.kt
│   │
│   ├── config/
│   │   └── SecurityConfig.kt
│   │
│   ├── security/
│   │   └── JwtAuthenticationFilter.kt
│   │
│   ├── auth/                                # FEATURE COMPLETA (S4)
│   │   ├── controller/AuthController.kt
│   │   ├── model/User.kt, RefreshToken.kt
│   │   ├── repository/UserRepository.kt, RefreshTokenRepository.kt
│   │   └── service/AuthService.kt, JwtService.kt, RefreshTokenService.kt
│   │
│   ├── exercise/                            # Esqueleto (S2, S3)
│   ├── ranking/                             # Esqueleto (S6)
│   ├── streak/                              # Esqueleto (S7)
│   ├── user/                                # Esqueleto (S1)
│   └── xp/                                  # Esqueleto (S5, S10)
│
├── src/main/resources/
│   ├── application.yml
│   ├── application-dev.yml
│   ├── application-prod.yml                 # DB Supabase remota, Actuator restringido
│   └── db/migration/
│       ├── V1__init.sql
│       ├── V2__xp_transactions.sql
│       ├── V3__ranking_materialized.sql
│       └── V4__seed_data.sql
```

---

## 5. Flujos de Datos Conceptuales (Coherentes con Apéndices)

[Idéntico a v4.0]

---

## 6. Decisiones Técnicas y Trade-offs (Coherentes con Arquitectura §14)

| Decisión | Valor | Justificación | Trade-off | Apéndice |
|----------|-------|---------------|-----------|----------|
| **:shared vía JitPack** | `com.github.reinaldojperalta:ace-shared` | Repo separado, compilación automática por tag, sin configuración de tokens PAT. | Requiere tag + push para publicar. No hay "snapshot local" compartido. | — |
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
| **H2 (tests)** | Sí | Tests unitarios rápidos sin Docker. | No prueba comportamiento real de PostgreSQL (índices, JSONB). Opcional. | — |

---

## 7. Contratos de API con Mobile (Coherentes con :shared)

[Idéntico a v4.0]

---

## 8. Roadmap: Fase Mínima (Coherente con Apéndices)

### Fase Mínima (Semanas 1-4) — TODOS los sistemas S1-S10 presentes

- [ ] Esqueleto Spring Boot 4.0.6 + Kotlin 2.2.21 + compilerOptions DSL + `:shared` vía JitPack
- [ ] PostgreSQL remoto configurado en perfil prod.
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
- [ ] **JitPack:** Verificar que `com.github.reinaldojperalta:ace-shared` se resuelve correctamente en build

### Fase de Transición (Semanas 5-8)
[Idéntico a v4.0]

### Fase Máxima (Semanas 9-12+)
[Idéntico a v4.0]

---

## 9. Checklist de Integración con Mobile (Coherente con Apéndices)

[Idéntico a v4.0, con adición:]

- [ ] `:shared` vía JitPack (`com.github.reinaldojperalta:ace-shared`) se resuelve sin errores de dependencia
- [ ] Cambio en DTO de `:shared` (nuevo tag) se refleja en backend tras actualizar versión y sincronizar Gradle

---

## 10. Nota sobre Coherencia de Kotlin en el Ecosistema A.C.E

[Idéntico a v4.0]

---

*Documento coherente con Apéndices S1-S10 y Arquitectura A.C.E v0.2. Cualquier divergencia debe ser reportada como bug de coherencia.*
