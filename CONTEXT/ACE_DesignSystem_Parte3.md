# A.C.E — Manual de Diseño de Interfaz (Design System v1.0)

## Parte 3: Especificaciones de Pantallas

---

## 10. Pantalla: Login (`01_login.html`)

### 10.1 Rol en el Sistema
- **Sistemas dominantes:** S4 (Auth).
- **Sistemas secundarios:** S5 (XP, descarga fórmulas), S10 (Stats, carga inicial).
- **Propósito:** Punto de entrada. Autenticación + setup inicial post-login.

### 10.2 Estructura Visual

```
┌─────────────────────────────────────┐
│  FONDO ANIMADO (cubos + partículas) │
│  + Vignette radial                  │
│                                     │
│         ┌─────────────┐             │
│         │   [LOGO]    │  ← Pulse    │
│         │   A.C.E     │  ← 32sp     │
│         │    ───      │  ← Glow     │
│         │ ACTIVE...   │  ← 9sp      │
│         │             │             │
│    ┌─────────────────────────┐      │
│    │ ┌─────────────────────┐ │      │
│    │ │ Iniciar sesión      │ │      │
│    │ │ [Email input      ] │ │      │
│    │ │ [Password input   ] │ │      │
│    │ │         ¿Olvidaste? │ │      │
│    │ │ [ INGRESAR        ] │ │      │
│    │ │    Error msg        │ │      │
│    │ │ ─────  ó  ─────     │ │      │
│    │ │ [ CREAR CUENTA    ] │ │      │
│    │ └─────────────────────┘ │      │
│    └─────────────────────────┘      │
│         ↑ CardBg + glow top         │
└─────────────────────────────────────┘
```

### 10.3 Especificaciones Detalladas

#### Zona Superior: Marca
- **Logo:** `AceLogoSmall` (80dp), centrado, con animación de pulso.
- **Título "A.C.E":** `DisplayBrand` (32sp, CinzelDecorative, Black, letterSpacing 2sp, blanco).
- **Línea divisoria glow:** 60dp × 2dp, gradient horizontal `Transparent` → `NeonRed@variable` → `Transparent`, cap Round.
- **Subtítulo "ACTIVE CARDIAC EFFORT":** `DisplaySubtitle` (9sp, sistema, Bold, letterSpacing 3sp, `NeonRed@75%`).
- **Spacer entre logo y título:** 18dp.
- **Spacer entre título y línea:** 8dp.
- **Spacer entre línea y subtítulo:** 4dp.
- **Spacer entre subtítulo y tarjeta:** 32dp.

#### Zona Central: Tarjeta de Formulario
- **Tarjeta:** `AceCard` (Surface con `CardBg`, radio 16dp, glow top `NeonRed@35%`, padding 24dp).
- **Título "Iniciar sesión":** `H1` (16sp, UnifrakturMaguntia, Bold, blanco), paddingBottom 20dp.
- **Input Email:** `AceTextField`, label "Correo electrónico".
- **Spacer:** 14dp.
- **Input Password:** `AceTextField`, label "Contraseña", isPassword=true.
- **Spacer:** 8dp.
- **Link "¿Olvidaste tu contraseña?":** Alineado a la derecha, `TextButton`, 12sp, `NeonRed@80%`.
- **Spacer:** 8dp.
- **Botón "Ingresar":** `AceButton` (filled, 52dp, `NeonRed`, texto blanco 14sp UnifrakturMaguntia Black letterSpacing 3sp).
- **Estado loading:** Muestra `CircularProgressIndicator` blanco 20dp en lugar del texto.
- **Mensaje de error:** 12sp, `NeonRed`, aparece debajo del botón con Spacer 12dp.
- **Divisor "ó":** `AceDivider`.
- **Botón "CREAR CUENTA":** `AceOutlinedButton` (outlined, 50dp, borde `NeonRed@50%`, texto `NeonRed` 12sp CinzelDecorative Black letterSpacing 2sp).

#### Fondo
- Completo: Grid + partículas + cubos 3D + vignette.
- Animación de rotación: 14s, linear.

### 10.4 Estados

| Estado | Visual |
|--------|--------|
| **Inicial** | Formulario vacío, botón activo, sin error. |
| **Loading** | Botón muestra spinner, inputs deshabilitados. |
| **Error** | Texto de error debajo del botón, color `NeonRed`, 12sp. |
| **Éxito** | Navegación inmediata a Home (sin transición visual en Login). |

