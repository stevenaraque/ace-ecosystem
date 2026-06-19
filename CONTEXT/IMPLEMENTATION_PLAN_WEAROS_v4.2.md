# A.C.E — Implementation Plan: Wear OS (`ace-wear`)

> **Estado:** ✅ En desarrollo activo (Steven)  
> **Versión:** 4.4 (Arquitectura Clean, `:shared` híbrido: se consumen enums pero NO data classes)  
> **Fecha:** 2026-06-19  
> **Stack:** Wear OS 3+ (API 30+) · Kotlin 2.2.21 · Gradle 8.10+ (Kotlin DSL) · AGP 9.0.1 · Health Services Client 1.0.0 · Wear OS Data Layer API · Jetpack Compose for Wear OS 1.6.2 · Hilt 2.59.2  
> **`:shared` (Híbrido):** Se consume para enums pero se mantienen mapeos manuales o no se usa DTOs complejos para evitar inflar el APK.  
> **Responsable:** Steven Araque (Mobile/Wear Lead)

---

## Historial de cambios

| Versión | Aspecto | Cambio | Justificación |
|---------|---------|--------|---------------|
| v4.1 → v4.2 | Estructura `data/`, DI modules, nombres de wrappers/usecases, UI state, repository, componentes, foreground service | Ver detalle en la versión anterior del doc | Más semántico, separación de responsabilidades (health vs. sync). |
| **v4.2 → v4.3** | **`:shared` en wear** | ❌ **Revertido: wear NO consume `:shared`.** Se eliminó la dependencia JitPack (`ace-shared:1.0.3`) y el `import com.ace.shared.*`. Los paths se definen como strings literales locales (`WearDataLayerPaths` en `WearDataClient.kt`). | **El consumo de `:shared` en v4.2 fue un error.** Contradice la arquitectura oficial (Arquitectura v0.3 §1, §16: *"Wear OS: NO `:shared`, NO Room"*) y el principio de "el reloj es tonto por diseño". Los paths se mantienen alineados manualmente con `DataLayerPaths` de `:shared`. |
| **v4.3 → v4.4** | **Arquitectura Clean y `:shared` Híbrido** | Implementación de Arquitectura Clean y uso selectivo (híbrido) de `:shared`. | Mejora mantenibilidad; consumo parcial evita peso innecesario. |

> ⚠️ **Cambio de decisión v4.3 → v4.4:** Se adopta Arquitectura Clean estructurada y consumo "híbrido" de `:shared` (solo constantes/enums, sin DTOs).

---

## 1. Visión y Alcance

El módulo `:wear` es el **sensor y transmisor** del ecosistema. Su única misión es:

1. **Capturar** frecuencia cardíaca (FC) en tiempo real vía Health Services API (S1).
2. **Transmitir** datos al móvil inmediatamente vía Wear OS Data Layer API (`DataClient`) (S1).
3. **Mostrar** UI ultra-minimalista: FC grande, estado de conexión, botón Detener (S1, S2).
4. **Escuchar** comandos START/STOP del móvil vía `MessageClient` (S1, S2).
5. **NO calcular** XP, NO agrupar bloques, NO persistir localmente, NO autenticarse (S1 §3.4).

**Principio rector:** *El reloj es un sensor con pantalla. Toda la inteligencia vive en el móvil o en la nube.* (Arquitectura §1)

**Regla de oro:** Si algo puede hacerlo el móvil o el backend, **no lo hagas en el reloj** (S1 §9).

**Nota sobre `:shared`:** El módulo `:wear` usa un enfoque **HÍBRIDO** para consumir `:shared`. Consume enums y constantes simples para evitar desync, pero **NO consume ni serializa DTOs complejos**, manteniendo el APK ligero.

---

## 2. Dependencias Gradle (`build.gradle.kts`)

> **⚠️ NO MODIFICAR.** Ya configurado en el proyecto. Documentado aquí para referencia.

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.legacy.kapt)        // com.android.legacy-kapt
    alias(libs.plugins.hilt.android)
}

