# Apéndice 7 — Sistema 7: Racha (Streaks) (Especificación Conceptual)

> **Estado:** Aprobado  
> **Versión:** 1.0  
> **Fecha:** 2026-06-02  
> **Depende de:** Documento base A.C.E v0.2 (Sistema 7) · Apéndice 5 (Sistema 5)

---

## 1. Propósito del Apéndice

Este documento descompone el Sistema 7 en sus cuatro subsistemas conceptuales, definiendo **qué hace cada uno, qué entradas recibe, qué salidas produce y qué invariantes debe respetar**. No contiene código fuente; su función es servir como contrato de comportamiento antes de la implementación.

Los cuatro subsistemas son:

1. **Subsistema de Evaluación de Racha** (cálculo backend de incremento, rotura o invarianza de la racha).
2. **Subsistema de Persistencia de Racha** (almacenamiento del estado de racha en la base de datos).
3. **Subsistema de Notificación Local** (recordatorio al usuario si no ha entrenado hoy).
4. **Subsistema de Cacheo y Sincronización** (propagación del estado de racha al móvil).

---

## 2. Subsistema de Evaluación de Racha

### 2.1. Principio rector
La racha es un hábito diario: el usuario debe realizar al menos un bloque válido de ejercicio cada día para mantenerla. La evaluación ocurre **exclusivamente en el backend**, usando el `timestamp_start` del bloque como fuente de verdad temporal. El móvil no decide si la racha sube o se rompe.

### 2.2. Disparador de evaluación
Cada vez que el backend valida un bloque (`201` en el Sistema 3), evalúa la racha del usuario **inmediatamente después** de persistir la transacción XP.

### 2.3. Reglas de evaluación
El backend compara la fecha del bloque recién validado (`timestamp_start` truncado a día) con la `last_exercise_date` almacenada del usuario:

| Escenario | Condición | Acción sobre `current_streak` | Acción sobre `best_streak` |
|-----------|-----------|------------------------------|---------------------------|
| **Mismo día** | `block_date == last_exercise_date` | No cambia. | No cambia. |
| **Día siguiente** | `block_date == last_exercise_date + 1 día` | `current_streak += 1` | Si `current_streak > best_streak`, actualiza `best_streak`. |
| **Hueco > 1 día** | `block_date > last_exercise_date + 1 día` | `current_streak = 1` (se rompe). | No cambia. |
| **Primera vez** | `last_exercise_date IS NULL` | `current_streak = 1`. | `best_streak = 1`. |

### 2.4. Qué cuenta como "entrenó hoy"
- **Un solo bloque válido** es suficiente para mantener o incrementar la racha.
- No se requiere una sesión completa ni una duración mínima adicional (la duración del bloque ya fue validada por el Sistema 5).
- Bloques atrasados (enviados días después de su captura) no afectan la racha actual. Solo el `timestamp_start` importa.

### 2.5. Invariantes
- El backend **nunca** evalúa la racha usando `server_received_at`. Siempre usa `timestamp_start` del bloque.
- El backend **nunca** decrementa `current_streak` directamente. Solo la incrementa o la resetea a 1.
- Un bloque rechazado (`422`) **no** dispara evaluación de racha.

---

## 3. Subsistema de Persistencia de Racha

### 3.1. Estructura de datos
La tabla `user_streaks` almacena exactamente estos campos:

| Campo | Tipo | Significado |
|-------|------|-------------|
| `user_id` | UUID | Clave primaria. |
| `current_streak` | Entero | Racha activa. Mínimo 0. |
| `best_streak` | Entero | Racha máxima histórica. |
| `last_exercise_date` | Fecha (sin hora) | Último día en que se validó un bloque. Fuente de verdad para la evaluación. |

### 3.2. Actualización
La evaluación de racha (sección 2) se ejecuta dentro de la misma transacción de base de datos que inserta la transacción XP. Esto garantiza atomicidad: si el bloque se persiste, la racha se evalúa; si el bloque falla, la racha no cambia.

### 3.3. Invariantes
- `current_streak` siempre es `<= best_streak`.
- `last_exercise_date` siempre corresponde a un bloque validado, no a una fecha futura.
- No hay historial de rachas rotas. Solo se guarda el valor actual y el máximo.

---

## 4. Subsistema de Notificación Local

### 4.1. Principio rector
El recordatorio de racha es una **notificación local** del móvil, no un push del backend. Esto elimina la dependencia de FCM (Firebase Cloud Messaging) en el MVP y garantiza que el recordatorio funcione incluso si el backend está caído.

### 4.2. Agente de notificación
**WorkManager** programa un worker diario (`CheckStreakWorker`) que se ejecuta a las **20:00 hora local** del dispositivo.

### 4.3. Lógica del worker
1. Lee `last_exercise_date` del cache local (recibido en la última sync).
2. Compara con la fecha actual del dispositivo.
3. Si `last_exercise_date != hoy`, muestra notificación local:
   - Título: "🔥 Tu racha está en peligro"
   - Cuerpo: "No has entrenado hoy. ¡Sal a correr para mantener tu racha de X días!"
4. Si `last_exercise_date == hoy`, no hace nada.

### 4.4. Persistencia del recordatorio
WorkManager sobrevive:
- Cierre de la app.
- Reinicio del teléfono.
- Modo Doze (se ejecuta en la ventana de mantenimiento).

Si el usuario reinicia el teléfono a las 19:00, la notificación de las 20:00 sigue programada.

### 4.5. Qué NO hace la notificación
- No usa FCM ni push notifications.
- No consulta al backend para saber si entrenó. Usa solo el cache local.
- No sondea constantemente. Se ejecuta una vez al día.

---

