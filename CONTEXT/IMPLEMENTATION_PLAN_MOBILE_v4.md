# A.C.E — Implementation Plan: Mobile (`ace-mobile`)

> **Estado:** Coherente con Apéndices Aprobados S1-S10 y Arquitectura v0.2  
> **Versión:** 4.0 (Actualizado a AGP 9.0 + Built-in Kotlin + Hilt 2.59.2 + KSP)  
> **Fecha:** Junio 2026  
> **Stack:** Android 13+ (API 33) · Kotlin 2.1.20 (built-in) · Gradle 8.10+ (Kotlin DSL) · AGP 9.0.1 · Wear OS Data Layer API · Retrofit 2.11.0 + Gson · Hilt 2.59.2 · Room 2.6.1 · Jetpack Compose BOM 2024.09.00 · KSP 2.1.20-1.0.32  
> **Depende de:** `:shared` JAR v1.0.0+  
> **Responsable:** Steven Araque (Mobile/Wear Lead) · Integración con Reinaldo/Santiago (Backend)

---

## 1. Visión y Alcance (Coherente con Arquitectura §1)

El módulo `:mobile` es el **orquestador, traductor, calculador y buffer** del ecosistema:

1. **Recibe** FC cruda del reloj vía Wear OS Data Layer API (S1).
2. **Agrupa** en bloques de ~300 segundos (S2).
3. **Calcula XP localmente** usando fórmulas cacheadas del backend (S5).
4. **Muestra recompensa inmediata** al usuario, offline (S5).
5. **Persiste** bloques, sesiones, historial, estadísticas y tokens en SQLite (S2, S3, S9, S10, S4).
6. **Sincroniza** batches de máximo 20 bloques + estadísticas con el backend (S3, S10).
7. **Notifica** al usuario: sesión activa (foreground), recordatorio de racha 20:00, errores de sync (S8).
8. **Cachea** ranking, racha, estadísticas para uso offline (S6, S7, S10).

**Principio rector:** *El móvil no es un pasamanos; es un traductor, validador, calculador y buffer entre el reloj y la nube.* (Arquitectura §4)

**Regla de oro:** El móvil **calcula XP primario**, el backend **valida**. El móvil **no decide** si la racha sube o se rompe, solo **cachea** lo que el backend dice (S7 §2.1).

---

## 2. Dependencias Gradle (`build.gradle.kts`)

### 2.1. `libs.versions.toml` (Version Catalog)

```toml
[versions]
agp = "9.0.1"
kotlin = "2.1.20"
ksp = "2.1.20-1.0.32"
hilt = "2.59.2"
coreKtx = "1.18.0"
lifecycle = "2.10.0"
activityCompose = "1.13.0"
composeBom = "2024.09.00"
coroutines = "1.9.0"
room = "2.6.1"
work = "2.10.0"
retrofit = "2.11.0"
okhttp = "4.12.0"
navigation-compose = "2.8.6"
datastore = "1.1.1"
hilt-navigation-compose = "1.2.0"
hilt-work = "1.2.0"
hilt-compiler = "1.2.0"
kotlinx-serialization = "1.8.0"
junit = "4.13.2"
junitVersion = "1.3.0"
espressoCore = "3.7.0"
mockk = "1.13.12"
truth = "1.4.4"

[libraries]
# --- Kotlin & Corrutinas ---
kotlin-stdlib = { group = "org.jetbrains.kotlin", name = "kotlin-stdlib", version.ref = "kotlin" }
kotlinx-coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }
kotlinx-coroutines-play-services = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-play-services", version.ref = "coroutines" }
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutines" }

# --- Android Core ---
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }

# --- Lifecycle ---
androidx-lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycle" }
androidx-lifecycle-viewmodel-ktx = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-ktx", version.ref = "lifecycle" }
androidx-lifecycle-livedata-ktx = { group = "androidx.lifecycle", name = "lifecycle-livedata-ktx", version.ref = "lifecycle" }

# --- Compose ---
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
androidx-compose-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-compose-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
androidx-compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
androidx-compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
androidx-compose-ui-test-manifest = { group = "androidx.compose.ui", name = "ui-test-manifest" }
androidx-compose-ui-test-junit4 = { group = "androidx.compose.ui", name = "ui-test-junit4" }
androidx-compose-material3 = { group = "androidx.compose.material3", name = "material3" }

# --- Navigation ---
navigation-compose = { module = "androidx.navigation:navigation-compose", version.ref = "navigation-compose" }

# --- Wear OS Data Layer ---
play-services-wearable = { module = "com.google.android.gms:play-services-wearable", version = "18.2.0" }

# --- Room ---
room-runtime = { module = "androidx.room:room-runtime", version.ref = "room" }
room-ktx = { module = "androidx.room:room-ktx", version.ref = "room" }
room-compiler = { module = "androidx.room:room-compiler", version.ref = "room" }

# --- WorkManager ---
work-runtime-ktx = { module = "androidx.work:work-runtime-ktx", version.ref = "work" }

# --- Retrofit & OkHttp ---
retrofit = { module = "com.squareup.retrofit2:retrofit", version.ref = "retrofit" }
retrofit-converter-gson = { module = "com.squareup.retrofit2:converter-gson", version.ref = "retrofit" }
okhttp = { module = "com.squareup.okhttp3:okhttp", version.ref = "okhttp" }
okhttp-logging = { module = "com.squareup.okhttp3:logging-interceptor", version.ref = "okhttp" }

# --- Hilt ---
hilt-android = { module = "com.google.dagger:hilt-android", version.ref = "hilt" }
hilt-compiler = { module = "com.google.dagger:hilt-compiler", version.ref = "hilt" }
hilt-work = { module = "androidx.hilt:hilt-work", version.ref = "hilt-work" }
hilt-work-compiler = { module = "androidx.hilt:hilt-compiler", version.ref = "hilt-compiler" }
hilt-navigation-compose = { module = "androidx.hilt:hilt-navigation-compose", version.ref = "hilt-navigation-compose" }

# --- DataStore ---
datastore-preferences = { module = "androidx.datastore:datastore-preferences", version.ref = "datastore" }

# --- Serialization ---
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "kotlinx-serialization" }

# --- Testing ---
junit = { group = "junit", name = "junit", version.ref = "junit" }
androidx-junit = { group = "androidx.test.ext", name = "junit", version.ref = "junitVersion" }
androidx-espresso-core = { group = "androidx.test.espresso", name = "espresso-core", version.ref = "espressoCore" }
mockk = { group = "io.mockk", name = "mockk", version.ref = "mockk" }
truth = { group = "com.google.truth", name = "truth", version.ref = "truth" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
hilt-android = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
```

