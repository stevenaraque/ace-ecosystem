# A.C.E — Implementation Plan: Wear OS (`ace-wear`)

> **Estado:** Coherente con Apéndices Aprobados S1-S10 y Arquitectura v0.2  
> **Versión:** 4.0 (Actualizado a AGP 9.0 + Built-in Kotlin + Hilt 2.59.2)  
> **Fecha:** Junio 2026  
> **Stack:** Wear OS 3+ (API 30+) · Kotlin 2.0.21 (built-in) · Gradle 8.10+ (Kotlin DSL) · AGP 9.0.1 · Health Services Client 1.0.0 · Wear OS Data Layer API · Jetpack Compose for Wear OS 1.6.2 · Hilt 2.59.2  
> **Depende de:** NO consume `:shared` JAR (primitivos JSON)  
> **Responsable:** Steven Araque (Mobile/Wear Lead)

---

## 1. Visión y Alcance (Coherente con Arquitectura §1)

El módulo `:wear` es el **sensor y transmisor** del ecosistema. Su única misión es:

1. **Capturar** frecuencia cardíaca (FC) en tiempo real vía Health Services API (S1).
2. **Transmitir** datos al móvil inmediatamente vía Wear OS Data Layer API (DataClient) (S1).
3. **Mostrar** UI ultra-minimalista: FC grande, estado de conexión, botón Detener (S1, S2).
4. **Escuchar** comandos START/STOP del móvil vía MessageClient (S1, S2).
5. **NO calcular** XP, NO agrupar bloques, NO persistir localmente, NO autenticarse (S1 §3.4).

**Principio rector:** *El reloj es un sensor con pantalla. Toda la inteligencia vive en el móvil o en la nube.* (Arquitectura §1)

**Regla de oro:** Si algo puede hacerlo el móvil o el backend, **no lo hagas en el reloj** (S1 §9).

---

## 2. Dependencias Gradle (`build.gradle.kts`)

### 2.1. `libs.versions.toml` (Version Catalog)

```toml
[versions]
agp = "9.0.1"
kotlin = "2.0.21"
playServicesWearable = "18.2.0"
healthServices = "1.0.0"
composeBom = "2026.05.00"
activityCompose = "1.13.0"
coreSplashscreen = "1.2.0"
hilt = "2.59.2"
coroutines = "1.9.0"
lifecycle = "2.8.4"
coreKtx = "1.13.1"
wear = "1.3.0"

[libraries]
# --- Kotlin & Corrutinas ---
kotlin-stdlib = { group = "org.jetbrains.kotlin", name = "kotlin-stdlib", version.ref = "kotlin" }
kotlinx-coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }
kotlinx-coroutines-play-services = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-play-services", version.ref = "coroutines" }

# --- Wear OS Core ---
core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
wear = { group = "androidx.wear", name = "wear", version.ref = "wear" }

# --- Health & Data Layer ---
health-services-client = { group = "androidx.health", name = "health-services-client", version.ref = "healthServices" }
play-services-wearable = { group = "com.google.android.gms", name = "play-services-wearable", version.ref = "playServicesWearable" }

# --- Compose (BOM) ---
compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
ui = { group = "androidx.compose.ui", name = "ui" }
ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }

# --- Wear Compose ---
wear-compose-foundation = { group = "androidx.wear.compose", name = "compose-foundation", version = "1.6.2" }
wear-compose-material = { group = "androidx.wear.compose", name = "compose-material", version = "1.6.2" }

# --- Lifecycle ---
lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycle" }
lifecycle-viewmodel-ktx = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-ktx", version.ref = "lifecycle" }
lifecycle-service = { group = "androidx.lifecycle", name = "lifecycle-service", version.ref = "lifecycle" }

# --- Hilt (DI) ---
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-compiler", version.ref = "hilt" }

# --- Tooling ---
wear-tooling-preview = { group = "androidx.wear", name = "wear-tooling-preview", version = "1.0.0" }
activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }
core-splashscreen = { group = "androidx.core", name = "core-splashscreen", version.ref = "coreSplashscreen" }

# --- Testing ---
junit = { group = "junit", name = "junit", version = "4.13.2" }
mockk = { group = "io.mockk", name = "mockk", version = "1.13.12" }
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutines" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
legacy-kapt = { id = "com.android.legacy-kapt", version.ref = "agp" }
hilt-android = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
```

