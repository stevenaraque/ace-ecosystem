# A.C.E — Implementation Plan: Mobile (`ace-mobile`)

> **Estado:** Coherente con Apéndices Aprobados S1-S10 y Arquitectura v0.2  
> **Versión:** 4.1 (Actualizado: JitPack para :shared, AGP 9.0 + Built-in Kotlin + Hilt 2.59.2 + KSP)  
> **Fecha:** Junio 2026  
> **Stack:** Android 13+ (API 33) · Kotlin 2.1.20 (built-in) · Gradle 8.10+ (Kotlin DSL) · AGP 9.0.1 · Wear OS Data Layer API · Retrofit 2.11.0 + Gson · Hilt 2.59.2 · Room 2.6.1 · Jetpack Compose BOM 2024.09.00 · KSP 2.1.20-1.0.32  
> **Depende de:** `com.github.reinaldojperalta:ace-shared` v1.0.0+ (JitPack)  
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

**Nota sobre :shared:** El móvil consume el módulo `:shared` vía **JitPack** como artifact Maven externo. La coordenada exacta es `com.github.reinaldojperalta:ace-shared`. No se incluye como `project(":shared")` ni como JAR local.

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
    // ─── :shared vía JitPack ───
    // Coordenada exacta: JitPack sobrescribe group a com.github.reinaldojperalta
    implementation("com.github.reinaldojperalta:ace-shared:1.0.0")

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
    ksp(libs.room.compiler)

    // ─── WorkManager ───
    implementation(libs.work.runtime.ktx)

    // ─── Retrofit & OkHttp ───
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // ─── Hilt (DI) ───
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)
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
| **Plugin kapt** | `org.jetbrains.kotlin.kapt` | **Eliminado** | Eliminado | Incompatible con built-in Kotlin. Mobile usa **KSP**. |
| `kotlinOptions { jvmTarget }` | `"21"` explícito | **Eliminado** | Eliminado | `compileOptions` Java 21 es suficiente; AGP 9.0 infiere automáticamente. |
| `composeOptions { kotlinCompilerExtensionVersion }` | `"1.5.14"` explícito | **Eliminado** | Eliminado | El plugin `org.jetbrains.kotlin.plugin.compose` gestiona el compiler automáticamente. |
| Hilt | `2.52` | **`2.59.2`** | 2.59.2 | Hilt 2.52 accede a `BaseExtension` removida en AGP 9.0. 2.59.2+ es compatible. |
| Annotation Processing | `kapt` (estándar) | **KSP** | KSP | KSP es el estándar moderno para AGP 9.0. Mobile usa KSP; Wear usa `legacy-kapt` por incompatibilidades específicas documentadas. |
| Kotlin | `2.1.0` (plugin explícito) | **`2.1.20` (built-in)** | 2.1.20 | Android Studio Otter 3 genera 2.1.20 built-in. Compatible con el ecosistema. |

---

## 3. Arquitectura y Patrones (Coherente con Arquitectura §2.1)

[Idéntico a v4.0]

---

## 4. Estructura de Carpetas

