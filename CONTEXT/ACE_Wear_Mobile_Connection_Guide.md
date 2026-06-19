# A.C.E — Guía de Integración: Conexión Wear OS ↔ Mobile

> **Fecha:** 2026-06-19  
> **Autor:** Equipo A.C.E (Steven Araque — Mobile/Wear Lead)  
> **Versión:** v1.0 — Post-integración funcional  
> **Rama:** `main` (`d536c4b`)

---

## 1. Resumen Ejecutivo

Este documento describe la integración completa del ecosistema A.C.E entre los tres nodos:

| Nodo | Tecnología | Rol en la conexión |
|------|-----------|-------------------|
| **Wear OS** (Reloj) | Kotlin 2.0.21, AGP 9.0, Wear OS 3+ | Captura FC vía Health Services API (~1 Hz), envía al móvil vía DataClient, recibe comandos START/STOP vía MessageClient |
| **APK Móvil** | Kotlin 2.1.20, AGP 9.0, Android 13+ | Orquestador: inicia sesiones, recibe FC, calcula XP, sincroniza offline-first |
| **Backend** | Spring Boot 4.0.6, Kotlin 2.2.21 | Validador, fuente de verdad final |

**Principio rector:** *"El reloj captura, el móvil calcula y transporta, el backend valida y decide."*

---

## 2. Problema Central Resuelto

### Contexto
El equipo no contaba con reloj físico, por lo que se utilizaron **dos emuladores** (teléfono + reloj) emparejados vía Google Pixel Watch. La comunicación real entre APK móvil y app Wear OS **no funcionaba inicialmente** debido a que el Data Layer de Google Play Services requiere condiciones específicas que no se cumplían.

### Síntomas Iniciales
- El reloj enviaba FC pero el móvil no las recibía
- El móvil enviaba START pero el reloj no iniciaba sesión
- El reloj enviaba STOPPED pero el móvil no procesaba la detención

---

## 3. Soluciones Aplicadas (Paso a Paso)

### 3.1 Unificación de `applicationId`

**Problema:** Los módulos mobile y wear tenían `applicationId` diferentes, impidiendo la comunicación via Data Layer.

| Antes | Después |
|-------|---------|
| `sena.adso.ace_wear` / `sena.adso.ace_mobile` | `sena.adso.ace` en **ambos** |

**Archivos modificados:**
- `ace-mobile/app/build.gradle.kts`
- `ace-wear/app/build.gradle.kts`

---

### 3.2 Manifest y Permisos

**Problema:** Faltaba permiso `WEARABLE_BIND_LISTENER` y `standalone=true` en wear bloqueaba la comunicación.

**Cambios en `ace-wear/app/src/main/AndroidManifest.xml`:**
```xml
<uses-permission android:name="com.google.android.gms.permission.WEARABLE_BIND_LISTENER" />

<application
    android:standalone="false">  <!-- Cambiado de true a false -->
```

**Cambios en `ace-mobile/app/src/main/AndroidManifest.xml`:**
```xml
<uses-permission android:name="com.google.android.gms.permission.WEARABLE_BIND_LISTENER" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_HEALTH" />
```

---

### 3.3 Inicialización del WearMessageClient (Reloj)

**Problema:** El reloj no inicializaba el listener de mensajes al arrancar.

**Solución:** Agregar en `WearApplication.onCreate()`:

```kotlin
override fun onCreate() {
    super.onCreate()
    val wearMessageClient = WearMessageClient.getInstance(this)
    wearMessageClient.startListening()
}
```

**Archivo:** `ace-wear/app/src/main/kotlin/com/ace/wear/WearApplication.kt`

---

### 3.4 ExerciseSyncService como Hybrid Service (Mobile)

**Problema:** El servicio no podía comunicar datos en vivo al ViewModel ni sobrevivir a rotaciones de pantalla.

**Arquitectura final — Hybrid Service (Started + Bound):**

