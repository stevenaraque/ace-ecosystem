# A.C.E — Implementation Plan: Mobile (`ace-mobile`)

> **Estado:** En desarrollo (~85% implementado). S1–S10 y F1–F7 completos.
> **Versión:** 4.4 (S1–S10 implementados; F1–F7 completos; layout por-feature `feature/`; `:shared` bumped a 1.0.9)
> **Fecha:** 2026-06-25
> **Stack:** Android 13+ (API 33) · Kotlin 2.2.21 · Gradle 8.10+ (Kotlin DSL) · AGP 9.0.1 · Wear OS Data Layer API · Retrofit 2.11.0 + Gson · Hilt 2.59.2 · Room 2.6.1 · Jetpack Compose BOM 2024.09.00 · legacy.kapt
> **Depende de:** `com.github.reinaldojperalta:ace-shared:1.0.9` (JitPack, `libs.versions.toml:25`)
> **Responsable:** Steven Araque (Mobile/Wear Lead) · Integración con Reinaldo/Santiago (Backend)
> **Deuda técnica:** ver `CONTEXT/DEUDA_TECNICA_v2.md` (ítems D1–D6: código muerto a limpiar, sin cambio de comportamiento).

---

## 1. Visión y Alcance (Coherente con Arquitectura §1)

El módulo `:mobile` es el **orquestador, traductor, calculador y buffer** del ecosistema:

1. **Recibe** FC cruda del reloj vía Wear OS Data Layer API (S1).
2. **Agrupa** en bloques de ~300 segundos (S2).
3. **Calcula XP localmente** usando fórmulas cacheadas del backend (S5).
4. **Muestra recompensa inmediata** al usuario, offline (S5).
5. **Persistencia** bloques, sesiones, historial, estadísticas y tokens en SQLite (S2, S3, S9, S10, S4). Nota: Los tokens van en Room plano (`LocalUserEntity`), no en EncryptedSharedPreferences.
6. **Sincroniza** batches de máximo 20 bloques + estadísticas con el backend (S3, S10).
7. **Notifica** al usuario: sesión activa (foreground), recordatorio de racha 20:00, errores de sync (S8).
8. **Cachea** ranking, racha, estadísticas para uso offline (S6, S7, S10).

**Principio rector:** *El móvil no es un pasamanos; es un traductor, validador, calculador y buffer entre el reloj y la nube.* (Arquitectura §4)

**Regla de oro:** El móvil **calcula XP primario**, el backend **valida**. El móvil **no decide** si la racha sube o se rompe, solo **cachea** lo que el backend dice (S7 §2.1).

**Nota sobre :shared:** El móvil consume el módulo `:shared` vía **JitPack** como artifact Maven externo. La coordenada exacta es `com.github.reinaldojperalta:ace-shared:1.0.4`. No se incluye como `project(":shared")` ni como JAR local.

### Estado Real por Sistema (a fecha 2026-06-25)
- **S1 (Sensor):** ✅ Implementado (`WearDataSource` + `WearDataListenerService`).
- **S4 (Auth):** ✅ Implementado (login/register/refresh/logout + `AuthInterceptor` con race-condition control).
- **S2 (Session):** ✅ Implementado — captura, cierre de bloque persistido en Room, **pausa manual + auto-pausa por FC** (F6: `PAUSE_BPM_THRESHOLD=110`, `LOW_BPM_PAUSE_SECONDS=30`, `SessionUiState.Paused(isAutoPaused)`).
- **S3 (Sync):** ✅ Implementado — `BlockRepository` (Room), estados PENDING/SYNCING/SYNCED/ERROR, batch, backoff.
- **S5 (XP):** ✅ Implementado — `CalculateBlockXpUseCase`, fórmulas cacheadas, recompensa inmediata.
- **S6 (Ranking):** ✅ Implementado — cache local, **municipal funcional vía `cityId`** (F4), auto-refresh ON_RESUME + pull-to-refresh/refresh button, invalidación de cache en `rankChanged`.
- **S7 (Streaks):** ✅ Implementado — cache de `StreakStateDto`.
- **S9 (History):** ✅ Implementado — FIFO 5 sesiones local.
- **S10 (Perfil/Stats):** ✅ Implementado — `UserApi`/`UserRepository`/`ProfileScreen` editable (F2), selector de ciudad, `setCityId` persistido; `AccumulateStatsUseCase`/reconciliación.

