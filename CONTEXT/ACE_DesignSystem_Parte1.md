# A.C.E — Manual de Diseño de Interfaz (Design System v1.0)

> **Estado:** Aprobado para generación de UI por LLM (Claude, GPT-4, etc.)  
> **Versión:** 1.0  
> **Fecha:** 2026-06-18  
> **Proyecto:** A.C.E (Active Cardiac Effort)  
> **Plataforma:** Android (Jetpack Compose)  
> **Enfoque:** Dark Theme, Neon Red, Cyberpunk/Sport Minimalist  
> **Uso:** Este documento es la fuente de verdad para generar pantallas completas, componentes individuales, animaciones y estados condicionales. **No incluye código.** Solo especificaciones visuales, de comportamiento y de tokens.

---

## Índice General

- **Parte 1 (esta):** Filosofía, Tokens, Tipografía, Espaciado, Formas, Animaciones
- **Parte 2:** Componentes Atómicos, Fondo A.C.E, Logo A.C.E
- **Parte 3:** Pantallas (Login, Home, Ejercicio, Sesión Activa, Resumen)
- **Parte 4:** Pantallas (Ranking, Estadísticas, Perfil, Registro, Diagnóstico), Estados Condicionales, Directivas para LLM, Glosario

---

## 1. Filosofía de Diseño

### 1.1 Identidad Visual

A.C.E es una aplicación de fitness cardíaco de alto rendimiento. Su identidad visual transmite:

- **Intensidad:** El rojo neón simboliza el esfuerzo cardíaco, la sangre, la energía.
- **Precisión:** Las líneas limpias, la tipografía gótica/fracturada y las formas geométricas sugieren disciplina y rigor.
- **Tecnología:** El fondo animado con cubos 3D wireframe y grid perspectivo evoca una interfaz de comando futurista.
- **Minimalismo funcional:** Cada elemento tiene un propósito. No hay decoración superflua.

### 1.2 Principios Rectores