### 2.2. Top-level `build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}
```

### 2.3. Módulo `:app` `build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
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
    // --- :shared (descomenta si aplica) ---
    // implementation(project(":shared"))

    // --- Kotlin & Corrutinas ---
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    // --- Android Core ---
    implementation(libs.androidx.core.ktx)

    // --- Lifecycle ---
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)

    // --- Activity & Compose ---
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // --- Navigation ---
    implementation(libs.navigation.compose)

    // --- Wear OS Data Layer ---
    implementation(libs.play.services.wearable)

    // --- Room ---
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // --- WorkManager ---
    implementation(libs.work.runtime.ktx)

    // --- Retrofit & OkHttp ---
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // --- Hilt (DI) ---
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)
    implementation(libs.hilt.navigation.compose)

    // --- DataStore ---
    implementation(libs.datastore.preferences)

    // --- Serialization ---
    implementation(libs.kotlinx.serialization.json)

    // --- Testing ---
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

### 2.4. Notas críticas sobre AGP 9.0 + Built-in Kotlin

| Aspecto | Pre-AGP 9.0 (plan v3.0) | AGP 9.0 (plan v4.0) | Justificación |
|---------|------------------------|---------------------|---------------|
| Plugin Kotlin Android | `org.jetbrains.kotlin.android` | **Eliminado** | Built-in en AGP 9.0. No se declara. |
| Plugin kapt | `org.jetbrains.kotlin.kapt` | **Eliminado** | Incompatible con built-in Kotlin. Mobile usa **KSP**. |
| `kotlinOptions { jvmTarget }` | `"21"` explícito | **Eliminado** | `compileOptions` Java 21 es suficiente; AGP 9.0 infiere automáticamente. |
| `composeOptions { kotlinCompilerExtensionVersion }` | `"1.5.14"` explícito | **Eliminado** | El plugin `org.jetbrains.kotlin.plugin.compose` gestiona el compiler automáticamente. |
| Hilt | `2.52` | **`2.59.2`** | Hilt 2.52 accede a `BaseExtension` removida en AGP 9.0. 2.59.2+ es compatible. |
| Annotation Processing | `kapt` (estándar) | **KSP** | KSP es el estándar moderno para AGP 9.0. Mobile usa KSP; Wear usa `legacy-kapt` por incompatibilidades específicas documentadas. |
| Kotlin | `2.1.0` (plugin explícito) | **`2.1.20` (built-in)** | Android Studio Otter 3 genera 2.1.20 built-in. Compatible con el ecosistema. |

---

## 3. Arquitectura y Patrones (Coherente con Arquitectura §2.1)

| Patrón | Dónde vive | Justificación | Coherencia |
|--------|-----------|---------------|------------|
| **MVVM** | `presentation/` | UI observa StateFlow. ViewModel sobrevive rotaciones. | S2, S5, S6, S7, S10 |
| **Repository** | `data/repository/` | Único punto de acceso a datos. Abstrae fuente (local/remoto). | S3, S9, S10 |
| **Singleton** | `di/` (Hilt) | Retrofit, Room, DataClient, WorkManager costosos de crear. | S1, S3, S4, S8 |
| **Observer** | `Flow` + `LiveData` + `WorkManager` | Reactivo: FC del reloj, estado de sesión, tiempo para notificaciones. | S1, S2, S8 |
| **DTO** | Consumido vía `:shared` | `ExerciseBlockDto`, `SyncBatchRequestDto`, etc. Garantizan compatibilidad. | S3, S5, S10 |
| **UseCase** | `domain/usecase/` | Cada caso de uso es clase independiente, testeable sin Android framework. | S2, S5 |