### 2.2. Top-level `build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.legacy.kapt) apply false
    alias(libs.plugins.hilt.android) apply false
}
```

### 2.3. Módulo `:app` `build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.legacy.kapt)        // Requerido para kapt en AGP 9.0+
    alias(libs.plugins.hilt.android)
}

android {
    namespace = "sena.adso.ace_wear"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "sena.adso.ace_wear"
        minSdk = 30          // Wear OS 3+ (API 30)
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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

    buildFeatures {
        compose = true
    }
}

dependencies {
    // Kotlin & Corrutinas
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    // Wear OS Core
    implementation(libs.core.ktx)
    implementation(libs.wear)

    // Health & Data
    implementation(libs.health.services.client)
    implementation(libs.play.services.wearable)

    // Compose (BOM)
    implementation(platform(libs.compose.bom))
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    debugImplementation(libs.ui.tooling)

    // Wear Compose
    implementation(libs.wear.compose.foundation)
    implementation(libs.wear.compose.material)

    // Lifecycle (MVVM)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.lifecycle.service)

    // Hilt (DI) — usando legacy-kapt
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)

    // Tooling
    implementation(libs.wear.tooling.preview)
    implementation(libs.activity.compose)
    implementation(libs.core.splashscreen)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}
```

### 2.4. Notas críticas sobre AGP 9.0 + Built-in Kotlin

| Aspecto | Pre-AGP 9.0 (plan v3.0) | AGP 9.0 (plan v4.0) | Justificación |
|---------|------------------------|---------------------|---------------|
| Plugin Kotlin Android | `org.jetbrains.kotlin.android` | **Eliminado** | Built-in en AGP 9.0. No se declara. |
| Plugin kapt | `org.jetbrains.kotlin.kapt` | **`com.android.legacy-kapt`** | `org.jetbrains.kotlin.kapt` es incompatible con built-in Kotlin. |
| `kotlinOptions { jvmTarget }` | Explícito | **Eliminado** | `compileOptions` Java 21 es suficiente; AGP 9.0 infiere automáticamente. |
| `composeOptions { kotlinCompilerExtensionVersion }` | `1.5.14` explícito | **Eliminado** | El plugin `org.jetbrains.kotlin.plugin.compose` gestiona el compiler automáticamente. |
| Hilt | `2.52` | **`2.59.2`** | Hilt 2.52 accede a `BaseExtension` removida en AGP 9.0. 2.59.2+ es compatible. |
| KSP para Hilt | No recomendado | **No usar** | Hilt 2.59.2 con AGP 9.0 requiere `legacy-kapt` (KSP tiene incompatibilidades documentadas). |

---

## 3. Arquitectura y Patrones (Coherente con Arquitectura §2.1)

| Patrón | Dónde vive | Justificación | Coherencia |
|--------|-----------|---------------|------------|
| **MVVM** | `presentation/` | ViewModel mantiene estado de sesión (FC, timer, sync). Sobrevive rotaciones. | S1, S2 |
| **Observer** | `HealthServicesManager` + `DataClient` | Reactivo: callbacks de FC, listeners de conectividad. Sin polling. | S1 §3.2 |
| **Singleton** | `di/` (Hilt) | `ExerciseClient` costoso de crear. `DataClient` único por app. | S1 §3.3 |
| **Repository ligero** | `data/` | `WearHealthRepository` abstrae lectura del sensor. Cambio de API fácil. | S1 §3.3 |
| **NO DTO / :shared** | — | El reloj envía primitivos (`Int bpm`, `Long timestamp`). Reduce footprint. | S1 §2.3 |

---

## 4. Estructura de Carpetas

```
ace-wear/
├── build.gradle.kts                          # Gradle, AGP 9.0.1, built-in Kotlin
├── settings.gradle.kts
├── gradle/wrapper/
│
├── src/main/kotlin/com/ace/wear/
│   ├── WearApplication.kt                    # @HiltAndroidApp
│   │
│   ├── di/
│   │   ├── HealthServicesModule.kt           # Provee ExerciseClient como Singleton
│   │   └── WearDataLayerModule.kt            # Provee DataClient, MessageClient
│   │
│   ├── data/
│   │   ├── health/
│   │   │   ├── HealthServicesManager.kt      # Wrapper sobre ExerciseClient (S1 §3.3)
│   │   │   └── model/
│   │   │       └── HeartRateSample.kt        # bpm: Int, timestamp: Long (epoch millis) (S1 §2.3)
│   │   │
│   │   ├── sync/
│   │   │   ├── WearDataClient.kt             # Abstrae DataClient de Google (S1 §2.1)
│   │   │   └── WearMessageClient.kt          # Recibe comandos del móvil (START/STOP) (S1 §2.1)
│   │   │
│   │   └── repository/
│   │       └── WearHealthRepository.kt       # Único punto de acceso: sensor → UI/transmisión
│   │
│   ├── domain/
│   │   └── usecase/
│   │       ├── StartExerciseUseCase.kt       # Llama HealthServicesManager.startExercise() (S2 §2.4)
│   │       └── StopExerciseUseCase.kt        # Llama HealthServicesManager.stopExercise() + envía STOP al móvil (S2 §2.4)
│   │
│   ├── presentation/
│   │   ├── theme/
│   │   │   └── WearTheme.kt                  # Colores A.C.E, tipografía escalada para reloj
│   │   ├── components/
│   │   │   ├── HeartRateDisplay.kt           # Composable: FC en fontSize 48sp, color según zona (S1 §4.3)
│   │   │   ├── ConnectionStatusChip.kt       # Composable: icono verde (sync) / amarillo (buffer) / rojo (error)
│   │   │   ├── TimerDisplay.kt               # Composable: tiempo transcurrido de sesión
│   │   │   └── StopButton.kt                 # Composable: botón grande rojo "DETENER"
│   │   ├── session/
│   │   │   ├── SessionScreen.kt              # Pantalla principal durante ejercicio
│   │   │   └── SessionViewModel.kt           # StateFlow<WearSessionState>
│   │   └── WearSessionState.kt               # currentBpm, elapsedSeconds, isConnected, isRunning, syncStatus
│   │
│   └── service/                              # ESTRUCTURA LISTA — vacío en MVP (S1 §3.3)
│       └── [Placeholder: Health Services maneja su propio foreground state]
│
├── src/main/res/
│   ├── values/
│   └── drawable/
│
└── src/main/AndroidManifest.xml
    # - Permisos: BODY_SENSORS, HIGH_SAMPLING_RATE_SENSORS, ACTIVITY_RECOGNITION, WAKE_LOCK
    # - NO necesita INTERNET directo (DataClient usa canal de Google)
    # - NO necesita FOREGROUND_SERVICE (Health Services maneja su propio lifecycle)