```
┌─────────────────────────────────────────────────────────────┐
│                    ExerciseSyncService                        │
│  ┌──────────────┐  ┌──────────────────────────────────────┐ │
│  │   STARTED    │  │              BOUND                    │ │
│  │              │  │                                      │ │
│  │ startForeground│  │ LocalBinder → getService()          │ │
│  │              │  │                                      │ │
│  │ Sobrevive en │  │ StateFlow expuestos:                 │ │
│  │ background   │  │   - heartRate: StateFlow<Double>     │ │
│  │              │  │   - elapsedSeconds: StateFlow<Int>   │ │
│  │ Recibe FC    │  │   - blockCount: StateFlow<Int>       │ │
│  │ Recibe STOPPED│  │   - isConnected: StateFlow<Boolean> │ │
│  │              │  │   - isSessionActive: StateFlow<Bool> │ │
│  │              │  │                                      │ │
│  │              │  │ SharedFlow de eventos:               │ │
│  │              │  │   - sessionStopped: SharedFlow<>     │ │
│  └──────────────┘  └──────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

**Separación de Scopes:**

| Scope | Vida | Propósito |
|-------|------|-----------|
| `serviceScope` | Todo el ciclo de vida del servicio | Emite eventos, mantiene binding |
| `sessionScope` | Solo durante sesión activa | Timer, detector de desconexión, recepción de FC |

**Archivo:** `ace-mobile/app/src/main/kotlin/com/ace/mobile/service/ExerciseSyncService.kt`

---

### 3.5 SessionViewModel con Service Binding

**Problema:** El ViewModel no podía leer datos en vivo del servicio ni enterarse cuando el reloj detenía la sesión.

**Solución:** `ServiceConnection` + observación de `StateFlow`/`SharedFlow`:

```kotlin
private val serviceConnection = object : ServiceConnection {
    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        val binder = service as ExerciseSyncService.LocalBinder
        exerciseSyncService = binder.getService()
        observeServiceStateFlows()  // Suscribe a heartRate, elapsedSeconds, etc.
    }
    // ...
}
```

**Flujo de detención (fuente de verdad = móvil):**

```
Usuario presiona STOP en mobile
    │
    ├──► SessionViewModel.stopSession()
    │       ├──► sendStopCommandUseCase(sessionId) → envía STOP al reloj
    │       ├──► _uiState = Stopping(session)
    │       └──► Espera SessionStoppedEvent del servicio (timeout 15s)
    │
    ├──► Reloj recibe STOP → detiene sensor → envía STOPPED al mobile
    │
    └──► ExerciseSyncService recibe STOPPED
            ├──► stopSessionInternal(USER_STOPPED_WATCH)
            ├──► Procesa bloque final
            ├──► Emite SessionStoppedEvent
            └──► Servicio se detiene (foreground off)

    ◄──► SessionViewModel recibe SessionStoppedEvent
            ├──► finalizeStopSession()
            ├──► stopSessionUseCase(sessionId) → persiste en SQLite
            └──► _uiState = Completed(session)
```

**Archivo:** `ace-mobile/app/src/main/kotlin/com/ace/mobile/presentation/exercise/SessionViewModel.kt`

---

### 3.6 UI en Tiempo Real (SessionScreen)

**Datos mostrados en vivo:**

| Dato | Fuente | Frecuencia |
|------|--------|------------|
| FC (BPM) | `WearDataSource` → `ExerciseSyncService` → `SessionViewModel.heartRate` | ~1 Hz |
| Timer (MM:SS) | `ExerciseSyncService.elapsedSeconds` | 1 segundo |
| Bloques | `ExerciseSyncService.blockCount` | Al cerrar bloque (~30s) |
| Conexión | Detector de timeout >5s sin muestras | 1 segundo |

**Estados de la UI:**

```
Idle → Loading → Active → Stopping → Completed
                │           │
                │           └── Spinner: "Waiting for watch confirmation"
                └── Muestra tarjetas: FC | Timer | Blocks + indicador de conexión
```

**Archivo:** `ace-mobile/app/src/main/kotlin/com/ace/mobile/presentation/exercise/SessionScreen.kt`

---

### 3.7 WearDataSource con Logs de Diagnóstico

**Problema:** No se sabía si el móvil recibía los DataItems del reloj.

**Solución:** Logs verbose en cada etapa:

```kotlin
Log.i("WearDataSource", "=== LISTENER REGISTRADO EN DATACLIENT ===")
Log.d("WearDataSource", "onDataChanged: ${dataEvents.count} eventos recibidos")
Log.i("WearDataSource", "FC RECIBIDA DEL RELOJ: bpm=$bpm, timestamp=$timestamp")
```

**Archivo:** `ace-mobile/app/src/main/kotlin/com/ace/mobile/data/wear/WearDataSource.kt`

---

### 3.8 Comandos START/STOP (Reloj)

**Problema:** El reloj no procesaba comandos ni enviaba confirmaciones.

**Flujo de comandos:**

```
Mobile envía START  ──►  WearMessageClient recibe  ──►  WearHealthRepository.startSession()
                                                          └── HealthServicesManager.iniciarMonitoreoFC()

Mobile envía STOP   ──►  WearMessageClient recibe  ──►  WearHealthRepository.stopSession()
                                                          ├── HealthServicesManager.detenerMonitoreoFC()
                                                          └── WearMessageClient.enviarSTOPPED()
