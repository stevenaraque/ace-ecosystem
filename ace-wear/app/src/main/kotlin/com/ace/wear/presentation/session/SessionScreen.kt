// ace-wear/app/src/main/kotlin/com/ace/wear/presentation/session/SessionScreen.kt

package com.ace.wear.presentation.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.ace.wear.presentation.components.ConnectionStatusChip
import com.ace.wear.presentation.components.HeartRateDisplay
import com.ace.wear.presentation.components.StopButton
import com.ace.wear.presentation.components.TimerDisplay

/**
 * Pantalla principal de sesion en el reloj Wear OS.
 *
 * @param viewModel ViewModel inyectado desde MainActivity
 */
@Composable
fun SessionScreen(
    viewModel: SessionViewModel
) {
    val state by viewModel.state.collectAsState()

    SessionContent(
        bpm = state.bpm,
        elapsedSeconds = state.elapsedSeconds,
        isSessionActive = state.isSessionActive,
        isConnected = state.isConnected,
        onStopClicked = { viewModel.onStopButtonClicked() }
    )
}

@Composable
private fun SessionContent(
    bpm: Double?,
    elapsedSeconds: Long,
    isSessionActive: Boolean,
    isConnected: Boolean,
    onStopClicked: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        ConnectionStatusChip(isConnected = isConnected)

        Spacer(modifier = Modifier.height(8.dp))

        HeartRateDisplay(bpm = bpm)

        Spacer(modifier = Modifier.height(4.dp))

        if (isSessionActive) {
            TimerDisplay(elapsedSeconds = elapsedSeconds)
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (isSessionActive) {
            StopButton(onClick = onStopClicked)
        } else {
            Text(
                text = "Esperando inicio...",
                style = MaterialTheme.typography.body2,
                textAlign = TextAlign.Center
            )
        }
    }
}