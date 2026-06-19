# A.C.E — Interacción Sistemas × Diseño de Interfaz (Integración de Contexto)

> **Estado:** Integrado al sistema de contexto del proyecto A.C.E  
> **Versión:** 1.0  
> **Fecha:** 2026-06-12  
> **Depende de:** Apéndices S1-S10 aprobados, Arquitectura v0.2, Planes de Implementación v4.1/v4.2  
> **Próposito:** Documentar cómo cada subsistema funcional (S1-S10) se materializa en las pantallas de la APK, sirviendo como puente entre la arquitectura backend y la experiencia de usuario.

---

## 1. Visión Integrada

Este documento no redefine los sistemas. Los **mapea** al diseño de interfaz validado en la sesión de arquitectura gráfica del 2026-06-12. Cada pantalla del prototipo HTML es el **punto de convergencia** de múltiples sistemas, y cada sistema es el **motor invisible** que alimenta esas pantallas.

**Principio rector:** *La interfaz no es decoración; es la manifestación visible de los contratos de comportamiento definidos en los apéndices.*

---

## 2. Mapeo Pantalla × Sistema

### 2.1 Pantalla: Login (`01_login.html`)

| Sistema | Rol | Contrato del Apéndice | Manifestación Visual |
|---------|-----|----------------------|---------------------|
| **S4 — Auth** | **Dominante** | S4 §2: Emisión del par JWT (access 15min, refresh 7días). S4 §3: Almacenamiento en `local_user` (Room). | Formulario de email/contraseña. Botón "Ingresar" dispara `POST /api/auth/login`. |
| **S4 — Auth** | Almacenamiento | S4 §3.2: Tabla `local_user` guarda `access_token`, `refresh_token`, `token_expires_at`, `device_id`. | Si tokens existen y son válidos, salta a Home (silent auth). |
| **S10 — Stats** | Recuperación | S10 §5.2: Si es reinstalación, descarga `official_stats` al primer login. | Transición invisible; usuario ve sus stats correctos en Home. |

**Flujo integrado:**
```
Usuario ingresa credenciales
    → S4 valida BCrypt en backend
    → S4 emite par JWT
    → S4 guarda en Room (local_user)
    → S5 descarga fórmulas si no cache (bloqueo en Ejercicio si falla)
    → S10 carga official_stats si reinstalación
    → Navega a Home
```

**Decisiones de diseño:**
- Sin pestaña de registro visible en login principal (pantalla secundaria accesible vía "Crear cuenta").
- No se muestra "Recordarme": el refresh token de 7 días ya cumple esa función.

---

### 2.2 Pantalla: Home / Principal (`02_home.html`)

| Sistema | Rol | Contrato del Apéndice | Manifestación Visual |
|---------|-----|----------------------|---------------------|
| **S4 — Auth** | Silencioso | S4 §5.2: `AuthInterceptor` verifica `token_expires_at` antes de cada petición. Refresh automático si expiró. | Sin UI visible. Si refresh falla, fuerza logout. |
| **S7 — Streak** | **Banner** | S7 §5.2: Cache de `current_streak`, `best_streak`, `last_exercise_date` desde respuesta de sync. | Banner rojo "🔥 Racha de X días" arriba. |
| **S6 — Ranking** | Resumen | S6 §5.2: Cache `local_ranking_cache` con TTL 1h. Solo guarda posición propia + top 10. | Número de posición global resumido (opcional, no obligatorio). |
| **S10 — Stats** | Resumen | S10 §2.3: `local_user_stats` con totales acumulados. | Quick stats: XP total, posición global, sesiones completadas. |
| **S3 — Sync** | Badge condicional | S3 §2.4: Estados `PENDING`, `SYNCING`, `SYNCED`, `ERROR`. | Indicador sutil si hay bloques sin sync. |
| **S8 — Notificaciones** | Banner condicional | S8 §4: `SyncErrorWorker` dispara notificación tras 5 reintentos o `422`. | Banner amarillo "X bloques sin sincronizar" si hay errores activos. |

