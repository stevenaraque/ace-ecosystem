# A.C.E — Implementation Plan: Wear OS (`ace-wear`)

> **Estado:** Coherente con Apéndices Aprobados S1-S10 y Arquitectura v0.2  
> **Versión:** 4.1 (Actualizado: nota sobre :shared, AGP 9.0 + Built-in Kotlin + Hilt 2.59.2)  
> **Fecha:** Junio 2026  
> **Stack:** Wear OS 3+ (API 30+) · Kotlin 2.0.21 (built-in) · Gradle 8.10+ (Kotlin DSL) · AGP 9.0.1 · Health Services Client 1.0.0 · Wear OS Data Layer API · Jetpack Compose for Wear OS 1.6.2 · Hilt 2.59.2  
> **Depende de:** NO consume `:shared` JAR (primitivos JSON)  
> **Paths de Data Layer:** Definidos en `:shared` (`DataLayerPaths.HEART_RATE = "/ace/health/heart_rate"`) pero **hardcodeados** en wear como strings literales  
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

**Nota sobre :shared:** El módulo `:wear` **NO consume** el JAR de `:shared`. Esto es intencional para reducir el footprint del APK del reloj. Los paths del Data Layer (`/ace/health/heart_rate`) y los formatos JSON están definidos en `:shared` (consumido por backend y mobile) pero en wear se usan como **strings literales** hardcodeados. Si el path cambia en `:shared`, wear debe actualizarse manualmente. En fases futuras, si wear necesita DTOs complejos (ej. modo standalone con LTE), se reconsiderará consumir `:shared`.

---

## 2. Dependencias Gradle (`build.gradle.kts`)

[Resto del documento idéntico a v4.0...]

### 2.4. Notas críticas sobre AGP 9.0 + Built-in Kotlin

| Aspecto | Pre-AGP 9.0 (plan v3.0) | AGP 9.0 (plan v4.0) | v4.1 (nota :shared) | Justificación |
|---------|------------------------|---------------------|---------------------|---------------|
| **NO `:shared` JAR** | Sí | Sí | **Sí (mantenido)** | Reduce footprint APK y memoria RAM. Reloj no necesita DTOs complejos. |
| **Paths Data Layer** | Hardcodeados | Hardcodeados | **Hardcodeados (coinciden con :shared)** | `/ace/health/heart_rate` definido en `:shared` pero wear usa string literal. Si cambia en `:shared`, wear debe actualizarse manualmente. |
| **Plugin Kotlin Android** | `org.jetbrains.kotlin.android` | **Eliminado** | Eliminado | Built-in en AGP 9.0. No se declara. |
| **Plugin kapt** | `org.jetbrains.kotlin.kapt` | **`com.android.legacy-kapt`** | `com.android.legacy-kapt` | `org.jetbrains.kotlin.kapt` es incompatible con built-in Kotlin. |
| `kotlinOptions { jvmTarget }` | Explícito | **Eliminado** | Eliminado | `compileOptions` Java 21 es suficiente; AGP 9.0 infiere automáticamente. |
| `composeOptions { kotlinCompilerExtensionVersion }` | `1.5.14` explícito | **Eliminado** | Eliminado | El plugin `org.jetbrains.kotlin.plugin.compose` gestiona el compiler automáticamente. |
| Hilt | `2.52` | **`2.59.2`** | 2.59.2 | Hilt 2.52 accede a `BaseExtension` removida en AGP 9.0. 2.59.2+ es compatible. |
| KSP para Hilt | No recomendado | **No usar** | No usar | Hilt 2.59.2 con AGP 9.0 requiere `legacy-kapt` (KSP tiene incompatibilidades documentadas). |
| Kotlin | `2.1.0` (plugin explícito) | **`2.0.21` (built-in)** | 2.0.21 | Android Studio genera 2.0.21 built-in. Compatible. |

[Resto del documento idéntico a v4.0...]

---

## 7. Contratos con Mobile (Coherentes con Apéndices)

### 7.1 DataClient Paths y Formatos (S1 §2.2, §2.3)

| Path | Dirección | Formato | Semántica | Definido en |
|------|-----------|---------|-----------|-------------|
| `/ace/health/heart_rate` | Wear → Mobile | `{"bpm": int, "timestamp": long}` | Muestra individual de FC. Epoch millis. | `:shared` (`DataLayerPaths.HEART_RATE`) — wear usa string literal |
| `/ace/session/{sessionId}/status` | Wear → Mobile | `{"status": "STOPPED"}` | Sesión detenida desde reloj. | `:shared` — wear usa string literal |
| `/ace/command` | Mobile → Wear | `{"command": "STOP"}` | Comando remoto desde móvil. | `:shared` — wear usa string literal |
| `/ace/initial_sync` | Mobile → Wear | `{"userId": UUID, "deviceId": String, "sessionId": UUID}` | Sincronización inicial de IDs. | `:shared` — wear usa string literal |

**Nota:** Los paths están definidos en `:shared` (`com.ace.shared.constants.DataLayerPaths`) y consumidos por mobile/backend vía JitPack. En wear se usan como **strings literales** para evitar dependencia del JAR. Si `:shared` cambia un path, wear debe actualizarse manualmente.

[Resto del documento idéntico a v4.0...]

---

*Documento coherente con Apéndices S1-S10 y Arquitectura A.C.E v0.2. Cualquier divergencia debe ser reportada como bug de coherencia.*