```

**Archivos:**
- `ace-wear/app/src/main/kotlin/com/ace/wear/data/repository/WearHealthRepository.kt`
- `ace-wear/app/src/main/kotlin/com/ace/wear/presentation/session/SessionViewModel.kt`

---

## 4. Arquitectura de Comunicación Final

### 4.1 Diagrama de Secuencia (Sesión Completa)

```
Mobile App          ExerciseSyncService      Wear OS (Reloj)
    │                      │                        │
    │── startSession() ───►│                        │
    │                      │── startForeground()    │
    │                      │── bindService()        │
    │                      │                        │
    │                      │── sendStartCommand() ─►│
    │                      │                        │── startSession()
    │                      │                        │── HealthServices.start()
    │                      │                        │
    │                      │◄── DataClient.putDataItem() ── FC cada ~1s
    │                      │                        │
    │                      │── onDataChanged()      │
    │                      │── buffer.add(sample)   │
    │                      │                        │
    │◄─ heartRate.value ───│                        │
    │◄─ elapsedSeconds ────│                        │
    │◄─ blockCount ────────│                        │
    │                      │                        │
    │                      │── checkBlockClosure()  │
    │                      │   (cada ~30s)          │
    │                      │                        │
    │── stopSession() ───►│                        │
    │                      │── sendStopCommand() ───►│
    │                      │                        │── stopSession()
    │                      │                        │── HealthServices.stop()
    │                      │◄── STOPPED (Message) ──│
    │                      │                        │
    │                      │── stopSessionInternal()│
    │                      │── emit(SessionStopped) │
    │                      │                        │
    │◄─ SessionStopped ────│                        │
    │── stopSessionUseCase()                       │
    │   (persiste SQLite)   │                        │
    │                      │                        │
