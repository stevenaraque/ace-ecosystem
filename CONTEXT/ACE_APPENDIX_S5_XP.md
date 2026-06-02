# Apéndice 5 — Sistema 5: Cálculo de XP y Gamificación (Especificación Conceptual)

> **Estado:** Aprobado  
> **Versión:** 1.0  
> **Fecha:** 2026-06-02  
> **Depende de:** Documento base A.C.E v0.2 (Sistema 5) · Apéndice 2 (Sistema 2)

---

## 1. Propósito del Apéndice

Este documento descompone el Sistema 5 en sus cinco subsistemas conceptuales, definiendo **qué hace cada uno, qué entradas recibe, qué salidas produce y qué invariantes debe respetar**. No contiene código fuente; su función es servir como contrato de comportamiento antes de la implementación.

Los cinco subsistemas son:

1. **Subsistema de Fórmulas** (definición, exposición y cacheo de reglas de conversión esfuerzo→XP).
2. **Subsistema de Cálculo Local** (aplicación de fórmulas sobre métricas de bloque en el móvil).
3. **Subsistema de Validación de Sanidad** (verificación backend de que la XP reportada es consistente con los datos).
4. **Subsistema de Transacciones XP** (persistencia inmutable de recompensas en la base de datos).
5. **Subsistema de Corrección y Reversión** (mecanismo para ajustar XP cuando el backend detecta inconsistencias).

---

## 2. Subsistema de Fórmulas

### 2.1. Principio rector
La lógica de conversión de esfuerzo físico en puntos de experiencia (XP) vive en el backend como fuente de verdad, pero se replica en el móvil para permitir cálculo offline. El backend expone las fórmulas activas; el móvil las cachea y aplica localmente.

### 2.2. Estructura de una fórmula
Cada fórmula está asociada a un `sport_type` y define los umbrales y multiplicadores de conversión:

| Campo | Significado | Ejemplo |
|-------|-------------|---------|
| `sport_type` | Deporte al que aplica. | `RUNNING`, `CYCLING` |
| `min_bpm` | Frecuencia cardíaca mínima para que el bloque genere XP. | `80` |
| `xp_per_minute` | Puntos otorgados por cada minuto que supere `min_bpm`. | `2` |
| `max_xp_per_block` | Techo de XP por bloque (anti-trampa). | `30` |
| `version` | Versión de la fórmula para invalidar caches obsoletos. | `1` |

### 2.3. Exposición del backend
El backend expone un endpoint protegido (`GET /api/xp/formulas`) que devuelve el catálogo completo de fórmulas activas. El móvil consulta este endpoint:

- En el primer login después de instalación.
- Cuando detecta que el `version` local no coincide con el del backend (encabezado `X-Formula-Version` en respuestas de sync).

### 2.4. Cacheo en el móvil
Las fórmulas se almacenan en SQLite (`local_config` o similar) como un JSON estructurado. El móvil puede calcular XP sin conexión mientras tenga fórmulas cacheadas. Si no tiene fórmulas (primer uso), muestra al usuario un mensaje de que necesita conexión inicial.

### 2.5. Invariantes
- El backend puede modificar fórmulas en cualquier momento (balanceo de juego). El móvil debe detectar el cambio en el próximo sync.
- El móvil **nunca** inventa fórmulas. Si no tiene cache, no calcula XP.
- Las fórmulas son globales (no personalizadas por usuario en MVP).

---

## 3. Subsistema de Cálculo Local

### 3.1. Principio rector
El móvil es la **fuente de cálculo primaria**. Cada vez que el Sistema 2 cierra un bloque, el Sistema 5 recibe las métricas agregadas y computa la recompensa inmediatamente, sin esperar al backend.

### 3.2. Entradas
Para cada bloque cerrado, el Sistema 5 recibe:

- `sport_type`: tipo de actividad seleccionada al iniciar la sesión.
- `avg_bpm`: promedio de frecuencia cardíaca del bloque.
- `duration_seconds`: duración real del bloque.
- `max_bpm`: valor máximo (para validación posterior).

### 3.3. Proceso de cálculo
1. Buscar la fórmula cacheada para el `sport_type`.
2. Verificar que `avg_bpm >= min_bpm`. Si no, `xp_calculated = 0`.
3. Calcular: `xp_calculated = (duration_seconds / 60) * xp_per_minute`.
4. Aplicar `max_xp_per_block` si el resultado lo excede.
5. Redondear al entero inferior (floor).
6. Devolver `xp_calculated` al Sistema 2 para que lo muestre al usuario y lo persista en `local_blocks`.

### 3.4. Recompensa inmediata
El resultado se muestra al usuario en la interfaz de la sesión (toast, animación, o incremento del contador de XP) **antes** de que exista conexión a internet. Esto es el núcleo de la experiencia offline-first.

### 3.5. Qué NO hace el cálculo local
- No consulta al backend para calcular. El backend solo valida después.
- No acumula XP en el perfil del usuario directamente. Eso es responsabilidad del Sistema 10 (Estadísticas de Perfil), que el Sistema 2 notifica.
- No filtra bloques por duración mínima. Eso es responsabilidad del Sistema 2 al cerrar el bloque.

