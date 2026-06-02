# Apéndice 8 — Sistema 8: Notificaciones y Recordatorios (Especificación Conceptual)

> **Estado:** Aprobado  
> **Versión:** 1.0  
> **Fecha:** 2026-06-02  
> **Depende de:** Documento base A.C.E v0.2 (Sistema 8) · Apéndice 7 (Sistema 7)

---

## 1. Propósito del Apéndice

Este documento descompone el Sistema 8 en sus cuatro subsistemas conceptuales, definiendo **qué hace cada uno, qué entradas recibe, qué salidas produce y qué invariantes debe respetar**. No contiene código fuente; su función es servir como contrato de comportamiento antes de la implementación.

Los cuatro subsistemas son:

1. **Subsistema de Notificación de Sesión Activa** (foreground service durante ejercicio).
2. **Subsistema de Recordatorio de Racha** (alerta diaria si no ha entrenado).
3. **Subsistema de Notificación de Error de Sync** (alerta cuando bloques fallan reiteradamente).
4. **Subsistema de Canal de Notificaciones** (gestión de canales Android y prioridades).

---

## 2. Subsistema de Notificación de Sesión Activa

### 2.1. Principio rector
Durante una sesión de ejercicio activa, el móvil debe mantener un **Foreground Service** que garantice que el sistema operativo no mate el proceso que recibe datos del reloj. Este service muestra una notificación persistente al usuario.

### 2.2. Agente
**ExerciseSyncService**: un `ForegroundService` de tipo `health` (requerido en Android 10+ para recibir datos de salud en background).

### 2.3. Contenido de la notificación
La notificación persistente muestra:

- Título: "A.C.E — Sesión activa"
- Cuerpo: "Sincronizando con Wear OS · FC: 145 bpm · 12:34 transcurrido"
- Acción: Botón "DETENER" que envía `STOP` al reloj y cierra la sesión.

### 2.4. Ciclo de vida
- **Inicio:** Cuando el usuario toca *Iniciar* y el Sistema 2 crea una sesión `ACTIVE`.
- **Actualización:** Cada vez que llega una nueva muestra de FC del Sistema 1, se actualiza el valor mostrado.
- **Fin:** Cuando la sesión pasa a `COMPLETED`, `ABORTED` o el usuario toca *Detener*.

### 2.5. Invariantes
- El foreground service **solo** existe mientras hay una sesión `ACTIVE`.
- Si el usuario desliza la notificación, el sistema operativo no permite descartarla (es una notificación de foreground service obligatoria).
- El botón "DETENER" en la notificación tiene el mismo efecto que tocar *Detener* en la UI de la app.

---

## 3. Subsistema de Recordatorio de Racha

### 3.1. Principio rector
Motivar al hábito diario mediante un recordatorio a las 20:00 si el usuario no ha entrenado ese día. Es una notificación **local**, no requiere backend activo.

### 3.2. Agente
**CheckStreakWorker**: un `PeriodicWorkRequest` o `OneTimeWorkRequest` programado diariamente a las 20:00 hora local, gestionado por WorkManager.

### 3.3. Lógica de disparo
1. WorkManager ejecuta el worker a las 20:00.
2. El worker lee `cached_last_exercise_date` del Sistema 7.
3. Compara con la fecha actual del dispositivo.
4. Si son distintas → muestra notificación.
5. Si son iguales → no hace nada.

### 3.4. Contenido de la notificación
- Título: "🔥 No has entrenado hoy"
- Cuerpo: "Tu racha de X días está en peligro. ¡Sal ahora!"
- Acción (opcional): Botón "INICIAR" que abre la app directamente en la pantalla de sesión.

### 3.5. Invariantes
- El recordatorio **nunca** consulta al backend. Usa solo cache local.
- Si el usuario entrena después de las 20:00, no recibe recordatorio ese día (el worker ya ejecutó).
- WorkManager sobrevive reinicios del teléfono y modo Doze.

---

## 4. Subsistema de Notificación de Error de Sync

### 4.1. Principio rector
Informar al usuario cuando hay datos que no pueden sincronizarse con el backend, para que no pierda la confianza en la app.

### 4.2. Disparadores

| Escenario | Condición | Contenido de la notificación |
|-----------|-----------|------------------------------|
| **Reintentos agotados** | Un bloque falló 5 reintentos de WorkManager y pasó a estado `ERROR`. | "Tienes X bloques sin sincronizar. Revisa tu conexión." |
| **Rechazo de sanidad** | El backend respondió `422` para uno o más bloques. | "Algunos bloques fueron rechazados. Toca para ver detalles." |

### 4.3. Agente
WorkManager dispara un worker de notificación (`SyncErrorWorker`) cuando detecta que bloques pasaron a `ERROR`. Alternativamente, el Sistema 3 puede notificar directamente al Sistema 8 al resolver una respuesta `422` o al agotar reintentos.

### 4.4. Contenido de la notificación
- Título: "A.C.E — Problema de sincronización"
- Cuerpo: "X bloques no pudieron sincronizarse. Toca para más información."
- Acción: Abre la app en una pantalla de diagnóstico que lista bloques en `ERROR` con sus motivos.

### 4.5. Invariantes
- La notificación de error **no** se repite en bucle. Se muestra una vez por lote de bloques que entra en `ERROR`.
- Si el usuario resuelve el problema (ej. reconecta internet y los bloques se sincronizan), la notificación se descarta automáticamente.

---

## 5. Subsistema de Canal de Notificaciones

### 5.1. Principio rector
Android 8+ requiere que las notificaciones se agrupen en canales con distintos niveles de importancia. El usuario puede silenciar o configurar cada canal independientemente.

### 5.2. Canales definidos

