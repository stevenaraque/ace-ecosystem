# Apéndice 2 — Sistema 2: Sesión de Ejercicio (Especificación Conceptual)

> **Estado:** Aprobado  
> **Versión:** 1.0  
> **Fecha:** 2026-06-02  
> **Depende de:** Documento base A.C.E v0.2 (Sistema 2) · Apéndice 1 (Sistema 1)

---

## 1. Propósito del Apéndice

Este documento descompone el Sistema 2 en sus cinco subsistemas conceptuales, definiendo **qué hace cada uno, qué entradas recibe, qué salidas produce y qué invariantes debe respetar**. No contiene código fuente; su función es servir como contrato de comportamiento antes de la implementación.

Los cinco subsistemas son:

1. **Subsistema de Gestión de Estado de Sesión** (creación, transiciones y cierre de una sesión lógica).
2. **Subsistema de Gestión de Bloques** (partición de la sesión en unidades de ~300 segundos).
3. **Subsistema de Acumulación de Métricas** (agregación de muestras de sensor en métricas de bloque).
4. **Subsistema de Cierre y Persistencia Local** (materialización de bloques y sesiones en SQLite).
5. **Subsistema de Disparo de Cálculo de XP** (entrega de métricas agregadas al Sistema 5 para recompensa inmediata).

---

## 2. Subsistema de Gestión de Estado de Sesión

### 2.1. Entidad Sesión
Una **sesión** es el contenedor lógico de todo el esfuerzo físico que el usuario realiza desde que toca *Iniciar* hasta que toca *Detener*. Es la unidad de trabajo que el usuario percibe como "una salida" o "un entrenamiento".

Atributos conceptuales:
- `session_id`: UUID generado por el móvil en el momento de la creación.
- `status`: estado del ciclo de vida.
- `sport_type`: tipo de actividad (Running, Cycling, etc.), seleccionado por el usuario al inicio.
- `timestamp_start`: instante de creación, tomado del reloj del móvil.
- `timestamp_end`: instante de cierre, definido cuando el usuario detiene o cuando el sistema aborta.

### 2.2. Estados del ciclo de vida
La sesión transita por exactamente estos estados:

| Estado | Significado | Quién puede disparar la transición |
|--------|-------------|-----------------------------------|
| **ACTIVE** | La sesión está en curso. El reloj está transmitiendo datos. El buffer del Sistema 1 está alimentándose. | Usuario (toca *Iniciar*). |
| **PAUSED** | La sesión está pausada temporalmente. El reloj deja de transmitir (o el móvil ignora las muestras). No se generan bloques nuevos. | Usuario (toca *Pausar*). |
| **COMPLETED** | La sesión terminó normalmente. Todos los bloques están cerrados. El cálculo de XP del bloque final ya ocurrió. | Usuario (toca *Detener*). |
| **ABORTED** | La sesión fue cerrada forzosamente por una nueva sesión o por una condición de error. | Backend (al validar sync) o móvil (si el usuario inicia una nueva sesión sin cerrar la anterior). |

### 2.3. Reglas de transición
- De `ACTIVE` puede pasar a `PAUSED`, `COMPLETED` o `ABORTED`.
- De `PAUSED` puede pasar a `ACTIVE` (reanudar) o `COMPLETED`.
- Una vez en `COMPLETED` o `ABORTED`, no hay transición posible. La sesión es inmutable.
- **Solo una sesión ACTIVE por instalación de APK.** Si el usuario inicia una nueva sesión mientras otra está ACTIVE, la anterior se marca como `ABORTED` automáticamente.

### 2.4. Responsabilidades por módulo
| Módulo | Responsabilidad | Qué NO hace |
|--------|----------------|-------------|
| **APK** | Crear la sesión con UUID propio. Mantener la máquina de estados. Garantizar que solo haya una ACTIVE. Persistir el estado en SQLite (`local_sessions`). | No valida si la sesión es legítima (eso es backend). No envía la sesión al backend inmediatamente. |
| **Wear OS** | Mostrar timer, FC en vivo y botón DETENER. Enviar señal STOP al móvil si el usuario toca el reloj. | No conoce el UUID de la sesión. No inicia sesiones por sí solo. |
| **Backend** | Al recibir el primer bloque de una sesión, validar que no exista otra ACTIVE para ese usuario. Si existe, marcarla como ABORTED. | No inicia sesiones remotamente. |