### 10.5 Decisiones de Diseño
- Sin pestaña de registro visible en login principal. Registro es pantalla secundaria vía "Crear cuenta".
- Sin "Recordarme": el refresh token de 7 días cumple esa función.
- Post-login exitoso: descarga fórmulas (S5) + carga stats (S10) en background antes de navegar.

---

## 11. Pantalla: Home / Principal (`02_home.html`)

### 11.1 Rol en el Sistema
- **Sistemas dominantes:** S7 (Streak), S10 (Stats), S6 (Ranking).
- **Sistemas secundarios:** S3 (Sync badge), S8 (Notificaciones banner), S4 (Auth silencioso).
- **Propósito:** Dashboard principal. Motivación + estado general.

### 11.2 Estructura Visual

```
┌─────────────────────────────────────┐
│  [Banner racha 🔥 Racha de X días]  │ ← Condicional, rojo
│  [Banner sync ⚠️ X bloques error]   │ ← Condicional, amarillo
│                                     │
│         ┌─────────────┐             │
│         │   A.C.E     │  ← Logo     │
│         └─────────────┘             │
│                                     │
│    ┌─────────────────────────┐      │
│    │  QUICK STATS            │      │
│    │  [XP]  |  [Pos]  | [Ses]│      │
│    │  1,240    #42      15   │      │
│    └─────────────────────────┘      │
│                                     │
│    [─── Iniciar Ejercicio ───]      │ ← CTA principal
│                                     │
│    ┌─────────────────────────┐      │
│    │  Última sesión          │      │
│    │  Running · 45 min · 30XP│      │
│    └─────────────────────────┘      │
│                                     │
│  [🏠]  [📊]  [🏆]  [👤]            │ ← BottomNav
└─────────────────────────────────────┘
```

### 11.3 Especificaciones Detalladas

#### Banners Condicionales (parte superior)
- **Banner racha (S7):** Aparece solo si `current_streak > 0`. Fondo `NeonRed`, texto blanco 12sp, icono 🔥. Altura 40dp. Tocable (navega a Estadísticas).
- **Banner sync error (S3/S8):** Aparece si hay bloques en estado ERROR. Fondo amarillo (`#FFD600`), texto negro 12sp, icono ⚠️. Altura 40dp. Tocable (navega a Diagnóstico).
- **Prioridad:** Si ambos banners están activos, el de sync error va arriba del de racha.
- **Margen:** 0dp (pegado al top, ancho completo).

#### Cabecera
- **Logo:** `AceLogoSmall` (48dp), sin pulso (estático o sutil). Centrado.
- **Fondo:** `BgBlack` plano o con grid muy sutil. **NO** cubos 3D (evitar distracción).

#### Quick Stats (S10)
- **Layout:** `AceQuickStats` — Row de 3 columnas con separadores verticales.
- **Valores:**
  - Columna 1: Valor = `total_xp`, Label = "XP TOTAL".
  - Columna 2: Valor = `#${global_position}`, Label = "POSICIÓN".
  - Columna 3: Valor = `total_sessions`, Label = "SESIONES".
- **Estilo:** Valor 24sp Black blanco, Label 10sp `TextMuted`.
- **Fondo:** `CardBg`, radio 16dp, padding 16dp.

#### CTA Principal
- **Botón "Iniciar Ejercicio":** `AceButton` (filled, 56dp, `NeonRed`, texto blanco 16sp UnifrakturMaguntia).
- **Posición:** Centrado, debajo de Quick Stats, marginTop 24dp.
- **Icono:** Opcional, flecha derecha o corazón pulsante.

#### Última Sesión (S9)
- **Tarjeta:** `AceCard` (más pequeña, padding 16dp).
- **Título:** "Última sesión", 14sp, `TextSecondary`.
- **Contenido:** Tipo de deporte + duración + XP, 13sp, `TextPrimary`.
- **Icono:** Emoji o icono del deporte a la izquierda.

#### Bottom Navigation
- **Altura:** 56dp.
- **Fondo:** `CardBg` con borde superior 0.5dp `BorderDim`.
- **Items:** 4 tabs — Home (🏠), Estadísticas (📊), Ranking (🏆), Perfil (👤).
- **Estado activo:** Icono + label `NeonRed`, label 11sp.
- **Estado inactivo:** Icono + label `TextMuted`, label 11sp.
- **Label:** Sistema, 11sp.