android {
    namespace = "sena.adso.ace_wear"
    compileSdk {
        version = release(36) { minorApiLevel = 1 }
    }
    defaultConfig {
        applicationId = "sena.adso.ace_wear"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures { compose = true }
}

dependencies {
    // ❌ NO hay dependencia a :shared (decisión v4.3). Paths como strings literales.
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.core.ktx)
    implementation(libs.wear)
    implementation(libs.health.services.client)               // Health Services API
    implementation(libs.play.services.wearable)               // DataClient + MessageClient
    implementation(platform(libs.compose.bom))
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    implementation(libs.wear.compose.foundation)
    implementation(libs.wear.compose.material)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.lifecycle.service)
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)                                  // legacy-kapt
    implementation(libs.wear.tooling.preview)
    implementation(libs.activity.compose)
    implementation(libs.core.splashscreen)
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}
```

### Notas críticas sobre AGP 9.0 + Built-in Kotlin (v4.3)

| Aspecto | Configuración actual | Justificación |
|---------|----------------------|---------------|
| **`:shared` JAR** | ❌ **No consume** (v4.3) | Wear NO depende de `:shared`. Paths del Data Layer como strings literales locales (`WearDataLayerPaths`). El repo JitPack puede permanecer en `settings.gradle.kts` sin usar. |
| **Plugin Kotlin Android** | Eliminado (built-in AGP 9.0) | No se declara explícitamente. |
| **Plugin kapt** | `com.android.legacy-kapt` | `org.jetbrains.kotlin.kapt` incompatible con built-in Kotlin. |
| `kotlinOptions { jvmTarget }` | Eliminado | `compileOptions` Java 21 es suficiente; AGP 9.0 infiere automáticamente. |
| `composeOptions { kotlinCompilerExtensionVersion }` | Eliminado | Plugin `kotlin-compose` gestiona compiler automáticamente. |
| Hilt | 2.59.2 | Compatible con AGP 9.0. Versiones <2.59.2 fallan con `BaseExtension not found`. |
| Kotlin | 2.0.21 (built-in AGP 9.0) | Generado por Android Studio. |

---

## 3. Estructura de Carpetas (v4.2 — REAL)

```
ace-wear/
├── build.gradle.kts                      # NO MODIFICAR — ya configurado
├── settings.gradle.kts
├── gradle/
│   ├── libs.versions.toml                # NO MODIFICAR — ya configurado
│   └── wrapper/
│
├── app/
│   ├── build.gradle.kts                  # NO MODIFICAR — ya configurado
│   ├── lint.xml
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── kotlin/com/ace/wear/
│       │   ├── WearApplication.kt              # @HiltAndroidApp
│       │   │
│       │   ├── di/
│       │   │   ├── HealthServicesModule.kt     # Provee HealthServicesClient
│       │   │   └── WearDataLayerModule.kt        # Provee DataClient, MessageClient
│       │   │
│       │   ├── data/
│       │   │   ├── health/
│       │   │   │   ├── HealthServicesManager.kt # Wrapper de MeasureClient (S1)
│       │   │   │   └── model/
│       │   │   │       └── HeartRateSample.kt   # Modelo de muestra FC
│       │   │   │
│       │   │   ├── sync/
│       │   │   │   ├── WearDataClient.kt        # Wrapper de DataClient (S1)
│       │   │   │   └── WearMessageClient.kt     # Listener de MessageClient (S1)
│       │   │   │
│       │   │   └── repository/
│       │   │       └── WearHealthRepository.kt    # Orquesta health + sync
│       │   │
│       │   ├── domain/
│       │   │   └── usecase/
│       │   │       ├── StartExerciseUseCase.kt   # Inicia sesión: registra callback, activa sensor
│       │   │       └── StopExerciseUseCase.kt    # Detiene sesión: cancela callback
│       │   │
│       │   └── presentation/
│       │       ├── WearSessionState.kt           # Sealed class: Idle, Active, Error
│       │       │
│       │       ├── components/
│       │       │   ├── ConnectionStatusChip.kt   # Indicador de conexión con móvil
│       │       │   ├── HeartRateDisplay.kt       # FC grande en pantalla
│       │       │   ├── StopButton.kt             # Botón DETENER en UI
│       │       │   └── TimerDisplay.kt           # Cronómetro de sesión
│       │       │
│       │       ├── session/
│       │       │   ├── SessionScreen.kt          # Pantalla principal de sesión
│       │       │   └── SessionViewModel.kt     # Expone estado, reacciona a comandos
│       │       │
│       │       └── theme/
│       │           └── WearTheme.kt              # Theme Compose para Wear OS
│       │
│       └── res/
│           ├── drawable/
│           ├── mipmap-*/
│           └── values/
│               ├── strings.xml
│               └── styles.xml
```

---

## 4. Flujo de Datos (S1 — Captura de Sensor)

```
┌─────────────────────────────────────────────────────────────────────┐
│                         MÓVIL (APK)                                │
│  Envia START vía MessageClient → path: /ace/session/{id}/status   │
└─────────────────────────────────────────────────────────────────────┘
                              │
                              │ Bluetooth / WiFi (gestionado por SO)
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│                         WEAR OS (Reloj)                              │
│                                                                      │
│  ┌──────────────────┐    ┌──────────────────┐    ┌──────────────┐  │
│  │ WearMessageClient│───►│ StartExerciseUse │───►│ HealthServices│  │
│  │ recibe START     │    │ Case             │    │ Manager       │  │
│  │                  │    │                  │    │ (MeasureClient) │  │
│  └──────────────────┘    └──────────────────┘    └──────┬───────┘  │
│                                                         │          │
│                              ┌──────────────────────────┘          │
│                              ▼                                     │
│  ┌──────────────────┐    ┌──────────────────┐    ┌──────────────┐  │
│  │ WearDataClient   │◄───│ HeartRateSample  │◄───│ Callback     │  │
│  │ (DataClient)     │    │ (bpm, timestamp) │    │ (onDataPoint)│  │
│  │ path: /ace/health│    │                  │    │              │  │
│  │  /heart_rate     │    │                  │    │              │  │
│  └──────────────────┘    └──────────────────┘    └──────────────┘  │
│                              │                                     │
│                              ▼                                     │
│  ┌──────────────────┐                                            │
│  │ UI: HeartRateDisplay│  Muestra FC en vivo                      │
│  │ TimerDisplay       │  Muestra tiempo transcurrido               │
│  │ StopButton         │  Envia STOP al móvil                       │
│  └──────────────────┘                                            │
│                                                                     │
│  ┌──────────────────┐                                            │
│  │ WearMessageClient│◄─── STOP desde móvil o desde botón        │
│  │                  │    → StopExerciseUseCase                     │
│  │                  │    → cancela MeasureClient                   │
│  └──────────────────┘                                            │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 5. Contratos con Mobile (S1 §2.2, §2.3)