**Flujo integrado:**
```
Home se abre
    → Lee S7 cache (racha)
    → Lee S6 cache (posición, si < 1h)
    → Lee S10 local_user_stats (totales)
    → Verifica S3 si hay bloques ERROR/PENDING
    → Si hay errores, muestra banner S8
    → S4 verifica token silenciosamente
```

**Decisiones de diseño:**
- Racha solo en Home (no en otras pantallas) para no saturar.
- Diagnóstico sync es banner en Home, detalle completo en Ejercicio.
- Sin peticiones de red en Home (todo cache local).

---

### 2.3 Pantalla: Ejercicio — Selección (`03_ejercicio.html`)

| Sistema | Rol | Contrato del Apéndice | Manifestación Visual |
|---------|-----|----------------------|---------------------|
| **S1 — Captura** | **Verificación previa** | S1 §4.3: Timeout 5s sin muestra = desconexión inferida. | Botón "Verificar conexión" con Wear OS. Estado: conectado/desconectado. |
| **S2 — Sesión** | Preparación | S2 §2.2: `sport_type` seleccionado al inicio. S2 §3.3: Bloque se cierra a ~300s. | BottomSheet con grid de deportes. Running/Cycling/HIIT/etc. |
| **S5 — XP** | **Bloqueo condicional** | S5 §2.4: Si no hay fórmulas cacheadas, no calcula XP. | Botón "Iniciar" deshabilitado si no hay fórmulas. Mensaje: "Conecta a internet para descargar fórmulas". |
| **S3 — Sync** | Diagnóstico detallado | S3 §6.3: Respuesta `422` indica qué bloques fallaron y por qué. | Lista expandible de bloques con error antes de iniciar sesión. |
| **S8 — Notificaciones** | Canal sync_error | S8 §5.2: Canal `ace_sync_error` con importancia alta. | Badge en botón si hay errores no vistos. |

**Flujo integrado:**
```
Abre Ejercicio
    → S1 verifica conexión Wear OS (DataClient)
    → Si desconectado, botón Iniciar deshabilitado
    → S5 verifica fórmulas cacheadas en Room
    → Si no hay fórmulas, bloquea Iniciar
    → S3 verifica bloques ERROR/PENDING
    → Si hay errores, muestra lista detallada
    → Usuario selecciona sport_type (S2)
    → Toca Iniciar → Crea sesión ACTIVE (S2)
```

**Decisiones de diseño:**
- Verificación Wear OS es **prerrequisito explícito**, no automático. El usuario debe confirmar que el reloj está listo.
- BottomSheet para selección de deporte: rápido, no interrumpe flujo.
- Fórmulas son gate: sin ellas, no hay sesión. Coherente con S5 §2.4.

---

### 2.4 Pantalla: Sesión Activa (`04_sesion_activa.html`)

| Sistema | Rol | Contrato del Apéndice | Manifestación Visual |
|---------|-----|----------------------|---------------------|
| **S1 — Captura** | **Datos en vivo** | S1 §3.2: FC vía Health Services `HEART_RATE_BPM` a 1Hz. S1 §5: Buffer circular RAM 300. | FC grande y animada (145 BPM). Indicador de conexión (verde/amarillo). |
| **S2 — Sesión** | **Máquina de estados** | S2 §2.2: Estados `ACTIVE`/`PAUSED`/`COMPLETED`/`ABORTED`. | Timer de sesión. Timer de bloque actual (03:15 / 05:00). |
| **S2 — Sesión** | Espejo del bloque | S2 §3.3: Bloque se cierra a ~300s (±10%). S2 §4: Métricas agregadas al cerrar. | Bloque actual: avg BPM acumulado, max BPM, muestras, progreso visual. **Sin XP** (bloque abierto). |
| **S5 — XP** | Cálculo por bloque | S5 §3.3: `xp_calculated = (duration/60) * xp_per_minute`, techo `max_xp_per_block`. | Al cerrar bloque, aparece "+10 XP" en lista de bloques completados. |
| **S8 — Notificaciones** | **Foreground service** | S8 §2: `ExerciseSyncService` tipo `health`. Notificación persistente obligatoria. | Chip flotante "A.C.E — Sesión activa". Notificación sistema no descartable. |
| **S10 — Stats** | Acumulación inmediata | S10 §2.3: `total_xp += xp_calculated`, `total_blocks += 1`, promedio ponderado. | Totales actualizados en background (no visibles en esta pantalla, pero persistidos). |