---

## 4. Subsistema de Validación de Sanidad

### 4.1. Principio rector
El backend no recalcula XP desde cero, pero sí valida que la XP que el móvil reportó sea **razonable** dado las métricas del bloque. Actúa como auditor, no como calculador primario.

### 4.2. Entradas
Cuando el Sistema 3 envía un batch, cada bloque incluye:

- `xp_calculated` (reportado por el móvil).
- `avg_bpm`, `max_bpm`, `min_bpm`, `duration_seconds`, `sample_count`.
- `sport_type`.

### 4.3. Reglas de validación
El backend aplica comprobaciones de sanidad:

| Regla | Descripción | Ejemplo de fallo |
|-------|-------------|------------------|
| **Rango fisiológico** | `avg_bpm` debe estar entre 30 y 250. | `avg_bpm = 300` → rechazo. |
| **Consistencia temporal** | `duration_seconds` debe estar entre 270 y 330 (±10% de 300). | `duration = 10` → rechazo. |
| **Consistencia de muestras** | `sample_count` debe ser coherente con `duration_seconds` (ej. no 2 muestras en 300 segundos). | `sample_count = 2, duration = 300` → rechazo. |
| **Consistencia de XP** | `xp_calculated` debe ser igual o menor que lo que la fórmula actual produce para esas métricas. | `xp = 999` para 5 min a 80 bpm → rechazo. |
| **Techo de bloque** | `xp_calculated` no debe exceder `max_xp_per_block`. | `xp = 50` cuando el techo es 30 → rechazo. |

### 4.4. Resultado de validación
- **Pasa:** El backend inserta la transacción XP con el valor recibido. Responde `201`.
- **Falla:** El backend responde `422` indicando qué bloques fallaron y por qué regla. El bloque se marca como `ERROR` en el móvil.

### 4.5. Invariantes
- El backend **nunca** corrige la XP al alza. Si el móvil reportó menos de lo que la fórmula indica, acepta el valor reportado (el usuario puede haber tenido una versión ligeramente desactualizada de la fórmula).
- El backend **nunca** recalcula y reemplaza. Solo valida y acepta/rechaza.

---

## 5. Subsistema de Transacciones XP

### 5.1. Principio rector
Las recompensas de XP son **inmutables**. Una vez validadas, se registran como transacciones en una tabla append-only. No se actualizan ni borran.

### 5.2. Estructura de una transacción
Cada fila en `xp_transactions` representa una recompensa validada:

| Campo | Origen | Significado |
|-------|--------|-------------|
| `id` | UUID generado por backend | Identificador único de la transacción. |
| `user_id` | JWT del request | Usuario beneficiario. |
| `block_id` | Reportado por móvil | Bloque que generó esta XP. |
| `amount` | `xp_calculated` del móvil | Puntos otorgados (puede ser positivo o negativo). |
| `balance_after` | Calculado por backend | Total acumulado del usuario tras esta transacción. Permite consultar total sin `SUM()`. |
| `reason` | Backend | `BLOCK_VALIDATED`, `CORRECTION`, `MANUAL_ADJUST`. |
| `timestamp` | `timestamp_start` del bloque | Momento del esfuerzo, no del procesamiento. |

### 5.3. Ventaja de `balance_after`
Al tener el saldo acumulado en cada transacción, el backend puede consultar el total actual del usuario leyendo la última transacción, sin necesidad de sumar toda la tabla. Esto acelera el ranking y el perfil.

### 5.4. Invariantes
- La tabla `xp_transactions` **nunca** recibe `UPDATE` ni `DELETE`. Solo `INSERT`.
- Si hay un error, se inserta una transacción negativa con `reason = CORRECTION`, no se borra la original.

---

## 6. Subsistema de Corrección y Reversión

### 6.1. Principio rector
Cuando el backend rechaza un bloque (`422`), la XP que el móvil mostró al usuario debe ser revertida para mantener la consistencia entre local y remoto.

### 6.2. Flujo de corrección
1. El backend responde `422` para un bloque específico.
2. El Sistema 3 (Sync) marca el bloque como `ERROR`.
3. El Sistema 3 notifica al Sistema 10 (Estadísticas de Perfil) para que revierta `xp_calculated` de su cache local.
4. El Sistema 5 (o la UI que este coordina) muestra al usuario una corrección visual: "-10 XP (bloque rechazado)".
5. El usuario puede ver el motivo del rechazo en un diálogo de diagnóstico (ej. "Duración inconsistente").

### 6.3. Corrección manual (futuro)
En fases posteriores, un administrador podría insertar manualmente una transacción de corrección positiva o negativa. Esto generaría una nueva fila en `xp_transactions` con `reason = MANUAL_ADJUST`.

### 6.4. Invariantes
- La XP revertida localmente debe coincidir exactamente con la `xp_calculated` del bloque rechazado.
- El usuario siempre tiene visibilidad de por qué un bloque fue rechazado (no es un fallo silencioso).

---

## 7. Flujo Integrado de Datos