| Path | Dirección | Formato | Semántica | Origen en `:shared` |
|------|-----------|---------|-----------|---------------------|
| `/ace/health/heart_rate` | Wear → Mobile | `{"bpm": double, "timestamp": long}` | Muestra individual de FC. Epoch millis. | `WearDataLayerPaths.HEART_RATE` (local) |
| `/ace/session/{sessionId}/status` | Mobile → Wear | `"START"` / `"STOP"` | Comando de control de sesión. | `WearDataLayerPaths.SESSION_STATUS` (local) |
| `/ace/session/{sessionId}/status` | Wear → Mobile | `"STOPPED"` | Sesión detenida desde reloj. | `WearDataLayerPaths.SESSION_STATUS` (local) |

**Nota:** Al ser un consumo híbrido, se prioriza usar literales y tipos básicos (Double, Long) en el transporte hacia Mobile, dejando la serialización pesada para el Mobile.

---

## 6. Estados del Sistema (WearSessionState)

```kotlin
sealed class WearSessionState {
    data object Idle : WearSessionState()           // Esperando START
    data class Active(
        val bpm: Double? = null,                     // Última FC recibida
        val elapsedSeconds: Long = 0,                // Tiempo transcurrido
        val isConnected: Boolean = true              // Estado de conexión con móvil
    ) : WearSessionState()
    data class Error(val message: String) : WearSessionState()
}
```

---

## 7. Roadmap: Fase Mínima (Semana 1 — HU-06)

### HU-06: Captura de frecuencia cardíaca en el reloj

- [ ] **S1.1** `WearMessageClient.kt` — Escucha START/STOP del móvil vía MessageClient
- [ ] **S1.2** `HealthServicesManager.kt` — Registra/cancela callback de MeasureClient para HEART_RATE_BPM
- [ ] **S1.3** `HeartRateSample.kt` — Modelo con `bpm` (Double) y `timestamp` (Long, epoch millis)
- [ ] **S1.4** `WearDataClient.kt` — Escribe muestras en `/ace/health/heart_rate` con timestamp en URI
- [ ] **S1.5** `WearHealthRepository.kt` — Orquesta: recibe comando → activa sensor → envía datos
- [ ] **S1.6** `StartExerciseUseCase.kt` — Expone: iniciar monitoreo de FC
- [ ] **S1.7** `StopExerciseUseCase.kt` — Expone: detener monitoreo de FC
- [ ] **S1.8** `SessionViewModel.kt` — Expone `WearSessionState`, reacciona a comandos del móvil
- [ ] **S1.9** `SessionScreen.kt` + componentes — UI: FC grande, timer, botón DETENER, chip de conexión
- [ ] **S1.10** `WearApplication.kt` + DI modules — Hilt provee dependencias
- [ ] **S1.11** Verificar que **NO hay** dependencia a `:shared` en build (paths son strings literales locales)
- [ ] **S1.12** Verificar que `MeasureClient` funciona en emulador Wear OS con Health Services

