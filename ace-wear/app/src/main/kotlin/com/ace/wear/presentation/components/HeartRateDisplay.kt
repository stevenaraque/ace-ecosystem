package com.ace.wear.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.ace.wear.presentation.theme.*

@Composable
fun HeartRateDisplay(bpm: Double?) {
    val displayText = bpm?.let { String.format("%.0f", it) } ?: "--"

    val heartColor = when {
        bpm == null -> AceZoneUnknown
        bpm < 60    -> AceZoneRest
        bpm < 100   -> AceZoneWarm
        bpm < 140   -> AceZoneCardio
        bpm < 180   -> AceZonePeak
        else        -> AceRed
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "heartScale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Canvas(modifier = Modifier.size(18.dp)) {
            val s = if (bpm != null) pulseScale else 1f
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f

            withTransform({
                scale(scaleX = s, scaleY = s, pivot = Offset(cx, cy))
            }) {
                val path = Path().apply {
                    moveTo(w / 2f, h * 0.88f)
                    cubicTo(w * 0.08f, h * 0.58f, w * 0.08f, h * 0.18f, w * 0.50f, h * 0.32f)
                    cubicTo(w * 0.92f, h * 0.18f, w * 0.92f, h * 0.58f, w / 2f, h * 0.88f)
                    close()
                }
                drawPath(path, heartColor)
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = displayText,
            style = MaterialTheme.typography.title1.copy(
                fontFamily = CinzelDecorative,
                color = heartColor
            ),
            textAlign = TextAlign.Center
        )

        if (bpm != null) {
            Text(
                text = "BPM",
                style = MaterialTheme.typography.caption3,
                color = AceTextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}