```
┌─────────────────────────────────────────────────────────────────────┐
│                         BACKEND (Render)                           │
│  ┌──────────────────┐    ┌──────────────────┐    ┌──────────────────┐  │
│  │ Fórmulas activas │───►│ Validación de    │───►│ Transacciones XP │  │
│  │ (GET /api/xp/    │    │ sanidad (reglas) │    │ (append-only)    │  │
│  │  formulas)        │    │                  │    │ balance_after    │  │
│  └──────────────────┘    └──────────────────┘    └──────────────────┘  │
│         │                          │                          │       │
│         │                          │                          │       │
│         ▼                          ▼                          ▼       │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │  Respuesta de sync: {xp_accepted, new_total, rank_changed}       │  │
│  └──────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────┘
         │                                              ▲
         │ HTTPS                                        │ 201 / 422
         ▼                                              │
┌─────────────────────────────────────────────────────────────────────┐
│                         APK (Móvil)                                  │
│                                                                      │
│  ┌──────────────────┐    ┌──────────────────┐    ┌──────────────────┐  │
│  │ Cache de         │───►│ Cálculo local    │───►│ Recompensa       │  │
│  │ fórmulas (SQLite)│    │ (por bloque)     │    │ inmediata (UI)   │  │
│  └──────────────────┘    └──────────────────┘    └──────────────────┘  │
│         ▲                                              │               │
│         │                                              │               │
│         │         ┌──────────────────┐                 │               │
│         │         │ Sistema 2 (cierra│─────────────────┘               │
│         │         │ bloque, entrega   │                                 │
│         │         │ métricas)         │                                 │
│         │         └──────────────────┘                                 │
│         │                                                              │
│         └──────────────────────────────────────────────────────────────┘
│              Sistema 3 detecta cambio de versión en sync
└─────────────────────────────────────────────────────────────────────┘
```

---

## 8. Decisiones Arquitectónicas Consolidadas (Sistema 5)

| Decisión | Valor | Justificación |
|----------|-------|---------------|
| **Fuente de cálculo primaria** | APK (móvil) | Recompensa inmediata visible offline. El usuario no espera al backend. |
| **Fuente de validación** | Backend | Auditoría anti-trampa. Detecta modificaciones maliciosas de la APK. |
| **Fórmulas** | Backend expone, APK cachea | El backend controla el balanceo del juego. El móvil aplica localmente. |
| **Inmutabilidad de XP** | Tabla `xp_transactions` append-only | Historial completo auditable. Correcciones via transacciones negativas. |
| **Saldo acumulado** | `balance_after` por transacción | Consulta O(1) del total. No requiere `SUM()` en toda la tabla. |
| **Rechazo de bloque** | `422` + reversión local | El usuario ve la corrección. No se acumula XP fantasma. |
| **No recálculo backend** | Validación, no reemplazo | El backend confía en el cálculo del móvil si pasa sanidad. |
| **Techo por bloque** | `max_xp_per_block` | Limita el daño de un solo bloque trampa. |
| **Versión de fórmula** | Header `X-Formula-Version` | Permite invalidar cache del móvil cuando el backend balancea. |

---

## 9. Glosario de Términos (Sistema 5)

| Término | Definición |
|---------|------------|
| **XP (Experience Points)** | Unidad de progresión del usuario, otorgada por esfuerzo físico validado. |
| **Fórmula de XP** | Regla que define cuántos puntos otorga un bloque según su deporte, duración y frecuencia cardíaca. |
| **Cache de fórmulas** | Copia local en SQLite de las reglas activas, descargadas del backend. |
| **Sanidad de XP** | Proceso backend que verifica si la XP reportada es consistente con las métricas del bloque. |
| **Transacción XP** | Registro inmutable en la base de datos de una recompensa otorgada. |
| **balance_after** | Campo en cada transacción que indica el total acumulado del usuario tras esa operación. |
| **Corrección** | Transacción negativa que revierte XP previamente otorgada por error o trampa. |
| **max_xp_per_block** | Techo máximo de recompensa por bloque, definido en la fórmula. |

---

## 10. Interacciones con otros sistemas

| Sistema | Interacción | Dirección |
|---------|-------------|-----------|
| **Sistema 2 — Sesión** | El Sistema 2 cierra un bloque y entrega métricas agregadas para cálculo. | Sistema 2 → Sistema 5 |
| **Sistema 3 — Sync** | El Sistema 3 envía `xp_calculated` al backend. Recibe validación o rechazo. | Sistema 5 ↔ Sistema 3 |
| **Sistema 4 — Auth** | El endpoint de fórmulas requiere JWT válido. | Sistema 4 → Sistema 5 |
| **Sistema 6 — Ranking** | El backend usa `balance_after` para recalcular posiciones. | Sistema 5 → Sistema 6 |
| **Sistema 7 — Racha** | Un bloque validado puede incrementar la racha. | Sistema 5 → Sistema 7 |
| **Sistema 10 — Estadísticas** | El Sistema 5 notifica la XP ganada para actualizar totales locales. | Sistema 5 → Sistema 10 |
