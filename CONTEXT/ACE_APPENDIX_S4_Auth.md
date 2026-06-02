# Apéndice 4 — Sistema 4: Autenticación JWT Híbrida (Especificación Conceptual)

> **Estado:** Aprobado  
> **Versión:** 1.0  
> **Fecha:** 2026-06-02  
> **Depende de:** Documento base A.C.E v0.2 (Sistema 4)

---

## 1. Propósito del Apéndice

Este documento descompone el Sistema 4 en sus seis subsistemas conceptuales, definiendo **qué hace cada uno, qué entradas recibe, qué salidas produce y qué invariantes debe respetar**. No contiene código fuente; su función es servir como contrato de comportamiento antes de la implementación.

Los seis subsistemas son:

1. **Subsistema de Emisión** (generación del par de tokens en el backend).
2. **Subsistema de Almacenamiento Local** (persistencia segura de tokens en la APK).
3. **Subsistema de Transporte Seguro** (inyección del token de acceso en cada petición).
4. **Subsistema de Renovación** (sustitución del access token expirado mediante refresh).
5. **Subsistema de Control de Race Conditions** (serialización de refreshes y prevención de reutilización).
6. **Subsistema de Revocación y Logout** (invalidación de tokens y limpieza de estado).

---

## 2. Subsistema de Emisión

### 2.1. Principio rector
El backend es la única entidad que puede emitir tokens. La autenticación inicial ocurre mediante credenciales del usuario (email/contraseña validadas con BCrypt). Tras la validación, el backend genera un **par de tokens** con vidas distintas.

### 2.2. Par de tokens
| Token | Duración | Función | Contenido mínimo |
|-------|----------|---------|------------------|
| **Access Token** | 15 minutos | Autorizar cada petición a recursos protegidos. | `user_id`, `device_id`, `exp`, `iat`. |
| **Refresh Token** | 7 días | Obtener un nuevo par de tokens cuando el access expira. | `user_id`, `token_id` (UUID único), `device_id`, `exp`. |

### 2.3. Proceso de emisión
1. El usuario envía credenciales al endpoint de login.
2. El backend valida contra la base de datos (BCrypt).
3. El backend genera el par access + refresh.
4. El backend persiste el **hash del refresh token** en la tabla `refresh_tokens`, junto con `expires_at`, `revoked_at` (null inicialmente) y `replaced_by` (null inicialmente).
5. El backend responde al móvil con ambos tokens en el cuerpo de la respuesta.

### 2.4. Invariantes
- El backend **nunca** emite un refresh token sin persistir su hash primero.
- El access token es **stateless**: el backend puede validarlo solo con la clave pública/privada, sin consultar base de datos.
- El refresh token es **stateful**: requiere consulta a la tabla `refresh_tokens` para verificar que no fue revocado.

---

## 3. Subsistema de Almacenamiento Local

### 3.1. Principio rector
El móvil es responsable de guardar ambos tokens de forma persistente para que el usuario no tenga que autenticarse cada 15 minutos. La persistencia es local y cifrada por el sandbox del sistema operativo.

### 3.2. Entidades almacenadas
En la tabla `local_user` (o equivalente) de SQLite:

| Campo | Origen | Propósito |
|-------|--------|-----------|
| `access_token` | Respuesta de login/refresh | Inyectar en header `Authorization` de cada request. |
| `refresh_token` | Respuesta de login/refresh | Enviar al endpoint de refresh cuando el access expire. |
| `token_expires_at` | Calculado localmente (`iat + 15 min`) | Decidir si el token local aún es válido sin parsear JWT. |
| `device_id` | Generado por el móvil en primera instalación | Identificar el dispositivo en el backend para rotación y revocación. |

### 3.3. Generación de `device_id`
- Se genera **una sola vez** por instalación de la APK (ej. UUIDv4 aleatorio).
- Se almacena en `SharedPreferences` o SQLite inmediatamente.
- Se envía en el payload de login y en cada refresh. Permite al backend saber desde qué dispositivo se solicita la renovación.