**Flujo integrado:**
```
Sesión ACTIVE
    → S1 recibe FC cada 1s → buffer circular RAM
    → S2 cuenta tiempo de bloque
    → Cuando bloque alcanza ~300s:
        → S2 cierra bloque (drain buffer)
        → S2 agrega métricas (avg, max, min, count)
        → S5 calcula XP con fórmula cacheada
        → S10 acumula totales
        → UI muestra "+X XP" y bloque en lista completados
        → S2 abre nuevo bloque automáticamente
    → Usuario puede Pausar (S2: ACTIVE→PAUSED) o Terminar (S2→COMPLETED)
```

**Decisiones de diseño:**
- **XP se calcula por bloque** (S5 §3), pero se **muestra** en lista de completados, no en el bloque actual.
- Bloque actual es "espejo": métricas en progreso, no finales.
- Foreground service obligatorio en Android 10+ (S8 §2.1). El usuario no puede evitar la notificación.
- Si app muere con bloque OPEN, se pierde (aceptable por diseño, S2 §5.3).

---

### 2.5 Pantalla: Resumen Post-Sesión (`05_resumen_sesion.html`)

| Sistema | Rol | Contrato del Apéndice | Manifestación Visual |
|---------|-----|----------------------|---------------------|
| **S2 — Sesión** | Cierre definitivo | S2 §2.2: `COMPLETED` = inmutable. `timestamp_end` fijado. | "¡Sesión completada!" + tipo deporte + duración total. |
| **S5 — XP** | Suma total | S5 §3.3: Suma de `xp_calculated` de todos los bloques. | XP total grande y celebratoria (ej: "30 XP"). |
| **S5 — XP** | Desglose | S5 §3: Cada bloque con su XP individual. | Lista de bloques: duración, avg BPM, XP ganada por cada uno. |
| **S7 — Streak** | Display | S7 §5.2: `current_streak` desde cache (actualizado por sync). | "🔥 Racha actualizada: X días". **Nota:** backend evalúa, móvil solo muestra. |
| **S9 — Historial** | Persistencia | S9 §3: FIFO 5 sesiones en `local_session_history`. | Guarda sesión automáticamente. No visible en esta pantalla, pero se ejecuta. |
| **S10 — Stats** | Totales finales | S10 §2.3: `total_sessions += 1`, `total_xp` final, duración total. | Stats de sesión: duración, avg BPM, bloques, muestras. |
| **S3 — Sync** | Disparo background | S3 §2: Bloques pasan a `PENDING` → WorkManager inicia sync. | Sin UI visible. Sync ocurre en background post-resumen. |

**Flujo integrado:**
```
Usuario toca Terminar
    → S2: ACTIVE → COMPLETED
    → S2 cierra bloque abierto (aunque sea corto)
    → S5 calcula XP del bloque final
    → S10 acumula totales finales
    → S9 inserta sesión en historial FIFO 5
    → S3 encola bloques para sync (WorkManager)
    → UI muestra resumen celebratorio
    → Usuario toca "Volver al inicio" → Home
```

**Decisiones de diseño:**
- **XP mostrada es local**, no espera respuesta de backend. Corrección silenciosa si dif < 10 XP (S10 §5.3).
- Racha mostrada es cache; el backend la evaluará al syncar el primer bloque.
- Animación celebratoria (sparks) refuerza el loop de recompensa.