### 11.4 Estados

| Estado | Visual |
|--------|--------|
| **Normal** | Todo cache local, sin banners. |
| **Con racha** | Banner rojo arriba. |
| **Con errores sync** | Banner amarillo arriba. |
| **Sin conexión** | Quick Stats muestran cache. CTA activo (sesión offline-first). |
| **Token expirado** | Refresh silencioso (sin UI). Si falla → logout forzado. |

### 11.5 Decisiones de Diseño
- Racha **solo** en Home. No en otras pantallas para no saturar.
- Todo en Home es cache local. **Sin peticiones de red** al abrir.
- Diagnóstico sync es banner en Home, detalle completo en Ejercicio.

---

## 12. Pantalla: Ejercicio — Selección (`03_ejercicio.html`)

### 12.1 Rol en el Sistema
- **Sistemas dominantes:** S1 (Wear OS verificación), S2 (sport_type), S5 (fórmulas gate).
- **Sistemas secundarios:** S3 (diagnóstico sync), S8 (badge sync error).
- **Propósito:** Preparación antes de iniciar sesión. Verificación de prerrequisitos.

### 12.2 Estructura Visual

```
┌─────────────────────────────────────┐
│  ← Ejercicio              [🔔]      │ ← AppBar
│                                     │
│    ┌─────────────────────────┐      │
│    │  ⌚ Estado del reloj    │      │
│    │  [Verificar conexión]   │      │ ← S1
│    │  🟢 Conectado / 🟡 Desc.│      │
│    └─────────────────────────┘      │
│                                     │
│    ┌─────────────────────────┐      │
│    │  ⚠️ Diagnóstico Sync    │      │ ← S3 (condicional)
│    │  [▼] 3 bloques con error│      │
│    │  [Lista expandible]     │      │
│    └─────────────────────────┘      │
│                                     │
│    Selecciona deporte:              │
│    ┌────┬────┬────┐                 │
│    │ 🏃 │ 🚴 │ 💪 │                 │ ← Grid deportes
│    │Run │Bike│HIIT│                 │
│    ├────┼────┼────┤                 │
│    │ 🏊 │ 🧗 │ 🥊 │                 │
│    │Swim│Climb│Box │                 │
│    └────┴────┴────┘                 │
│                                     │
│    [──── Iniciar ────]              │ ← Deshabilitado si gate falla
│    "Conecta a internet para         │
│     descargar fórmulas"             │ ← S5 gate
└─────────────────────────────────────┘
```

### 12.3 Especificaciones Detalladas

#### AppBar
- **Título:** "Ejercicio", 18sp, `TextPrimary`, alineado a la izquierda.
- **Back:** Flecha ←, `NeonRed`, 24dp.
- **Badge notificaciones:** Icono 🔔, `NeonRed` si hay errores no vistos, `TextMuted` si no.
- **Fondo:** `BgBlack`, sin elevación.

#### Verificación Wear OS (S1)
- **Tarjeta:** `AceCard` (padding 16dp).
- **Título:** "Estado del reloj", 14sp, `TextSecondary`.
- **Estado conectado:** Indicador verde pulsante (círculo 8dp, `NeonRed` con animación de alpha 0.5→1.0, 800ms) + texto "Conectado" 13sp verde.
- **Estado desconectado:** Indicador amarillo (círculo 8dp, `#FFD600`) + texto "Desconectado" 13sp amarillo + mensaje "Verifica tu reloj".
- **Botón "Verificar conexión":** `AceOutlinedButton` (más pequeño, height 40dp). Dispara verificación manual.
- **Prerrequisito:** Si desconectado >5s, botón "Iniciar" deshabilitado.

#### Diagnóstico Sync (S3, condicional)
- **Tarjeta:** Fondo amarillo tenue (`#FFD600` con alpha 0.1), borde amarillo 1dp.
- **Título:** "Diagnóstico de sincronización", 14sp, amarillo.
- **Contenido:** Texto "X bloques con error", 13sp.
- **Lista expandible:** Items con ID de bloque, duración, motivo del error. Expande al tocar ▼.
- **Acciones:** Botones "Reintentar" y "Descartar" por bloque.