**Features F1–F7 (todas completas):** F1 editar perfil (backend), F2 editar perfil (mobile), F3 crear cuenta (`RegisterScreen` + wiring), F4 ranking municipal + auto-refresh, F5 fórmulas deportes (selector), F6 pausa/auto-pausa, F7 JWT único (backend).

**Deuda (código muerto, sin impacto funcional):** `SessionRepositoryImpl` placeholder (D1), 6 stubs de 0 bytes (D2), `WearDataListenerService` duplicado (D6 — verificar antes de eliminar), botón "olvidar contraseña" vacío (D7, fuera de alcance).

---

## 2. Dependencias Gradle (`build.gradle.kts`)

### 2.1. `libs.versions.toml` (Version Catalog)

[Idéntico a v4.0]

### 2.2. Top-level `build.gradle.kts`

[Idéntico a v4.0]

### 2.3. Módulo `:app` `build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.legacy.kapt)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "sena.adso.ace_mobile"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "sena.adso.ace_mobile"
        minSdk = 33
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    // AGP 9.0 built-in Kotlin: kotlinOptions y composeOptions YA NO SON NECESARIOS
    // El plugin kotlin-compose gestiona el compiler automáticamente

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // ─── :shared vía JitPack ───
    // Coordenada exacta: JitPack sobrescribe group a com.github.reinaldojperalta
    implementation("com.github.reinaldojperalta:ace-shared:1.0.9")

    // ─── Kotlin & Corrutinas ───
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    // ─── Android Core ───
    implementation(libs.androidx.core.ktx)

    // ─── Lifecycle ───
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)

    // ─── Activity & Compose ───
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // ─── Navigation ───
    implementation(libs.navigation.compose)

    // ─── Wear OS Data Layer ───
    implementation(libs.play.services.wearable)

    // ─── Room ───
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    kapt(libs.room.compiler)

    // ─── WorkManager ───
    implementation(libs.work.runtime.ktx)

    // ─── Retrofit & OkHttp ───
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // ─── Hilt (DI) ───
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.hilt.work)
    kapt(libs.hilt.work.compiler)
    implementation(libs.hilt.navigation.compose)

    // ─── DataStore ───
    implementation(libs.datastore.preferences)

    // ─── Serialization ───
    implementation(libs.kotlinx.serialization.json)

    // ─── Testing ───
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.truth)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
```

### 2.4. Notas críticas sobre AGP 9.0 + Built-in Kotlin + JitPack

| Aspecto | Pre-AGP 9.0 (plan v3.0) | AGP 9.0 (plan v4.0) | v4.1 (JitPack) | Justificación |
|---------|------------------------|---------------------|----------------|---------------|
| **:shared** | `project(":shared")` | `project(":shared")` o JAR local | **JitPack `com.github.reinaldojperalta:ace-shared`** | Repo separado, compilación automática, sin JAR manual. |
| **Repositorios** | google + mavenCentral | google + mavenCentral | **google + mavenCentral + JitPack** | JitPack añadido para resolver :shared. |
| **Plugin Kotlin Android** | `org.jetbrains.kotlin.android` | **Eliminado** | Eliminado | Built-in en AGP 9.0. No se declara. |
| **Plugin kapt** | `org.jetbrains.kotlin.kapt` | **Revertido a legacy** | Usado | KSP dio problemas. Se usa `legacy.kapt`. |
| `kotlinOptions { jvmTarget }` | `"21"` explícito | **Eliminado** | Eliminado | `compileOptions` Java 21 es suficiente; AGP 9.0 infiere automáticamente. |
| `composeOptions { kotlinCompilerExtensionVersion }` | `"1.5.14"` explícito | **Eliminado** | Eliminado | El plugin `org.jetbrains.kotlin.plugin.compose` gestiona el compiler automáticamente. |
| Hilt | `2.52` | **`2.59.2`** | 2.59.2 | Hilt 2.52 accede a `BaseExtension` removida en AGP 9.0. 2.59.2+ es compatible. |
| Annotation Processing | `kapt` (estándar) | **legacy.kapt** | legacy.kapt | KSP falló. Mobile usa `legacy.kapt`. |
| Kotlin | `2.1.0` (plugin explícito) | **`2.2.21`** | 2.2.21 | Versión unificada con el ecosistema. |

---

## 3. Arquitectura y Patrones (Coherente con Arquitectura §2.1)

[Idéntico a v4.0]

---

## 4. Estructura de Carpetas

```
ace-mobile/
├── build.gradle.kts                      # AGP 9.0.1, Kotlin 2.2.21 built-in, Hilt 2.59.2, legacy.kapt, Room, Compose, Wearable
├── settings.gradle.kts                   # include ':app'
├── gradle/libs.versions.toml             # ace-shared = "1.0.9"
├── gradle/wrapper/
│
├── app/build.gradle.kts                  # applicationId = "sena.adso.ace"; JitPack ace-shared:1.0.9
│
├── src/main/kotlin/com/ace/mobile/
│   ├── MobileApplication.kt              # @HiltAndroidApp, crea NotificationChannels (S8 §5.2)
│   │
│   ├── core/                             # Núcleo transversal
│   │   ├── data/                         # SessionRepository/Impl, SessionSampleBuffer/Impl, BlockRepository
│   │   ├── database/{dao,entity}/        # Room: BlockDao, SessionDao, UserDao, RankingCacheDao, etc. + entities
│   │   ├── datastore/                    # UserPreferencesDataStore (cityId, prefs)
│   │   ├── di/                           # Hilt: NetworkModule, DatabaseModule, RepositoryModule, ...
│   │   └── model/                        # HeartRateSample, ...
│   │
│   └── feature/                          # LAYOUT POR-FEATURE (cada feature: data/domain/presentation/service)
│       ├── auth/                         # S4 + F3: data/AuthApi, AuthRepository; presentation/{Login,Register}Screen+ViewModel
│       ├── exercise/                     # S2/S5 + F6: domain/{Start,Pause,Resume,Stop}SessionUseCase; presentation/{Session,SessionUiState}ViewModel; service/ExerciseSyncService
│       ├── history/                      # S9: HistoryScreen/ViewModel
│       ├── profile/                      # S10 + F2: data/{UserApi,UserRepository}; presentation/{Profile,ProfileViewModel}Screen
│       ├── ranking/                      # S6 + F4: data/RankingCacheRepository; presentation/{Ranking,RankingViewModel}Screen
│       ├── stats/                        # S10: domain/AccumulateStatsUseCase, ReconcileStatsUseCase
│       ├── streak/                       # S7: domain/CheckStreakUseCase
│       ├── wear/                         # S1: data/WearDataSource, WearMessageClient; service/WearDataListenerService
│       └── xp/                           # S5: domain/{CalculateBlockXp,CacheXpFormulas}UseCase
│
├── src/main/res/
│   └── values/
│       └── strings.xml
│
└── src/main/AndroidManifest.xml
```

> **Nota sobre el layout:** la app usa **estructura por-feature** (`core/` + `feature/<nombre>/` con subcarpetas `data/domain/presentation/service`), no el layout plano por capas del plan v4.3. `applicationId = "sena.adso.ace"` (no `sena.adso.ace_mobile`).

---

## 5. Flujos de Datos Conceptuales (Coherentes con Apéndices)

[Idéntico a v4.0]

---

## 6. Room: Esquema de Entidades (Coherentes con Apéndices)

[Idéntico a v4.0]

---

## 7. NotificationChannels (S8 §5.2)

[Idéntico a v4.0]

---

## 8. Decisiones Técnicas y Trade-offs (Coherentes con Apéndices)

| Decisión | Valor | Justificación | Trade-off | Apéndice |
|----------|-------|---------------|-----------|----------|
| **:shared vía JitPack** | `com.github.reinaldojperalta:ace-shared` | Repo separado, compilación automática por tag, sin JAR manual. | Requiere tag + push para actualizar. No hay "snapshot local" compartido. | — |
| **AGP 9.0.1** | Sí | Generado por Android Studio Otter 3. Built-in Kotlin reduce configuración. | Requiere Hilt 2.59.2+ (2.52 rompe con `BaseException`). | — |
| **legacy.kapt** | Sí | Estándar de compatibilidad. KSP falló. | Menos rápido que KSP. | — |
| **Kotlin 2.2.21** | Sí | Versión unificada en el ecosistema. | — | — |
| **Room (SQLite)** | Sí, para tokens y datos | Coherente con S4 §3.2 (local_user en SQLite). DataStore solo para preferencias. | Más complejidad que DataStore puro. | S4, S9, S10 |
| **MVVM + Clean** | Sí | Testabilidad. Use Cases puro Kotlin sin Android. | Más boilerplate. | — |
| **WorkManager** | Sync + Streak + Error | Sobrevive cierre de app, Doze, reinicio. | Configuración inicial más compleja. | S3 §5.2, S7 §4.2, S8 §4 |
| **Retrofit + Gson** | Sí | Gson maduro, integración con Kotlinx Serialization. | Si backend usa Moshi, verificar compatibilidad de nulls/nombres. | S3 §4.1 |
| **Hilt 2.59.2** | Sí | Compatible con AGP 9.0. Reduce boilerplate de Dagger. Integración con ViewModels y Compose. | Versiones <2.59.2 fallan con `BaseExtension not found`. | — |
| **Foreground health** | Sí | Android 10+ requiere tipo "health" para datos del reloj. | Notificación persistente obligatoria. | S8 §2.1 |
| **Wear DataClient** | Sí | Google maneja reconexión, buffer, sync. | Dependencia de Google Play Services. | S1 §2.1 |
| **Buffer RAM 300** | Sí | 5 minutos a 1 Hz. No toca disco. | Si app muere, bloque OPEN se pierde. | S1 §5.2 |
| **NO Room en wear** | Sí | Reduce APK del reloj. SO bufferiza. | Límite no documentado de caché Google. | S1 §3.4 |
| **Timestamps epoch millis** | Long | Coherente con :shared. No ISO-8601. | Legibilidad en debug (usar Instant.toString()). | S1 §2.3 |
| **CheckStreakWorker 20:00** | Sí | Coherente con S7 §4.2. Sin FCM. | Si entrena después de 20:00, no recibe recordatorio ese día. | S7 §4.5 |
| **client_stats en batch** | Sí | Coherente con S10 §3.5. Reduce peticiones. | Payload ligeramente más grande. | S10 §3.2 |
| **Compose BOM 2024.09.00** | Sí | Versión estable con Material3. | Actualizaciones periódicas necesarias. | — |

---

## 9. Roadmap: Fase Mínima (Coherente con Apéndices S1-S10)

### Fase Mínima (Semanas 1-4) — TODOS los sistemas presentes

- [x] Esqueleto Android Studio: módulo `:mobile`, AGP 9.0.1, Kotlin 2.2.21, Hilt 2.59.2, legacy.kapt, Compose, Room
- [x] Integración con `:shared` vía JitPack: `com.github.reinaldojperalta:ace-shared:1.0.9` (DTOs y enums)
- [x] **S4 Auth:** Login/Registro UI + AuthRepository + Room `local_user` (tokens, device_id)
- [x] **S4 Auth:** `AuthInterceptor` con flag `isRefreshing` + cola de peticiones + manejo `REFRESH_REUSED`
- [x] **F3 Crear cuenta:** `RegisterScreen` + `RegisterViewModel`, botón "CREAR CUENTA" cableado, ruta `register_screen_route`
- [x] **S1 Sensor:** `WearDataSource` (+ `WearDataListenerService`) recibe FC por DataClient en path `/ace/health/heart_rate`
- [x] **S1 Sensor:** Buffer circular RAM, timeout desconexión > 5s
- [x] **S2 Session:** `ExerciseSyncService` (Foreground `health`) con notificación persistente
- [x] **S2 Session:** Estados ACTIVE, PAUSED, COMPLETED, ABORTED. Solo 1 ACTIVE.
- [x] **S2 Session:** `BuildExerciseBlockUseCase` arma `ExerciseBlockDto` con `block_id` UUIDv4; cierre de bloque persistido en Room
- [x] **F6 Pausa:** `PauseSessionUseCase`/`ResumeSessionUseCase` implementados; `SessionUiState.Paused(isAutoPaused)`; botones Pausar/Reanudar
- [x] **F6 Auto-pausa:** watcher FC (`PAUSE_BPM_THRESHOLD=110`, `LOW_BPM_PAUSE_SECONDS=30`); reanudación automática si no fue pausa manual
- [x] **S5 XP:** `CalculateBlockXpUseCase` con fórmulas cacheadas de Room (descargadas de `GET /api/xp/formulas`)
- [x] **S5 XP:** Recompensa inmediata "+X XP" visible offline
- [x] **F5 Selector deporte:** `SessionScreen` renderiza un botón por cada `SportType.entries`
- [x] **S3 Sync:** `SendPendingBlocksUseCase` con WorkManager, batch ≤ 20, backoff exponencial, reintentos
- [x] **S3 Sync:** Estados PENDING → SYNCING → SYNCED/ERROR. Idempotencia por block_id.
- [x] **S7 Streaks:** `CheckStreakWorker` diario a las 20:00. Notificación local si no entrenó.
- [x] **S7 Streaks:** Cache de `current_streak`, `best_streak`, `last_exercise_date` desde respuesta de sync
- [x] **S8 Notif:** 3 NotificationChannels: ace_session_active, ace_streak_reminder, ace_sync_error
- [x] **S8 Notif:** Foreground service notif durante sesión. Sync error notif tras 5 reintentos.
- [x] **S6 Ranking:** Pantalla de ranking con cache local 1h (posición propia + top 10)
- [x] **S6 Ranking:** Invalida cache si `rankChanged = true` en respuesta de sync; auto-refresh ON_RESUME + botón refresh
- [x] **F4 Municipal:** `cityId` persistido vía `setCityId` (F2); tab municipal funcional
- [x] **F2 Editar perfil:** `UserApi`/`UserRepository`/`ProfileScreen` editable (username, nickname, city, weight, birthDate)
- [x] **S9 History:** `local_session_history` FIFO 5 sesiones, sin discriminar categoría
- [x] **S10 Stats:** `local_user_stats` con acumulación inmediata y promedio ponderado por sampleCount
- [x] **S10 Stats:** Envía `client_stats` en CADA batch. Recibe `official_stats`.
- [x] **Built-in Kotlin:** No se usa `org.jetbrains.kotlin.android` ni `org.jetbrains.kotlin.kapt` (legacy.kapt)
- [x] **JitPack:** `com.github.reinaldojperalta:ace-shared:1.0.9` se resuelve correctamente en build

### Fase de Transición (Semanas 5-8)
[Idéntico a v4.0]

### Fase Máxima (Semanas 9-12+)
[Idéntico a v4.0]

---

## 10. Checklist de Integración con Backend (Coherente con Apéndices)

[Idéntico a v4.0, con adición:]

- [ ] `:shared` vía JitPack (`com.github.reinaldojperalta:ace-shared`) se resuelve sin errores de dependencia
- [ ] Cambio en DTO de `:shared` (nuevo tag) se refleja en mobile tras actualizar versión y sincronizar Gradle

---

*Documento coherente con Apéndices S1-S10 y Arquitectura A.C.E v0.2. Cualquier divergencia debe ser reportada como bug de coherencia.*
