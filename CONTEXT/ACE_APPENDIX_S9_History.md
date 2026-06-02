# Apéndice 9 — Sistema 9: Historial de Sesiones (Especificación Conceptual)

> **Estado:** Aprobado  
> **Versión:** 1.0  
> **Fecha:** 2026-06-02  
> **Depende de:** Documento base A.C.E v0.2 (Sistema 9) · Apéndice 2 (Sistema 2)

---

## 1. Propósito del Apéndice

Este documento descompone el Sistema 9 en sus tres subsistemas conceptuales, definiendo **qué hace cada uno, qué entradas recibe, qué salidas produce y qué invariantes debe respetar**. No contiene código fuente; su función es servir como contrato de comportamiento antes de la implementación.

Los tres subsistemas son:

1. **Subsistema de Captura de Sesiones** (recepción de sesiones completadas desde el Sistema 2).
2. **Subsistema de Ventana Deslizante** (mantenimiento de solo las últimas 5 sesiones).
3. **Subsistema de Presentación** (renderizado en la UI de perfil/historial).

---

## 2. Subsistema de Captura de Sesiones

### 2.1. Principio rector
El historial local es una **vista rápida offline** de las actividades recientes. No es la fuente de verdad permanente (esa está en el backend). Su propósito es que el usuario vea un resumen inmediato sin esperar sync ni consultar al servidor.

### 2.2. Entrada
El Sistema 2 notifica al Sistema 9 cada vez que una sesión pasa a estado `COMPLETED`. La notificación incluye:

- `session_id`: UUID de la sesión.
- `timestamp_start`, `timestamp_end`: instantes de inicio y cierre.
- `sport_type`: tipo de actividad.
- `duration_seconds`: duración total.
- `avg_bpm`: promedio de frecuencia cardíaca de todos los bloques de la sesión.
- `total_blocks`: cantidad de bloques generados.
- `total_xp`: suma de `xp_calculated` de todos los bloques.

### 2.3. Qué NO recibe el historial
- No recibe muestras individuales de FC.
- No recibe métricas por bloque.
- No recibe datos de otros sensores (Speed, Distance, etc.).

### 2.4. Invariante
- Solo las sesiones `COMPLETED` entran al historial. Las sesiones `ABORTED` se descartan.

---

## 3. Subsistema de Ventana Deslizante

### 3.1. Principio rector
El historial local mantiene **exactamente las últimas 5 sesiones completadas**, sin discriminar por categoría. Cuando llega la sexta, la más antigua se descarta automáticamente (FIFO).

### 3.2. Estructura de almacenamiento
En SQLite (`local_session_history`):

| Campo | Tipo | Significado |
|-------|------|-------------|
| `session_id` | UUID | Identificador único. |
| `timestamp_start` | Epoch millis | Inicio de la sesión. |
| `timestamp_end` | Epoch millis | Cierre de la sesión. |
| `sport_type` | String | Running, Cycling, etc. |
| `duration_seconds` | Entero | Duración total. |
| `avg_bpm` | Double | Promedio de FC de la sesión. |
| `total_blocks` | Entero | Bloques generados. |
| `total_xp` | Entero | XP total ganada en la sesión. |

### 3.3. Regla de descarte
- Al insertar una nueva sesión, se cuenta el total de filas.
- Si el total es 6, se elimina la fila con `timestamp_start` más antiguo.
- El orden de presentación es cronológico descendente (la más reciente primero).

### 3.4. Sin discriminación de categoría
Las 5 sesiones se muestran **mezcladas**: Running, Cycling, etc., aparecen juntas ordenadas solo por fecha. No hay pestañas ni filtros por deporte en MVP.

### 3.5. Qué NO hace la ventana
- No sincroniza con el backend. El backend ya tiene los bloques.
- No mantiene historial completo. Si el usuario quiere ver más allá de 5 sesiones, debe consultar al backend (paginado).
- No se recupera si la APK se reinstala. El backend puede reconstruirlo si se solicita.

---

## 4. Subsistema de Presentación

### 4.1. Principio rector
La UI de perfil/historial lee directamente de `local_session_history` y renderiza una lista visual. No requiere internet.

### 4.2. Formato de presentación
Cada ítem de la lista muestra:

- **Fecha y hora** de inicio (formateada legiblemente).
- **Tipo de actividad** (icono + texto: 🏃 Running, 🚴 Cycling).
- **Duración** (mm:ss o hh:mm:ss).
- **FC promedio** (bpm).
- **XP ganada** (puntos).
- **Cantidad de bloques** (opcional, para detalle).