```

---

## 5. Flujos de Datos Conceptuales (Coherentes con Apéndices)

### 5.1 Captura y Transmisión de FC (S1)

```kotlin
// Usuario toca "INICIAR" en reloj
// → StartExerciseUseCase:
//   1. Llama HealthServicesManager.startExercise()
//   2. Registra callback de ExerciseUpdateCallback
//   3. Health Services API activa sensor de FC a 1 Hz
//   4. Cada segundo: ExerciseUpdate con HEART_RATE_BPM
//      { dataType: HEART_RATE_BPM, value: 145.0, time_interval: [start, end] }

// WearHealthRepository:
//   1. Recibe HeartRateSample (primitivo: bpm, timestamp)
//   2. Actualiza StateFlow en ViewModel (UI se recompone)
//   3. Envía INMEDIATAMENTE a DataClient:
//      PutDataMapRequest.create("/ace/health/heart_rate")
//      .putInt("bpm", 145)
//      .putLong("timestamp", 1716812400000)  // Epoch millis
//      // NOTA: path es /ace/health/heart_rate (S1 §2.2, DataLayerPaths.HEART_RATE)

// Google Wearable Data Layer API:
//   - Si móvil conectado: entrega inmediata
//   - Si móvil desconectado: almacena en caché interna del SO
//     (estimado ~100-200 items o 24h, luego descarta antiguos)
//   - Al reconectar: libera ráfaga automática

// WearDataClient NO hace:
//   - NO agrupa muestras en lotes
//   - NO calcula promedios
//   - NO conoce bloques ni sesiones
//   - NO decide si muestra es válida
//   - NO persiste en disco del reloj
```

### 5.2 Comando de Detención (S2)

```kotlin
// ESCENARIO A: Usuario presiona "DETENER" en reloj
// → StopButton → StopExerciseUseCase:
//   1. Llama HealthServicesManager.stopExercise()
//   2. Cancela callback de FC
//   3. Envía señal al móvil:
//      WearDataClient.putDataItem(
//        "/ace/session/{sessionId}/status",
//        "STOPPED"
//      )
//   4. UI muestra resumen breve (duración, FC promedio — enviado por móvil)