## 5. Subsistema de Cacheo y Sincronización

### 5.1. Principio rector
El móvil no calcula la racha, pero sí la cachea para mostrarla en la UI y para decidir si enviar el recordatorio diario.

### 5.2. Entrada
En cada respuesta de sync exitosa (`201`), el backend incluye:

- `current_streak`: valor actualizado tras procesar el batch.
- `last_exercise_date`: fecha del último bloque validado (puede ser anterior a hoy si el usuario no ha sincronizado recientemente).

### 5.3. Almacenamiento local
El móvil guarda estos valores en `local_user` o `local_streak_cache`:

| Campo | Origen | Uso |
|-------|--------|-----|
| `cached_current_streak` | Respuesta de sync | Mostrar en UI de perfil. |
| `cached_last_exercise_date` | Respuesta de sync | Decidir si enviar notificación de recordatorio. |
| `cached_best_streak` | Respuesta de sync | Mostrar récord personal. |

### 5.4. Invariantes
- El cache puede estar desactualizado (si no ha habido sync reciente). La UI debe mostrar el valor cacheado con un indicador opcional de "última sync: X".
- Si el cache dice `last_exercise_date = ayer` y hoy es un nuevo día, el móvil sabe que la racha aún no ha sido evaluada para hoy. No asume que está rota hasta que el backend lo confirme.

---

## 6. Flujo Integrado de Datos

```
┌─────────────────────────────────────────────────────────────────────┐
│                         BACKEND (Render)                           │
│  ┌──────────────────┐    ┌──────────────────┐    ┌──────────────────┐  │
│  │ Validar bloque   │───►│ Evaluar racha    │───►│ Persistir en     │  │
│  │ (Sistema 5)      │    │ (reglas día)     │    │ user_streaks     │  │
│  └──────────────────┘    └──────────────────┘    └──────────────────┘  │
│                              │                    │                 │
│                              │                    │                 │
│                              ▼                    ▼                 │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │  Respuesta de sync: {current_streak, best_streak,               │  │
│  │   last_exercise_date}                                            │  │
│  └──────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────┘
                              │
                              │ HTTPS + JWT (Sistema 3)
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│                         APK (Móvil)                                  │
│  ┌──────────────────┐    ┌──────────────────┐    ┌──────────────────┐  │
│  │ Cache local      │◄───│ Sistema 3 recibe │    │ UI de perfil     │  │
│  │ (streak, date)   │    │ respuesta de sync│    │ (muestra racha)  │  │
│  └──────────────────┘    └──────────────────┘    └──────────────────┘  │
│         │                                              ▲               │
│         │                                              │               │
│         ▼                                              │               │
│  ┌──────────────────┐                                  │               │
│  │ WorkManager        │                                  │               │
│  │ CheckStreakWorker  │                                  │               │
│  │ (20:00 diario)     │                                  │               │
│  │                    │                                  │               │
│  │ ¿last_exercise_date│─── NO ───► Notificación local   │               │
│  │   == hoy?          │         "No has entrenado hoy 🔥" │               │
│  └──────────────────┘                                  │               │
│         │                                              │               │
│         └────────────────── SÍ ────────────────────────┘               │
│                            (no hace nada)                             │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 7. Decisiones Arquitectónicas Consolidadas (Sistema 7)

| Decisión | Valor | Justificación |
|----------|-------|---------------|
| **Evaluador de racha** | Backend exclusivo | Fuente de verdad única. El móvil no puede manipular su racha. |
| **Fuente temporal** | `timestamp_start` del bloque | Bloques atrasados no rompen la racha actual. |
| **Qué cuenta** | 1 bloque válido = racha +1 | No requiere sesión completa ni duración extra. |
| **Notificación** | WorkManager local a las 20:00 | Sin FCM en MVP. Funciona offline y sobrevive reinicios. |
| **Cache del móvil** | `current_streak`, `best_streak`, `last_exercise_date` | UI muestra valor cacheado. Recordatorio usa cache para decidir. |
| **No historial** | Solo `current` y `best` | MVP no requiere ver todas las rachas rotas. |
| **Atomicidad** | Evaluación en misma transacción que bloque | Si el bloque falla, la racha no cambia. |
| **No push** | Todo local | Reduce dependencias externas. El backend no necesita estar online a las 20:00. |

---

## 8. Glosario de Términos (Sistema 7)

| Término | Definición |
|---------|------------|
| **Racha (Streak)** | Número de días consecutivos con al menos un bloque válido de ejercicio. |
| **current_streak** | Racha activa actual del usuario. |
| **best_streak** | Racha máxima histórica alcanzada por el usuario. |
| **last_exercise_date** | Fecha (sin hora) del último bloque validado. Fuente de verdad para evaluación. |
| **CheckStreakWorker** | Worker de WorkManager que ejecuta la verificación diaria de racha. |
| **Notificación local** | Alerta generada por el propio dispositivo, sin intervención de servidor remoto. |

---

## 9. Interacciones con otros sistemas

| Sistema | Interacción | Dirección |
|---------|-------------|-----------|
| **Sistema 3 — Sync** | El backend incluye `current_streak` y `last_exercise_date` en la respuesta de sync. | Sistema 3 → Sistema 7 |
| **Sistema 5 — XP** | Un bloque validado dispara la evaluación de racha. | Sistema 5 → Sistema 7 |
| **Sistema 8 — Notificaciones** | El Sistema 7 dispara la notificación local de recordatorio via WorkManager. | Sistema 7 → Sistema 8 |
| **Sistema 10 — Estadísticas** | La racha se muestra en la UI de perfil junto a otras estadísticas. | Sistema 7 ↔ Sistema 10 |
