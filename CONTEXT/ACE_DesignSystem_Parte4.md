# A.C.E — Manual de Diseño de Interfaz (Design System v1.0)

## Parte 4: Pantallas restantes, Estados, Directivas LLM, Glosario

---

## 15. Pantalla: Ranking (`06_ranking.html`)

### 15.1 Rol en el Sistema
- **Sistemas dominantes:** S6 (Ranking).
- **Sistemas secundarios:** S4 (Auth JWT), S3 (invalidación cache).
- **Propósito:** Visualización de posición competitiva. Motivación social.

### 15.2 Estructura Visual

```
┌─────────────────────────────────────┐
│  Ranking              [🔄]          │ ← AppBar
│                                     │
│  ┌──────────────┬──────────────┐   │
│  │   GLOBAL     │  MUNICIPAL   │   │ ← TabRow
│  └──────────────┴──────────────┘   │
│                                     │
│  Actualizado hace 45 min            │ ← Cache stale indicator
│                                     │
│  ┌─────────────────────────┐        │
│  │  #42  Tú                │        │ ← Mi posición (sticky)
│  │  1,240 XP               │        │
│  └─────────────────────────┘        │
│                                     │
│  TOP 10                             │
│  ┌─────────────────────────┐        │
│  │  1  🥇  Alex      5,420 │        │
│  │  2  🥈  Maria     4,890 │        │
│  │  3  🥉  Carlos    4,230 │        │
│  │  4      Juan      3,980 │        │
│  │  ...                    │        │
│  │  10     Ana       2,100 │        │
│  └─────────────────────────┘        │
│                                     │
│  [🏠]  [📊]  [🏆]  [👤]            │ ← BottomNav
└─────────────────────────────────────┘
```

### 15.3 Especificaciones Detalladas

#### TabRow
- **Tabs:** "Global" y "Municipal".
- **Indicador:** Línea 2dp `NeonRed` debajo del tab activo.
- **Fondo:** `BgBlack`.
- **Texto activo:** 14sp, `NeonRed`, `FontWeight.Medium`.
- **Texto inactivo:** 14sp, `TextMuted`.
- **Comportamiento:** Al cambiar de tab, recarga datos desde cache o red.

#### Indicador de Cache Stale (S6)
- **Texto:** "Actualizado hace X min", 10sp, `TextMuted`.
- **Posición:** Arriba de la lista, alineado a la derecha, padding 16dp.
- **Condición:** Aparece si `cache_age > 1h`.

#### Mi Posición (Sticky)
- **Tarjeta:** `AceCard` con borde 2dp `NeonRed@50%` (destacada).
- **Posición:** Arriba de todo, sticky (no se desplaza con scroll).
- **Contenido:**
  - Número de posición: 24sp, `FontWeight.Black`, `NeonRed`.
  - Label "Tú": 14sp, `TextPrimary`, `FontWeight.Bold`.
  - Nickname: 12sp, `TextSecondary`.
  - XP: 16sp, `FontWeight.Bold`, `NeonRed`, alineado a la derecha.
