# Apéndice 10 — Sistema 10: Estadísticas Persistentes de Perfil (Especificación Conceptual)

> **Estado:** Aprobado  
> **Versión:** 1.0  
> **Fecha:** 2026-06-02  
> **Depende de:** Documento base A.C.E v0.2 (Sistema 10) · Apéndice 5 (Sistema 5) · Apéndice 3 (Sistema 3)

---

## 1. Propósito del Apéndice

Este documento descompone el Sistema 10 en sus cuatro subsistemas conceptuales, definiendo **qué hace cada uno, qué entradas recibe, qué salidas produce y qué invariantes debe respetar**. No contiene código fuente; su función es servir como contrato de comportamiento antes de la implementación.

Los cuatro subsistemas son:

1. **Subsistema de Acumulación Local** (actualización inmediata de estadísticas al cerrar un bloque).
2. **Subsistema de Sincronización de Estadísticas** (envío de totales al backend y recepción de correcciones).
3. **Subsistema de Validación de Consistencia** (verificación backend de que los totales reportados son coherentes con los bloques recibidos).
4. **Subsistema de Corrección y Reconciliación** (ajuste de estadísticas locales cuando el backend detecta discrepancias).

---

## 2. Subsistema de Acumulación Local

### 2.1. Principio rector
El móvil mantiene un resumen agregado del progreso del usuario visible **inmediatamente** y **offline**. Cada vez que el Sistema 5 calcula XP para un bloque, el Sistema 10 actualiza sus totales locales.

### 2.2. Entradas
El Sistema 10 recibe del Sistema 2 (al cerrar un bloque):

- `xp_calculated`: XP del bloque recién cerrado.
- `duration_seconds`: duración del bloque.
- `avg_bpm`: promedio de FC del bloque.
- `sample_count`: cantidad de muestras del bloque.
- `is_new_session`: booleano que indica si este bloque es el primero de una nueva sesión.

### 2.3. Métricas acumuladas
En SQLite (`local_user_stats`):

| Campo | Cálculo | Significado |
|-------|---------|-------------|
| `total_xp` | `+= xp_calculated` | XP total acumulada. |
| `total_sessions` | `+= 1` si `is_new_session` | Cantidad de sesiones completadas. |
| `total_blocks` | `+= 1` | Cantidad de bloques cerrados. |
| `total_duration_seconds` | `+= duration_seconds` | Tiempo total de ejercicio. |
| `avg_bpm_all_time` | Recalcular ponderado | Promedio de FC de toda la historia. |
| `last_updated` | `now()` | Timestamp de última actualización local. |

### 2.4. Cálculo de `avg_bpm_all_time`
El promedio ponderado se recalcula como:

```
nuevo_avg = (avg_anterior * total_muestras_anterior + avg_bpm_bloque * sample_count_bloque) 
            / (total_muestras_anterior + sample_count_bloque)
```

Esto evita almacenar todas las muestras individuales.

### 2.5. Qué NO acumula
- No separa por categoría (Running vs Cycling). En MVP las estadísticas son **globales**.
- No acumula máximos ni mínimos históricos (solo promedio).
- No mantiene tendencias ni gráficos temporales.

---

## 3. Subsistema de Sincronización de Estadísticas

### 3.1. Principio rector
El móvil envía sus estadísticas locales al backend en cada batch de sync (Sistema 3). El backend valida la consistencia y responde con los valores oficiales.

### 3.2. Payload de estadísticas
En cada batch de bloques, el móvil incluye un objeto `client_stats`:

```json
{
  "total_xp": 1250,
  "total_sessions": 15,
  "total_blocks": 45,
  "total_duration_seconds": 13500,
  "avg_bpm_all_time": 142.5
}
```

### 3.3. Respuesta del backend
El backend responde con `official_stats`:

```json
{
  "official_total_xp": 1240,
  "official_total_sessions": 15,
  "official_total_blocks": 45,
  "correction_applied": true,
  "correction_reason": "BLOCK_422_REVERTED"
}
```

### 3.4. Actualización local
Si hay discrepancia (`official != local`):
- El móvil sobrescribe sus valores locales con los oficiales.
- Muestra una notificación silenciosa de corrección si la diferencia es significativa.