```

### 4.2 Paths del Data Layer

| Dirección | Path | Tipo | Contenido |
|-----------|------|------|-----------|
| Mobile → Wear | `/ace/session/{sessionId}/status` | MessageClient | `command=START/STOP` |
| Wear → Mobile | `/ace/session/{sessionId}/status` | MessageClient | `command=STOPPED` |
| Wear → Mobile | `/ace/health/heart_rate/{timestamp}` | DataClient | `bpm=Double, timestamp=Long` |

---

## 5. Archivos Modificados/Creados

### Módulo Mobile (`ace-mobile/`)

| Archivo | Cambio |
|---------|--------|
| `app/build.gradle.kts` | `applicationId = "sena.adso.ace"` |
| `app/src/main/AndroidManifest.xml` | Permisos, MainActivity como launcher, ExerciseSyncService |
| `app/src/main/kotlin/com/ace/mobile/MobileApplication.kt` | NotificationChannels (session_active, streak_reminder, sync_status) |
| `data/wear/WearDataSource.kt` | Logs de diagnóstico, DataClient listener |
| `presentation/exercise/SessionViewModel.kt` | ServiceConnection, bindService, observación de StateFlow |
| `presentation/exercise/SessionScreen.kt` | UI con datos en vivo, estado Stopping |
| `presentation/exercise/SessionUiState.kt` | Agregado `Stopping` |
| `presentation/profile/ProfileScreen.kt` | Botón "Start Exercise" navega a sesión |
| `service/ExerciseSyncService.kt` | Hybrid service, StateFlow, SharedFlow, buffer circular |
| `service/WearDataListenerService.kt` | Creado (comentado en manifest, no se usa en MVP) |
| `presentation/diagnostic/*` | Creados para debug (WearDiagnosticActivity, Screen, ViewModel) |

### Módulo Wear (`ace-wear/`)

| Archivo | Cambio |
|---------|--------|
| `app/build.gradle.kts` | `applicationId = "sena.adso.ace"` |
| `app/src/main/AndroidManifest.xml` | `standalone=false`, permisos |
| `app/src/main/kotlin/com/ace/wear/WearApplication.kt` | Inicializa WearMessageClient |
| `data/health/HealthServicesManager.kt` | Inicia/detiene monitoreo de FC |
| `data/repository/WearHealthRepository.kt` | `startSession()`, `stopSession()`, envío de STOPPED |
| `presentation/session/SessionViewModel.kt` | Botón DETENER funcional, logs |
| `presentation/session/SessionScreen.kt` | UI con FC en vivo, timer, botón DETENER |
| `presentation/components/ConnectionStatusChip.kt` | Estado de conexión mejorado |
| `presentation/components/DiagLogPanel.kt` | Panel de logs en pantalla (nuevo) |

---

## 6. Métricas de la Integración

| Métrica | Valor |
|---------|-------|
| Frecuencia de captura FC | ~1 Hz (Health Services API) |
| Capacidad del buffer | 300 muestras (circular) |
| Duración de bloque | 30 segundos (pruebas) / 300 segundos (producción) |
| Timeout de desconexión | 5 segundos sin muestras |
| Timeout de espera STOPPED | 10 segundos (servicio) / 15 segundos (ViewModel) |
| Batch de sync offline | Máximo 20 bloques por request |
| Backoff de reintentos | 15min → 30min → 1h → 2h → 4h |

---

## 7. Pruebas Realizadas

### 7.1 Prueba 1: Sesión completa con detención desde reloj

```
01:12:44  Wear:  START recibido
01:12:57  Mobile: === INICIANDO SESION ===
01:12:57  Mobile: MessageClient listener registrado
01:12:57  Mobile: ExerciseSyncService conectado
01:13:28  Mobile: BLOCK CLOSED: 31 samples, 30s, avgBPM: 98.2
01:13:25  Wear:   STOPPED enviado al movil
01:13:37  Mobile: STOPPED recibido de reloj
01:13:38  Mobile: SessionStoppedEvent emitido
01:13:38  Mobile: ViewModel recibió SessionStopped → UI Completed
```

### 7.2 Prueba 2: Sesión con detención desde mobile

```
01:13:59  Mobile: === INICIANDO SESION ===
01:14:03  Wear:   STOP recibido
01:14:03  Wear:   STOPPED enviado al movil
01:14:16  Mobile: STOPPED recibido
01:14:16  Mobile: SessionStoppedEvent emitido
```

### 7.3 Resultado

| Escenario | Estado |
|-----------|--------|
| Mobile → Wear: START | ✅ Funciona |
| Wear → Mobile: FC (DataClient) | ✅ Funciona |
| Wear → Mobile: STOPPED (MessageClient) | ✅ Funciona |
| Mobile → Wear: STOP | ✅ Funciona |
| Cierre de bloques (~30s) | ✅ Funciona |
| Bloque final al detener | ✅ Funciona |
| UI en vivo (FC, timer, bloques) | ✅ Funciona |
| Notificación foreground | ✅ Funciona |
| Persistencia SQLite (StopSessionUseCase) | ✅ Funciona |

---

## 8. Decisiones de Arquitectura

### 8.1 Fuente de Verdad

| Aspecto | Fuente de verdad | Justificación |
|---------|------------------|---------------|
| Estado de sesión | **Mobile** | S2: "El móvil orquesta, el reloj reacciona" |
| FC capturada | **Mobile** (buffer) | S1: "El reloj no persiste, no decide, solo reacciona" |
| XP calculado | **Mobile** | S5: Fórmulas cacheadas del backend |
| Ranking/Rachas | **Backend** | S6/S7: Validación y recálculo batch |

### 8.2 Patrón de Servicio

**Decisión:** Hybrid Service (Started + Bound) con StateFlow.

**Alternativas descartadas:**
- `LocalBroadcastManager`: Deprecated por Google, sin type safety.
- `BroadcastReceiver`: Acoplamiento implícito, difícil de testear.

**Justificación del híbrido:**
- `startForegroundService()` garantiza supervivencia en background.
- `bindService()` permite comunicación bidireccional tipada.
- `StateFlow` es el patrón moderno recomendado por Google para UI reactiva.

---

## 9. Próximos Pasos (Backlog)

| Prioridad | Tarea | Sistema |
|-----------|-------|---------|
| Alta | Implementar guardar bloques en SQLite (`BlockDao`) | S2/S3 |
| Alta | Implementar cálculo de XP (`CalculateBlockXpUseCase`) | S5 |
| Media | Implementar sync offline-first con WorkManager | S3 |
| Media | Agregar debounce al botón STOP para evitar duplicados | S2 |
| Baja | Limpiar archivos de diagnóstico (`diagnostic/` folder) | — |
| Baja | Quitar logs verbose de `WearDataSource` | — |
| Baja | Implementar pausa/reanudación de sesión | S2 |

---

## 10. Referencias

- Documentación del proyecto A.C.E: Apéndices S1-S10, Arquitectura v0.3, Implementation Plans v4.1/v4.2
- [Android Health Services API](https://developer.android.com/health-and-fitness/guides/health-services)
- [Wear OS Data Layer](https://developer.android.com/training/wearables/data/data-layer)
- [Foreground Services](https://developer.android.com/develop/background-work/services/foreground-services)
- [Kotlin StateFlow](https://developer.android.com/kotlin/flow/stateflow-and-sharedflow)

---

> **Nota:** Este documento refleja el estado del código en el commit `d536c4b` de la rama `main`. Para actualizaciones posteriores, consultar el historial de commits en GitHub.

---
*Fin del documento*