---

## 4. Estructura de Carpetas

```
ace-mobile/
├── build.gradle.kts                      # AGP 9.0.1, Kotlin 2.1.20 built-in, Hilt, KSP, Room, Compose, Wearable
├── settings.gradle.kts                   # include ':shared'
├── gradle/wrapper/
│
├── src/main/kotlin/com/ace/mobile/
│   ├── MobileApplication.kt              # @HiltAndroidApp, crea NotificationChannels (S8 §5.2)
│   │
│   ├── di/                               # Hilt Modules
│   │   ├── NetworkModule.kt              # Retrofit + Gson + OkHttp + AuthInterceptor
│   │   ├── DatabaseModule.kt             # Room database (SQLite local)
│   │   ├── DataStoreModule.kt            # DataStore para preferencias simples (NO tokens — tokens van a Room)
│   │   ├── WearModule.kt                 # DataClient, MessageClient
│   │   └── WorkManagerModule.kt          # ConfigurationProvider
│   │
│   ├── data/
│   │   ├── local/
│   │   │   ├── database/
│   │   │   │   ├── AceDatabase.kt        # Room database principal
│   │   │   │   ├── dao/
│   │   │   │   │   ├── SessionDao.kt     # CRUD local_sessions (S2)
│   │   │   │   │   ├── BlockDao.kt       # CRUD local_blocks con estados PENDING/SYNCING/SYNCED/ERROR (S3)
│   │   │   │   │   ├── UserDao.kt        # CRUD local_user: tokens, device_id, profile (S4, S10)
│   │   │   │   │   ├── HistoryDao.kt     # CRUD local_session_history FIFO 5 (S9)
│   │   │   │   │   ├── StatsDao.kt       # CRUD local_user_stats (S10)
│   │   │   │   │   └── RankingCacheDao.kt # CRUD local_ranking_cache (S6)
│   │   │   │   └── entity/
│   │   │   │       ├── LocalSessionEntity.kt        # session_id, status, sport_type, timestamps
│   │   │   │       ├── LocalBlockEntity.kt          # block_id, session_id, metrics, xp_calculated, status
│   │   │   │       ├── LocalUserEntity.kt           # access_token, refresh_token, token_expires_at, device_id, user_id
│   │   │   │       ├── LocalSessionHistoryEntity.kt  # session_id, timestamps, sport_type, duration, avg_bpm, total_blocks, total_xp (S9)
│   │   │   │       ├── LocalUserStatsEntity.kt      # total_xp, total_sessions, total_blocks, total_duration, avg_bpm_all_time, last_updated (S10)
│   │   │   │       └── LocalRankingCacheEntity.kt   # type, my_position, my_total_xp, top_json, cached_at, valid_until (S6)
│   │   │   │
│   │   │   └── datastore/
│   │   │       └── UserPreferencesDataStore.kt       # Solo preferencias UI (tema, notificaciones ON/OFF)
│   │   │
│   │   ├── remote/
│   │   │   ├── api/
│   │   │   │   ├── AuthApi.kt              # POST /api/auth/login, /refresh, /register, /logout
│   │   │   │   ├── ExerciseApi.kt            # POST /api/exercise/blocks
│   │   │   │   ├── RankingApi.kt             # GET /api/ranking/global, /municipal/{cityId}
│   │   │   │   ├── XpFormulaApi.kt           # GET /api/xp/formulas (S5 §2.3)
│   │   │   │   └── UserApi.kt                # GET /api/user/profile
│   │   │   ├── dto/                          # NO USAR — importar desde com.ace.shared.dto.*
│   │   │   └── interceptor/
│   │   │       └── AuthInterceptor.kt          # Bearer + manejo 401 → refresh + flag isRefreshing (S4 §6.2)
│   │   │
│   │   ├── wear/
│   │   │   ├── WearDataSource.kt             # Listener DataClient, filtra path /ace/health/heart_rate (S1 §2.2)
│   │   │   ├── WearMessageClient.kt          # Envía START/STOP al reloj (S1 §2.1)
│   │   │   └── model/
│   │   │       └── WearHeartRateSample.kt      # bpm: Int, timestamp: Long (epoch millis) (S1 §2.3)
│   │   │
│   │   └── repository/
│   │       ├── AuthRepository.kt             # Login, logout, refresh, estado de sesión
│   │       ├── ExerciseRepository.kt         # Recibe bloques del domain, arma batch, envía al API
│   │       ├── WearSyncRepository.kt         # Recibe FC del reloj, acumula en buffer circular
│   │       ├── SessionRepository.kt            # CRUD local_sessions (S2)
│   │       ├── BlockRepository.kt            # CRUD local_blocks (S3)
│   │       ├── HistoryRepository.kt            # CRUD local_session_history FIFO 5 (S9)
│   │       ├── StatsRepository.kt            # CRUD local_user_stats, cálculo ponderado (S10)
│   │       ├── RankingCacheRepository.kt     # CRUD local_ranking_cache (S6)
│   │       └── UserRepository.kt             # CRUD local_user (tokens, device_id)
│   │
│   ├── domain/
│   │   ├── model/                          # Objetos de dominio (no DTOs de red)
│   │   │   ├── ExerciseSession.kt            # id, sportType, startedAt, status (ACTIVE/PAUSED/COMPLETED/ABORTED)
│   │   │   ├── HeartRateAggregate.kt       # avgBpm, maxBpm, minBpm, sampleCount, windowStart, windowEnd
│   │   │   ├── SyncStatus.kt               # IDLE, SYNCING, ERROR, OFFLINE_BUFFER
│   │   │   └── StreakReminderState.kt      # cached_current_streak, cached_best_streak, cached_last_exercise_date (S7 §5.2)
│   │   │
│   │   └── usecase/                          # Casos de uso desacoplados (Clean Architecture)
│   │       ├── auth/
│   │       │   ├── LoginUseCase.kt
│   │       │   ├── LogoutUseCase.kt
│   │       │   └── RefreshTokenUseCase.kt      # Serializado con flag isRefreshing (S4 §6.2)
│   │       ├── exercise/
│   │       │   ├── StartSessionUseCase.kt      # Crea sesión ACTIVE, notifica Wear OS
│   │       │   ├── PauseSessionUseCase.kt      # PAUSED: detiene acumulación de bloques (S2 §2.3)
│   │       │   ├── ResumeSessionUseCase.kt     # ACTIVE: reanuda acumulación (S2 §2.3)
│   │       │   ├── StopSessionUseCase.kt       # COMPLETED: cierra bloque final, notifica S9
│   │       │   └── SendPendingBlocksUseCase.kt # Arma batch ≤ 20, envía con WorkManager (S3 §5.2)
│   │       ├── wear/
│   │       │   ├── ReceiveWearDataUseCase.kt   # Recibe FC crudo, alimenta buffer circular RAM 300 (S1 §5)
│   │       │   └── BuildExerciseBlockUseCase.kt # Cada ~300s o al DETENER: drena buffer, arma ExerciseBlockDto, dispara S5
│   │       ├── xp/
│   │       │   ├── CalculateBlockXpUseCase.kt  # Aplica fórmula cacheada, muestra recompensa (S5 §3)
│   │       │   └── CacheXpFormulasUseCase.kt   # Descarga y guarda fórmulas del backend (S5 §2.4)
│   │       ├── streak/
│   │       │   └── CheckStreakUseCase.kt       # Lee cached_last_exercise_date, decide notificación (S7 §4.3)
│   │       └── stats/
│   │           ├── AccumulateStatsUseCase.kt   # total_xp += xp_calculated, promedio ponderado (S10 §2.3)
│   │           └── ReconcileStatsUseCase.kt    # Aplica official_stats del backend (S10 §5.2)
│   │
│   ├── presentation/
│   │   ├── common/                           # Componentes Compose reutilizables
│   │   ├── auth/
│   │   │   ├── LoginScreen.kt
│   │   │   └── LoginViewModel.kt
│   │   ├── exercise/
│   │   │   ├── SessionScreen.kt              # FC en vivo, timer, botón Detener/Pausar
│   │   │   ├── SessionViewModel.kt           # StateFlow<SessionUiState>
│   │   │   └── SessionUiState.kt             # isRunning, isPaused, currentBpm, elapsedSeconds, blocksSent, currentXp
│   │   ├── ranking/
│   │   │   ├── RankingScreen.kt
│   │   │   └── RankingViewModel.kt
│   │   ├── profile/
│   │   │   ├── ProfileScreen.kt              # Muestra stats + historial + racha
│   │   │   └── ProfileViewModel.kt
│   │   └── history/
│   │       ├── HistoryScreen.kt              # Lista de últimas 5 sesiones (S9)
│   │       └── HistoryViewModel.kt
│   │
│   └── service/                              # FOREGROUND SERVICE + WORKERS
│       ├── ExerciseSyncService.kt            # ForegroundService tipo "health" (S8 §2)
│       │   # - Vinculado a WearDataSource
│       │   # - Acumula FC en buffer circular RAM 300 muestras (S1 §5)
│       │   # - Cada ~300s: BuildExerciseBlockUseCase → CalculateBlockXpUseCase → AccumulateStatsUseCase
│       │   # - Notificación persistente: "A.C.E sincronizando... FC: 145 bpm" (S8 §2.3)
│       │   # - Canal: ace_session_active (importancia BAJA, sin sonido) (S8 §5.2)
│       │
│       └── worker/                             # WorkManager workers
│           ├── SyncBlockWorker.kt              # Reintentos de sync con backoff exponencial (S3 §5.3)
│           │   # - Delay: 15min → 30min → 1h → 2h → 4h
│           │   # - Máximo 5 reintentos
│           │   # - Condición: requiere network
│           │   # - Input: block_ids a sincronizar
│           │
│           ├── CheckStreakWorker.kt            # Diario a las 20:00 hora local (S7 §4.2, S8 §3)
│           │   # - Lee cached_last_exercise_date
│           │   # - Si != hoy: notificación "🔥 No has entrenado hoy"
│           │   # - Canal: ace_streak_reminder (importancia ALTA, sonido+vibración)
│           │
│           └── SyncErrorNotificationWorker.kt  # Disparado cuando bloques pasan a ERROR (S8 §4)
│               # - Canal: ace_sync_error (importancia ALTA)
│               # - "Tienes X bloques sin sincronizar. Revisa tu conexión."
│               # - Solo una vez por lote de bloques ERROR
│
├── src/main/kotlin/com/ace/shared/           # Consumido como JAR, NO modificado aquí
│   └── [ExerciseBlockDto, SportType, etc. desde :shared]
│
├── src/main/res/
│   └── values/
│       └── strings.xml                         # Textos de notificaciones (coherentes con StreakConstants)
│
└── src/main/AndroidManifest.xml
    # - Foreground Service: android:foregroundServiceType="health"
    # - Permisos: INTERNET, BLUETOOTH, BODY_SENSORS (fallback), WAKE_LOCK
    # - WorkManager: android:name="androidx.work.impl.WorkManagerInitializer"
```