---

### 2.6 Pantalla: Ranking (`06_ranking.html`)

| Sistema | Rol | Contrato del Apéndice | Manifestación Visual |
|---------|-----|----------------------|---------------------|
| **S6 — Ranking** | **Dominante** | S6 §4.2: Endpoints `GET /api/ranking/global` y `/municipal/{cityId}`. | Tabs Global / Municipal. Top 10 lista. Posición propia destacada. |
| **S6 — Ranking** | Cache local | S6 §5.2: `local_ranking_cache`: posición propia + top 10. TTL 1h. | Si cache < 1h, muestra inmediatamente. Si stale, indicador "Actualizado hace X min". |
| **S4 — Auth** | Requerido | S4 §4.2: JWT en header `Authorization: Bearer`. | Si 401, S4 refresh automático. Si refresh falla, logout. |
| **S3 — Sync** | Invalidación | S6 §5.4: Si `rank_changed = true` en respuesta de sync, invalida cache. | Forzará refresh en próxima apertura de Ranking. |

**Flujo integrado:**
```
Abre Ranking
    → Lee S6 cache local
    → Si cache válido (< 1h), muestra inmediatamente
    → Si cache stale o vacío:
        → Petición S6 con JWT S4
        → Backend responde: my_position + top 100
        → Guarda en cache (solo top 10 + posición propia)
        → Renderiza
```

**Decisiones de diseño:**
- Top 100 devuelto por backend, pero móvil solo cachea top 10 (S6 §5.2).
- Si usuario está fuera del top 100, `my_position` muestra número real pero no los usuarios intermedios.
- Cambio de ciudad no permitido en MVP (S6 §3.4). City_id fijado en registro.

---

### 2.7 Pantalla: Estadísticas (`07_estadisticas.html`)

| Sistema | Rol | Contrato del Apéndice | Manifestación Visual |
|---------|-----|----------------------|---------------------|
| **S10 — Stats** | **Dominante** | S10 §2.3: `local_user_stats` con totales. S10 §3: `client_stats` enviado en batch. | Grid de stats: XP total, sesiones, duración, avg BPM. |
| **S10 — Stats** | Reconciliación | S10 §5: Si `official_stats != local`, sobrescribe. Corrección silenciosa o notificada. | "Sincronizado hace X minutos". Toast si corrección > 10 XP. |
| **S9 — Historial** | Sub-sección | S9 §3: `local_session_history` FIFO 5. Sin discriminar categoría. | Lista de últimas 5 sesiones: fecha, tipo, duración, XP, avg BPM. |
| **S7 — Streak** | Contexto | S7 §5.2: `current_streak`, `best_streak` desde cache. | Tarjeta de racha para contexto histórico. |
| **S3 — Sync** | Estado | S3 §6.2: Respuesta 201 incluye `official_stats`. | Timestamp de última sincronización exitosa. |

**Flujo integrado:**
```
Abre Estadísticas
    → Lee S10 local_user_stats (inmediato, offline)
    → Lee S9 local_session_history (FIFO 5)
    → Lee S7 cache de racha
    → Muestra todo inmediatamente
    → En próximo batch S3:
        → Enviará client_stats
        → Recibirá official_stats
        → Si discrepancia, ajusta local y notifica si es grande
```

**Decisiones de diseño:**
- Historial dentro de Estadísticas (no pantalla propia) porque S9 es "vista rápida" (S9 §2.1).
- Sin filtros por deporte en MVP (S9 §3.4).
- Stats son globales, no por categoría (S10 §2.5).

---

### 2.8 Pantalla: Perfil — Side Menu (`08_perfil.html`)