---

## 3. Subsistema de Gestión de Bloques

### 3.1. Entidad Bloque
Un **bloque** es una partición temporal de aproximadamente 300 segundos (5 minutos) dentro de una sesión. Es la unidad mínima de procesamiento, persistencia y sincronización.

Atributos conceptuales:
- `block_id`: UUIDv4 generado por el móvil al momento de cerrar el bloque.
- `session_id`: referencia a la sesión padre.
- `timestamp_start`: instante del primer heartbeat del bloque (fuente de verdad temporal).
- `timestamp_end`: instante del último heartbeat del bloque.
- `duration_seconds`: derivado de `timestamp_end - timestamp_start`.
- `status`: estado del bloque en el ciclo de vida local.

### 3.2. Ciclo de vida de un bloque
| Estado | Significado |
|--------|-------------|
| **OPEN** | El bloque está recibiendo muestras del buffer del Sistema 1. No es visible para el usuario ni persistido en SQLite. Existe solo en RAM. |
| **CLOSED** | El bloque alcanzó los ~300 segundos o la sesión terminó. Sus métricas están agregadas. Se materializa en SQLite con estado `PENDING`. |
| **PENDING** | El bloque está en SQLite esperando sincronización. El usuario ya vio su XP. |
| **SYNCING** | El bloque está en vuelo hacia el backend. |
| **SYNCED** | El backend confirmó recepción con `201`. |
| **ERROR** | El backend rechazó el bloque (XP inválida) o fallaron 5 reintentos de red. |

### 3.3. Reglas de cierre de bloque
- Un bloque se cierra automáticamente cuando acumula **~300 segundos de muestras** (con tolerancia de ±10%, es decir, entre 270 y 330 segundos).
- Si el usuario detiene la sesión antes de los 270 segundos, el bloque abierto se cierra como **bloque final corto** y se procesa de todas formas.
- Si el usuario pausa la sesión, el cronómetro del bloque se congela. Al reanudar, continúa el mismo bloque OPEN.
- El `block_id` lo genera el móvil, no el backend. Esto permite reenviar el mismo bloque si la red falla (idempotencia).

### 3.4. Invariantes
- Un bloque pertenece exactamente a una sesión.
- Un bloque CLOSED siempre drena el buffer circular del Sistema 1 (operación `drain()`).
- No puede existir más de un bloque OPEN por sesión.

---

## 4. Subsistema de Acumulación de Métricas

### 4.1. Entrada
Cuando un bloque se cierra, recibe el conjunto de muestras drenadas del buffer circular del Sistema 1. Cada muestra es una tupla `(bpm, timestamp)` proveniente del tipo nativo `HEART_RATE_BPM` de Health Services.

### 4.2. Proceso de agregación
Para cada bloque cerrado, el móvil computa las siguientes métricas agregadas:

| Métrica | Cómo se calcula | Para qué sirve |
|---------|----------------|----------------|
| `avg_bpm` | Promedio aritmético de todos los valores `bpm` del bloque. | Input para el cálculo de XP (Sistema 5). |
| `max_bpm` | Valor máximo de `bpm` en el bloque. | Validación de sanidad por el backend. |
| `min_bpm` | Valor mínimo de `bpm` en el bloque. | Validación de sanidad por el backend. |
| `sample_count` | Cantidad de muestras recibidas. | Validación de densidad (¿llegaron suficientes muestras?). |
| `duration_seconds` | `timestamp_end - timestamp_start`. | Validación de que el bloque duró lo razonable. |

### 4.3. Qué NO se agrega aquí
- **No se calcula XP.** Las métricas agregadas se pasan al Sistema 5, que aplica las fórmulas cacheadas.
- **No se filtran outliers.** Si una muestra de 220 bpm llegó, entra en el promedio. El Sistema 5 o el backend deciden si es sospechosa.
- **No se usan datos de otros sensores.** Speed, Distance, Calories del emulador no entran en la agregación del bloque en MVP.

---

## 5. Subsistema de Cierre y Persistencia Local

### 5.1. Materialización en SQLite
Cuando un bloque se cierra y sus métricas están agregadas, el móvil escribe dos registros:

1. **En `local_sessions`**: si es el primer bloque de la sesión, asegura que la sesión exista con estado `ACTIVE`.
2. **En `local_blocks`**: inserta el bloque con:
   - `block_id` (UUID móvil)
   - `session_id` (UUID móvil)
   - `timestamp_start`, `timestamp_end`, `duration_seconds`
   - `avg_bpm`, `max_bpm`, `min_bpm`, `sample_count`
   - `sport_type`
   - `status = PENDING`
   - `xp_calculated = NULL` (se llenará tras el cálculo del Sistema 5)

### 5.2. Materialización de la sesión al cerrar
Cuando la sesión pasa a `COMPLETED` o `ABORTED`:
- Se actualiza `local_sessions.timestamp_end`.
- Se cierra el bloque OPEN si quedaba alguno (incluso si es corto).
- Se actualiza el estado de la sesión en SQLite.

### 5.3. Reglas de persistencia
- El móvil **no** persiste muestras individuales de FC. Solo persiste el bloque agregado.
- El móvil **no** envía nada al backend inmediatamente. El bloque queda en `PENDING` hasta que el Sistema 3 (Sincronización) lo tome en un batch.
- Si la app se cierra abruptamente con un bloque OPEN, ese bloque se pierde (estaba en RAM). Esto es aceptable porque el buffer del Sistema 1 es volátil por diseño.

---

## 6. Subsistema de Disparo de Cálculo de XP

### 6.1. Momento de disparo
Inmediatamente después de que un bloque se cierra, sus métricas agregadas se envían al **Sistema 5 — Cálculo de XP** para computar la recompensa. Esto ocurre **offline**, sin necesidad de internet.

### 6.2. Flujo conceptual
```
Bloque cerrado → Métricas agregadas (avg_bpm, sport_type, duration)
→ Sistema 5 aplica fórmula cacheada (ej. Running = 2 XP/min si avg_bpm > 80)
→ Sistema 5 devuelve xp_calculated
→ APK muestra al usuario: "+10 XP" (toast o animación)
→ APK escribe xp_calculated en local_blocks
→ Bloque ahora está listo para sync (status = PENDING)
```

### 6.3. Responsabilidades
- El Sistema 2 **dispara** el cálculo.
- El Sistema 5 **ejecuta** el cálculo usando fórmulas descargadas del backend.
- El Sistema 2 **muestra** el resultado inmediatamente y lo asocia al bloque.

### 6.4. Invariante
- Un bloque en estado `PENDING` siempre tiene un `xp_calculated` definido. No se envía un bloque al backend sin XP calculada.

---

## 7. Flujo Integrado de Datos