#### Selector de Deporte (S2)
- **Layout:** Grid de 2 o 3 columnas.
- **Items:** Tarjetas cuadradas o rectangulares, `CardBg`, radio 10dp.
- **Estado no seleccionado:** Borde 1dp `BorderDim`, icono `TextSecondary`, label 12sp `TextSecondary`.
- **Estado seleccionado:** Borde 2dp `NeonRed`, fondo `ContainerFocused`, icono `NeonRed`, label 12sp `NeonRed`, `FontWeight.Bold`.
- **Deportes:** Running 🏃, Cycling 🚴, HIIT 💪, Swimming 🏊, Climbing 🧗, Boxing 🥊.
- **BottomSheet:** Al tocar un deporte, aparece BottomSheet con confirmación y config rápida (si aplica).

#### CTA "Iniciar"
- **Botón:** `AceButton` (filled, 52dp).
- **Estado habilitado:** `NeonRed`, texto "Iniciar".
- **Estado deshabilitado:** Fondo gris oscuro (`#1A1A1A`), texto `TextMuted`, no interactivo.
- **Condiciones para habilitar:**
  1. Wear OS conectado (S1).
  2. Fórmulas cacheadas (S5).
  3. Sport_type seleccionado (S2).
- **Mensaje de gate:** Si fórmulas faltan, muestra texto debajo del botón: "Conecta a internet para descargar fórmulas", 12sp, `TextMuted`.

### 12.4 Estados

| Estado | Visual |
|--------|--------|
| **Todo listo** | Wear OS verde, fórmulas OK, deporte seleccionado. Botón "Iniciar" activo. |
| **Wear OS desconectado** | Indicador amarillo. Botón deshabilitado. |
| **Sin fórmulas** | Mensaje de gate visible. Botón deshabilitado. |
| **Con errores sync** | Banner amarillo expandible. No bloquea inicio. |
| **Sin deporte seleccionado** | Botón deshabilitado. |

### 12.5 Decisiones de Diseño
- Verificación Wear OS es **prerrequisito explícito**, no automático. El usuario debe confirmar.
- BottomSheet para selección de deporte: rápido, no interrumpe flujo.
- Fórmulas son gate: sin ellas, no hay sesión.

---

## 13. Pantalla: Sesión Activa (`04_sesion_activa.html`)

### 13.1 Rol en el Sistema
- **Sistemas dominantes:** S1 (FC en vivo), S2 (máquina de estados), S5 (XP por bloque).
- **Sistemas secundarios:** S8 (foreground service), S10 (acumulación stats).
- **Propósito:** Interfaz de ejercicio en tiempo real. Datos cardíacos + temporizador + bloques.

### 13.2 Estructura Visual

```
┌─────────────────────────────────────┐
│  ← Sesión Activa          [⏸] [⏹] │ ← AppBar
│                                     │
│           ┌─────────┐               │
│           │  145    │               │
│           │   BPM   │               │ ← FC grande
│           │  🟢──── │               │ ← Indicador conexión
│           └─────────┘               │
│                                     │
│    ⏱ Tiempo sesión: 12:34          │
│    ⏱ Bloque actual: 03:15 / 05:00  │
│                                     │
│    ┌─────────────────────────┐      │
│    │ BLOQUE ACTUAL (espejo)  │      │
│    │ Avg BPM: 142            │      │
│    │ Max BPM: 158            │      │
│    │ Muestras: 195           │      │
│    │ [████████░░░░░░░░░░]    │      │ ← Progreso visual
│    └─────────────────────────┘      │
│                                     │
│    ┌─────────────────────────┐      │
│    │ BLOQUES COMPLETADOS     │      │
│    │ #1 · 05:01 · 142bpm ·   │      │
│    │      +10 XP             │      │
│    │ #2 · 05:03 · 138bpm ·   │      │
│    │      +10 XP             │      │
│    └─────────────────────────┘      │
│                                     │
│         [──── Terminar ────]        │
│                                     │
│  ┌─────────────────────────────┐    │
│  │  A.C.E — Sesión activa      │    │ ← Foreground chip
│  └─────────────────────────────┘    │
└─────────────────────────────────────┘
```

### 13.3 Especificaciones Detalladas

#### AppBar
- **Título:** "Sesión Activa", 18sp, `TextPrimary`.
- **Acciones:**
  - ⏸ Pausa: Icono pause, `NeonRed`, 24dp. Cambia estado S2 a PAUSED.
  - ⏹ Terminar: Icono stop, `TextSecondary`, 24dp. Cambia estado S2 a COMPLETED.