// ESCENARIO B: Móvil envía comando remoto
// → Mobile envía MessageClient.sendMessage("/ace/command", "STOP")
// → WearMessageClient.onMessageReceived():
//   1. Verifica path "/ace/command"
//   2. Verifica payload "STOP"
//   3. Llama StopExerciseUseCase (mismo flujo que escenario A)
//   4. Móvil puede detener sesión desde la app

// Nota: MessageClient es fire-and-forget. Si reloj no está al alcance,
// el comando se pierde. Aceptable porque usuario puede detener desde móvil (S1 §2.1).
```

### 5.3 Estados de la UI (S1 §4.3)

```
┌─────────────────────────────────────────┐
│  [🔴 145]  <- FC actual, fontSize 48sp  │
│  ━━━━━━━━━━  <- barra de progreso         │
│  12:34  <- tiempo transcurrido          │
│  [●] Sync  <- chip de conexión          │
│                                         │
│  [ DETENER ] <- botón rojo grande       │
└─────────────────────────────────────────┘

Estados de conexión (ConnectionStatusChip):
- Verde (● Sync): DataClient tiene peer (móvil) conectado. Datos entregando inmediatamente.
- Amarillo (◐ Buffer): Sin peer, bufferizando en caché del SO. Datos no perdidos aún.
- Rojo (✕ Error): Error crítico (sin permisos de sensor, por ejemplo).

Colores de FC según zona (futuro, no MVP):
- Gris < 80 bpm
- Verde 80-120 bpm
- Naranja 120-150 bpm
- Rojo > 150 bpm
```

### 5.4 Sincronización Inicial (Emparejamiento)

```kotlin
// Al emparejar móvil con reloj (primera vez o nueva sesión):
// → Móvil envía al reloj vía DataClient o MessageClient:
//   - userId: UUID del usuario
//   - deviceId: String identificador del reloj
//   - sessionId: UUID de la sesión actual

// Reloj recibe y almacena en memoria (RAM, no disco):
//   - NO necesita autenticarse
//   - NO necesita login
//   - Hereda identidad del móvil