```
┌─────────────────────────────────────────────────────────────────────┐
│                         USUARIO                                      │
│              Toca INICIAR ──────► Toca PAUSA ──────► Toca DETENER   │
└─────────────────────────────────────────────────────────────────────┘
                              │              │              │
                              ▼              ▼              ▼
┌─────────────────────────────────────────────────────────────────────┐
│                         APK (Móvil)                                  │
│                                                                      │
│  ┌──────────────────┐    ┌──────────────────┐    ┌──────────────────┐  │
│  │ Gestión de       │    │ Gestión de       │    │ Acumulación de   │  │
│  │ Estado de Sesión │───►│ Bloques          │───►│ Métricas         │  │
│  │ (ACTIVE)         │    │ (OPEN → CLOSED)  │    │ (avg, max, min)  │  │
│  └──────────────────┘    └──────────────────┘    └──────────────────┘  │
│           │                       │                       │           │
│           │                       │                       ▼           │
│           │                       │              ┌──────────────────┐  │
│           │                       │              │ Sistema 5 (XP)   │  │
│           │                       │              │ Calcula XP local │  │
│           │                       │              └──────────────────┘  │
│           │                       │                       │           │
│           ▼                       ▼                       ▼           │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │              Persistencia Local (SQLite)                          │  │
│  │  ┌──────────────┐              ┌──────────────┐                   │  │
│  │  │ local_sessions│              │ local_blocks  │                   │  │
│  │  │ (ACTIVE)      │◄────────────│ (PENDING)     │                   │  │
│  │  └──────────────┘              └──────────────┘                   │  │
│  └──────────────────────────────────────────────────────────────────┘  │
│                              │                                       │
│                              │ WorkManager (Sistema 3)                │
│                              ▼                                       │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │  Envío batch (máx 20 bloques + XP) → Backend (Sistema 3)        │  │
│  └──────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│                         WEAR OS (Reloj)                              │
│  ┌─────────────────────────────────────────────────────────────────┐  │
│  │  Muestra FC en vivo, timer, botón DETENER                       │  │
│  │  Enviar señal STOP vía MessageClient si usuario toca reloj      │  │
│  └─────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 8. Decisiones Arquitectónicas Consolidadas (Sistema 2)

| Decisión | Valor | Justificación |
|----------|-------|---------------|
| **Generador de `session_id`** | Móvil (UUID) | La sesión existe offline antes de tocar el backend. |
| **Generador de `block_id`** | Móvil (UUIDv4) | Idempotencia de red. Reenvío sin duplicar. |
| **Fuente de verdad temporal** | `timestamp_start` del bloque | El reloj la genera; el backend no reinterpreta. |
| **Duración de bloque** | ~300 segundos (±10%) | Balance entre granularidad de sync y overhead de red. Fuera de rango = rechazo `422`. |
| **Estados de sesión** | ACTIVE, PAUSED, COMPLETED, ABORTED | Cubre todas las transiciones del mundo real sin sobre-diseñar. |
| **Estados de bloque** | OPEN, CLOSED, PENDING, SYNCING, SYNCED, ERROR | Separa la vida en RAM de la vida en disco y de la vida en red. |
| **Cálculo de XP** | Disparado al cerrar bloque, ejecutado offline | Recompensa inmediata visible para el usuario. |
| **Persistencia de muestras** | NO. Solo bloques agregados | Ahorro de espacio y simplificación de SQLite. |
| **Bloque OPEN en RAM** | Sí. Si la app muere, se pierde | Aceptable porque el buffer del Sistema 1 es volátil por diseño. |
| **Sesiones ACTIVE** | Solo 1 por usuario | Evita contaminación de XP y ranking. |

---

## 9. Glosario de Términos (Sistema 2)

| Término | Definición |
|---------|------------|
| **Sesión** | Contenedor lógico de una actividad física completa. Tiene inicio, desarrollo y fin. |
| **Bloque** | Unidad temporal de ~300 segundos dentro de una sesión. Es la unidad de agregación, cálculo de XP y sincronización. |
| **session_id** | UUID generado por el móvil al crear la sesión. |
| **block_id** | UUIDv4 generado por el móvil al cerrar el bloque. Garantiza idempotencia en reenvío. |
| **OPEN** | Estado transitorio de un bloque que aún recibe muestras del buffer. Solo existe en RAM. |
| **CLOSED** | Estado de un bloque que ya drenó el buffer, tiene métricas agregadas y está listo para persistirse. |
| **PENDING** | Estado en SQLite de un bloque que ya tiene XP calculada y espera sincronización. |
| **timestamp_start** | Instante de la primera muestra del bloque. Fuente de verdad temporal para toda la lógica de negocio. |
| **Agregación** | Proceso de calcular avg, max, min y count a partir de las muestras individuales de un bloque. |

---

## 10. Interacciones con otros sistemas

| Sistema | Interacción | Dirección |
|---------|-------------|-----------|
| **Sistema 1 — Captura** | El Sistema 1 alimenta el buffer que el Sistema 2 drena al cerrar un bloque. | Sistema 1 → Sistema 2 |
| **Sistema 3 — Sync** | El Sistema 3 toma bloques en estado `PENDING` y los envía al backend en batches. | Sistema 2 → Sistema 3 |
| **Sistema 5 — XP** | El Sistema 2 dispara el cálculo de XP al cerrar cada bloque. | Sistema 2 → Sistema 5 |
| **Sistema 8 — Notificaciones** | Si una sesión está ACTIVE y el reloj se desconecta (timeout del Sistema 1), el Sistema 8 puede notificar. | Sistema 1/2 → Sistema 8 |
| **Sistema 9 — Historial** | Al cerrar una sesión COMPLETED, el Sistema 2 notifica al Sistema 9 para que guarde la sesión en el historial local de 5. | Sistema 2 → Sistema 9 |
