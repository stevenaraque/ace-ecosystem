package com.ace.wear.presentation.components

import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
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
 * Fondo de vórtice gótico "A.C.E.WEAR" con brillo atenuado para pantallas OLED.
 * Reduce la intensidad lumínica para un acabado más elegante y premium.
 */
@Composable
fun AceRingBackground(
    modifier: Modifier = Modifier,
    animated: Boolean = false,
    ringOpacities: List<Float>? = null,
    showCenterDot: Boolean = false
) {
    val context = LocalContext.current
    val typeface = remember {
        ResourcesCompat.getFont(context, R.font.unifrakturmaguntia_regular)
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val maxRadius = size.minDimension / 2f

        drawIntoCanvas { canvas ->
            val nativeCanvas = canvas.nativeCanvas

            val startRadius = maxRadius * 0.98f
            val innerLimit = maxRadius * 0.18f
            var currentRadius = startRadius

            // CONTROL DE BRILLO: Bajamos el tope del 1.0f al 0.60f para que no encandile
            val maxBrightness = 0.60f

            while (currentRadius > innerLimit) {
                val progress = (currentRadius - innerLimit) / (startRadius - innerLimit)
                val fontSizePx = currentRadius * 0.105f

                // Aplicamos el atenuador de brillo a la curva de profundidad
                val opacity = (progress * progress * maxBrightness).coerceIn(0.05f, maxBrightness)

                val path = Path().apply {
                    addArc(
                        RectF(
                            centerX - currentRadius,
                            centerY - currentRadius,
                            centerX + currentRadius,
                            centerY + currentRadius
                        ),
                        270f,
                        360f
                    )
                }

                val paint = Paint().apply {
                    this.typeface = typeface
                    this.textSize = fontSizePx
                    color = AceRed.copy(alpha = opacity).toArgb()
                    isAntiAlias = true
                    textAlign = Paint.Align.LEFT
                    this.letterSpacing = -0.04f
                }

                val baseText = "A.C.E.WEAR."
                val circumference = 2.0 * PI * currentRadius

                val massiveText = baseText.repeat(40)
                val measuredWidth = FloatArray(1)
                val charsThatFit = paint.breakText(massiveText, true, circumference.toFloat(), measuredWidth)

                if (charsThatFit > 0) {
                    val truncatedText = massiveText.substring(0, charsThatFit)
                    val actualTextWidth = paint.measureText(truncatedText)

                    if (actualTextWidth > 0f) {
                        paint.textScaleX = (circumference / actualTextWidth).toFloat()
                    }

                    nativeCanvas.drawTextOnPath(truncatedText, path, 0f, 0f, paint)
                }

                currentRadius -= (fontSizePx * 0.72f)
            }

            // Núcleo oscuro central
            val finalInnerRadius = currentRadius + (maxRadius * 0.01f)
            val layers = listOf(
                finalInnerRadius         to 0.85f,
                finalInnerRadius * 0.75f to 0.93f,
                finalInnerRadius * 0.50f to 1.00f
            )

            layers.forEach { (r, alpha) ->
                if (r > 0f) {
                    val bgPaint = Paint().apply {
                        color = android.graphics.Color.argb((alpha * 255).toInt(), 8, 8, 8)
                        isAntiAlias = true
                    }
                    nativeCanvas.drawCircle(centerX, centerY, r, bgPaint)
                }
            }

            if (showCenterDot) {
                val dotPaint = Paint().apply {
                    color = AceRed.toArgb()
                    alpha = (maxBrightness * 255).toInt() // También atenuamos el punto central
                    isAntiAlias = true
                }
                nativeCanvas.drawCircle(centerX, centerY, 4f.dp.toPx(), dotPaint)
            }
        }
    }
}