### 3.5. Invariantes
- El móvil siempre muestra sus valores locales inmediatamente.
- El móvil solo acepta correcciones del backend, nunca al revés.
- La sincronización de estadísticas ocurre en el mismo batch que los bloques, no en una petición separada.

---

## 4. Subsistema de Validación de Consistencia

### 4.1. Principio rector
El backend verifica que los totales que el móvil reporta sean **coherentes** con los bloques que ha recibido y validado. No recalcula desde cero a menos que haya una auditoría explícita.

### 4.2. Reglas de validación
| Regla | Descripción | Acción si falla |
|-------|-------------|-----------------|
| **XP coherente** | `total_xp` reportado ≤ suma de `amount` en `xp_transactions` para ese usuario + bloques en vuelo. | Responder con `official_total_xp` corregido. |
| **Sesiones coherente** | `total_sessions` reportado ≤ cantidad de sesiones distintas en bloques `SYNCED`. | Responder con `official_total_sessions`. |
| **Bloques coherente** | `total_blocks` reportado ≤ cantidad de bloques `SYNCED` + `SYNCING`. | Responder con `official_total_blocks`. |
| **Duración coherente** | `total_duration_seconds` reportado ≤ suma de `duration_seconds` en bloques `SYNCED`. | Responder con corrección. |

### 4.3. Tolerancia
El backend permite una pequeña discrepancia (ej. bloques en vuelo que aún no fueron confirmados). Solo corrige si la diferencia es mayor al margen razonable.

### 4.4. Invariantes
- El backend **nunca** genera estadísticas desde cero en cada sync. Solo valida contra lo que ya tiene.
- Si el móvil reporta menos de lo que el backend tiene (ej. por reinstalación), el backend corrige al alza.

---

## 5. Subsistema de Corrección y Reconciliación

### 5.1. Principio rector
Cuando el backend detecta una inconsistencia (ej. un bloque fue rechazado con `422`), las estadísticas locales del móvil deben ajustarse para reflejar la realidad del servidor.

### 5.2. Escenarios de corrección

| Escenario | Causa | Acción del móvil |
|-----------|-------|------------------|
| **Bloque rechazado (`422`)** | XP inválida detectada por backend. | Revertir `total_xp` en `local_user_stats`. |
| **Reinstalación de APK** | El móvil pierde todas sus estadísticas locales. | Al primer login, descargar `official_stats` del backend. |
| **Divergencia por fórmula desactualizada** | El móvil calculó con fórmula vieja; backend validó con nueva. | Aceptar `official_total_xp` del backend. |
| **Bloque duplicado enviado** | Idempotencia del backend ignoró duplicado, pero móvil sumó 2 veces. | Ajustar al valor oficial. |

### 5.3. Experiencia de usuario
- Las correcciones son **silenciosas** si la diferencia es menor a 10 XP.
- Si la diferencia es mayor, se muestra un toast: "Tus estadísticas han sido actualizadas desde el servidor."
- El usuario siempre puede ver la fecha de última sincronización en la UI de perfil.

### 5.4. Invariantes
- La corrección siempre prima el valor del backend.
- El móvil nunca rechaza una corrección del backend.
- Las correcciones no generan transacciones XP negativas (eso es responsabilidad del Sistema 5). Aquí solo se ajustan los totales acumulados.

---

## 6. Flujo Integrado de Datos