#### Zona de FC (S1)
- **Valor principal:** FC actual en BPM, 72sp, `FontWeight.Black`, `NeonRed`.
- **Label "BPM":** 14sp, `TextSecondary`, debajo del valor.
- **Indicador de conexión:**
  - Conectado: Círculo verde pulsante (8dp, alpha 0.5→1.0, 800ms) + línea horizontal animada.
  - Desconectado: Círculo amarillo + texto "Desconectado" 12sp amarillo.
- **Posición:** Centrado, ocupa ~30% de la pantalla vertical.

#### Temporizadores (S2)
- **Tiempo de sesión:** 14sp, `TextSecondary`. Formato MM:SS.
- **Tiempo de bloque:** 16sp, `TextPrimary`, `FontWeight.Medium`. Formato "MM:SS / 05:00".
- **Posición:** Debajo del FC, alineado a la izquierda con padding 24dp.

#### Bloque Actual — Espejo (S2)
- **Tarjeta:** `AceCard` (padding 16dp).
- **Título:** "BLOQUE ACTUAL", 12sp, `TextMuted`, letterSpacing 2sp.
- **Métricas:**
  - "Avg BPM: [valor]", 14sp, `TextPrimary`.
  - "Max BPM: [valor]", 14sp, `TextPrimary`.
  - "Muestras: [valor]", 14sp, `TextPrimary`.
- **Barra de progreso:**
  - Ancho: fillMaxWidth.
  - Altura: 8dp.
  - Fondo: `BorderDim`.
  - Progreso: `NeonRed`, radio 4dp.
  - Valor: `tiempo_actual / 300s` (5 minutos).
- **IMPORTANTE:** NO muestra XP en el bloque actual. El bloque es "espejo": métricas en progreso, no finales.

#### Bloques Completados (S2 + S5)
- **Tarjeta:** `AceCard` (padding 16dp).
- **Título:** "BLOQUES COMPLETADOS", 12sp, `TextMuted`, letterSpacing 2sp.
- **Lista:** `AceBlockList`.
- **Cada item:**
  - "#[número]", 12sp, `TextSecondary`.
  - Duración + avg BPM, 13sp, `TextPrimary`.
  - "+[XP] XP", 14sp, `NeonRed`, `FontWeight.Bold`. Aparece al cerrar el bloque con animación de fade in + scale (200ms).
- **Scroll:** La lista es scrollable si hay más de 3 bloques.

#### Botón Terminar
- **Posición:** Fijo en la parte inferior, encima del foreground chip.
- **Estilo:** `AceButton` (filled, 52dp, `NeonRed`, texto "Terminar Sesión").
- **Comportamiento:** Al tocar, cierra bloque abierto, calcula XP final, navega a Resumen.

#### Foreground Chip (S8)
- **Posición:** Anclado a bottom, centrado, marginBottom 16dp.
- **Estilo:** `AceForegroundChip`.
- **Texto:** "A.C.E — Sesión activa".
- **Nota:** Refleja la notificación persistente del sistema. No interactivo.

### 13.4 Estados de la Sesión (S2)

| Estado | Visual |
|--------|--------|
| **ACTIVE** | Temporizador corriendo. FC en vivo. Botón ⏸ visible. Foreground chip visible. |
| **PAUSED** | Temporizador pausado. FC congelado (último valor). Botón ▶ visible (reanudar). |
| **COMPLETED** | Navega a Resumen. No hay UI en esta pantalla para este estado. |
| **ABORTED** | No implementado en MVP. |

### 13.5 Decisiones de Diseño
- XP se calcula por bloque pero se **muestra** en lista de completados, no en el bloque actual.
- Bloque actual es "espejo": métricas en progreso, no finales.
- Foreground service obligatorio. El usuario no puede evitar la notificación.
- Si app muere con bloque OPEN, se pierde (aceptable por diseño).

---

## 14. Pantalla: Resumen Post-Sesión (`05_resumen_sesion.html`)

### 14.1 Rol en el Sistema
- **Sistemas dominantes:** S2 (cierre), S5 (suma XP), S10 (totales).
- **Sistemas secundarios:** S7 (racha display), S9 (historial FIFO), S3 (disparo sync).
- **Propósito:** Recompensa visual post-ejercicio. Celebración + resumen.

### 14.2 Estructura Visual