---

## 5. Flujos de Datos Conceptuales (Coherentes con Apéndices)

### 5.1 Sesión de Ejercicio Completa (S1 → S2 → S5 → S10)

```kotlin
// Usuario toca INICIAR
// → StartSessionUseCase:
//   - Genera session_id UUID
//   - Crea LocalSessionEntity(status = ACTIVE, sportType = seleccionado)
//   - Envía START al reloj vía WearMessageClient
//   - Inicia ExerciseSyncService (Foreground health)

// Reloj envía FC cada 1-2s
// → WearDataSource.onDataChanged() filtra path /ace/health/heart_rate
// → ReceiveWearDataUseCase:
//   - Valida tipo HEART_RATE_BPM
//   - Valida rango 30-250 bpm
//   - Inserta en buffer circular RAM (capacidad 300)
//   - Actualiza UI en vivo

// Cada ~300 segundos (o al DETENER):
// → BuildExerciseBlockUseCase:
//   - Drena buffer circular (operación drain)
//   - Calcula avgBpm, maxBpm, minBpm, sampleCount, durationSeconds
//   - Genera block_id UUIDv4
//   - Crea ExerciseBlockDto (de :shared) con schemaVersion = 1
//   - Persiste LocalBlockEntity(status = PENDING, xp_calculated = null)

// → CalculateBlockXpUseCase:
//   - Lee fórmula cacheada de Room (descargada de GET /api/xp/formulas)
//   - Si avgBpm < minBpm → xp = 0
//   - Calcula: xp = (durationSeconds / 60) * xpPerMinute
//   - Aplica maxXpPerBlock si excede
//   - Redondea floor
//   - Muestra al usuario: "+X XP" (toast/animación)
//   - Actualiza LocalBlockEntity.xp_calculated = X

// → AccumulateStatsUseCase:
//   - total_xp += xp_calculated
//   - total_sessions += 1 (si es primer bloque de sesión)
//   - total_blocks += 1
//   - total_duration_seconds += durationSeconds
//   - avg_bpm_all_time = promedio ponderado por sampleCount (S10 §2.4)
//   - Persiste en LocalUserStatsEntity

// Usuario toca DETENER
// → StopSessionUseCase:
//   - Actualiza LocalSessionEntity(status = COMPLETED, timestamp_end)
//   - Cierra bloque abierto si queda (incluso si corto < 270s)
//   - Notifica S9 (History): inserta en local_session_history, descarta si > 5 (FIFO)
//   - ExerciseSyncService se detiene, notificación persistente desaparece

// WorkManager programa SyncBlockWorker
// → Toma bloques PENDING (hasta 20), marca como SYNCING
// → Arma SyncBatchRequestDto:
//    - blocks: List<ExerciseBlockDto>
//    - clientStats: ClientStatsDto (de :shared) con totales locales
//    - clientTimestamp: epoch millis del dispositivo
//    - deviceId: UUID de instalación
// → AuthInterceptor añade Bearer token
// → POST /api/exercise/blocks

// Respuesta 201:
// → Para cada bloque: marca SYNCED
// → Actualiza official_stats si correctionApplied = true
// → Actualiza ranking cache si rankChanged = true
// → Actualiza streak cache (current_streak, last_exercise_date)

// Respuesta 422:
// → Bloque marcado ERROR
// → ReconcileStatsUseCase: revierte xp_calculated de LocalUserStatsEntity
// → Muestra corrección: "-X XP (bloque rechazado)"
// → No reintenta automáticamente
```