| Sistema | Rol | Contrato del Apéndice | Manifestación Visual |
|---------|-----|----------------------|---------------------|
| **S4 — Auth** | **Logout** | S4 §7.1: `POST /api/auth/logout` revoca refresh. Limpia `local_user`. | Botón "Cerrar sesión" en menu lateral. |
| **S4 — Auth** | Perfil básico | S4 §3.2: `local_user` guarda tokens y `device_id`. | Nickname, email, ciudad. No editable en MVP. |
| **S6 — Ranking** | Ciudad fija | S6 §3.4: `city_id` inmutable en MVP. | Muestra ciudad seleccionada en registro. |
| **S8 — Notificaciones** | Preferencias | S8 §5.2: 3 canales con importancias distintas. | Toggle por canal: sesión activa (baja), racha (alta), sync error (alta). |
| **S1 — Captura** | Reloj emparejado | S1 §2.1: DataClient paths. | Estado de conexión Wear OS. Opción re-verificar. |

**Flujo integrado:**
```
Abre Side Menu (desde Home)
    → Lee datos S4 (nickname, email)
    → Lee ciudad S6
    → Muestra estado S1
    → Usuario toca Logout
        → S4 envía logout al backend
        → Backend revoca refresh token
        → S4 limpia Room (tokens, device_id)
        → Navega a Login
```

**Decisiones de diseño:**
- Side menu (no pantalla completa) porque logout debe ser accesible desde cualquier lugar.
- Perfil no editable en MVP. Solo visualización.
- Ciudad no se cambia: implicaría perder posición municipal (S6 §3.4).

---

### 2.9 Pantalla: Registro (`09_registro.html`)

| Sistema | Rol | Contrato del Apéndice | Manifestación Visual |
|---------|-----|----------------------|---------------------|
| **S4 — Auth** | **Dominante** | S4 §2.1: Registro con BCrypt. Emite par JWT igual que login. | Formulario: email, nickname, password, ciudad. |
| **S4 — Auth** | Emisión JWT | S4 §2.3: Access 15min + Refresh 7días. Persiste hash de refresh. | Post-registro, usuario está logueado automáticamente. |
| **S6 — Ranking** | Ciudad inicial | S6 §3.4: `city_id` define ranking municipal de por vida en MVP. | Selector de ciudad obligatorio. Bogotá/Medellín/Cali/Barranquilla. |
| **S5 — XP** | Descarga inicial | S5 §2.3: `GET /api/xp/formulas` con header `X-Formula-Version`. | Post-registro, descarga fórmulas automáticamente. Si falla, bloquea Ejercicio. |
| **S10 — Stats** | Inicialización | S10 §2.3: `local_user_stats` creado con ceros. | Stats empiezan en 0. Primer sync establecerá baseline. |

**Flujo integrado:**
```
Usuario completa registro
    → S4: POST /api/auth/register
    → Backend: BCrypt hash, crea usuario, emite JWT
    → S4 guarda tokens en Room
    → S5 descarga fórmulas (obligatorio para Ejercicio)
    → S10 inicializa stats en 0
    → Navega a Home
```

**Decisiones de diseño:**
- Registro es pantalla secundaria (accesible desde Login), no flujo principal.
- Ciudad es obligatoria y permanente: afecta S6 de por vida.
- Post-registro automáticamente logueado (no requiere login separado).

---

### 2.10 Pantalla: Diagnóstico Sync (`10_diagnostico.html`)

| Sistema | Rol | Contrato del Apéndice | Manifestación Visual |
|---------|-----|----------------------|---------------------|
| **S3 — Sync** | **Dominante** | S3 §2.4: Estados `ERROR` (422 o 5 reintentos agotados). | Lista de bloques con error: ID, duración, BPM, XP, motivo. |
| **S5 — XP** | Reversión | S5 §6: Si bloque 422, revierte `xp_calculated` local. | Muestra XP que será revertida. Motivo del rechazo (ej. "XP inconsistente"). |
| **S8 — Notificaciones** | Canal sync_error | S8 §4: Notificación tras 5 reintentos o `422`. | Notificación que llevó a esta pantalla se descarta al abrir. |
| **S10 — Stats** | Corrección | S10 §5.3: Si se descarta bloque, ajusta `total_xp`. Toast si dif > 10 XP. | "Tus estadísticas han sido actualizadas" (solo si corrección grande). |
| **S3 — Sync** | Acciones | S3 §5: WorkManager reintenta con backoff. | Botones: "Reintentar" (fuerza sync), "Descartar" (marca visto), "Sincronizar todo". |