### 3.4. Qué NO hace el almacenamiento local
- No almacena la contraseña del usuario.
- No almacena el access token en memoria compartida ni en logs.
- No sincroniza tokens entre dispositivos (cada móvil tiene su propio par).

---

## 4. Subsistema de Transporte Seguro

### 4.1. Principio rector
Cada petición HTTP que sale del móvil hacia el backend debe llevar autenticación, excepto los endpoints explícitamente públicos (login, registro, recuperación de contraseña).

### 4.2. Interceptor de autenticación
El móvil implementa un interceptor que actúa **antes** de que cualquier petición viaje:

1. Lee el `access_token` almacenado.
2. Lo añade al header: `Authorization: Bearer <access_token>`.
3. Deja que la petición continúe.

### 4.3. Endpoints excluidos
- `POST /api/auth/login`
- `POST /api/auth/register`
- `POST /api/auth/forgot-password`

Todo lo demás requiere Bearer.

### 4.4. Invariante
- El interceptor **nunca** envía el `refresh_token` en el header de una petición normal. El refresh token solo viaja en el cuerpo de una petición explícita al endpoint de renovación.

---

## 5. Subsistema de Renovación

### 5.1. Principio rector
El access token expira cada 15 minutos. El móvil debe renovarlo automáticamente sin intervención del usuario y sin que el usuario perciba la transición.

### 5.2. Disparador de renovación
Hay dos mecanismos de detección de expiración:

| Mecanismo | Cuándo ocurre | Acción |
|-----------|---------------|--------|
| **Local** | El móvil detecta que `token_expires_at` ya pasó (margen de 60s de seguridad). | Dispara refresh **antes** de enviar la petición. |
| **Remoto** | El backend responde `401 Unauthorized` con código `TOKEN_EXPIRED`. | El interceptor captura el 401, bloquea la petición original, dispara refresh, y reintenta la original tras éxito. |

### 5.3. Flujo de renovación
1. El móvil envía `POST /api/auth/refresh` con:
   - `refresh_token` en el cuerpo.
   - `device_id` en el cuerpo.
2. El backend valida:
   - Que el hash del refresh exista en `refresh_tokens`.
   - Que no esté revocado (`revoked_at IS NULL`).
   - Que no haya expirado (`expires_at > now()`).
   - Que el `device_id` coincida con el registrado (opcional, para seguridad adicional).
3. El backend **rota** el refresh:
   - Marca el actual como revocado (`revoked_at = now()`).
   - Escribe `replaced_by = <nuevo_token_id>`.
   - Genera un **nuevo par** access + refresh.
   - Persiste el hash del nuevo refresh.
4. El backend responde con el nuevo par.
5. El móvil:
   - Guarda el nuevo access y refresh (sobrescribe los anteriores).
   - Actualiza `token_expires_at`.
   - Reintenta la petición original que causó el 401.

### 5.4. Fallo de renovación
Si el refresh es rechazado (`401 REFRESH_INVALID` o `401 REFRESH_EXPIRED`):
- El móvil **limpia todos los tokens** de `local_user`.
- Fuerza al usuario a la pantalla de login.
- Esto ocurre cuando el usuario no abre la app por más de 7 días.

---

## 6. Subsistema de Control de Race Conditions

### 6.1. Problema
Si dos peticiones paralelas del móvil detectan simultáneamente que el access token expiró, ambas podrían intentar refrescar con el mismo refresh token. El backend solo puede aceptar una rotación; la segunda debería fallar.

### 6.2. Solución en el móvil: Serialización
El `AuthInterceptor` mantiene un flag booleano `isRefreshing`:

- **Hilo A** detecta 401, pone `isRefreshing = true`, envía la petición de refresh.
- **Hilo B** detecta 401, ve que `isRefreshing = true`, **encola su petición original** y espera el resultado del refresh.
- Cuando **Hilo A** recibe el nuevo par, notifica a todos los hilos encolados.
- **Hilo B** reintenta su petición original con el nuevo access token.

**Resultado:** Solo una petición de refresh llega al backend, independientemente de cuántas peticiones paralelas fallaron por 401.

### 6.3. Solución en el backend: Transacción atómica
La rotación de refresh token se ejecuta dentro de una transacción de base de datos:

```
BEGIN;
SELECT * FROM refresh_tokens WHERE token_hash = ? FOR UPDATE;
-- Verificar no revocado, no expirado
UPDATE refresh_tokens SET revoked_at = now(), replaced_by = ? WHERE id = ?;
INSERT INTO refresh_tokens (token_hash, expires_at, ...) VALUES (?, ?, ...);
COMMIT;
```

- `SELECT FOR UPDATE` bloquea la fila. El segundo refresh concurrente espera.
- Cuando el segundo lee la fila, ya está revocada. Responde `401 REFRESH_REUSED`.

### 6.4. Fallback ante robo de token
Si el móvil recibe `401 REFRESH_REUSED` mientras él mismo no disparó dos refreshes simultáneos:
- Esto indica que otro dispositivo (o un atacante) usó el refresh token.
- El móvil **limpia todos los tokens** y fuerza re-login.
- El backend mantiene el registro de `replaced_by` para auditoría.

### 6.5. Invariantes
- En cualquier instante, para un par usuario-dispositivo, existe **cero o un** refresh token válido en la base de datos.
- Un refresh token revocado **nunca** vuelve a ser válido.
- Si el móvil recibe `REFRESH_REUSED`, asume compromiso de seguridad y invalida sesión local.

---

## 7. Subsistema de Revocación y Logout

### 7.1. Logout iniciado por usuario
Cuando el usuario toca "Cerrar sesión" en la APK:

1. El móvil envía `POST /api/auth/logout` con el refresh token actual.
2. El backend marca ese refresh como revocado (`revoked_at = now()`).
3. El móvil borra `access_token`, `refresh_token` y `token_expires_at` de SQLite.
4. El móvil redirige a pantalla de login.

**Nota:** El access token aún es técnicamente válido hasta su `exp` (15 min), pero sin refresh token no puede renovarse. El backend puede optar por mantener una lista negra de access tokens revocados, pero en MVP esto no es necesario por la corta vida del access.

### 7.2. Revocación remota (cambio de contraseña, robo detectado)
Si el backend detecta comportamiento sospechoso o el usuario cambia contraseña desde otro dispositivo:

- El backend puede revocar **todos** los refresh tokens de ese `user_id`.
- La próxima vez que el móvil intente refrescar, recibirá `401 REFRESH_REVOKED`.
- El móvil limpia tokens y fuerza re-login.

### 7.3. Limpieza de tokens expirados
Un job programado en el backend (o una política de PostgreSQL) elimina periódicamente los refresh tokens cuyo `expires_at` ya pasó hace más de 30 días, manteniendo la tabla limpia.

---

## 8. Flujo Integrado de Datos

