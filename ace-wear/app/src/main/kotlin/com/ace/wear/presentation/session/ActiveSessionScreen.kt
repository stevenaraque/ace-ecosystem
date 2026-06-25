package com.ace.wear.presentation.session

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.Text
import com.ace.wear.presentation.components.AceRingBackground
import com.ace.wear.presentation.theme.CinzelDecorative

@Composable
@Suppress("UNUSED_PARAMETER")
fun ActiveSessionScreen(
    bpm: Double?,
    elapsedSeconds: Long,
    samplesSent: Int,
    isConnected: Boolean,
    isPaused: Boolean = false,
    onStopClicked: () -> Unit,
    onPauseClicked: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "activeSessionLayout")
    val neonAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bpmNeonPulse"
    )

    val saturatedNeonRed = Color(0xFFFF0018)

    val safeBpm = bpm ?: 0.0
    val dynamicHeartColor = when {
        safeBpm < 110.0 -> saturatedNeonRed
        safeBpm <= 130.0 -> Color(0xFFFF9100)
        else -> Color(0xFF00E676)
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        AceRingBackground(
            modifier = Modifier.fillMaxSize(),
            animated = false,
            showCenterDot = false
        )

        Text(
            text = "A.C.E WEAR",
            fontFamily = CinzelDecorative,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = Color.White.copy(alpha = 0.18f),
            textAlign = TextAlign.Center,
            letterSpacing = 2.5.sp,
            modifier = Modifier.align(Alignment.Center)
        )

        Button(
            onClick = onStopClicked,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 52.dp)
                .size(38.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color.Transparent
            )
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Stop Session",
                tint = saturatedNeonRed,
                modifier = Modifier.size(18.dp)
            )
        }

        Column(
            modifier = Modifier.wrapContentSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = "Heart Rate Icon",
                tint = dynamicHeartColor.copy(alpha = neonAlpha * 0.8f),
                modifier = Modifier.size(14.dp)
            )

            Spacer(modifier = Modifier.height(2.dp))

            val bpmFormatted = if (bpm != null) "%.0f".format(bpm) else "--"

            Text(
                text = bpmFormatted,
                fontFamily = CinzelDecorative,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                color = Color.White,
                style = TextStyle(
                    shadow = Shadow(
                        color = saturatedNeonRed.copy(alpha = neonAlpha),
                        offset = Offset(0f, 0f),
                        blurRadius = 14f
                    )
                ),
                textAlign = TextAlign.Center
            )

            Text(
                text = "BPM",
                fontFamily = CinzelDecorative,
                fontSize = 8.sp,
                color = saturatedNeonRed,
                fontWeight = FontWeight.Normal,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            val minutes = elapsedSeconds / 60
            val seconds = elapsedSeconds % 60
            val timeString = "%02d:%02d".format(minutes, seconds)

            Text(
                text = timeString,
                fontFamily = CinzelDecorative,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
                letterSpacing = 0.5.sp
            )
        }

        val pauseIconColor = if (isPaused) Color(0xFF00C853) else Color.White.copy(alpha = 0.85f)

        Button(
            onClick = onPauseClicked,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 52.dp)
                .size(38.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color.Transparent
            )
        ) {
            Icon(
                imageVector = if (isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                contentDescription = "Resume",
                tint = pauseIconColor,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}