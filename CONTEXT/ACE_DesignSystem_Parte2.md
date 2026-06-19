# A.C.E — Manual de Diseño de Interfaz (Design System v1.0)

## Parte 2: Componentes Atómicos, Fondo A.C.E, Logo A.C.E

---

## 7. Componentes Atómicos

### 7.1 Logo A.C.E (AceLogo)

**Descripción:** El logo es un círculo con un trazo estilizado de electrocardiograma (ECG) en su interior, representando el esfuerzo cardíaco.

#### Variantes

| Variante | Tamaño | Uso |
|----------|--------|-----|
| `AceLogoSmall` | 72dp–80dp | Pantallas de auth (Login, Registro). |
| `AceLogoMedium` | 120dp | Splash screen, pantallas de carga. |
| `AceLogoLarge` | 180dp | Pantalla de onboarding (si aplica). |

#### Estructura Visual (de adentro hacia afuera)

1. **Glow radial interno:** Círculo relleno con gradiente radial `NeonRed@20%` → `Transparent`. Radio: 0.7× del tamaño total.
2. **Círculo exterior tenue:** Stroke de 1dp, `NeonRed@15%`, radio: (size/2) - 1dp.
3. **Círculo principal:** Stroke de 2.5dp, `NeonRed` puro, radio: (size/2) - 2dp.
4. **Trazo ECG (sombra):** Path de líneas conectadas formando una onda cardíaca estilizada. Stroke: 11.25dp, `NeonRed@20%`, cap Round, join Round.
5. **Trazo ECG (principal):** Mismo path. Stroke: 4.5dp, `Color.White`, cap Round, join Round.
6. **Barra horizontal:** Línea horizontal centrada en Y=50% del canvas, desde X=42% hasta X=58%. Stroke: 3dp, `NeonRed`, cap Round.

#### Coordenadas del Path ECG (relativas al canvas W×H)

```
moveTo(0.18W, 0.55H)
lineTo(0.31W, 0.55H)
lineTo(0.37W, 0.65H)
lineTo(0.50W, 0.22H)   // Pico central (subida)
lineTo(0.63W, 0.76H)   // Valle (bajada)
lineTo(0.69W, 0.55H)
lineTo(0.82W, 0.55H)
```

#### Comportamiento
- Siempre centrado horizontalmente en su contenedor.
- Aplica animación de pulso (`animPulse`) cuando está en pantallas de auth.
- El glow radial debe ser más grande que el círculo principal (radio 0.7× vs 0.5×).

---

### 7.2 Tarjeta Surface (AceCard)

**Descripción:** Contenedor principal de contenido en pantallas de auth. Flota sobre el fondo animado.

#### Especificaciones

