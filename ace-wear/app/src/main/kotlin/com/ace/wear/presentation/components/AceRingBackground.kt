package com.ace.wear.presentation.components

import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import com.ace.wear.presentation.theme.AceRed
import sena.adso.ace_wear.R
import kotlin.math.PI

/**
 * Fondo de anillos concentricos "A.C.E WEAR" con fuente UnifrakturMaguntia.
 * Usa drawTextOnPath nativo de Android para distribucion perfecta del texto en circulo.
 * Fuente cargada desde res/font/ via ResourcesCompat.
 * Usado en Splash (animado) e Idle (estatico).
 *
 * @param animated Si true, los anillos se renderizan con opacidad maxima
 * @param ringOpacities Lista de opacidades para cada anillo (8 valores). Si null, usa defecto
 */
@Composable
fun AceRingBackground(
    modifier: Modifier = Modifier,
    animated: Boolean = false,
    ringOpacities: List<Float>? = null
) {
    val context = LocalContext.current
    val typeface = remember {
        ResourcesCompat.getFont(context, R.font.unifrakturmaguntia_regular)
    }

    val defaultOpacities = listOf(1.00f, 0.85f, 0.70f, 0.55f, 0.42f, 0.30f, 0.20f, 0.10f)
    val opacities = ringOpacities ?: defaultOpacities

    // Configuracion AJUSTADA: fontSize mas pequeno, radios mejor distribuidos
    // Basado en el HTML original: fontSizes = [22, 19, 18, 17, 16, 14, 13, 12]
    // Reducidos para mejor legibilidad en pantalla Wear
    val ringConfigs = listOf(
        18f to 0.98f,   // Anillo 1 - mas pequeno, mas espacio
        15f to 0.93f,   // Anillo 2
        14f to 0.88f,   // Anillo 3
        13f to 0.83f,   // Anillo 4
        12f to 0.78f,   // Anillo 5
        10f to 0.74f,   // Anillo 6
        9f to 0.70f,    // Anillo 7
        8f to 0.66f     // Anillo 8 - muy pequeno, muy tenue
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val maxRadius = size.minDimension / 2f

        drawIntoCanvas { canvas ->
            val nativeCanvas = canvas.nativeCanvas

            ringConfigs.forEachIndexed { index, (fontSizePx, radiusRatio) ->
                val radius = maxRadius * radiusRatio
                val opacity = if (animated) 1.0f else opacities.getOrElse(index) { 0.1f }

                // Crear path circular
                val path = Path().apply {
                    addArc(
                        RectF(
                            centerX - radius,
                            centerY - radius,
                            centerX + radius,
                            centerY + radius
                        ),
                        270f, // Empezar desde arriba
                        360f  // Circulo completo
                    )
                }

                // Configurar paint con Typeface desde res/font/
                val paint = Paint().apply {
                    this.typeface = typeface
                    textSize = fontSizePx.dp.toPx()
                    color = AceRed.copy(alpha = opacity).toArgb()
                    isAntiAlias = true
                    textAlign = Paint.Align.CENTER
                }

                // Texto a repetir
                val text = "A.C.E WEAR "

                // Medir ancho de una repeticion
                val textWidth = paint.measureText(text)

                // Calcular repeticiones para llenar la circunferencia
                val circumference = 2 * PI * radius
                val repetitions = (circumference / textWidth).toInt().coerceAtLeast(1)
                val repeatedText = text.repeat(repetitions)

                // Dibujar texto en el path circular
                nativeCanvas.drawTextOnPath(
                    repeatedText,
                    path,
                    0f, // offset horizontal
                    0f, // offset vertical (descenso de fuente)
                    paint
                )
            }

            // Centro oscuro (circulos concentricos de profundidad)
            val lastRadius = maxRadius * 0.66f
            val lastFontSize = 8f
            val centerStart = lastRadius - (lastFontSize.dp.toPx() * 2f)

            val centerCircles = listOf(
                centerStart to 0.95f,
                (centerStart - 12f.dp.toPx()) to 0.97f,
                (centerStart - 24f.dp.toPx()) to 0.99f,
                (centerStart - 36f.dp.toPx()) to 1.0f
            )

            centerCircles.forEach { (r, alpha) ->
                if (r > 0) {
                    val centerPaint = Paint().apply {
                        color = android.graphics.Color.argb(
                            (alpha * 255).toInt(),
                            10, 10, 10
                        )
                        isAntiAlias = true
                    }
                    nativeCanvas.drawCircle(centerX, centerY, r, centerPaint)
                }
            }

            // Punto rojo central
            val dotPaint = Paint().apply {
                color = AceRed.toArgb()
                alpha = (0.6f * 255).toInt()
                isAntiAlias = true
            }
            nativeCanvas.drawCircle(centerX, centerY, 3f.dp.toPx(), dotPaint)
        }
    }
}