// Si móvil nunca se conectó al reloj:
//   - Reloj no puede etiquetar datos con userId/sessionId
//   - Mitigado: primer emparejamiento obligatorio sincroniza IDs
```

---

## 6. Decisiones Técnicas y Trade-offs (Coherentes con Apéndices)

| Decisión | Valor | Justificación | Trade-off | Apéndice |
|----------|-------|---------------|-----------|----------|
| **AGP 9.0.1** | Sí | Generado por Android Studio Otter 3. Built-in Kotlin reduce configuración. | Requiere Hilt 2.59.2+ (2.52 rompe con `BaseException`). | — |
| **Built-in Kotlin (AGP 9.0)** | Sí | No requiere plugin `org.jetbrains.kotlin.android`. Menos boilerplate. | `org.jetbrains.kotlin.kapt` es incompatible; se usa `com.android.legacy-kapt`. | — |
| **Health Services API** | Sí | API oficial Wear OS. Optimiza batería (batching, frecuencia adaptativa). Gestiona foreground state sin Service propio. | Requiere Wear OS 3+ (API 30). Dispositivos 2.x quedan fuera. | S1 §3.1 |
| **NO Room / archivos locales** | Sí | Cero persistencia en disco = cero I/O de batería. DataClient maneja buffer. | Si Google limpia caché (reinicio, límite), perdemos datos no entregados. Riesgo bajo: sesiones rara vez > 2h. | S1 §3.4 |
| **NO `:shared` JAR** | Sí | Reduce footprint APK y memoria RAM. Reloj no necesita DTOs complejos. | Móvil debe confiar en primitivos enviados. Mitigado con validación en móvil. | S1 §2.3 |
| **DataClient (no MessageClient para FC)** | Sí | DataClient usa sync automático y buffer. Garantiza entrega eventual. | Latencia ligeramente mayor (batching), pero confiable. | S1 §2.1 |
| **Compose for Wear OS** | Sí | Estándar declarativo moderno. Manejo de estados reactivos con StateFlow. | Curva de aprendizaje. Componentes diferentes a móvil. | — |
| **Kotlin 2.0.21** | Sí | Built-in en AGP 9.0. Un solo lenguaje en ecosistema. Corrutinas nativas para callbacks de Health Services. | Plan anterior decía 2.1.0, pero Android Studio genera 2.0.21 y es compatible. | — |
| **UI minimalista** | Sí | MVP solo requiere FC. Pantalla limpia = mejor legibilidad en movimiento. | Usuario no ve calorías ni distancia en reloj. Eso vive en móvil/backend. | S1 §4.3 |
| **Sin autenticación propia** | Sí | Reloj hereda sesión del móvil. No login en reloj. | Si móvil nunca conectado, reloj no envía datos identificados. Mitigado con emparejamiento obligatorio. | S1 §2.4 |
| **Path /ace/health/heart_rate** | Sí | Coherente con DataLayerPaths en :shared. | Si se usara /ace/heart_rate (sin health/), móvil no recibiría. | S1 §2.2 |
| **Timestamp epoch millis** | Long | Coherente con :shared. No segundos, no ISO-8601. | Móvil debe convertir time_interval de Health Services a epoch millis. | S1 §2.3 |
| **Sensor FC 1 Hz** | Sí | Frecuencia estándar en contexto de ejercicio. | No aumentar a 5 Hz (HIIT) salvo requerido. Más frecuencia = más batería. | S1 §3.2 |
| **NO Service explícito** | Sí | Health Services API maneja su propio foreground state. | Si API cambia, podría requerir Service en futuro. | S1 §3.3 |
| **Hilt 2.59.2** | Sí | Compatible con AGP 9.0. Reduce boilerplate de DI. Consistente con módulo :mobile. | Opiniado, puede pelear contra framework. Versión <2.59 rompe con AGP 9.0. | — |
| **legacy-kapt** | Sí | Único mecanismo de annotation processing compatible con AGP 9.0 built-in Kotlin. | Deprecado en favor de KSP, pero KSP con Hilt 2.59 + AGP 9.0 tiene incompatibilidades documentadas. | — |

---

## 7. Contratos con Mobile (Coherentes con Apéndices)

### 7.1 DataClient Paths y Formatos (S1 §2.2, §2.3)

| Path | Dirección | Formato | Semántica |
|------|-----------|---------|-----------|
| `/ace/health/heart_rate` | Wear → Mobile | `{"bpm": int, "timestamp": long}` | Muestra individual de FC. Epoch millis. |
| `/ace/session/{sessionId}/status` | Wear → Mobile | `{"status": "STOPPED"}` | Sesión detenida desde reloj. |
| `/ace/command` | Mobile → Wear | `{"command": "STOP"}` | Comando remoto desde móvil. |
| `/ace/initial_sync` | Mobile → Wear | `{"userId": UUID, "deviceId": String, "sessionId": UUID}` | Sincronización inicial de IDs. |

**Invariantes:**
- Cada muestra en `/ace/health/heart_rate` tiene URI única (path + timestamp) para evitar sobrescritura (S1 §2.2).
- El reloj **nunca** inicia transmisión por iniciativa propia. Solo tras recibir START del móvil (S1 §2.4).
- El móvil **nunca** solicita datos al reloj. Modelo puramente push (S1 §2.4).

### 7.2 Comandos MessageClient (S1 §2.1)

| Comando | Origen | Destino | Acción | Fiabilidad |
|---------|--------|---------|--------|------------|
| `START` | Mobile | Wear | Iniciar sesión de ejercicio | Fire-and-forget (no garantizado) |
| `STOP` | Mobile | Wear | Detener sesión de ejercicio | Fire-and-forget (no garantizado) |
| `MILESTONE_10MIN` | Mobile | Wear | Vibrar en logro (futuro) | Fire-and-forget (no garantizado) |

**Nota:** MessageClient es **no persistente**. Si el destinatario no está conectado, el mensaje se pierde. Esto es aceptable para comandos de control porque el usuario tiene alternativa (UI del móvil o reloj) (S1 §2.1).

---

## 8. Roadmap: Fase Mínima (Coherente con Apéndices S1-S2)

### Fase Mínima (Semanas 1-4) — S1 y S2 completos

- [ ] Esqueleto Android Studio: módulo `:wear`, AGP 9.0.1, Compose for Wear OS, Hilt 2.59.2, legacy-kapt
- [ ] **S1 Sensor:** `HealthServicesManager`: suscripción a FC, manejo de permisos `BODY_SENSORS`
- [ ] **S1 Sensor:** `WearDataClient`: envío de FC por DataClient en path `/ace/health/heart_rate`
- [ ] **S1 Sensor:** Formato: `{"bpm": int, "timestamp": long}` (epoch millis)
- [ ] **S1 Sensor:** `WearMessageClient`: listener para comandos del móvil (`START`, `STOP`)
- [ ] **S2 Session:** `StartExerciseUseCase`: activa sensor, registra callback
- [ ] **S2 Session:** `StopExerciseUseCase`: detiene sensor, envía STOP al móvil
- [ ] **S2 Session:** UI de sesión: FC grande (48sp), timer, chip de conexión, botón Detener
- [ ] **S1 Sync:** Sincronización inicial: recibir `userId`, `deviceId`, `sessionId` del móvil
- [ ] **S1 Sync:** Manejo de desconexión: chip amarillo cuando no hay móvil al alcance
- [ ] **S1 Sync:** NO persiste localmente (sin Room/SQLite en reloj)
- [ ] **S1 Sync:** NO consume `:shared` JAR
- [ ] Pruebas de batería: 1 hora de sesión no debe consumir > 15% de batería del reloj

### Fase de Transición (Semanas 5-8)
- [ ] Añadir pasos (Steps) vía Health Services: `ExerciseType.RUNNING` activa podómetro
- [ ] Mostrar zona de FC en UI (color cambia según umbral: gris < 80, verde 80-120, naranja 120-150, rojo > 150)
- [ ] Vibración en logros: móvil envía comando "MILESTONE_10MIN" y reloj vibra
- [ ] Pantalla de resumen post-sesión: duración, FC promedio (calculado por móvil y enviado al reloj)

### Fase Máxima (Semanas 9-12+)
- [ ] GPS en reloj (si hardware lo permite): coordenadas como primitivos adicionales
- [ ] Detección de deporte automático: reloj infiere Running vs Cycling vía acelerómetro
- [ ] Modo standalone: si reloj tiene LTE, enviar bloques directamente al backend (requiere `:shared` y JWT en reloj — complejidad alta)
- [ ] Tiles: widget de reloj que muestra racha actual y botón rápido "Iniciar sesión"
- [ ] Complications: mostrar FC en watch face durante sesión activa

---

## 9. Checklist de Integración con Mobile (Coherente con Apéndices)

- [ ] Reloj envía FC cada 1-2 segundos y móvil los recibe sin pérdida (Bluetooth < 5m)
- [ ] Al desconectar Bluetooth durante 10 min y reconectar, móvil recibe ráfaga de FC atrasados y reconstruye bloques correctamente
- [ ] Botón "Detener" en reloj notifica al móvil inmediatamente (latencia < 2s)
- [ ] Reloj no solicita autenticación al usuario; hereda identidad del móvil
- [ ] 1 hora de sesión activa consume < 15% de batería del reloj (prueba en hardware real, no emulador)
- [ ] App del reloj compila con `minSdk 30` (Wear OS 3) y no usa APIs deprecated de Wearable Support Library
- [ ] Path correcto: reloj escribe en `/ace/health/heart_rate`, móvil lee del mismo path
- [ ] Formato correcto: `{"bpm": int, "timestamp": long}` (epoch millis, no segundos, no ISO-8601)
- [ ] Comando STOP remoto desde móvil funciona: MessageClient `/ace/command` con payload "STOP"
- [ ] Sincronización inicial: móvil envía `userId`, `deviceId`, `sessionId` y reloj los recibe
- [ ] Sin Room/SQLite en reloj: verificar que no hay dependencia de androidx.room en build.gradle.kts
- [ ] NO consume `:shared`: verificar que no hay `implementation(project(":shared"))` en build.gradle.kts
- [ ] **AGP 9.0 + Hilt 2.59.2 + legacy-kapt:** verificar que build es successful y no hay error `BaseExtension not found`
- [ ] **Built-in Kotlin:** verificar que no se usa `org.jetbrains.kotlin.android` ni `org.jetbrains.kotlin.kapt`

---

## 10. Nota Crítica sobre Batería y Performance (S1 §9)

El reloj es el dispositivo más restrictivo del ecosistema:

- **CPU:** SoC de bajo consumo (Snapdragon Wear 3100/4100). Un `for` loop innecesario causa lag en UI.
- **Pantalla siempre encendida (AOD):** Si sesión dura 1h, pantalla debe permanecer activa. Health Services API gestiona esto, pero declarar `WAKE_LOCK` en manifest.
- **Sensor de FC:** 1 Hz estándar. No aumentar a 5 Hz salvo HIIT. Más frecuencia = más batería.
- **Transmisión:** DataClient batchifica automáticamente. No forzar sync manual con `putDataItem` urgente a menos que sea comando STOP.

**Regla de oro:** *Si algo puede hacerlo el móvil o el backend, no lo hagas en el reloj.* (S1 §9)

---

*Documento coherente con Apéndices S1-S10 y Arquitectura A.C.E v0.2. Cualquier divergencia debe ser reportada como bug de coherencia.*