**Flujo integrado:**
```
Usuario abre Diagnóstico (desde banner Home o Ejercicio)
    → Lee S3: lista bloques ERROR
    → Para cada bloque:
        → Si 422: muestra motivo S5, permite descartar
        → Si timeout: muestra reintentos agotados, permite reintentar manual
    → Usuario toma acción:
        → Reintentar: S3 fuerza WorkManager
        → Descartar: S3 marca visto, S10 ajusta stats si 422
        → Sincronizar todo: fuerza batch de todos PENDING
```

**Decisiones de diseño:**
- Pantalla completa (no modal): el usuario necesita ver detalles y tomar decisiones.
- Bloques 422 no se reintentan automáticamente (error permanente, S3 §5.4).
- Corrección silenciosa si < 10 XP, toast si es mayor (S10 §5.3).

---

## 3. Matriz de Interacción Cruzada

### 3.1 Qué sistema dispara qué sistema (desde la UI)

| Evento de UI | Sistema Origen | Sistemas Destino | Razón |
|-------------|----------------|------------------|-------|
| Login exitoso | S4 | S5, S10 | Descarga fórmulas + carga stats oficiales |
| Toca "Iniciar" sesión | S2 | S1, S5 | Verifica Wear OS + valida fórmulas |
| Bloque se cierra (~5min) | S2 | S5, S10 | Calcula XP + acumula stats |
| Toca "Terminar" | S2 | S5, S9, S10, S3 | XP final + historial + stats + encola sync |
| Sync batch exitoso (201) | S3 | S6, S7, S10 | Actualiza ranking, racha, stats oficiales |
| Sync batch rechazado (422) | S3 | S5, S8, S10 | Revierte XP, notifica error, corrige stats |
| Reintentos agotados | S3 | S8 | Notificación sync_error |
| `rank_changed = true` | S3 | S6 | Invalida cache de ranking |
| Toca "Cerrar sesión" | S4 | — | Revoca refresh, limpia todo |
| Cierra app con sesión ACTIVE | S2 | S1 | Buffer RAM se pierde (aceptable) |

### 3.2 Estados de UI condicionales por sistema

| Condición del Sistema | Estado de UI | Pantalla afectada |
|----------------------|-------------|-------------------|
| S1: Wear OS desconectado (>5s sin muestra) | Indicador amarillo, botón Iniciar deshabilitado | Ejercicio, Sesión Activa |
| S1: Wear OS conectado | Indicador verde pulsante | Ejercicio, Sesión Activa |
| S3: Bloques en ERROR | Banner amarillo en Home, detalle en Ejercicio | Home, Ejercicio |
| S3: Sync en progreso | Spinner sutil en Estadísticas | Estadísticas |
| S4: Token expirado | Refresh silencioso (sin UI) | Todas |
| S4: Refresh fallado | Forzar logout, redirigir a Login | Todas |
| S5: Sin fórmulas cacheadas | Botón Iniciar deshabilitado, mensaje de conexión | Ejercicio |
| S6: Cache stale (>1h) | Indicador "Actualizado hace X min" | Ranking |
| S7: `last_exercise_date != hoy` | Banner racha en peligro (solo en Home) | Home |
| S8: Sesión ACTIVE | Notificación persistente no descartable | Sistema (notificación) |
| S10: Corrección > 10 XP | Toast "Estadísticas actualizadas desde servidor" | Estadísticas |

---

## 4. Glosario de Términos (UI × Sistemas)