| Principio | Definición | Manifestación |
|-----------|-----------|---------------|
| **Oscuridad como canvas** | El fondo negro profundo es el 95% de la superficie. El contenido flota sobre él. | `BgBlack` (#050505) como base universal. |
| **Rojo neón como acento único** | Un solo color de acento para toda la app. Nunca se mezcla con otros colores primarios. | `NeonRed` (#FF1744) para CTA, estados activos, bordes glow, indicadores. |
| **Profundidad por capas** | El fondo tiene profundidad (cubos 3D, partículas). El contenido está en primer plano (tarjetas). | Fondo animado → Vignette radial → Contenido centrado. |
| **Tipografía dual** | Dos fuentes con roles estrictamente separados: una gótica para títulos/emoción, una serif decorativa para identidad/estructura. | `UnifrakturMaguntia` (gótica) vs `CinzelDecorative` (serif). |
| **Feedback inmediato** | Cada interacción tiene respuesta visual en <100ms. | Animaciones de pulso, glow, transiciones suaves. |
| **Estados visibles** | Los estados del sistema (sync, conexión, errores) se manifiestan visualmente sin ser intrusivos. | Banners sutiles, indicadores de color, spinners mínimos. |

### 1.3 Metáfora Visual

> **"Un cockpit deportivo de alta tecnología."**
>
> El usuario no está en una app de fitness común. Está en una interfaz de control de rendimiento cardíaco. Los números son grandes y claros. Los estados críticos son rojos. El fondo es oscuro para reducir fatiga visual durante sesiones prolongadas.

---

## 2. Tokens Fundamentales

### 2.1 Paleta de Colores

#### Colores Primarios

| Token | Hex | Uso | Restricciones |
|-------|-----|-----|---------------|
| `NeonRed` | `#FF1744` | CTA primario, bordes activos, cursor, indicadores de estado crítico, logo. | **NUNCA** usar como fondo de área grande. Solo acentos, bordes, texto pequeño. |
| `BgBlack` | `#050505` | Fondo universal de toda la app. | Siempre debe ser el color base. |
| `CardBg` | `#0D0D0D` | Fondo de tarjetas, surfaces elevadas, modales. | Un grado más claro que el fondo para crear separación sutil. |

#### Colores de Estado y Feedback

| Token | Hex | Uso |
|-------|-----|-----|
| `BorderDim` | `#2A2A2A` | Bordes inactivos, divisores, líneas de separación. |
| `GridColor` | `rgba(255,23,68,0.03)` | Líneas de grid de fondo. Muy tenue, casi imperceptibles. |
| `TextPrimary` | `#FFFFFF` | Texto principal, títulos, valores importantes. |
| `TextSecondary` | `#CCCCCC` | Texto de input inactivo, descripciones. |
| `TextMuted` | `#555555` | Labels de input inactivos, texto de menor jerarquía. |
| `TextDim` | `#444444` | Texto de separadores ("ó"), timestamps. |
| `ContainerFocused` | `#100808` | Fondo de input cuando tiene foco. Tinte rojo muy sutil. |
| `ContainerUnfocused` | `#0A0A0A` | Fondo de input sin foco. |

#### Escalas de Alpha para NeonRed

| Alpha | Uso |
|-------|-----|
| `1.0` | Botón primario, cursor, borde activo de input. |
| `0.80` | Links secundarios ("¿Olvidaste tu contraseña?"). |
| `0.75` | Subtítulos de marca ("ACTIVE CARDIAC EFFORT"). |
| `0.50` | Bordes de botón outlined secundario. |
| `0.35` | Borde superior glow de tarjetas. |
| `0.22` | Cubos 3D (capa exterior). |
| `0.20` | Glow radial del logo, sombra de trazo del logo. |
| `0.18` | Cubos 3D (capa interior). |
| `0.15` | Círculo exterior del logo (stroke tenue). |
| `0.12` | Cubos 3D más lejanos. |
| `0.04` | Partículas de fondo más lejanas. |

#### Colores de Estado del Sistema

| Estado | Color | Manifestación |
|--------|-------|---------------|
| Éxito / Conectado | Verde neón (`#00E676` o similar) | Indicador pulsante en Ejercicio/Sesión Activa. |
| Advertencia / Desconectado | Amarillo (`#FFD600` o similar) | Banner de sync error, indicador Wear OS desconectado. |
| Error | `NeonRed` puro | Mensajes de error, banner sync crítico. |
| Info / Neutral | Blanco tenue | Texto descriptivo, indicadores inactivos. |

### 2.2 Sistema de Sombras y Glow

A.C.E no usa sombras tradicionales (elevation de Material). Usa **glow** basado en gradientes radiales y bordes luminosos.

| Tipo | Especificación |
|------|---------------|
| **Glow de tarjeta** | Borde superior con gradiente vertical: `NeonRed@35%` → `Transparent`. Grosor: 1dp. |
| **Glow de logo** | Gradient radial: `NeonRed@20%` → `Transparent`. Radio: 0.7× del tamaño del logo. |
| **Vignette central** | Gradient radial: `Transparent` → `BgBlack@70%`. Radio: 0.75× min(width, height). Centrado. |
| **Glow de línea divisoria** | Gradient horizontal: `Transparent` → `NeonRed@variable` → `Transparent`. Variable controlada por animación (0.4–1.0). |

---

## 3. Sistema Tipográfico

### 3.1 Fuentes

| Fuente | Rol | Peso | Archivo | Uso |
|--------|-----|------|---------|-----|
| **UnifrakturMaguntia** | Emocional / Acción | Normal (400) | `unifrakturmaguntia_regular.ttf` | Títulos de acción, botones primarios, textos que requieren impacto visual. |
| **CinzelDecorative** | Identidad / Estructura | Normal (400) | `cinzeldecorative_regular.ttf` | Nombre de marca ("A.C.E"), botones secundarios, textos de identidad. |
| **Sistema** | Funcional / Legible | Variable | Roboto o Inter (default del sistema) | Labels de input, mensajes de error, texto de cuerpo, descripciones. |

### 3.2 Escala Tipográfica

| Token | Tamaño | Peso | Letter Spacing | Fuente | Uso |
|-------|--------|------|----------------|--------|-----|
| `DisplayBrand` | 32sp | Black (900) | 2sp | CinzelDecorative | Nombre de marca "A.C.E" en pantallas de auth. |
| `DisplaySubtitle` | 9sp | Bold (700) | 3sp | Sistema | Subtítulo de marca "ACTIVE CARDIAC EFFORT". Color: `NeonRed@75%`. |
| `H1` | 16sp | Bold (700) | 0sp | UnifrakturMaguntia | Títulos de tarjetas ("Iniciar sesión"). |
| `H2` | 14sp | Black (900) | 3sp | UnifrakturMaguntia | Texto de botón primario ("Ingresar"). |
| `H3` | 12sp | Black (900) | 2sp | CinzelDecorative | Texto de botón secundario outlined ("CREAR CUENTA"). |
| `Body` | 13sp | Normal (400) | 0sp | Sistema | Labels de input, texto de cuerpo. |
| `Caption` | 12sp | Normal (400) | 0sp | Sistema | Links secundarios, mensajes de error. |
| `Micro` | 11sp | Normal (400) | 0sp | Sistema | Texto de separador ("ó"), timestamps. |

### 3.3 Reglas Tipográficas

- **NUNCA** mezclar UnifrakturMaguntia y CinzelDecorative en la misma línea de texto.
- **NUNCA** usar UnifrakturMaguntia para texto de cuerpo o descripciones largas. Es puramente decorativa/emocional.
- El letter spacing amplio (2–3sp) se reserva para elementos de marca y botones. El texto funcional usa spacing normal (0sp).
- Los títulos de tarjeta (H1) deben tener `padding(bottom = 20dp)` para separarlos del contenido.

---

## 4. Sistema de Espaciado

### 4.1 Escala de Espaciado

A.C.E usa una escala base de **4dp**, con multiplicadores que siguen una progresión lógica:

| Token | Valor | Uso |
|-------|-------|-----|
| `spaceMicro` | 4dp | Separación mínima entre elementos relacionados (ej: debajo de línea divisoria glow). |
| `spaceSmall` | 8dp | Separación entre elementos cercanos (ej: entre label y input, entre error y siguiente elemento). |
| `spaceMedium` | 14dp | Separación entre campos de formulario. |
| `spaceStandard` | 16dp | Separación entre secciones dentro de una tarjeta. |
| `spaceLarge` | 18dp | Separación entre logo y título de marca. |
| `spaceXLarge` | 20dp | Separación entre título de tarjeta y primer campo. |
| `spaceXXLarge` | 32dp | Separación entre cabecera de marca y tarjeta de contenido. |

### 4.2 Padding de Contenedores

| Contenedor | Padding Horizontal | Padding Vertical |
|------------|-------------------|------------------|
| Pantalla completa (Column raíz) | 28dp | 0dp (centrado verticalmente) |
| Tarjeta Surface | 24dp | 24dp |
| Input OutlinedTextField | 0dp (fillMaxWidth) | 0dp |

### 4.3 Tamaños de Componentes

| Componente | Altura | Anchura | Notas |
|------------|--------|---------|-------|
| Botón primario (filled) | 52dp | fillMaxWidth | |
| Botón secundario (outlined) | 50dp | fillMaxWidth | 2dp menos que el primario para jerarquía visual. |
| TextField | 56dp (implícito Material) | fillMaxWidth | |
| Logo pequeño | 72dp–80dp | 72dp–80dp | Escalable con pulso. |
| CircularProgressIndicator | 20dp | 20dp | Dentro de botón primario durante carga. |
| Línea divisoria glow | 2dp | 60dp | Debajo del título de marca. |

---

## 5. Sistema de Formas y Bordes

### 5.1 Radio de Esquinas

| Token | Valor | Uso |
|-------|-------|-----|
| `radiusSmall` | 10dp | Inputs (OutlinedTextField), botones (Button, OutlinedButton). |
| `radiusLarge` | 16dp | Tarjetas Surface, modales, BottomSheets. |

### 5.2 Bordes

| Tipo | Grosor | Color/Brush | Uso |
|------|--------|-------------|-----|
| `BorderActive` | 1dp | `NeonRed` | Borde de input enfocado, borde de botón outlined. |
| `BorderInactive` | 1dp | `BorderDim` (#2A2A2A) | Borde de input sin foco. |
| `BorderCardGlow` | 1dp | Gradient vertical `NeonRed@35%` → `Transparent` | Borde superior de tarjetas Surface. |
| `BorderLogoOuter` | 1dp | `NeonRed@15%` | Círculo exterior del logo. |
| `BorderLogoMain` | 2.5dp | `NeonRed` puro | Círculo principal del logo. |
| `BorderDivider` | 0.5dp | `BorderDim` | Líneas divisorias horizontales ("ó"). |

### 5.3 Strokes de Dibujo Canvas

| Elemento | Grosor | Cap | Join | Notas |
|----------|--------|-----|------|-------|
| Trazo del logo (sombra) | 11.25dp (4.5 × 2.5) | Round | Round | `NeonRed@20%`, detrás del trazo blanco. |
| Trazo del logo (principal) | 4.5dp | Round | Round | `Color.White`. |
| Barra horizontal del logo | 3dp | Round | — | `NeonRed`. |
| Líneas de cubos 3D (exterior) | 5dp × factor profundidad | Round | — | `NeonRed@alpha_variable`. |
| Líneas de cubos 3D (interior) | 1.2dp × factor profundidad | Round | — | `lerp(NeonRed, White, 0.25)@alpha_variable`. |

---

## 6. Sistema de Animaciones

### 6.1 Filosofía de Animación

> **"Las animaciones en A.C.E no son decorativas. Son funcionales y atmosféricas."**
>
> - Las animaciones de fondo crean inmersión sin distraer.
> - Las animaciones de UI proporcionan feedback de estado.
> - Las animaciones de transición guían al usuario entre estados.

### 6.2 Tokens de Animación

| Token | Tipo | Duración | Easing | Valores | Uso |
|-------|------|----------|--------|---------|-----|
| `animRotation` | Infinite, tween | 14,000ms (14s) | LinearEasing | 0 → 2π | Rotación de cubos 3D y partículas de fondo. |
| `animPulse` | Infinite, tween, reverse | 950ms | FastOutSlowInEasing | 1.0 → 1.07 | Pulso del logo (escala 7%). |
| `animLineGlow` | Infinite, tween, reverse | 1,400ms | LinearEasing | 0.4 → 1.0 | Brillo de la línea divisoria debajo del título. |
| `animFadeIn` | One-shot, tween | 300ms | FastOutSlowInEasing | 0 → 1 | Aparición de elementos (default si no se especifica). |
| `animSlideUp` | One-shot, tween | 400ms | FastOutSlowInEasing | 20dp → 0dp | Entrada de modales, BottomSheets. |

### 6.3 Especificaciones de Animaciones Clave

#### Animación de Pulso del Logo
- **Trigger:** Siempre activa (pantalla visible).
- **Comportamiento:** Escala del logo oscila entre 1.0 y 1.07 (7% de crecimiento).
- **Duración:** 950ms por ciclo.
- **Easing:** FastOutSlowInEasing (aceleración rápida, desaceleración suave).
- **Repetición:** Infinite, Reverse.
- **Aplicación:** `graphicsLayer { scaleX = pulse; scaleY = pulse }`.

#### Animación de Rotación de Fondo
- **Trigger:** Siempre activa.
- **Comportamiento:** Rotación continua de 0 a 360° (2π radianes).
- **Duración:** 14,000ms (14 segundos) por ciclo completo.
- **Easing:** LinearEasing (velocidad constante, sin aceleración).
- **Repetición:** Infinite, Restart.
- **Afecta:** Rotación de cubos 3D (cada cubo tiene speedX/speedY propios) y partículas de profundidad.

#### Animación de Glow de Línea
- **Trigger:** Siempre activa.
- **Comportamiento:** Alpha del color rojo en la línea divisoria oscila entre 0.4 y 1.0.
- **Duración:** 1,400ms por ciclo.
- **Easing:** LinearEasing.
- **Repetición:** Infinite, Reverse.
- **Aplicación:** Línea horizontal debajo del título "A.C.E".

### 6.4 Reglas de Animación

- **NUNCA** usar animaciones que duren más de 500ms para feedback de UI (botones, inputs).
- Las animaciones de fondo pueden ser lentas (10s+) porque son atmosféricas.
- Las animaciones de estado (carga, éxito, error) deben ser inmediatas (<300ms).
- **NUNCA** animar la opacidad del fondo (BgBlack). El fondo siempre es opaco.

---

*Fin de la Parte 1. Continúa en Parte 2: Componentes Atómicos, Fondo A.C.E, Logo A.C.E.*
