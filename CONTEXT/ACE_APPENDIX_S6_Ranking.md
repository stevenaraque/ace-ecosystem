# Apéndice 6 — Sistema 6: Ranking y Posicionamiento (Especificación Conceptual)

> **Estado:** Aprobado  
> **Versión:** 1.0  
> **Fecha:** 2026-06-02  
> **Depende de:** Documento base A.C.E v0.2 (Sistema 6) · Apéndice 5 (Sistema 5)

---

## 1. Propósito del Apéndice

Este documento descompone el Sistema 6 en sus cuatro subsistemas conceptuales, definiendo **qué hace cada uno, qué entradas recibe, qué salidas produce y qué invariantes debe respetar**. No contiene código fuente; su función es servir como contrato de comportamiento antes de la implementación.

Los cuatro subsistemas son:

1. **Subsistema de Recálculo Periódico** (job backend que ordena usuarios por XP total).
2. **Subsistema de Materialización de Rankings** (tablas denormalizadas para lectura rápida).
3. **Subsistema de Exposición de Rankings** (endpoints REST que sirven posiciones y tops).
4. **Subsistema de Cacheo Local en APK** (almacenamiento de posición propia y top N en el móvil).

---

## 2. Subsistema de Recálculo Periódico

### 2.1. Principio rector
El ranking no se recalcula en tiempo real por cada bloque sincronizado. Eso sería costoso y no necesario para la experiencia de usuario. En su lugar, un **job programado** recalcula todas las posiciones en intervalos regulares.

### 2.2. Frecuencia de recálculo
- **Global:** cada 1 hora.
- **Municipal:** cada 1 hora (mismo job, filtrado por ciudad).

### 2.3. Proceso de recálculo
1. El job consulta `xp_transactions` y agrupa por `user_id`, sumando `amount` (o leyendo el `balance_after` más reciente de cada usuario).
2. Ordena los usuarios por total XP descendente.
3. Asigna posiciones (`position = 1, 2, 3...`) sin gaps (si hay empate, el orden es determinista por `user_id` o timestamp).
4. Para el ranking municipal, repite el proceso filtrando por `city_id` del perfil del usuario.

### 2.4. Trigger adicional
Además del job horario, el recálculo puede dispararse manualmente por un administrador o por un endpoint interno si se detecta una corrección masiva de XP.

### 2.5. Qué NO hace el recálculo
- No notifica a los usuarios cuando cambian de posición. Las notificaciones son responsabilidad del Sistema 8.
- No recalcula on-write. Cada bloque sincronizado no dispara un nuevo ranking.

---

## 3. Subsistema de Materialización de Rankings

### 3.1. Principio rector
Las consultas de ranking son lecturas frecuentes y costosas si se hacen sobre la tabla transaccional. Por eso, el resultado del recálculo se materializa en tablas dedicadas.

### 3.2. Tablas materializadas

| Tabla | Contenido | Clave primaria |
|-------|-----------|----------------|
| `ranking_global` | `user_id`, `position`, `total_xp`, `updated_at` | `position` (con índice) |
| `ranking_municipal` | `user_id`, `city_id`, `position`, `total_xp`, `updated_at` | `(city_id, position)` |

### 3.3. Actualización
El job de recálculo ejecuta una transacción que:
1. Trunca la tabla materializada (o marca todas las filas como obsoletas).
2. Inserta las nuevas posiciones.
3. Confirma con `COMMIT`.

Durante la transacción, las lecturas pueden ver datos parcialmente actualizados. Esto es aceptable porque el ranking es eventualmente consistente.

### 3.4. Invariantes
- Un usuario aparece **exactamente una vez** en `ranking_global`.
- Un usuario aparece **exactamente una vez** por `city_id` en `ranking_municipal`.
- Si un usuario cambia de ciudad, pierde su posición anterior y comienza de cero en la nueva (snapshot en el momento del recálculo).

---

## 4. Subsistema de Exposición de Rankings

### 4.1. Principio rector
El backend expone endpoints REST para que el móvil consulte posiciones. El móvil no mantiene el ranking completo; solo solicita lo que necesita mostrar.

### 4.2. Endpoints

| Endpoint | Qué devuelve | Autenticación |
|----------|--------------|---------------|
| `GET /api/ranking/global` | Posición del usuario autenticado + top 100 global. | Bearer JWT |
| `GET /api/ranking/municipal/{cityId}` | Posición del usuario en esa ciudad + top 100 municipal. | Bearer JWT |

### 4.3. Respuesta típica
```json
{
  "my_position": 42,
  "my_total_xp": 1250,
  "top": [
    {"position": 1, "user_id": "...", "total_xp": 5000, "username": "..."},
    {"position": 2, "user_id": "...", "total_xp": 4800, "username": "..."}
  ],
  "last_updated": "2026-06-02T07:00:00Z"
}
```

### 4.4. Paginación
- El top se limita a 100 posiciones. No hay paginación adicional en MVP.
- Si el usuario está fuera del top 100, `my_position` indica su posición real, pero no se devuelven los usuarios entre el top 100 y él.

### 4.5. Qué NO hace el backend
- No calcula ranking on-demand para cada petición. Lee de la tabla materializada.
- No filtra por amigos ni por grupos personalizados en MVP.

---

## 5. Subsistema de Cacheo Local en APK

### 5.1. Principio rector
El móvil no consulta el ranking en cada apertura de app. Cachea la respuesta del backend para mostrarla offline y reducir peticiones de red.

### 5.2. Estructura del cache
En SQLite (`local_ranking_cache`):