### 5.2 Autenticación y Refresh Token (S4)

```kotlin
// LoginScreen → LoginUseCase → AuthApi.login()
// → Guarda en Room (LocalUserEntity):
//    access_token, refresh_token, token_expires_at, device_id, user_id

// Cada request:
// → AuthInterceptor:
//    1. Lee access_token de Room
//    2. Añade Authorization: Bearer <token>
//    3. Si token expirado localmente (token_expires_at - 60s margen):
//       - Dispara RefreshTokenUseCase ANTES de enviar request

// 401 detectado en response:
// → AuthInterceptor:
//    1. Verifica flag isRefreshing (AtomicBoolean)
//    2. Si isRefreshing = false:
//       - Pone isRefreshing = true
//       - Bloquea request original
//       - Llama RefreshTokenUseCase → AuthApi.refresh()
//       - Si éxito: guarda nuevos tokens, actualiza token_expires_at
//       - Pone isRefreshing = false
//       - Reintenta request original con nuevo token
//    3. Si isRefreshing = true:
//       - Encola request original (cola thread-safe)
//       - Espera resultado del refresh en curso
//       - Cuando refresh termina, reintenta con nuevo token

// Refresh falla (401 REFRESH_REUSED o REFRESH_REVOKED):
// → AuthRepository.clearAllTokens()
// → Navega a LoginScreen
// → Muestra: "Sesión expirada. Por favor inicia sesión nuevamente."
```

