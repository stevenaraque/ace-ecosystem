package com.ace.wear.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.ace.wear.presentation.theme.*

@Composable
fun ConnectionStatusChip(
    isConnected: Boolean,
    nodeCount: Int = 0,
    lastError: String? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "chipNeon")
    val neonAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "chipNeonPulse"
    )

    val indicatorColor: Color
    val statusText: String

    when {
        isConnected && nodeCount > 0 -> {
            indicatorColor = AceGreen
            statusText = "Conectado"
        }
        !isConnected && lastError != null -> {
            indicatorColor = AceRed
            statusText = "Error"
        }
        else -> {
            indicatorColor = AceOrange
            statusText = "Esperando..."
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Text(
            text = "●",
            style = MaterialTheme.typography.caption3,
            color = indicatorColor,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.width(5.dp))

        Text(
            text = statusText.uppercase(),
            fontFamily = CinzelDecorative,
            fontSize = 8.sp,
            color = Color.White,
            style = TextStyle(
                shadow = Shadow(
                    color = indicatorColor.copy(alpha = neonAlpha),
                    offset = Offset(0f, 0f),
                    blurRadius = 10f
                )
            ),
            textAlign = TextAlign.Center,
            letterSpacing = 1.2.sp
        )
    }
}