| Campo | Significado |
|-------|-------------|
| `type` | `GLOBAL` o `MUNICIPAL_{cityId}` |
| `my_position` | Posición del usuario en el momento del cache. |
| `my_total_xp` | XP total en el momento del cache. |
| `top_json` | Array serializado del top 10 (no 100; solo guarda lo que muestra). |
| `cached_at` | Timestamp de la última consulta exitosa. |
| `valid_until` | `cached_at + 1 hora`. Después de esto, el cache se considera stale. |

### 5.3. Política de actualización
- Si el usuario abre la pantalla de ranking y el cache tiene < 1 hora, se muestra el cache.
- Si el cache tiene > 1 hora o no existe, el móvil dispara una petición al backend.
- Si no hay conexión y el cache existe (aunque sea stale), se muestra con un indicador de "Datos de hace X horas".

### 5.4. Actualización tras sync
Cuando el Sistema 3 recibe una respuesta de sync con `rank_changed = true`, invalida inmediatamente el cache de ranking para forzar una consulta fresca en la próxima apertura.

### 5.5. Qué NO hace el cache
- No mantiene el ranking completo de todos los usuarios. Solo guarda la posición propia y el top 10.
- No calcula posiciones localmente. Solo refleja lo que el backend dijo.

---

## 6. Flujo Integrado de Datos

```
┌─────────────────────────────────────────────────────────────────────┐
│                         PostgreSQL (Neon)                          │
│  ┌──────────────────┐              ┌──────────────────────────────┐  │
│  │ xp_transactions  │───► SUM ───►│ ranking_global               │  │
│  │ (balance_after)  │              │ (materializada, índice pos)  │  │
│  └──────────────────┘              ├──────────────────────────────┤  │
│                                    │ ranking_municipal            │  │
│                                    │ (por city_id)                │  │
│                                    └──────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────┘
                              │
                              │ Job @Scheduled (cada 1h)
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│                         BACKEND (Render)                           │
│  ┌──────────────────┐    ┌──────────────────┐    ┌──────────────────┐  │
│  │ Recálculo        │───►│ Materialización  │───►│ Endpoints REST   │  │
│  │ (ordenar users)  │    │ (tablas snapshot)│    │ (global / city)  │  │
│  └──────────────────┘    └──────────────────┘    └──────────────────┘  │
└─────────────────────────────────────────────────────────────────────┘
                              │
                              │ HTTPS + JWT
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│                         APK (Móvil)                                  │
│  ┌──────────────────┐    ┌──────────────────┐    ┌──────────────────┐  │
│  │ Cache local      │◄───│ Consulta ranking │◄──│ UI de ranking    │  │
│  │ (SQLite, 1h)     │    │ si stale o vacío │    │ (posición + top) │  │
│  └──────────────────┘    └──────────────────┘    └──────────────────┘  │
│         │                                              ▲               │
│         │ Invalida si rank_changed=true (desde Sistema 3)              │
│         └──────────────────────────────────────────────┘               │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 7. Decisiones Arquitectónicas Consolidadas (Sistema 6)

| Decisión | Valor | Justificación |
|----------|-------|---------------|
| **Recálculo** | Batch cada 1 hora (job programado) | Performance. No recalcular por cada bloque. |
| **Materialización** | Tablas `ranking_global` y `ranking_municipal` | Lecturas O(1) por posición. No sumar transacciones en cada request. |
| **Top devuelto** | 100 posiciones | Suficiente para motivación. MVP no requiere paginación profunda. |
| **Cache del móvil** | Posición propia + top 10, válido 1 hora | Reduce peticiones. Permite ver ranking offline (aunque stale). |
| **Cambio de ciudad** | Pierde posición anterior, empieza de cero | Snapshot simple. No migrar historial municipal. |
| **Empates** | Orden determinista por user_id/timestamp | Evita posiciones compartidas o gaps ambiguos. |
| **Eventualidad** | Hasta 1 hora de delay en reflejar nuevo bloque | Aceptable para la experiencia. El usuario ve su XP inmediatamente, no su posición. |

---

## 8. Glosario de Términos (Sistema 6)

| Término | Definición |
|---------|------------|
| **Ranking global** | Clasificación de todos los usuarios de la plataforma por XP total. |
| **Ranking municipal** | Clasificación de usuarios dentro de una misma ciudad por XP total. |
| **Materialización** | Técnica de almacenar el resultado de una consulta costosa en una tabla dedicada para lecturas rápidas. |
| **Job de recálculo** | Proceso programado que reordena usuarios y actualiza las tablas materializadas. |
| **Stale cache** | Cache que ha superado su tiempo de validez pero aún se muestra por falta de conexión. |
| **rank_changed** | Flag booleano que el backend incluye en respuestas de sync para indicar que el usuario cambió de posición. |

---

## 9. Interacciones con otros sistemas

| Sistema | Interacción | Dirección |
|---------|-------------|-----------|
| **Sistema 3 — Sync** | El backend incluye `rank_changed` en la respuesta de sync. El Sistema 6 invalida el cache si es true. | Sistema 3 → Sistema 6 |
| **Sistema 4 — Auth** | Los endpoints de ranking requieren JWT válido. | Sistema 4 → Sistema 6 |
| **Sistema 5 — XP** | El backend usa `balance_after` de `xp_transactions` para el recálculo. | Sistema 5 → Sistema 6 |
| **Sistema 10 — Estadísticas** | El móvil muestra ranking junto a estadísticas de perfil. | Sistema 6 ↔ Sistema 10 |
| **Sistema 8 — Notificaciones** | Si `rank_changed` es true, el Sistema 8 podría notificar al usuario (futuro). | Sistema 6 → Sistema 8 |