### 5.3 Recordatorio de Racha (S7 + S8)

```kotlin
// WorkManager programa CheckStreakWorker diariamente a las 20:00 hora local

// CheckStreakWorker:
// 1. Lee cached_last_exercise_date de Room (StreakReminderState)
// 2. Compara con fecha actual del dispositivo
// 3. Si son distintas:
//    - Muestra notificación local:
//      * Canal: ace_streak_reminder (importancia ALTA, sonido, vibración)
//      * Título: "🔥 Tu racha está en peligro"
//      * Cuerpo: "No has entrenado hoy. ¡Sal a correr para mantener tu racha de X días!"
//      * Acción: "INICIAR" → abre app en pantalla de sesión
// 4. Si son iguales: no hace nada

// Invariantes:
// - NUNCA consulta al backend (S7 §4.1)
// - Usa SOLO cache local recibido en última sync
// - Sobrevive reinicio del teléfono (WorkManager)
// - Respetado por Doze (ventana de mantenimiento)
```

### 5.4 Notificación de Error de Sync (S8)

```kotlin
// SyncBlockWorker detecta que bloque pasó a ERROR después de 5 reintentos:
// → Dispara SyncErrorNotificationWorker

// SyncErrorNotificationWorker:
// 1. Cuenta bloques en estado ERROR
// 2. Si count > 0 y no se notificó antes:
//    - Muestra notificación:
//      * Canal: ace_sync_error (importancia ALTA)
//      * Título: "A.C.E — Problema de sincronización"
//      * Cuerpo: "X bloques no pudieron sincronizarse. Toca para más información."
//      * Acción: Abre app en pantalla de diagnóstico
// 3. Si bloques se resincronizan (estado cambia de ERROR a SYNCED):
//    - Cancela notificación automáticamente

// Invariante: No se repite en bucle. Una vez por lote de bloques ERROR.
```

---

## 6. Room: Esquema de Entidades (Coherentes con Apéndices)

### 6.1 LocalUserEntity (S4, S10)

```kotlin
@Entity(tableName = "local_user")
data class LocalUserEntity(
    @PrimaryKey
    val userId: String,              // UUID del usuario

    val accessToken: String,         // JWT access token
    val refreshToken: String,        // JWT refresh token
    val tokenExpiresAt: Long,        // Epoch millis de expiración del access
    val deviceId: String,            // UUID de instalación del móvil (S4 §3.3)
    val email: String,
    val nickname: String,
    val cityId: String               // Para ranking municipal (S6)
)
```

**Nota:** Tokens van a Room (SQLite), NO a DataStore. DataStore solo para preferencias UI (tema, notificaciones ON/OFF). Esto es coherente con el apéndice S4 §3.2 que habla de `local_user` en SQLite.

### 6.2 LocalSessionEntity (S2)

```kotlin
@Entity(tableName = "local_sessions")
data class LocalSessionEntity(
    @PrimaryKey
    val sessionId: String,           // UUID generado por móvil

    val status: String,              // ACTIVE, PAUSED, COMPLETED, ABORTED
    val sportType: String,           // RUNNING, CYCLING...
    val timestampStart: Long,        // Epoch millis
    val timestampEnd: Long? = null   // Epoch millis (null si ACTIVE/PAUSED)
)
```

### 6.3 LocalBlockEntity (S2, S3)

```kotlin
@Entity(
    tableName = "local_blocks",
    foreignKeys = [ForeignKey(
        entity = LocalSessionEntity::class,
        parentColumns = ["sessionId"],
        childColumns = ["sessionId"]
    )]
)
data class LocalBlockEntity(
    @PrimaryKey
    val blockId: String,             // UUIDv4 generado por móvil al cerrar

    val sessionId: String,           // FK a local_sessions
    val timestampStart: Long,        // Epoch millis
    val timestampEnd: Long,          // Epoch millis
    val durationSeconds: Int,        // timestampEnd - timestampStart
    val avgBpm: Double,
    val maxBpm: Double,
    val minBpm: Double,
    val sampleCount: Int,
    val sportType: String,
    val xpCalculated: Int,           // Calculado por móvil, validado por backend
    val status: String,              // PENDING, SYNCING, SYNCED, ERROR
    val retryCount: Int = 0          // Contador de reintentos (0-5)
)
```

### 6.4 LocalSessionHistoryEntity (S9)