```
┌─────────────────────────────────────────────────────────────────────┐
│                         Sistema 2 — Sesión                           │
│  ┌─────────────────────────────────────────────────────────────────┐  │
│  │  Cierra bloque → Sistema 5 calcula XP                           │  │
│  │  → Notifica a Sistema 10: {xp, duration, avg_bpm, samples}      │  │
│  └─────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│                         Sistema 10 — Acumulación                     │
│  ┌──────────────────┐    ┌──────────────────┐    ┌──────────────────┐  │
│  │ local_user_stats │◄───│ Actualiza totales│◄───│ UI de perfil     │  │
│  │ (total_xp, etc.) │    │ (offline, inmed.)│    │ (muestra progreso)│  │
│  └──────────────────┘    └──────────────────┘    └──────────────────┘  │
│         │                                              ▲               │
│         │                                              │               │
│         │ Sistema 3 envía en batch                     │               │
│         ▼                                              │               │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │  client_stats: {total_xp, total_sessions, total_blocks, ...}     │  │
│  └──────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────┘
                              │
                              │ HTTPS + JWT
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│                         BACKEND (Render)                           │
│  ┌──────────────────┐    ┌──────────────────┐    ┌──────────────────┐  │
│  │ Validar bloques  │───►│ Validar consist. │───►│ Comparar con     │  │
│  │ (Sistema 5)      │    │ de client_stats  │    │ official_stats   │  │
│  └──────────────────┘    └──────────────────┘    └──────────────────┘  │
│                              │                    │                 │
│                              │ discrepancia?      │                 │
│                              ▼                    ▼                 │
│                        ┌──────────────┐    ┌──────────────┐        │
│                        │ Responder con│    │ Responder con│        │
│                        │ official_    │    │ client_stats │        │
│                        │ stats (corr.)│    │ confirmados  │        │
│                        └──────────────┘    └──────────────┘        │
└─────────────────────────────────────────────────────────────────────┘
                              │
                              │ Respuesta
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│                         Sistema 10 — Reconciliación                  │
│  ┌─────────────────────────────────────────────────────────────────┐  │
│  │  Si official != local: sobrescribir local, notificar si dif > 10 │  │
│  └─────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 7. Decisiones Arquitectónicas Consolidadas (Sistema 10)

| Decisión | Valor | Justificación |
|----------|-------|---------------|
| **Fuente de verdad final** | Backend | El móvil puede estar desactualizado o manipulado. |
| **Fuente de verdad temporal** | Móvil (local) | El usuario ve progreso inmediatamente, sin esperar sync. |
| **Estadísticas globales** | No separar por categoría en MVP | Simplicidad. En fases futuras se pueden agregar filtros por `sport_type`. |
| **Sincronización** | Mismo batch que bloques | Reduce peticiones de red. Las estadísticas viajan con los datos. |
| **Corrección** | Silenciosa si dif < 10 XP; notificada si es mayor | No molestar al usuario por discrepancias menores. |
| **Promedio de FC** | Ponderado por cantidad de muestras | Precisión sin almacenar todas las muestras. |
| **Reinstalación** | Descargar official_stats al login | Recuperación automática del progreso. |
| **No recálculo backend** | Validación, no regeneración desde cero | Performance. Solo se recalcula si hay auditoría explícita. |

---

## 8. Glosario de Términos (Sistema 10)

| Término | Definición |
|---------|------------|
| **Estadísticas de perfil** | Datos agregados del progreso del usuario: total XP, sesiones, duración, FC promedio. |
| **client_stats** | Objeto que el móvil envía al backend con sus totales locales. |
| **official_stats** | Objeto que el backend responde con los totales validados y corregidos. |
| **Reconciliación** | Proceso de ajustar estadísticas locales para que coincidan con las del backend. |
| **Consistencia** | Propiedad de que los totales reportados sean coherentes con los bloques validados. |
| **Promedio ponderado** | Cálculo de promedio que considera el tamaño de cada muestra (cantidad de heartbeats por bloque). |

---

## 9. Interacciones con otros sistemas

| Sistema | Interacción | Dirección |
|---------|-------------|-----------|
| **Sistema 2 — Sesión** | Notifica al Sistema 10 cuando cierra un bloque con métricas y XP. | Sistema 2 → Sistema 10 |
| **Sistema 3 — Sync** | Envía `client_stats` en el batch y recibe `official_stats` en la respuesta. | Sistema 10 ↔ Sistema 3 |
| **Sistema 5 — XP** | El Sistema 10 recibe `xp_calculated` del Sistema 5 para acumular. | Sistema 5 → Sistema 10 |
| **Sistema 6 — Ranking** | El ranking usa `total_xp` de las estadísticas para posicionar. | Sistema 10 → Sistema 6 |
| **Sistema 7 — Racha** | La racha se muestra junto a las estadísticas en el perfil. | Sistema 7 ↔ Sistema 10 |
| **Sistema 9 — Historial** | El perfil muestra tanto estadísticas agregadas como historial de sesiones. | Sistema 9 ↔ Sistema 10 |