### 4.3. Datos sin procesar (raw)
Los datos mostrados son **sin procesar** en el sentido de que no incluyen análisis derivado (no hay tendencias, gráficos, ni comparativas). Son los valores brutos de la sesión: duración, FC promedio, tipo.

### 4.4. Estado vacío
Si no hay sesiones completadas (tabla vacía), se muestra un mensaje ilustrativo: "Aún no has completado ninguna sesión. ¡Empieza hoy!"

### 4.5. Qué NO hace la presentación
- No muestra bloques individuales. El nivel de detalle es la sesión completa.
- No permite editar o borrar sesiones del historial.
- No muestra estado de sync (si la sesión fue syncada o no). Eso es responsabilidad del Sistema 3.

---

## 5. Flujo Integrado de Datos

```
┌─────────────────────────────────────────────────────────────────────┐
│                         Sistema 2 — Sesión                           │
│  ┌─────────────────────────────────────────────────────────────────┐  │
│  │  Sesión pasa a COMPLETED                                         │  │
│  │  Notifica a Sistema 9: {session_id, timestamps, sport_type,      │  │
│  │   duration, avg_bpm, total_blocks, total_xp}                     │  │
│  └─────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│                         Sistema 9 — Historial                        │
│  ┌──────────────────┐    ┌──────────────────┐    ┌──────────────────┐  │
│  │ Insertar en      │───►│ Ventana FIFO     │───►│ SQLite           │  │
│  │ local_session_   │    │ (máx 5 sesiones) │    │ local_session_   │  │
│  │ history          │    │ descarta la más  │    │ history          │  │
│  │                  │    │ antigua si > 5    │    │                  │  │
│  └──────────────────┘    └──────────────────┘    └──────────────────┘  │
│                              │                                       │
│                              │ Lectura directa                       │
│                              ▼                                       │
│  ┌─────────────────────────────────────────────────────────────────┐  │
│  │  UI de Perfil / Historial                                        │  │
│  │  Lista de 5 sesiones (cronológico, sin filtro de categoría)       │  │
│  │  Datos raw: fecha, duración, FC promedio, tipo, XP               │  │
│  └─────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 6. Decisiones Arquitectónicas Consolidadas (Sistema 9)

| Decisión | Valor | Justificación |
|----------|-------|---------------|
| **Límite de sesiones** | 5 | Vista rápida sin sobrecargar SQLite ni la UI. |
| **Sin discriminación** | Todas las categorías mezcladas | Simplicidad en MVP. El usuario ve todo su historial reciente junto. |
| **Datos almacenados** | Raw (sin procesar) | Duración, FC promedio, tipo, XP. No hay análisis derivado. |
| **No sincroniza** | Solo local | El backend ya tiene los bloques. Este historial es solo vista rápida. |
| **Fuente de notificación** | Sistema 2 al cerrar sesión COMPLETED | Solo sesiones completadas entran. ABORTED se descarta. |
| **No persiste bloques** | Solo sesiones agregadas | El historial no es un ledger; es un resumen visual. |
| **Reconstruible** | Si se pierde, el backend puede regenerarlo | No es crítico. Es un cache de conveniencia. |

---

## 7. Glosario de Términos (Sistema 9)

| Término | Definición |
|---------|------------|
| **Historial local** | Conjunto de las últimas 5 sesiones completadas, almacenadas en SQLite del móvil. |
| **Ventana deslizante (FIFO)** | Estructura que mantiene un número fijo de elementos, descartando el más antiguo al insertar uno nuevo. |
| **Sin discriminar categoría** | Las sesiones se muestran todas juntas, sin filtros por tipo de deporte. |
| **Datos raw** | Valores directos de la sesión (duración, FC promedio) sin análisis ni procesamiento adicional. |
| **Vista rápida** | Interfaz que muestra información inmediatamente, sin necesidad de consultar al backend. |

---

## 8. Interacciones con otros sistemas

| Sistema | Interacción | Dirección |
|---------|-------------|-----------|
| **Sistema 2 — Sesión** | Notifica al Sistema 9 cuando una sesión se completa. | Sistema 2 → Sistema 9 |
| **Sistema 3 — Sync** | El historial no depende del sync. Se llena al cerrar la sesión, no al sincronizarla. | — |
| **Sistema 10 — Estadísticas** | La UI de perfil puede mostrar historial y estadísticas juntas, pero son datos distintos. | Sistema 9 ↔ Sistema 10 |