```
ace-mobile/
├── build.gradle.kts                      # AGP 9.0.1, Kotlin 2.1.20 built-in, Hilt, KSP, Room, Compose, Wearable
├── settings.gradle.kts                   # include ':app'
├── gradle/libs.versions.toml
├── gradle/wrapper/
│
├── app/build.gradle.kts                  # JitPack: com.github.reinaldojperalta:ace-shared
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
│   │   │   │       ├── LocalSessionEntity.kt
│   │   │   │       ├── LocalBlockEntity.kt
│   │   │   │       ├── LocalUserEntity.kt
│   │   │   │       ├── LocalSessionHistoryEntity.kt
│   │   │   │       ├── LocalUserStatsEntity.kt
│   │   │   │       └── LocalRankingCacheEntity.kt
│   │   │   │
│   │   │   └── datastore/
│   │   │       └── UserPreferencesDataStore.kt
│   │   │
│   │   ├── remote/
│   │   │   ├── api/
│   │   │   │   ├── AuthApi.kt
│   │   │   │   ├── ExerciseApi.kt
│   │   │   │   ├── RankingApi.kt
│   │   │   │   ├── XpFormulaApi.kt
│   │   │   │   └── UserApi.kt
│   │   │   ├── dto/                          # NO USAR — importar desde com.ace.shared.dto.*
│   │   │   └── interceptor/
│   │   │       └── AuthInterceptor.kt
│   │   │
│   │   ├── wear/
│   │   │   ├── WearDataSource.kt
│   │   │   ├── WearMessageClient.kt
│   │   │   └── model/
│   │   │       └── WearHeartRateSample.kt
│   │   │
│   │   └── repository/
│   │       ├── AuthRepository.kt
│   │       ├── ExerciseRepository.kt
│   │       ├── WearSyncRepository.kt
│   │       ├── SessionRepository.kt
│   │       ├── BlockRepository.kt
│   │       ├── HistoryRepository.kt
│   │       ├── StatsRepository.kt
│   │       ├── RankingCacheRepository.kt
│   │       └── UserRepository.kt
│   │
│   ├── domain/
│   │   ├── model/
│   │   │   ├── ExerciseSession.kt
│   │   │   ├── HeartRateAggregate.kt
│   │   │   ├── SyncStatus.kt
│   │   │   └── StreakReminderState.kt
│   │   │
│   │   └── usecase/
│   │       ├── auth/
│   │       │   ├── LoginUseCase.kt
│   │       │   ├── LogoutUseCase.kt
│   │       │   └── RefreshTokenUseCase.kt
│   │       ├── exercise/
│   │       │   ├── StartSessionUseCase.kt
│   │       │   ├── PauseSessionUseCase.kt
│   │       │   ├── ResumeSessionUseCase.kt
│   │       │   ├── StopSessionUseCase.kt
│   │       │   └── SendPendingBlocksUseCase.kt
│   │       ├── wear/
│   │       │   ├── ReceiveWearDataUseCase.kt
│   │       │   └── BuildExerciseBlockUseCase.kt
│   │       ├── xp/
│   │       │   ├── CalculateBlockXpUseCase.kt
│   │       │   └── CacheXpFormulasUseCase.kt
│   │       ├── streak/
│   │       │   └── CheckStreakUseCase.kt
│   │       └── stats/
│   │           ├── AccumulateStatsUseCase.kt
│   │           └── ReconcileStatsUseCase.kt
│   │
│   ├── presentation/
│   │   ├── common/
│   │   ├── auth/
│   │   │   ├── LoginScreen.kt
│   │   │   └── LoginViewModel.kt
│   │   ├── exercise/
│   │   │   ├── SessionScreen.kt
│   │   │   ├── SessionViewModel.kt
│   │   │   └── SessionUiState.kt
│   │   ├── ranking/
│   │   │   ├── RankingScreen.kt
│   │   │   └── RankingViewModel.kt
│   │   ├── profile/
│   │   │   ├── ProfileScreen.kt
│   │   │   └── ProfileViewModel.kt
│   │   └── history/
│   │       ├── HistoryScreen.kt
│   │       └── HistoryViewModel.kt
│   │
│   └── service/
│       ├── ExerciseSyncService.kt
│       └── worker/
│           ├── SyncBlockWorker.kt
│           ├── CheckStreakWorker.kt
│           └── SyncErrorNotificationWorker.kt
│
├── src/main/res/
│   └── values/
│       └── strings.xml
│
└── src/main/AndroidManifest.xml
```

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
- [ ] Integración con `:shared` vía JitPack: `com.github.reinaldojperalta:ace-shared:1.0.0` (DTOs y enums)
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
- [ ] **Built-in Kotlin:** Verificar que no se usa `org.jetbrains.kotlin.android` ni `org.jetbrains.kotlin.kapt`
- [ ] **JitPack:** Verificar que `com.github.reinaldojperalta:ace-shared` se resuelve correctamente en build

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