| Canal | ID | Importancia | Uso |
|-------|-----|-------------|-----|
| **Sesión activa** | `ace_session_active` | **Baja** (sin sonido, sin vibración) | Foreground service. Solo informa. No interrumpe. |
| **Recordatorio de racha** | `ace_streak_reminder` | **Alta** (sonido, vibración) | Motivación diaria. Debe ser visible. |
| **Error de sync** | `ace_sync_error` | **Alta** (sonido, vibración) | Problema que requiere atención del usuario. |

### 5.3. Comportamiento por canal
- **Sesión activa:** No hace sonido. No vibra. No aparece en la pantalla de bloqueo como alerta. Solo en la barra de notificaciones.
- **Recordatorio de racha:** Hace sonido. Vibra. Aparece en pantalla de bloqueo. Puede despertar la pantalla.
- **Error de sync:** Hace sonido. Vibra. Aparece en pantalla de bloqueo.

### 5.4. Invariantes
- Cada canal se crea una sola vez al iniciar la app (en `Application.onCreate()`).
- El usuario puede desactivar cualquier canal desde Configuración del sistema → Aplicaciones → A.C.E → Notificaciones. La app respeta esta decisión.

---

## 6. Flujo Integrado de Datos

```
┌─────────────────────────────────────────────────────────────────────┐
│                         APK (Móvil)                                  │
│                                                                      │
│  ┌─────────────────────────────────────────────────────────────────┐  │
│  │  ExerciseSyncService (Foreground)                               │  │
│  │  Canal: ace_session_active (baja)                               │  │
│  │  "A.C.E sincronizando... FC: 145 bpm"                           │  │
│  │  [DETENER]                                                      │  │
│  └─────────────────────────────────────────────────────────────────┘  │
│                              │                                       │
│                              │ Sesión ACTIVE                         │
│                              ▼                                       │
│  ┌─────────────────────────────────────────────────────────────────┐  │
│  │  WorkManager                                                      │  │
│  │  ├── CheckStreakWorker (20:00 diario)                            │  │
│  │  │   Canal: ace_streak_reminder (alta)                           │  │
│  │  │   "🔥 No has entrenado hoy"                                   │  │
│  │  │                                                               │  │
│  │  └── SyncErrorWorker (disparado por Sistema 3)                   │  │
│  │      Canal: ace_sync_error (alta)                                 │  │
│  │      "Tienes X bloques sin sincronizar"                           │  │
│  └─────────────────────────────────────────────────────────────────┘  │
│                              │                                       │
│                              │ Notificaciones locales                 │
│                              ▼                                       │
│  ┌─────────────────────────────────────────────────────────────────┐  │
│  │  Sistema de notificaciones de Android                             │  │
│  │  (gestión de canales, prioridades, Doze, App Standby)             │  │
│  └─────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────┘
                              │
                              │ (no hay conexión con backend para notificaciones)
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│                         WEAR OS (Reloj)                              │
│  ┌─────────────────────────────────────────────────────────────────┐  │
│  │  Vibración en logros futuros (máximo)                            │  │
│  │  (no implementado en MVP)                                        │  │
│  └─────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 7. Decisiones Arquitectónicas Consolidadas (Sistema 8)

| Decisión | Valor | Justificación |
|----------|-------|---------------|
| **Notificaciones** | 100% locales (sin FCM) | Sin dependencia de backend online. Reduce costo y complejidad en MVP. |
| **Foreground service** | Tipo `health`, notificación persistente | Obligatorio en Android 10+ para recibir datos del reloj en background. |
| **Recordatorio de racha** | WorkManager a las 20:00 | Hora fija, predecible. Sobrevive reinicios y Doze. |
| **Errores de sync** | Notificación tras 5 reintentos o `422` | No alertar por cada fallo transitorio. Solo cuando hay un problema persistente. |
| **Canales** | 3 canales con importancias distintas | El usuario controla qué notificaciones quiere recibir. |
| **Wear OS** | Sin notificaciones al móvil | El reloj no empuja notificaciones. Solo vibra en logros futuros (fuera de MVP). |
| **No push** | Sin FCM | El backend no programa ni envía notificaciones. Todo es local o background. |

---

## 8. Glosario de Términos (Sistema 8)

| Término | Definición |
|---------|------------|
| **Foreground Service** | Servicio de Android que se ejecuta en primer plano con una notificación persistente, inmune a ser matado por el sistema. |
| **WorkManager** | API de Android para tareas diferidas y garantizadas que sobreviven al ciclo de vida de la app y al reinicio del dispositivo. |
| **Canal de notificación** | Categoría de notificaciones en Android 8+ que permite al usuario configurar importancia, sonido y vibración por separado. |
| **Notificación local** | Alerta generada por el propio dispositivo sin intervención de servidor remoto. |
| **Doze** | Modo de ahorro de batería de Android que restringe tareas en background. WorkManager respeta sus ventanas de mantenimiento. |

---

## 9. Interacciones con otros sistemas

| Sistema | Interacción | Dirección |
|---------|-------------|-----------|
| **Sistema 1 — Captura** | El foreground service se inicia cuando hay sesión ACTIVE y recibe datos del reloj. | Sistema 1 ↔ Sistema 8 |
| **Sistema 2 — Sesión** | El Sistema 2 inicia/detiene el foreground service según el estado de la sesión. | Sistema 2 → Sistema 8 |
| **Sistema 3 — Sync** | El Sistema 3 dispara notificación de error cuando bloques pasan a `ERROR`. | Sistema 3 → Sistema 8 |
| **Sistema 7 — Racha** | El Sistema 7 dispara el recordatorio diario via CheckStreakWorker. | Sistema 7 → Sistema 8 |
| **Sistema 4 — Auth** | Si el refresh falla y se fuerza logout, el Sistema 8 puede notificar al usuario. | Sistema 4 → Sistema 8 |
