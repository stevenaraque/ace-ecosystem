package com.ace.wear.presentation.session

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import com.ace.wear.presentation.components.HeartRateDisplay
import com.ace.wear.presentation.components.TimerDisplay
import com.ace.wear.presentation.theme.AceBlack
import com.ace.wear.presentation.theme.AceRed
import com.ace.wear.presentation.theme.AceTextMuted

/**
 * Pantalla de sesion activa. Muestra FC, timer, botones DETENER y PAUSAR.
 */
@Composable
fun ActiveSessionScreen(
    bpm: Double?,
    elapsedSeconds: Long,
    samplesSent: Int,
    isConnected: Boolean,
    isPaused: Boolean = false,
    onStopClicked: () -> Unit,
    onPauseClicked: () -> Unit = {}
) {
    Scaffold(
        timeText = { TimeText() }
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp)
        ) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    ConnectionDot(isConnected = isConnected)
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                HeartRateDisplay(bpm = bpm)
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
                TimerDisplay(elapsedSeconds = elapsedSeconds)
            }

            if (samplesSent > 0) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$samplesSent enviados",
                        style = MaterialTheme.typography.caption3,
                        color = AceTextMuted,
                        textAlign = TextAlign.Center
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onStopClicked,
                        modifier = Modifier.size(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = AceRed
                        )
                    ) {
                        Text(
                            text = "■",
                            color = AceBlack,
                            fontSize = 16.sp
                        )
                    }

                    Button(
                        onClick = onPauseClicked,
                        modifier = Modifier.size(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = if (isPaused)
                                Color(0xFF00E676) else AceTextMuted.copy(alpha = 0.3f)
                        )
                    ) {
                        Text(
                            text = if (isPaused) "▶" else "⏸",
                            color = if (isPaused) AceBlack else AceTextMuted,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectionDot(isConnected: Boolean) {
    val color = if (isConnected) Color(0xFF00E676) else Color(0xFFFF9100)

    Canvas(modifier = Modifier.size(6.dp)) {
        drawCircle(
            color = color,
            radius = size.minDimension / 2f
        )
    }
}