| Término UI | Sistema | Definición en contexto de interfaz |
|-----------|---------|-----------------------------------|
| **Banner racha** | S7 | Indicador visual en Home que muestra `current_streak`. Se actualiza por respuesta de sync. |
| **Banner sync** | S3/S8 | Indicador condicional que aparece cuando hay bloques `ERROR`. Navega a Diagnóstico. |
| **Espejo del bloque** | S2 | Visualización del bloque en progreso: métricas acumuladas pero XP no calculada aún. |
| **BottomSheet deporte** | S2 | Selector de `sport_type` que define la fórmula de XP a aplicar. |
| **Chip foreground** | S8 | Indicador flotante que refleja la notificación persistente del sistema. |
| **Quick stats** | S10 | Resumen numérico en Home: XP total, posición global, sesiones. Lee `local_user_stats`. |
| **Verificación Wear OS** | S1 | Botón explícito que prueba conectividad con reloj antes de permitir iniciar sesión. |

---

## 5. Decisiones Arquitectónicas de Diseño Consolidadas

| Decisión | Sistemas | Justificación |
|----------|----------|---------------|
| **Racha solo en Home** | S7 | Evita saturación visual. Es motivación primaria al abrir app. |
| **Diagnóstico: banner en Home + detalle en Ejercicio** | S3, S8 | No interrumpe flujo principal. Escalado progresivo de información. |
| **XP por bloque, mostrada en lista** | S2, S5 | Cálculo inmediato (apéndice), visualización progresiva (diseño). |
| **Resumen XP solo local** | S5, S10 | Recompensa instantánea. Corrección silenciosa posterior si dif < 10 XP. |
| **Registro incluye ciudad** | S4, S6 | City_id inmutable en MVP. Define ranking municipal de por vida. |
| **Perfil como side menu** | S4 | Logout accesible globalmente. Espacio para futuras funcionalidades. |
| **Historial dentro de Estadísticas** | S9, S10 | S9 es "vista rápida", no merece tab propio. |
| **Sin filtros por deporte** | S9, S10 | MVP simplificado. Stats globales, historial mezclado. |
| **Fórmulas como gate** | S5 | Sin fórmulas cacheadas, no hay sesión. Coherente con S5 §2.4. |
| **Verificación Wear OS explícita** | S1 | El usuario confirma que el reloj está listo. Reduce errores de soporte. |

---

## 6. Referencias Cruzadas

| Sección | Documento fuente |
|---------|-----------------|
| S1 — Captura de Sensor | `ACE_APPENDIX_S1_Capture_Sensor.md` |
| S2 — Sesión de Ejercicio | `ACE_APPENDIX_S2_Session.md` |
| S3 — Sincronización Offline-First | `ACE_APPENDIX_S3_Sync.md` |
| S4 — Autenticación JWT Híbrida | `ACE_APPENDIX_S4_Auth.md` |
| S5 — Cálculo de XP | `ACE_APPENDIX_S5_XP.md` |
| S6 — Ranking | `ACE_APPENDIX_S6_Ranking.md` |
| S7 — Racha (Streaks) | `ACE_APPENDIX_S7_Streaks.md` |
| S8 — Notificaciones | `ACE_APPENDIX_S8_Notifications.md` |
| S9 — Historial | `ACE_APPENDIX_S9_History.md` |
| S10 — Estadísticas | `ACE_APPENDIX_S10_Profile_Stats.md` |
| Plan Backend | `IMPLEMENTATION_PLAN_BACKEND_v4.1.md` |
| Plan Mobile | `IMPLEMENTATION_PLAN_MOBILE_v4.1.md` |
| Plan Shared | `IMPLEMENTATION_PLAN_SHARED_v4.2.md` |
| Plan Wear OS | `IMPLEMENTATION_PLAN_WEAROS_v4.1.md` |

---

*Documento integrado al sistema de contexto de A.C.E. Cualquier modificación en los apéndices S1-S10 debe reflejarse en este mapeo para mantener coherencia entre arquitectura y diseño.*