```
┌─────────────────────────────────────┐
│                                     │
│         🎉 ¡Sesión completada!      │
│              Running                │
│           45 minutos                │
│                                     │
│              ┌─────┐                │
│              │ 30  │                │
│              │  XP │                │ ← Celebratorio
│              └─────┘                │
│                                     │
│    ┌─────────────────────────┐      │
│    │ DESGLOSE POR BLOQUE     │      │
│    │ #1 · 05:01 · 142bpm ·   │      │
│    │      +10 XP             │      │
│    │ #2 · 05:03 · 138bpm ·   │      │
│    │      +10 XP             │      │
│    │ #3 · 04:58 · 145bpm ·   │      │
│    │      +10 XP             │      │
│    └─────────────────────────┘      │
│                                     │
│    ┌─────────────────────────┐      │
│    │ STATS DE LA SESIÓN      │      │
│    │ Duración: 45:02         │      │
│    │ Avg BPM: 142            │      │
│    │ Bloques: 3              │      │
│    │ Muestras: 1,247         │      │
│    └─────────────────────────┘      │
│                                     │
│    🔥 Racha actualizada: 5 días     │
│                                     │
│    [─── Volver al inicio ───]       │
│                                     │
└─────────────────────────────────────┘
```

### 14.3 Especificaciones Detalladas

#### Cabecera Celebratoria
- **Icono/Título:** "¡Sesión completada!", 24sp, `FontWeight.Black`, `Color.White`, `CinzelDecorative`.
- **Tipo deporte:** 16sp, `TextSecondary`. Ej: "Running".
- **Duración total:** 18sp, `TextPrimary`, `FontWeight.Medium`.
- **Animación:** Fade in + slide up, 400ms, easing FastOutSlowInEasing.

#### XP Total (S5)
- **Valor:** XP total de la sesión, 56sp, `FontWeight.Black`, `NeonRed`.
- **Label "XP":** 14sp, `TextSecondary`, debajo.
- **Animación:** Contador animado de 0 al valor final, duración 800ms, easing FastOutSlowInEasing.
- **Glow:** Gradient radial `NeonRed@30%` → `Transparent` detrás del número.

#### Desglose por Bloque (S5)
- **Tarjeta:** `AceCard`.
- **Título:** "DESGLOSE POR BLOQUE", 12sp, `TextMuted`, letterSpacing 2sp.
- **Lista:** Misma estructura que en Sesión Activa, pero todos los bloques ya tienen XP calculada.
- **Animación:** Stagger fade in, 100ms entre items.

#### Stats de la Sesión (S10)
- **Tarjeta:** `AceCard`.
- **Título:** "STATS DE LA SESIÓN", 12sp, `TextMuted`, letterSpacing 2sp.
- **Items:**
  - "Duración: [MM:SS]"
  - "Avg BPM: [valor]"
  - "Bloques: [número]"
  - "Muestras: [número]"
- **Estilo:** Label 12sp `TextSecondary`, Valor 14sp `TextPrimary`.

#### Racha (S7)
- **Texto:** "🔥 Racha actualizada: X días", 14sp, `NeonRed`, `FontWeight.Medium`.
- **Nota:** El backend evalúa la racha al syncar. El móvil solo muestra el valor del cache. Puede estar desactualizado hasta el primer sync.

#### CTA
- **Botón:** `AceButton` (filled, 52dp, `NeonRed`, "Volver al inicio").
- **Comportamiento:** Navega a Home.

#### Fondo
- `BgBlack` plano. **NO** cubos ni partículas (la atención debe estar en el resumen).
- Opcional: Partículas de celebración (confeti sutil en `NeonRed` y blanco) que caen desde arriba durante 2 segundos.

### 14.4 Estados

| Estado | Visual |
|--------|--------|
| **Resumen normal** | Todo visible, XP local mostrada. |
| **Sync en progreso** | Spinner sutil en esquina superior. No bloquea UI. |
| **Corrección XP > 10** | Toast: "Estadísticas actualizadas desde servidor". |

### 14.5 Decisiones de Diseño
- XP mostrada es **local**, no espera respuesta de backend. Corrección silenciosa si dif < 10 XP.
- Racha mostrada es cache; backend la evaluará al syncar.
- Animación celebratoria (sparks/confeti) refuerza el loop de recompensa.

---

*Fin de la Parte 3. Continúa en Parte 4: Pantallas (Ranking, Estadísticas, Perfil, Registro, Diagnóstico), Estados Condicionales, Directivas para LLM, Glosario.*
