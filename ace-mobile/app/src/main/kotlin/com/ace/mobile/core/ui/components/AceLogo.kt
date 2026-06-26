// Location: com/ace/mobile/core/ui/components/AceLogo.kt
package com.ace.mobile.core.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.ace.mobile.core.ui.theme.AceColors

@Composable
fun AceLogo(
    sizeDp: Float = 72f,
    animated: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "LogoPulseLoop")
    val pulseScale by if (animated) {
        infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.07f,
            animationSpec = infiniteRepeatable(
                animation = tween(950, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "Pulse"
        )
    } else {
        remember { androidx.compose.runtime.mutableFloatStateOf(1f) }
    }

    Canvas(modifier = Modifier.size((sizeDp * pulseScale).dp)) {
        val w = size.width
        val h = size.height

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(AceColors.NeonRed.copy(alpha = 0.20f), Color.Transparent),
                center = Offset(w / 2f, h / 2f), radius = w * 0.7f
            ),
            radius = w * 0.7f, center = Offset(w / 2f, h / 2f)
        )
        drawCircle(
            color = AceColors.NeonRed.copy(alpha = 0.15f), radius = w / 2f - 1.dp.toPx(),
            style = Stroke(width = 1.dp.toPx())
        )
        drawCircle(
            color = AceColors.NeonRed, radius = w / 2f - 2.dp.toPx(),
            style = Stroke(width = 2.5.dp.toPx())
        )
        val sw = 4.5.dp.toPx()
        val path = Path().apply {
            moveTo(w * 0.18f, h * 0.55f)
            lineTo(w * 0.31f, h * 0.55f)
            lineTo(w * 0.37f, h * 0.65f)
            lineTo(w * 0.50f, h * 0.22f)
            lineTo(w * 0.63f, h * 0.76f)
            lineTo(w * 0.69f, h * 0.55f)
            lineTo(w * 0.82f, h * 0.55f)
        }
        drawPath(path, AceColors.NeonRed.copy(alpha = 0.20f), style = Stroke(sw * 2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(path, Color.White, style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
        val bar = Path().apply {
            moveTo(w * 0.42f, h * 0.50f); lineTo(w * 0.58f, h * 0.50f)
        }
        drawPath(bar, AceColors.NeonRed, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
    }
}