```kotlin
@Entity(tableName = "local_session_history")
data class LocalSessionHistoryEntity(
    @PrimaryKey
    val sessionId: String,

    val timestampStart: Long,
    val timestampEnd: Long,
    val sportType: String,
    val durationSeconds: Int,
    val avgBpm: Double,
    val totalBlocks: Int,
    val totalXp: Int,
    val insertedAt: Long             // Para ordenar y descartar FIFO
)
```

**Regla FIFO:** Al insertar, si count > 5, eliminar el de `insertedAt` más antiguo. Sin discriminar por categoría (S9 §3.4).

### 6.5 LocalUserStatsEntity (S10)

```kotlin
@Entity(tableName = "local_user_stats")
data class LocalUserStatsEntity(
    @PrimaryKey
    val userId: String,

    val totalXp: Long,
    val totalSessions: Int,
    val totalBlocks: Int,
    val totalDurationSeconds: Long,
    val avgBpmAllTime: Double,       // Promedio PONDERADO por sampleCount
    val totalSamples: Long,          // Para recalcular promedio ponderado
    val lastUpdated: Long            // Epoch millis
)
```

**Cálculo ponderado:**
```kotlin
val nuevoAvg = (avgAnterior * totalMuestrasAnterior + avgBpmBloque * sampleCountBloque)
            / (totalMuestrasAnterior + sampleCountBloque)
```

### 6.6 LocalRankingCacheEntity (S6)

```kotlin
@Entity(tableName = "local_ranking_cache")
data class LocalRankingCacheEntity(
    @PrimaryKey
    val type: String,                // "GLOBAL" o "MUNICIPAL_{cityId}"

    val myPosition: Int,
    val myTotalXp: Long,
    val topJson: String,             // Array JSON del top 10 (no 100)
    val cachedAt: Long,              // Epoch millis
    val validUntil: Long             // cachedAt + 1 hora
)
```

### 6.7 StreakReminderState (S7)

```kotlin
@Entity(tableName = "streak_reminder_state")
data class StreakReminderState(
    @PrimaryKey
    val userId: String,

    val cachedCurrentStreak: Int,
    val cachedBestStreak: Int,
    val cachedLastExerciseDate: String,  // ISO-8601 date (sin hora)
    val lastSyncAt: Long                   // Epoch millis de última sync
)
```

---

## 7. NotificationChannels (S8 §5.2)

```kotlin
// MobileApplication.onCreate()
private fun createNotificationChannels() {
    // Canal 1: Sesión activa (foreground service)
    val sessionChannel = NotificationChannel(
        "ace_session_active",
        "Sesión activa",
        NotificationManager.IMPORTANCE_LOW  // Sin sonido, sin vibración
    ).apply {
        description = "Notificación persistente durante ejercicio"
    }

    // Canal 2: Recordatorio de racha
    val streakChannel = NotificationChannel(
        "ace_streak_reminder",
        "Recordatorio de racha",
        NotificationManager.IMPORTANCE_HIGH  // Sonido, vibración, pantalla de bloqueo
    ).apply {
        description = "Alerta diaria si no has entrenado"
    }

    // Canal 3: Error de sync
    val syncErrorChannel = NotificationChannel(
        "ace_sync_error",
        "Error de sincronización",
        NotificationManager.IMPORTANCE_HIGH
    ).apply {
        description = "Alerta cuando bloques no pueden sincronizarse"
    }

    val manager = getSystemService(NotificationManager::class.java)
    manager.createNotificationChannels(listOf(sessionChannel, streakChannel, syncErrorChannel))
}
```

---

## 8. Decisiones Técnicas y Trade-offs (Coherentes con Apéndices)

| Decisión | Valor | Justificación | Trade-off | Apéndice |
|----------|-------|---------------|-----------|----------|
| **AGP 9.0.1** | Sí | Generado por Android Studio Otter 3. Built-in Kotlin reduce configuración. | Requiere Hilt 2.59.2+ (2.52 rompe con `BaseException`). | — |
| **Built-in Kotlin (AGP 9.0)** | Sí | No requiere plugin `org.jetbrains.kotlin.android`. Menos boilerplate. | `org.jetbrains.kotlin.kapt` es incompatible; mobile usa KSP. | — |
| **KSP** | Sí | Estándar moderno para annotation processing en AGP 9.0. Más rápido que kapt. | Curva de aprendizaje. Room y Hilt requieren configuración específica. | — |
| **Kotlin 2.1.20** | Sí | Built-in en AGP 9.0.1. Compatible con KSP 2.1.20-1.0.32. | Plan anterior decía 2.1.0, pero Android Studio genera 2.1.20 y es compatible. | — |
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