```
┌─────────────────────────────────────────────────────────────────────┐
│                         APK (Móvil)                                │
│                                                                      │
│  ┌──────────────────┐    ┌──────────────────┐    ┌──────────────────┐  │
│  │ Almacenamiento   │    │ Interceptor      │    │ Serializador de  │  │
│  │ Local (SQLite)   │───►│ Auth (Bearer)    │───►│ Refresh (flag)   │  │
│  │ access + refresh │    │ 401? → Dispara   │    │ isRefreshing     │  │
│  └──────────────────┘    │      refresh     │    └──────────────────┘  │
│                          └──────────────────┘            │           │
│                              │                           │           │
│                              ▼                           ▼           │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │  Petición original reintentada con nuevo access token             │  │
│  └──────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────┘
                              │
                              │ HTTPS
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│                         BACKEND (Render)                           │
│                                                                      │
│  ┌──────────────┐    ┌──────────────────────┐    ┌──────────────┐  │
│  │ Validar      │───►│ Rotación atómica     │───►│ Generar nuevo│  │
│  │ refresh hash │    │ (SELECT FOR UPDATE)  │    │ par access+  │  │
│  │ (no revocado)│    │ Revocar anterior     │    │ refresh      │  │
│  └──────────────┘    └──────────────────────┘    └──────────────┘  │
│                              │                    │                 │
│                              ▼                    ▼                 │
│                        ┌──────────────┐    ┌──────────────┐        │
│                        │ 401 REFRESH  │    │ Persistir    │        │
│                        │ _REUSED      │    │ nuevo hash   │        │
│                        └──────────────┘    └──────────────┘        │
└─────────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│                         PostgreSQL (Neon)                          │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │  refresh_tokens                                              │  │
│  │  token_hash | user_id | device_id | expires_at | revoked_at |  │
│  │  replaced_by                                                 │  │
│  └──────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 9. Decisiones Arquitectónicas Consolidadas (Sistema 4)

| Decisión | Valor | Justificación |
|----------|-------|---------------|
| **Par de tokens** | Access (15 min) + Refresh (7 días) | Access corto limita daño de robo. Refresh largo evita login constante. |
| **Access token** | Stateless (JWT firmado) | Backend valida sin tocar base de datos. Escalable. |
| **Refresh token** | Stateful (persistido en DB) | Permite revocación inmediata. Permite detectar reutilización. |
| **Rotación de refresh** | Sí. Cada refresh genera un par nuevo. | Si un token es robado, solo sirve una vez. Limita ventana de ataque. |
| **Control de race condition (móvil)** | Flag `isRefreshing` + cola de peticiones | Solo un refresh llega al backend. El resto espera y reutiliza el nuevo token. |
| **Control de race condition (backend)** | Transacción atómica con `SELECT FOR UPDATE` | El segundo refresh concurrente ve el token revocado y recibe `REFRESH_REUSED`. |
| **Fallback ante reutilización** | Limpiar tokens locales + forzar re-login | Asume compromiso de seguridad (robo de token o dispositivo). |
| **Device ID** | UUID generado por móvil, persistente por instalación | Permite al backend auditar desde qué dispositivo se renovó. |
| **Almacenamiento local** | SQLite en sandbox de APK | El sistema operativo cifra el sandbox. No usamos Keychain/Keystore explícito en MVP. |
| **Logout** | Revocar refresh en backend + limpiar local | El access token muere solo por expiración (15 min), aceptable en MVP. |

---

## 10. Glosario de Términos (Sistema 4)

| Término | Definición |
|---------|------------|
| **Access Token** | JWT de corta duración (15 min) que autoriza cada petición a recursos protegidos. |
| **Refresh Token** | Token de larga duración (7 días) que permite obtener un nuevo par de tokens sin pedir contraseña. |
| **Rotación** | Política de invalidar el refresh token anterior y emitir uno nuevo en cada renovación. |
| **Revocación** | Marcado explícito de un token como inválido antes de su fecha natural de expiración. |
| **Race Condition (refresh)** | Escenario donde dos peticiones paralelas intentan usar el mismo refresh token simultáneamente. |
| **REFRESH_REUSED** | Código de error que el backend devuelve cuando un refresh token ya revocado es presentado de nuevo. |
| **Device ID** | Identificador único generado por el móvil en su primera instalación, usado para auditar renovaciones. |
| **AuthInterceptor** | Componente del móvil que intercepta todas las peticiones HTTP para inyectar el token de acceso. |
| **SELECT FOR UPDATE** | Cláusula SQL que bloquea una fila durante una transacción, evitando lecturas concurrentes. |

---

## 11. Interacciones con otros sistemas

| Sistema | Interacción | Dirección |
|---------|-------------|-----------|
| **Sistema 3 — Sync** | El Sistema 3 delega al Sistema 4 cuando recibe `401` en un batch. | Sistema 3 → Sistema 4 |
| **Sistema 5 — XP** | El endpoint de fórmulas de XP (`GET /api/xp/formulas`) requiere access token válido. | Sistema 4 → Sistema 5 |
| **Sistema 6 — Ranking** | Los endpoints de ranking requieren autenticación. | Sistema 4 → Sistema 6 |
| **Sistema 10 — Estadísticas** | El endpoint de perfil requiere autenticación. | Sistema 4 → Sistema 10 |
| **Sistema 8 — Notificaciones** | Si el refresh falla y se fuerza logout, el Sistema 8 puede notificar al usuario. | Sistema 4 → Sistema 8 |