- **Comportamiento:** Si estoy fuera del top 10, muestra mi posición real (ej: #42) pero no los usuarios intermedios.

#### Lista Top 10
- **Título:** "TOP 10", 12sp, `TextMuted`, letterSpacing 2sp, padding 16dp horizontal.
- **Items:**
  - Posición 1: Número `NeonRed`, icono 🥇.
  - Posición 2: Número `#FFD700` (oro), icono 🥈.
  - Posición 3: Número `#C0C0C0` (plata), icono 🥉.
  - Posiciones 4+: Número `TextSecondary`, sin icono.
  - Nickname: 14sp, `TextPrimary`.
  - XP: 14sp, `TextSecondary`, alineado a la derecha.
- **Separador:** Divider 0.5dp `BorderDim` entre items.
- **Padding:** 12dp vertical por item, 16dp horizontal.

#### Empty State
- Si cache vacío y sin conexión: "No hay datos de ranking disponibles", 14sp, `TextMuted`, centrado.
- Icono: 🏆, 48dp, `TextMuted`.

### 15.4 Estados

| Estado | Visual |
|--------|--------|
| **Cache fresco** | Muestra inmediatamente. Indicador "Actualizado hace X min" si >1h. |
| **Cache vacío** | Muestra spinner 32dp `NeonRed` centrado. Luego carga datos. |
| **Sin conexión** | Muestra cache si existe. Si no, empty state. |
| **Rank changed** | Invalida cache. Próxima apertura fuerza refresh. |

### 15.5 Decisiones de Diseño
- Top 100 devuelto por backend, móvil solo cachea top 10 + posición propia.
- Cambio de ciudad no permitido en MVP.
- Posición propia siempre visible arriba para motivación inmediata.

---

## 16. Pantalla: Estadísticas (`07_estadisticas.html`)

### 16.1 Rol en el Sistema
- **Sistemas dominantes:** S10 (Stats), S9 (Historial).
- **Sistemas secundarios:** S7 (Streak contexto), S3 (Sync estado).
- **Propósito:** Vista histórica del rendimiento. Totales + historial reciente.

### 16.2 Estructura Visual

```
┌─────────────────────────────────────┐
│  Estadísticas         [🔄]          │ ← AppBar
│  Sincronizado hace 12 min           │
│                                     │
│  ┌────────┐ ┌────────┐              │
│  │ 1,240  │ │   15   │              │
│  │ XP     │ │ Sesiones│              │
│  └────────┘ └────────┘              │
│  ┌────────┐ ┌────────┐              │
│  │ 12:34  │ │  142   │              │
│  │ Tiempo │ │ Avg BPM│              │
│  └────────┘ └────────┘              │
│  ┌────────┐ ┌────────┐              │
│  │   45   │ │  #42   │              │
│  │ Bloques│ │ Posición│              │
│  └────────┘ └────────┘              │
│                                     │
│  ┌─────────────────────────┐        │
│  │ RACHA                   │        │
│  │ 5 días actual           │        │
│  │ 12 días mejor           │        │
│  │ [███░░░░]               │        │
│  └─────────────────────────┘        │
│                                     │
│  HISTORIAL (últimas 5)              │
│  ┌─────────────────────────┐        │
│  │ 18 Jun · Running        │        │
│  │ 45 min · 30 XP · 142bpm│        │
│  └─────────────────────────┘        │
│  ┌─────────────────────────┐        │
│  │ 17 Jun · Cycling        │        │
│  │ 30 min · 20 XP · 138bpm│        │
│  └─────────────────────────┘        │
│                                     │
│  [🏠]  [📊]  [🏆]  [👤]            │
└─────────────────────────────────────┘
```

### 16.3 Especificaciones Detalladas

#### AppBar
- **Título:** "Estadísticas", 22sp, `CinzelDecorative`, Black, `TextPrimary`.
- **Refresh:** Icono 🔄, `NeonRed`, 24dp. Dispara sync manual.
- **Timestamp:** "Sincronizado hace X minutos", 11sp, `TextMuted`, debajo del título.

#### Grid de Stats (S10)
- **Layout:** 2 columnas, spacing 12dp.
- **Items:** `StatCardLarge`.
  - Fondo: `CardBg`, radio 12dp, padding 16dp.
  - Icono: 24dp, `NeonRed@60%`, arriba a la izquierda.
  - Valor: 28sp, `FontWeight.Black`, `Color.White`.
  - Label: 11sp, `TextMuted`, abajo.
- **Stats mostrados:**
  - XP Total (icono trophy 🏆)
  - Sesiones (icono calendar 📅)
  - Duración total (icono clock ⏱)
  - Avg BPM (icono heart ❤️)
  - Bloques (icono layers 📊)
  - Posición global (icono trophy 🏆)

#### Tarjeta de Racha (S7)
- **Tarjeta:** `AceCard` (padding 16dp).
- **Título:** "RACHA", 12sp, `TextMuted`, letterSpacing 2sp.
- **Valor actual:** `current_streak`, 32sp, `FontWeight.Black`, `NeonRed`.
- **Label actual:** "días actual", 11sp, `TextMuted`.
- **Valor mejor:** `best_streak`, 20sp, `FontWeight.Bold`, `Color.White`.
- **Label mejor:** "mejor", 11sp, `TextMuted`.
- **Barra visual:** 7 segmentos (últimos 7 días). Segmento relleno = `NeonRed`, vacío = `BorderDim`. Altura 8dp, radio 4dp.

#### Historial (S9)
- **Título:** "HISTORIAL", 12sp, `TextMuted`, letterSpacing 2sp, padding 16dp.
- **Subtítulo:** "(últimas 5)", 11sp, `TextMuted`.
- **Items:** Tarjetas `AceCard` (padding 12dp, radio 10dp, marginBottom 8dp).
- **Contenido por item:**
  - Fecha: 12sp, `TextSecondary`.
  - Tipo deporte: 14sp, `FontWeight.Bold`, `TextPrimary`.
  - Duración: 12sp, `TextSecondary`.
  - XP: 14sp, `FontWeight.Bold`, `NeonRed`.
  - Avg BPM: 11sp, `TextMuted`.
- **Layout:** Row con fecha+tipo a la izquierda, duración+XP a la derecha.

#### Empty State de Historial
- Si no hay sesiones: "Aún no has completado sesiones", 14sp, `TextMuted`, centrado.
- Icono: 📊, 48dp, `TextMuted`.

### 16.4 Estados

| Estado | Visual |
|--------|--------|
| **Normal** | Todo cache local. Timestamp visible. |
| **Sync en progreso** | Spinner sutil 20dp en esquina superior derecha. No bloquea UI. |
| **Corrección > 10 XP** | Toast: "Estadísticas actualizadas desde servidor". |
| **Corrección < 10 XP** | Silenciosa. No hay UI. |

### 16.5 Decisiones de Diseño
- Stats globales, no por categoría.
- Historial mezclado (sin filtros por deporte).
- Historial dentro de Estadísticas (no pantalla propia) porque S9 es "vista rápida".

---

## 17. Pantalla: Perfil — Side Menu (`08_perfil.html`)

### 17.1 Rol en el Sistema
- **Sistemas dominantes:** S4 (Auth, logout).
- **Sistemas secundarios:** S6 (ciudad fija), S8 (preferencias notificaciones), S1 (Wear OS estado).
- **Propósito:** Configuración personal. Logout accesible globalmente.
- **Tipo:** Side Drawer (modal lateral), no pantalla completa.

### 17.2 Estructura Visual

```
┌─────────────────────────────────────┐
│                                     │
│  ┌─────────────────────────────┐    │ ← Overlay semitransparente
│  │  A.C.E                      │    │
│  │                             │    │
│  │  PERFIL                     │    │
│  │  ┌─────────────────────┐    │    │
│  │  │ 👤 Nickname         │    │    │
│  │  │    Alex             │    │    │
│  │  ├─────────────────────┤    │    │
│  │  │ ✉️ Email            │    │    │
│  │  │    alex@ace.com     │    │    │
│  │  ├─────────────────────┤    │    │
│  │  │ 📍 Ciudad           │    │    │
│  │  │    Bogotá           │    │    │
│  │  ├─────────────────────┤    │    │
│  │  │ ⌚ Wear OS          │    │    │
│  │  │    Conectado   [↻]  │    │    │
│  │  └─────────────────────┘    │    │
│  │                             │    │
│  │  NOTIFICACIONES             │    │
│  │  ┌─────────────────────┐    │    │
│  │  │ 🔔 Sesión activa    │    │    │
│  │  │    [Toggle ON]      │    │    │
│  │  ├─────────────────────┤    │    │
│  │  │ 🔥 Racha            │    │    │
│  │  │    [Toggle ON]      │    │    │
│  │  ├─────────────────────┤    │    │
│  │  │ ⚠️ Error de sync    │    │    │
│  │  │    [Toggle ON]      │    │    │
│  │  └─────────────────────┘    │    │
│  │                             │    │
│  │  [──── Cerrar sesión ────]  │    │
│  └─────────────────────────────┘    │
│                                     │
└─────────────────────────────────────┘
```

### 17.3 Especificaciones Detalladas

#### Drawer
- **Ancho:** 300dp (o 80% del ancho de pantalla en móviles pequeños).
- **Fondo:** `BgBlack`.
- **Overlay:** `BgBlack@80%` sobre la pantalla anterior, tocable para cerrar.
- **Entrada:** Slide desde la izquierda, 300ms, easing FastOutSlowInEasing.
- **Padding:** 24dp.

#### Cabecera
- **Logo:** `AceLogoSmall` (48dp), estático.
- **Texto:** "A.C.E", 18sp, `CinzelDecorative`, Black, `TextPrimary`.
- **Spacer:** 32dp debajo.

#### Sección Perfil
- **Título:** "PERFIL", 11sp, `NeonRed@75%`, `FontWeight.Bold`, letterSpacing 3sp.
- **Spacer:** 16dp.
- **Items:**
  - **Nickname:** Icono 👤, label "Nickname" 12sp `TextSecondary`, valor 14sp `TextPrimary`.
  - **Email:** Icono ✉️, label "Email" 12sp `TextSecondary`, valor 14sp `TextPrimary`.
  - **Ciudad:** Icono 📍, label "Ciudad" 12sp `TextSecondary`, valor 14sp `TextPrimary`.
  - **Wear OS:** Icono ⌚, label "Wear OS" 12sp `TextSecondary`, valor 14sp `TextPrimary` (color verde/amarillo según estado). Botón "Re-verificar" a la derecha, 12sp `NeonRed`.
- **Separador:** Divider 0.5dp `BorderDim` entre secciones.
- **Nota:** Ningún campo es editable en MVP. Solo visualización.

#### Sección Notificaciones (S8)
- **Título:** "NOTIFICACIONES", 11sp, `NeonRed@75%`, `FontWeight.Bold`, letterSpacing 3sp.
- **Spacer:** 12dp.
- **Items:** Toggle switches estilizados.
  - Track ON: `NeonRed`.
  - Track OFF: `BorderDim`.
  - Thumb: `Color.White`.
  - Label: 14sp, `TextPrimary`.
  - Descripción: 11sp, `TextMuted` (ej: "Baja", "Alta").
- **Canales:**
  1. "Sesión activa" — importancia baja.
  2. "Racha" — importancia alta.
  3. "Error de sync" — importancia alta.

#### Botón Logout
- **Estilo:** Variante de `AceButton` con fondo `CardBg` y texto `NeonRed`.
- **Altura:** 48dp.
- **Texto:** "Cerrar sesión", 14sp, `NeonRed`, `FontWeight.Black`.
- **Comportamiento:** Envía logout al backend, limpia Room, navega a Login.

### 17.4 Estados

| Estado | Visual |
|--------|--------|
| **Normal** | Todos los toggles activos por defecto. |
| **Wear OS desconectado** | Valor en amarillo. Botón "Re-verificar" visible. |
| **Logout** | Overlay de carga mientras se procesa. Luego navega a Login. |

### 17.5 Decisiones de Diseño
- Side menu (no pantalla completa) porque logout debe ser accesible desde cualquier lugar.
- Perfil no editable en MVP. Solo visualización.
- Ciudad no se cambia: implicaría perder posición municipal (S6 §3.4).

---

## 18. Pantalla: Registro (`09_registro.html`)

### 18.1 Rol en el Sistema
- **Sistemas dominantes:** S4 (Auth registro).
- **Sistemas secundarios:** S6 (ciudad inicial), S5 (descarga fórmulas), S10 (inicialización stats).
- **Propósito:** Creación de cuenta. Post-registro automáticamente logueado.

### 18.2 Estructura Visual

```
┌─────────────────────────────────────┐
│  FONDO ANIMADO (cubos + partículas) │
│  + Vignette radial                  │
│                                     │
│         ┌─────────────┐             │
│         │   [LOGO]    │             │
│         │   A.C.E     │             │
│         └─────────────┘             │
│                                     │
│    ┌─────────────────────────┐      │
│    │ ┌─────────────────────┐ │      │
│    │ │ Crear cuenta        │ │      │
│    │ │ [Email            ] │ │      │
│    │ │ [Nickname         ] │ │      │
│    │ │ [Password         ] │ │      │
│    │ │ [Confirm password ] │ │      │
│    │ │                     │ │      │
│    │ │ Ciudad: [Bogotá ▼]  │ │      │
│    │ │                     │ │      │
│    │ │ [ REGISTRARME     ] │ │      │
│    │ │    Error msg        │ │      │
│    │ │ ─────  ó  ─────     │ │      │
│    │ │ [ YA TENGO CUENTA ] │ │      │
│    │ └─────────────────────┘ │      │
│    └─────────────────────────┘      │
└─────────────────────────────────────┘
```

### 18.3 Especificaciones Detalladas

#### Fondo
- Completo: Grid + partículas + cubos 3D + vignette. **Mismo que Login**.

#### Marca
- Logo: `AceLogoSmall` (80dp), con pulso.
- Título "A.C.E": `DisplayBrand`.
- Línea glow + subtítulo: Mismo que Login.

#### Formulario
- **Tarjeta:** `AceCard` (padding 24dp).
- **Título:** "Crear cuenta", 16sp, `UnifrakturMaguntia`, Bold, blanco, paddingBottom 20dp.
- **Inputs:**
  - Email: `AceTextField`, label "Correo electrónico".
  - Nickname: `AceTextField`, label "Nickname".
  - Password: `AceTextField`, label "Contraseña", isPassword=true.
  - Confirm password: `AceTextField`, label "Confirmar contraseña", isPassword=true.
- **Spacer entre inputs:** 14dp.
- **Selector de ciudad:**
  - Dropdown o BottomSheet.
  - Opciones: Bogotá, Medellín, Cali, Barranquilla.
  - Label: "Ciudad", 13sp, `TextSecondary`.
  - Valor seleccionado: 14sp, `TextPrimary`.
  - Icono: ▼ o flecha, `TextMuted`.
  - **Obligatorio.** City_id inmutable en MVP.
- **Botón "Registrarme":** `AceButton` (filled, 52dp, `NeonRed`).
- **Botón "Ya tengo cuenta":** `AceOutlinedButton` (outlined, 50dp, `NeonRed@50%`). Navega a Login.

#### Estados

| Estado | Visual |
|--------|--------|
| **Inicial** | Formulario vacío, botón activo. |
| **Validación** | Si campos vacíos o password ≠ confirm, error inline. |
| **Loading** | Spinner en botón. Campos bloqueados. |
| **Error** | Texto de error debajo del botón, `NeonRed`, 12sp. |
| **Éxito** | Post-registro automáticamente logueado. Navega a Home. |

### 18.4 Decisiones de Diseño
- Registro es pantalla secundaria (accesible desde Login), no flujo principal.
- Ciudad es obligatoria y permanente: afecta S6 de por vida.
- Post-registro automáticamente logueado (no requiere login separado).
- Descarga fórmulas automáticamente post-registro. Si falla, bloquea Ejercicio.

---

## 19. Pantalla: Diagnóstico Sync (`10_diagnostico.html`)

### 19.1 Rol en el Sistema
- **Sistemas dominantes:** S3 (Sync error).
- **Sistemas secundarios:** S5 (reversión XP), S10 (corrección stats), S8 (notificación descartada).
- **Propósito:** Gestión manual de bloques con error de sync. Pantalla completa para detalle y acciones.

### 19.2 Estructura Visual

```
┌─────────────────────────────────────┐
│  ← Diagnóstico Sync                  │
│                                     │
│  3 bloques con error                │
│                                     │
│  ┌─────────────────────────┐        │
│  │ ⚠️ Bloque #42           │        │
│  │ Duración: 05:01         │        │
│  │ Avg BPM: 142            │        │
│  │ XP: 10 (será revertida) │        │
│  │ Motivo: XP inconsistente  │        │
│  │                         │        │
│  │ [Reintentar] [Descartar]│        │
│  └─────────────────────────┘        │
│                                     │
│  ┌─────────────────────────┐        │
│  │ ⚠️ Bloque #43           │        │
│  │ Duración: 05:03         │        │
│  │ Avg BPM: 138            │        │
│  │ XP: 10 (será revertida) │        │
│  │ Motivo: Timeout (5x)    │        │
│  │                         │        │
│  │ [Reintentar] [Descartar]│        │
│  └─────────────────────────┘        │
│                                     │
│  [──── Sincronizar todo ────]       │
│                                     │
└─────────────────────────────────────┘
```

### 19.3 Especificaciones Detalladas

#### AppBar
- **Título:** "Diagnóstico Sync", 18sp, `TextPrimary`.
- **Back:** Flecha ←, `NeonRed`, 24dp.

#### Contador
- **Texto:** "X bloques con error", 16sp, `FontWeight.Bold`, `NeonRed`.
- **Posición:** Debajo del AppBar, padding 16dp.

#### Lista de Bloques con Error
- **Cada item:** Tarjeta `AceCard` con borde amarillo 1dp (`#FFD600`).
- **Contenido:**
  - Icono: ⚠️, 20dp, amarillo.
  - "Bloque #[ID]", 14sp, `FontWeight.Bold`, `TextPrimary`.
  - Duración: 12sp, `TextSecondary`.
  - Avg BPM: 12sp, `TextSecondary`.
  - XP: "XP: [valor] (será revertida)", 12sp, `NeonRed`.
  - Motivo: 11sp, `TextMuted`.
- **Acciones por bloque:**
  - **"Reintentar":** `AceOutlinedButton` (más pequeño, height 36dp, borde `NeonRed`). Fuerza sync manual de ese bloque.
  - **"Descartar":** `TextButton`, 12sp, `TextMuted`. Marca bloque como visto. Si es 422, revierte XP local.
- **Separador:** 16dp entre items.

#### CTA Global
- **Botón "Sincronizar todo":** `AceButton` (filled, 52dp, `NeonRed`).
- **Comportamiento:** Fuerza batch sync de todos los bloques PENDING.
- **Posición:** Fijo en la parte inferior, padding 24dp.

#### Nota de Corrección (S10)
- Si se descarta un bloque 422 y la corrección de XP es > 10:
  - Toast: "Estadísticas actualizadas desde servidor".
- Si es < 10:
  - Corrección silenciosa. Sin UI.

### 19.4 Estados

| Estado | Visual |
|--------|--------|
| **Con errores** | Lista de bloques con error. Cada uno con acciones. |
| **Reintentando** | Botón "Reintentar" muestra spinner. Bloque temporalmente deshabilitado. |
| **Descartado** | Bloque desaparece de la lista con animación de slide out (200ms). |
| **Sin errores** | Empty state: "No hay bloques con error", 14sp `TextMuted`, icono ✅ 48dp. |

### 19.5 Decisiones de Diseño
- Pantalla completa (no modal): el usuario necesita ver detalles y tomar decisiones.
- Bloques 422 no se reintentan automáticamente (error permanente, S3 §5.4).
- Corrección silenciosa si < 10 XP, toast si es mayor (S10 §5.3).
- Notificación que llevó a esta pantalla se descarta al abrir (S8).

---

## 20. Estados Condicionales y Feedback

### 20.1 Matriz de Estados de UI por Condición del Sistema

| Condición del Sistema | Estado de UI | Pantalla afectada | Prioridad |
|----------------------|-------------|-------------------|-----------|
| S1: Wear OS desconectado (>5s) | Indicador amarillo, botón Iniciar deshabilitado | Ejercicio, Sesión Activa | Alta |
| S1: Wear OS conectado | Indicador verde pulsante | Ejercicio, Sesión Activa | Normal |
| S3: Bloques en ERROR | Banner amarillo en Home, detalle en Ejercicio | Home, Ejercicio | Media |
| S3: Sync en progreso | Spinner sutil en Estadísticas | Estadísticas | Baja |
| S4: Token expirado | Refresh silencioso (sin UI) | Todas | Crítica |
| S4: Refresh fallado | Forzar logout, redirigir a Login | Todas | Crítica |
| S5: Sin fórmulas cacheadas | Botón Iniciar deshabilitado, mensaje de conexión | Ejercicio | Alta |
| S6: Cache stale (>1h) | Indicador "Actualizado hace X min" | Ranking | Baja |
| S7: `last_exercise_date != hoy` | Banner racha en peligro (solo en Home) | Home | Media |
| S8: Sesión ACTIVE | Notificación persistente no descartable | Sistema | Normal |
| S10: Corrección > 10 XP | Toast "Estadísticas actualizadas desde servidor" | Estadísticas | Media |

### 20.2 Jerarquía de Banners (cuando hay múltiples)

```
1. Error crítico (S4 refresh fallado) → Overlay + logout forzado
2. Sync error (S3) → Banner amarillo
3. Racha en peligro (S7) → Banner rojo
4. Racha normal (S7) → Banner rojo
5. Wear OS desconectado (S1) → Banner amarillo
```

### 20.3 Tipos de Feedback

| Tipo | Duración | Estilo | Uso |
|------|----------|--------|-----|
| **Toast** | 2.5s | Fondo `CardBg`, texto `TextPrimary`, 14sp. Sin icono. | Corrección stats, acción completada. |
| **SnackBar** | 4s + acción | Fondo `CardBg`, borde izquierdo `NeonRed`, texto 14sp. Botón acción `NeonRed`. | Acción con undo (ej: descartar bloque). |
| **Banner** | Persistente | Altura 40-48dp, ancho completo. Texto 12sp. | Racha, sync error, Wear OS. |
| **Inline error** | Persistente | Texto 12sp, `NeonRed`, debajo del campo. | Validación de formulario. |
| **Spinner** | Hasta completar | 20-32dp, `NeonRed` o blanco. | Carga, sync en progreso. |
| **Empty state** | Persistente | Icono 48dp `TextMuted`, texto 14sp `TextMuted`. | Sin datos. |

---

## 21. Directivas para LLM (Claude, GPT-4, etc.)

### 21.1 Cómo usar este manual

Este manual es la fuente de verdad para generar UI de A.C.E. Cuando un LLM reciba este manual, debe seguir estas reglas:

#### Reglas Generales
1. **NUNCA** inventar colores que no estén en la paleta definida (§2.1).
2. **NUNCA** usar fuentes que no sean `UnifrakturMaguntia`, `CinzelDecorative` o la fuente del sistema.
3. **NUNCA** omitir el fondo animado en pantallas de auth (Login, Registro).
4. **NUNCA** usar sombras tradicionales (elevation). Usar glow y bordes luminosos.
5. **SIEMPRE** respetar la jerarquía tipográfica (§3.2).
6. **SIEMPRE** usar los tokens de espaciado (§4.1).
7. **SIEMPRE** usar los tokens de radio de esquinas (§5.1).
8. **SIEMPRE** respetar las reglas de animación (§6.4).

#### Generación de Pantallas Completas
Cuando se pide generar una pantalla:
1. Identificar el ID de pantalla (ej: `screen_login`).
2. Leer la sección correspondiente de este manual.
3. Usar los componentes atómicos definidos (§7) como bloques de construcción.
4. Aplicar el fondo correspondiente (§8).
5. Incluir todos los estados condicionales (§20).
6. Respetar las decisiones de diseño documentadas.

#### Generación de Componentes Individuales
Cuando se pide generar un componente:
1. Identificar el componente en §7 (ej: `AceButton`, `AceTextField`).
2. Copiar las especificaciones exactas (colores, tamaños, fuentes, espaciado).
3. Incluir todos los estados del componente (normal, loading, disabled, etc.).
4. Si el componente tiene animaciones, incluir los tokens de animación (§6.2).

#### Generación de Animaciones
Cuando se pide generar una animación:
1. Identificar el tipo de animación (§6.2).
2. Usar los valores exactos: duración, easing, valores inicial/final.
3. Para animaciones de fondo: usar las especificaciones de §8.3 y §8.4.
4. Para animaciones de UI: asegurar que duren <500ms.

#### Adaptaciones Permitidas
- **Tamaños de fuente:** Pueden ajustarse ±1sp para ajuste fino, pero nunca más.
- **Espaciado:** Pueden ajustarse ±2dp, pero manteniendo la escala de 4dp.
- **Colores de alpha:** Pueden ajustarse ±0.05 para ajuste fino visual.
- **Nuevos componentes:** Si se necesita un componente no documentado, debe seguir la filosofía de diseño (§1.2) y usar los tokens existentes.

#### Adaptaciones PROHIBIDAS
- **NO** cambiar el color primario (`NeonRed` #FF1744).
- **NO** cambiar el fondo base (`BgBlack` #050505).
- **NO** introducir nuevos colores primarios (azul, verde, etc.) como acentos principales.
- **NO** cambiar las fuentes principales.
- **NO** omitir la vignette en pantallas con fondo animado.
- **NO** usar Material Design estándar sin personalización (siempre aplicar los tokens A.C.E).

### 21.2 Prompt Template para LLM

Cuando se pide a un LLM generar una pantalla de A.C.E, usar este template:

```
Genera la pantalla [ID_PANTALLA] de la app A.C.E siguiendo el Manual de Diseño v1.0.

Contexto:
- Sistema dominante: [SISTEMA]
- Fondo: [COMPLETO / PLANO / GRID_SUTIL]
- Componentes necesarios: [LISTA]

Restricciones:
- Usar SOLO colores de la paleta A.C.E (NeonRed #FF1744, BgBlack #050505, CardBg #0D0D0D, etc.)
- Usar SOLO fuentes UnifrakturMaguntia (títulos/emoción), CinzelDecorative (marca/estructura), sistema (funcional)
- Respetar tokens de espaciado: 4, 8, 14, 16, 18, 20, 32 dp
- Respetar radios de esquinas: 10dp (inputs/botones), 16dp (tarjetas)
- Animaciones de UI <500ms. Animaciones de fondo pueden ser lentas (10s+).
- Incluir todos los estados condicionales documentados.

Estructura esperada:
[Describir la estructura de layout de la pantalla]

No generar código. Solo especificaciones visuales y de comportamiento.
```

---

## 22. Glosario de Términos UI

| Término UI | Sistema | Definición en contexto de interfaz |
|-----------|---------|-----------------------------------|
| **Banner racha** | S7 | Indicador visual en Home que muestra `current_streak`. Se actualiza por respuesta de sync. Color `NeonRed`, texto blanco. |
| **Banner sync** | S3/S8 | Indicador condicional que aparece cuando hay bloques `ERROR`. Fondo amarillo, navega a Diagnóstico. |
| **Espejo del bloque** | S2 | Visualización del bloque en progreso: métricas acumuladas pero XP **NO calculada aún**. Se muestra en Sesión Activa. |
| **BottomSheet deporte** | S2 | Selector de `sport_type` que define la fórmula de XP a aplicar. Grid de deportes, radio 16dp arriba. |
| **Chip foreground** | S8 | Indicador flotante que refleja la notificación persistente del sistema. Pill shape, fondo `CardBg`, borde `NeonRed@30%`. |
| **Quick stats** | S10 | Resumen numérico en Home: XP total, posición global, sesiones. Lee `local_user_stats`. Tres columnas con separadores. |
| **Verificación Wear OS** | S1 | Botón explícito que prueba conectividad con reloj antes de permitir iniciar sesión. Tarjeta con estado y botón de acción. |
| **Glow de tarjeta** | — | Borde superior de tarjetas Surface con gradiente vertical `NeonRed@35%` → `Transparent`. Grosor 1dp. |
| **Vignette** | — | Gradient radial `Transparent` → `BgBlack@70%` que oscurece los bordes de la pantalla. Radio 0.75×min(width, height). |
| **Fondo animado** | — | Sistema de 5 capas: grid + partículas + cubos 3D + vignette + contenido. Exclusivo de pantallas de auth. |
| **XP celebratoria** | S5 | Número grande de XP en Resumen Post-Sesión. 56-72sp, `NeonRed`, con animación de conteo. |
| **Diagnóstico sync** | S3 | Pantalla completa para gestionar bloques con error. Lista de tarjetas con acciones Reintentar/Descartar. |
| **Cache stale** | S6 | Indicador de edad del cache en Ranking. Texto 10sp `TextMuted`, aparece si >1h. |
| **Gate de fórmulas** | S5 | Bloqueo condicional: si no hay fórmulas cacheadas, botón "Iniciar" deshabilitado. Mensaje de conexión. |
| **Silent auth** | S4 | Refresh automático de token sin UI visible. Si falla, logout forzado. |
| **Corrección silenciosa** | S10 | Ajuste de stats sin notificación si dif < 10 XP. Si es mayor, toast. |

---

## 23. Decisiones Arquitectónicas de Diseño Consolidadas

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
| **Fondo animado solo en auth** | — | Cubos 3D + partículas son atmosféricos pero distraen en pantallas funcionales. |
| **Tipografía dual estricta** | — | UnifrakturMaguntia = emoción/acción. CinzelDecorative = marca/estructura. Nunca mezcladas. |
| **Sin elevation Material** | — | La separación se logra con color, bordes glow y vignette. No sombras. |
| **Animaciones de fondo lentas** | — | 14s de rotación es atmosférico, no funcional. No distrae. |
| **Feedback inmediato <100ms** | — | Cada interacción tiene respuesta visual instantánea. Pulso, glow, etc. |

---

## 24. Referencias Cruzadas

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
| Integración UI × Sistemas | `ACE_UI_SYSTEMS_INTEGRATION.md` |
| Implementación Login | `LoginScreen.kt`, `LoginViewModel.kt` |

---

*Fin del Manual de Diseño A.C.E v1.0. Este documento es la fuente de verdad para todas las decisiones de diseño de interfaz del proyecto A.C.E. Cualquier modificación debe reflejarse aquí y en los apéndices S1-S10 para mantener coherencia entre arquitectura y diseño.*