- [ ] Esqueleto Android Studio: módulo `:mobile`, AGP 9.0.1, Kotlin 2.1.20 built-in, Hilt 2.59.2, KSP, Compose, Room
- [ ] Integración con `:shared` JAR v1.0.0 (DTOs y enums)
- [ ] **S4 Auth:** Login/Registro UI + AuthRepository + Room `local_user` (tokens, device_id)
- [ ] **S4 Auth:** `AuthInterceptor` con flag `isRefreshing` + cola de peticiones + manejo `REFRESH_REUSED`
- [ ] **S1 Sensor:** `WearDataSource` recibe FC por DataClient en path `/ace/health/heart_rate`
- [ ] **S1 Sensor:** Buffer circular RAM 300 muestras, timeout desconexión > 5s
- [ ] **S2 Session:** `ExerciseSyncService` (Foreground `health`) con notificación persistente
- [ ] **S2 Session:** Estados ACTIVE, PAUSED, COMPLETED, ABORTED. Solo 1 ACTIVE.
- [ ] **S2 Session:** `BuildExerciseBlockUseCase` arma `ExerciseBlockDto` con `block_id` UUIDv4
- [ ] **S5 XP:** `CalculateBlockXpUseCase` con fórmulas cacheadas de Room (descargadas de `GET /api/xp/formulas`)
- [ ] **S5 XP:** Recompensa inmediata "+X XP" visible offline
- [ ] **S3 Sync:** `SendPendingBlocksUseCase` con WorkManager, batch ≤ 20, backoff exponencial 15min→4h, 5 reintentos
- [ ] **S3 Sync:** Estados PENDING → SYNCING → SYNCED/ERROR. Idempotencia por block_id.
- [ ] **S7 Streaks:** `CheckStreakWorker` diario a las 20:00. Notificación local si no entrenó.
- [ ] **S7 Streaks:** Cache de `current_streak`, `best_streak`, `last_exercise_date` desde respuesta de sync
- [ ] **S8 Notif:** 3 NotificationChannels: ace_session_active, ace_streak_reminder, ace_sync_error
- [ ] **S8 Notif:** Foreground service notif durante sesión. Sync error notif tras 5 reintentos.
- [ ] **S6 Ranking:** Pantalla de ranking con cache local 1h (posición propia + top 10)
- [ ] **S6 Ranking:** Invalida cache si `rankChanged = true` en respuesta de sync
- [ ] **S9 History:** `local_session_history` FIFO 5 sesiones, sin discriminar categoría
- [ ] **S10 Stats:** `local_user_stats` con acumulación inmediata y promedio ponderado por sampleCount
- [ ] **S10 Stats:** Envía `client_stats` en CADA batch. Recibe `official_stats`. Aplica corrección.
- [ ] **S10 Stats:** Corrección silenciosa si dif < 10 XP. Toast si dif ≥ 10 XP.
- [ ] **AGP 9.0 + KSP:** Verificar que build es successful con KSP procesando Room y Hilt correctamente.

### Fase de Transición (Semanas 5-8)
- [ ] Reintento inteligente: WorkManager encola bloques fallidos
- [ ] Pantalla de perfil: edición nickname, peso, ciudad (impacta ranking municipal)
- [ ] Pantalla de diagnóstico de bloques ERROR (motivo del rechazo)
- [ ] Notificaciones ricas: progreso de racha, nuevos rangos
- [ ] Historial completo paginado desde backend

### Fase Máxima (Semanas 9-12+)
- [ ] Análisis post-sesión: gráficos de FC, zonas de intensidad
- [ ] Modo "solo móvil": sensor de pulso del teléfono (fallback)
- [ ] Sincronización de configuración: objetivos diarios, recordatorios personalizados
- [ ] Deep links: compartir perfil/ranking
- [ ] Widget de home screen: FC en vivo y racha actual

---

## 10. Checklist de Integración con Backend (Coherente con Apéndices)

- [ ] `ExerciseSyncService` sobrevive 30 min en background sin ser matado (prueba Samsung, Xiaomi, Pixel)
- [ ] Bloque de prueba enviado desde Postman con mismo formato que móvil → backend acepta `201`
- [ ] Refresh token se rota: después de 401, móvil obtiene nuevo access sin que usuario note
- [ ] Si backend caído, Foreground Service sigue acumulando FC y loguea error (no crashea)
- [ ] Notificación WorkManager 20:00 aparece incluso si teléfono fue reiniciado esa mañana
- [ ] Batch de 21 bloques: backend responde `400` (validación de tamaño)
- [ ] Bloque con `xpCalculated = 999` para 5min a 80bpm: backend rechaza `422`, móvil revierte XP local
- [ ] Bloque con `xpCalculated = 8` cuando fórmula dice 10: backend acepta (móvil con fórmula vieja)
- [ ] `client_stats` incluido en cada batch: backend valida y responde `official_stats`
- [ ] Promedio ponderado de FC: verificar con bloques de distinto sampleCount
- [ ] Historial local: insertar 6 sesiones, verificar que la más antigua se descarta (FIFO)
- [ ] Rachas: bloque con `timestamp_start` de ayer no afecta racha de hoy
- [ ] Paths: reloj escribe en `/ace/health/heart_rate`, móvil lee del mismo path
- [ ] **AGP 9.0 + KSP + Hilt 2.59.2:** build successful, KSP genera código de Room y Hilt sin errores
- [ ] **Built-in Kotlin:** verificar que no se usa `org.jetbrains.kotlin.android` ni `org.jetbrains.kotlin.kapt`

---

*Documento coherente con Apéndices S1-S10 y Arquitectura A.C.E v0.2. Cualquier divergencia debe ser reportada como bug de coherencia.*