### Criterios de aceptación de HU-06

- [ ] Wear OS registra callback de `MeasureClient` para `HEART_RATE_BPM` al recibir `START`.
- [ ] Cada muestra conserva `value` y `time_interval` del tipo nativo.
- [ ] `DataClient` escribe en `/ace/health/heart_rate`.
- [ ] Al recibir `STOP`, se cancela el callback.
- [ ] El reloj no persiste muestras localmente.
- [ ] La URI incluye timestamp para no sobrescribir nodos.

---

## 8. Decisiones Técnicas y Trade-offs (v4.3)

| Decisión | Valor | Justificación | Trade-off | Apéndice |
|----------|-------|---------------|-----------|----------|
| **`:shared` en wear** | **Híbrido** (v4.4) | Alineado con Clean Architecture, usa constantes sin DTOs. | Riesgo menor de desync, APK moderadamente ligero. | Arq. §16 |
| **Paths Data Layer** | Híbrido | Algunos literales, algunas constantes. | Balance entre seguridad de tipos y tamaño. | S1 §2.2 |
| **DI Modules separados** | `HealthServicesModule` + `WearDataLayerModule` | Separación de responsabilidades. Health y Sync son dominios distintos. | Más archivos, pero más limpio. | — |
| **Callback interno** | Dentro de `HealthServicesManager` | No expone implementación detallada. Manager encapsula todo el ciclo de vida. | Menos granularidad, pero más cohesión. | S1 §3.3 |
| **No Room en wear** | Sin SQLite/Room | Reduce APK y RAM. SO bufferiza vía DataClient si hay desconexión. | Límite no documentado de caché Google (~100-200 items, 24h). | S1 §3.4 |
| **Timestamps epoch millis** | `Long` | Coherente con `:shared`. No ISO-8601. | Menos legible en debug (usar `Instant.toString()`). | S1 §2.3 |
| **No foreground service** | No necesario en MVP | `MeasureClient` funciona sin foreground service cuando app es visible. | Si app va a background, el SO puede pausar. Aceptable en MVP. | S8 §2.1 |
| **Wear NO calcula XP** | Sin lógica de gamificación | Toda la inteligencia en móvil/backend. | Reloj es "tonto" por diseño. | S1 §3.4 |
| **Wear NO agrupa bloques** | Muestras individuales | El móvil agrupa en bloques de ~300s. | Mayor tráfico Bluetooth, pero más simple en wear. | S1 §3.4 |

---

## 9. Checklist de Integración con Mobile

- [ ] Móvil envía `START` vía `MessageClient` en path `/ace/session/{id}/status`
- [ ] Wear recibe `START` y activa `MeasureClient` para `HEART_RATE_BPM`
- [ ] Wear envía muestras vía `DataClient` en path `/ace/health/heart_rate`
- [ ] Móvil recibe muestras y las almacena en buffer circular RAM
- [ ] Móvil envía `STOP` → Wear cancela callback
- [ ] Wear envía `STOPPED` desde botón DETENER → Móvil cierra sesión

---

## 10. Nota sobre Emulador vs. Dispositivo Físico

| Aspecto | Emulador | Dispositivo físico |
|---------|----------|-------------------|
| Health Services | Simulado vía `HealthServicesManager.isAvailable()` | Hardware real |
| FC | Valores simulados (configurables en emulador) | Sensor PPG real |
| Data Layer | Emulador + móvil emulador (pareados) | Bluetooth real |
| Prueba | Suficiente para lógica de transmisión | Necesario para validar precisión de sensor |

**Para probar en emulador:**
1. Crear AVD Wear OS 3+ (API 30+) con Google Play.
2. Parear con AVD móvil (Phone) via Wear OS companion app.
3. En emulador de wear: Extended Controls → Sensors → Heart Rate → set values.

---

*Documento coherente con Apéndices S1-S10 y Arquitectura A.C.E v0.3.  
Correcciones v4.1→v4.2 validadas contra estructura real del proyecto de Steven.*