| Atributo | Valor |
|----------|-------|
| Fondo | `CardBg` (#0D0D0D) |
| Radio de esquinas | 16dp (`radiusLarge`) |
| Borde superior | Gradient vertical: `NeonRed@35%` → `Transparent`, 1dp |
| Elevación | 0dp (sin sombra Material). La separación se logra con color y borde. |
| Padding interno | 24dp en todos los lados |
| Ancho | fillMaxWidth (con padding horizontal de pantalla de 28dp) |

#### Uso
- Envuelve formularios de Login y Registro.
- Puede usarse en otras pantallas para agrupar contenido relacionado.
- **NUNCA** anidar tarjetas dentro de tarjetas.

---

### 7.3 Input de Texto (AceTextField)

**Descripción:** Campo de texto estilizado para formularios. Basado en Material OutlinedTextField con colores personalizados.

#### Especificaciones

| Estado | Border | Label | Texto | Fondo | Cursor |
|--------|--------|-------|-------|-------|--------|
| **Enfocado** | `NeonRed`, 1dp | `NeonRed`, 13sp | `TextPrimary` (#FFFFFF), 13sp | `ContainerFocused` (#100808) | `NeonRed` |
| **Sin foco** | `BorderDim` (#2A2A2A), 1dp | `TextMuted` (#555555), 13sp | `TextSecondary` (#CCCCCC), 13sp | `ContainerUnfocused` (#0A0A0A) | — |

#### Comportamiento
- Siempre `singleLine = true`.
- Radio de esquinas: 10dp (`radiusSmall`).
- Para contraseñas: `PasswordVisualTransformation()`.
- Ancho: `fillMaxWidth()`.
- Altura implícita de Material (~56dp).

---

### 7.4 Botón Primario (AceButton)

**Descripción:** Botón de acción principal. Filled, alto contraste.

#### Especificaciones

| Atributo | Valor |
|----------|-------|
| Fondo | `NeonRed` (#FF1744) |
| Texto | `Color.White`, 14sp, `FontWeight.Black`, letterSpacing 3sp |
| Fuente | `UnifrakturMaguntia` |
| Altura | 52dp |
| Ancho | fillMaxWidth |
| Radio de esquinas | 10dp (`radiusSmall`) |
| Estado disabled | Mantiene fondo `NeonRed` pero con alpha reducida (o gris oscuro si se prefiere). |

#### Estados

| Estado | Visual |
|--------|--------|
| **Normal** | Fondo `NeonRed`, texto blanco. |
| **Loading** | Muestra `CircularProgressIndicator` blanco (20dp, strokeWidth 2dp) en lugar del texto. |
| **Disabled** | Fondo atenuado o gris oscuro. Texto atenuado. |

---

### 7.5 Botón Secundario Outlined (AceOutlinedButton)

**Descripción:** Botón de acción secundaria. Outlined, menos prominente.

#### Especificaciones

| Atributo | Valor |
|----------|-------|
| Fondo | Transparente |
| Borde | 1dp, `NeonRed@50%` |
| Texto | `NeonRed`, 12sp, `FontWeight.Black`, letterSpacing 2sp |
| Fuente | `CinzelDecorative` |
| Altura | 50dp (2dp menos que el primario) |
| Ancho | fillMaxWidth |
| Radio de esquinas | 10dp (`radiusSmall`) |

---

### 7.6 Divisor con Texto (AceDivider)

**Descripción:** Línea horizontal con texto centrado, usado para separar opciones de auth.

#### Especificaciones

- Dos `HorizontalDivider` con `weight(1f)`, color `BorderDim`, grosor 0.5dp.
- Texto centrado: " ó " (con espacios), 11sp, color `TextDim` (#444444).
- Altura total del Row: implícita.

---

### 7.7 Indicador de Carga (AceLoading)

**Descripción:** CircularProgressIndicator estilizado.

#### Especificaciones

| Contexto | Tamaño | Color | Stroke |
|----------|--------|-------|--------|
| Dentro de botón | 20dp | White | 2dp |
| Pantalla completa | 48dp | `NeonRed` | 4dp |
| Sobre contenido | 32dp | `NeonRed` | 3dp |

---

### 7.8 Banner de Estado (AceBanner)

**Descripción:** Indicador horizontal condicional que aparece en la parte superior de pantallas.

#### Variantes

| Tipo | Color de fondo | Texto | Icono | Pantalla |
|------|---------------|-------|-------|----------|
| **Racha** | Rojo intenso (`NeonRed`) | "🔥 Racha de X días" | Fuego | Home |
| **Sync Error** | Amarillo (`#FFD600`) | "X bloques sin sincronizar" | Warning | Home, Ejercicio |
| **Sync en progreso** | `CardBg` con borde sutil | "Sincronizando..." | Spinner sutil | Estadísticas |
| **Wear OS desconectado** | Amarillo (`#FFD600`) | "Reloj desconectado" | Bluetooth off | Ejercicio |

#### Especificaciones
- Altura: 40dp–48dp.
- Ancho: fillMaxWidth.
- Padding horizontal: 16dp.
- Texto: 12sp, `FontWeight.Medium`.
- **Comportamiento:** Tocable. Navega a la pantalla de detalle correspondiente.

---

### 7.9 BottomSheet (AceBottomSheet)

**Descripción:** Panel deslizable desde abajo para selección de opciones.

#### Especificaciones

- Fondo: `CardBg` (#0D0D0D).
- Radio superior: 16dp (`radiusLarge`).
- Borde superior: Gradient `NeonRed@35%` → `Transparent`, 1dp.
- Padding: 24dp horizontal, 16dp vertical.
- Handle (indicador de arrastre): Rectángulo redondeado 32dp × 4dp, color `BorderDim`, centrado horizontalmente, margen superior 8dp.
- Contenido: Grid de opciones (ej: deportes) o lista de items.

---

### 7.10 Chip Foreground (AceForegroundChip)

**Descripción:** Indicador flotante que refleja el estado del foreground service.

#### Especificaciones

- Forma: RoundedCornerShape(24dp) (pill shape).
- Fondo: `CardBg` con borde sutil `NeonRed@30%`.
- Texto: "A.C.E — Sesión activa", 11sp, `TextSecondary`.
- Posición: Flotante, anclado a la parte inferior de la pantalla, centrado horizontalmente, marginBottom 16dp.
- Comportamiento: No interactivo. Es puramente informativo.

---

### 7.11 Lista de Bloques (AceBlockList)

**Descripción:** Lista de bloques de ejercicio completados, mostrados en Sesión Activa y Resumen.

#### Especificaciones

- Cada item: Row con padding 12dp vertical.
- Separador: Divider de 0.5dp, `BorderDim`.
- Contenido por item:
  - Izquierda: Número de bloque ("Bloque 1"), 12sp, `TextSecondary`.
  - Centro: Duración + avg BPM, 13sp, `TextPrimary`.
  - Derecha: XP ganada ("+10 XP"), 14sp, `NeonRed`, `FontWeight.Bold`.
- Animación de entrada: Fade in + slide up, 200ms, stagger 50ms por item.

---

### 7.12 Quick Stats (AceQuickStats)

**Descripción:** Resumen numérico en Home. Tres valores en fila horizontal.

#### Especificaciones

- Layout: Row con 3 Columnas de `weight(1f)`, espaciado evenly.
- Cada columna:
  - Valor: 24sp, `FontWeight.Black`, `Color.White`.
  - Label: 10sp, `TextMuted`.
- Separadores verticales: 1dp, `BorderDim`, altura 24dp.
- Valores típicos: XP total, Posición global, Sesiones completadas.

---

## 8. El Fondo A.C.E

### 8.1 Descripción General

El fondo de A.C.E no es un color plano. Es un **sistema de capas visuales** que crea profundidad e inmersión:

```
Capa 5 (frontal): Contenido de UI (tarjetas, texto, botones)
Capa 4: Vignette radial oscurecedor (BgBlack@70%)
Capa 3: Cubos 3D wireframe (7 cubos, rotación lenta)
Capa 2: Partículas de profundidad (40 partículas, movimiento Z)
Capa 1 (fondo): Grid perspectivo (líneas cada 65dp)
Capa 0 (base): BgBlack (#050505)
```

### 8.2 Grid Perspectivo (Capa 1)

- **Tipo:** Grid ortogonal, no perspectiva real.
- **Color:** `GridColor` = `NeonRed` con alpha 0.03 (casi invisible).
- **Espaciado:** 65dp entre líneas (horizontal y vertical).
- **Grosor:** 0.5dp.
- **Comportamiento:** Estático. No se anima.

### 8.3 Partículas de Profundidad (Capa 2)

- **Cantidad:** 40 partículas.
- **Forma:** Círculos pequeños.
- **Color:** `NeonRed` con alpha variable (0.04 a 0.35).
- **Tamaño:** 1.6dp × factor de escala (basado en profundidad Z).
- **Movimiento:** Simulación de profundidad Z. Las partículas "vienen" desde lejos (Z grande, pequeñas, tenues) hacia el frente (Z pequeño, grandes, brillantes).
- **Ciclo:** Cada partícula tiene un `progressZ` que avanza con la rotación global.
- **Semillas:** `seedX = sin(i × 38.2)`, `seedY = cos(i × 71.4)` para distribución pseudoaleatoria.
- **Distancia de proyección:** 300f (valor de cámara).
- **Rango Z:** -80f a 320f (400f de rango total).
- **Alpha:** `(1 - (pz + 80) / 400).coerceIn(0.04, 0.35)`.

### 8.4 Cubos 3D Wireframe (Capa 3)

- **Cantidad:** 7 cubos.
- **Geometría:** Cada cubo tiene 8 vértices y 12 aristas.
- **Proyección:** Perspectiva simple con distancia de cámara = 300f.
- **Rotación:** Cada cubo rota en X e Y a velocidades diferentes (`speedX`, `speedY`).
- **Posición:** Definida en porcentajes de pantalla (`xPercent`, `yPercent`).
- **Tamaño:** Variable por cubo (55f a 110f).
- **Alpha base:** Variable por cubo (0.12 a 0.22).
- **Dibujo:** Cada arista se dibuja DOS veces:
  1. **Exterior:** Grosor 5dp × factor profundidad, `NeonRed@alpha`, cap Round.
  2. **Interior:** Grosor 1.2dp × factor profundidad, `lerp(NeonRed, White, 0.25)@alpha`, cap Round.
- **Factor de profundidad (`df`):** `((cube.size - (p1.depth + p2.depth)/2) / (2 × cube.size)).coerceIn(0.15, 1.0)`. Aristas más cercanas al observador son más gruesas y opacas.

#### Configuración de Cubos (LOGIN_CUBES)

| # | x% | y% | Tamaño | speedX | speedY | Alpha |
|---|-----|-----|--------|--------|--------|-------|
| 1 | 0.08 | 0.12 | 70 | 0.5 | 1.1 | 0.18 |
| 2 | 0.90 | 0.08 | 95 | -0.7 | 0.6 | 0.22 |
| 3 | 0.85 | 0.45 | 65 | 1.1 | -0.8 | 0.14 |
| 4 | 0.05 | 0.60 | 85 | -0.4 | 1.0 | 0.18 |
| 5 | 0.92 | 0.82 | 110 | 0.8 | 0.5 | 0.22 |
| 6 | 0.18 | 0.88 | 60 | 0.9 | -0.9 | 0.14 |
| 7 | 0.50 | 0.05 | 55 | -0.6 | 0.7 | 0.12 |

### 8.5 Vignette Radial (Capa 4)

- **Tipo:** Gradient radial.
- **Colores:** `Transparent` → `BgBlack@70%`.
- **Centro:** Centro de la pantalla.
- **Radio:** `min(width, height) × 0.75`.
- **Propósito:** Oscurecer los bordes de la pantalla para enfocar la atención en el centro donde está el contenido.

### 8.6 Reglas del Fondo

- **NUNCA** omitir el fondo animado en pantallas de auth (Login, Registro).
- En pantallas funcionales (Home, Ejercicio, Sesión Activa), el fondo puede simplificarse a `BgBlack` plano o con grid sutil para no competir con el contenido.
- El fondo completo (cubos + partículas + grid) es **exclusivo de pantallas de auth y splash**.
- La vignette SIEMPRE debe estar presente cuando hay cubos/partículas para evitar que los elementos del fondo distraigan cerca de los bordes.

---

## 9. El Logo A.C.E

### 9.1 Estructura Geométrica

El logo es un círculo que contiene un trazo estilizado de onda cardíaca (ECG).

#### Capas (de fondo a frente)

1. **Glow radial:** Círculo relleno, gradiente radial `NeonRed@20%` → `Transparent`, radio 0.7×size.
2. **Círculo tenue:** Stroke 1dp, `NeonRed@15%`, radio (size/2) - 1dp.
3. **Círculo principal:** Stroke 2.5dp, `NeonRed`, radio (size/2) - 2dp.
4. **ECG sombra:** Path con stroke 11.25dp, `NeonRed@20%`, cap Round, join Round.
5. **ECG principal:** Mismo path, stroke 4.5dp, `Color.White`, cap Round, join Round.
6. **Barra horizontal:** Línea de (0.42W, 0.50H) a (0.58W, 0.50H), stroke 3dp, `NeonRed`, cap Round.

#### Path del ECG (coordenadas relativas)

```
M(0.18, 0.55) → L(0.31, 0.55) → L(0.37, 0.65) → L(0.50, 0.22) → L(0.63, 0.76) → L(0.69, 0.55) → L(0.82, 0.55)
```

- El pico central está en Y=0.22 (arriba).
- El valle está en Y=0.76 (abajo).
- La línea base está en Y=0.55.
- La barra horizontal está en Y=0.50, cruzando visualmente el centro.

### 9.2 Comportamiento

- **Pulso:** Escala entre 1.0 y 1.07, duración 950ms, easing FastOutSlowInEasing, infinite reverse.
- **Aplicación:** `graphicsLayer { scaleX = pulse; scaleY = pulse }`.
- **Centrado:** Siempre centrado horizontal y verticalmente en su contenedor.

---

*Fin de la Parte 2. Continúa en Parte 3: Pantallas (Login, Home, Ejercicio, Sesión Activa, Resumen).*
