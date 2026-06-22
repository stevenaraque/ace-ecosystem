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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.ace.wear.presentation.components.ConnectionStatusChip
import com.ace.wear.presentation.components.DiagLogPanel
import com.ace.wear.presentation.components.HeartRateDisplay
import com.ace.wear.presentation.components.StopButton
import com.ace.wear.presentation.components.TimerDisplay

/**
 * Pantalla principal de sesion en el reloj Wear OS con diagnostico.
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
        nodeCount = state.nodeCount,
        lastError = state.lastError,
        diagLogs = state.diagLogs,
        hasSensorPermission = state.hasSensorPermission,
        permissionDenied = state.permissionDenied,
        isSimulationMode = state.isSimulationMode,
        samplesSent = state.samplesSent,
        onStopClicked = { viewModel.onStopButtonClicked() }
    )
}

@Composable
private fun SessionContent(
    bpm: Double?,
    elapsedSeconds: Long,
    isSessionActive: Boolean,
    isConnected: Boolean,
    nodeCount: Int,
    lastError: String?,
    diagLogs: List<String>,
    hasSensorPermission: Boolean,
    permissionDenied: Boolean,
    isSimulationMode: Boolean,
    samplesSent: Int,
    onStopClicked: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // === DIAGNOSTICO DE CONEXION ===
        ConnectionStatusChip(
            isConnected = isConnected,
            nodeCount = nodeCount,
            lastError = lastError
        )

        Spacer(modifier = Modifier.height(4.dp))

        // === PANEL DE LOGS (solo si hay logs) ===
        if (diagLogs.isNotEmpty()) {
            DiagLogPanel(logs = diagLogs)
            Spacer(modifier = Modifier.height(4.dp))
        }

        // === ALERTA DE PERMISO ===
        if (permissionDenied && !isSimulationMode) {
            Text(
                text = "Permiso denegado - modo simulacion",
                style = MaterialTheme.typography.body2,
                color = Color(0xFFFFA000),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        // === MODO SIMULACION ===
        if (isSimulationMode) {
            Text(
                text = "SIMULACION",
                style = MaterialTheme.typography.caption2,
                color = Color(0xFF00E5FF),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(2.dp))
        }

        // === FC EN VIVO ===
        HeartRateDisplay(bpm = bpm)

        Spacer(modifier = Modifier.height(4.dp))

        // === TIMER (solo si sesion activa) ===
        if (isSessionActive) {
            TimerDisplay(elapsedSeconds = elapsedSeconds)
        }

        Spacer(modifier = Modifier.height(4.dp))

        // === SAMPLES ENVIADOS ===
        if (isSessionActive && samplesSent > 0) {
            Text(
                text = "$samplesSent enviados",
                style = MaterialTheme.typography.caption3,
                color = Color(0xFFAAAAAA),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(2.dp))
        }

        // === BOTON DETENER O MENSAJE DE ESPERA ===
        if (isSessionActive) {
            StopButton(onClick = onStopClicked)
        } else {
            Text(
                text = when {
                    !isConnected -> "Sin conexion al movil"
                    permissionDenied && !isSimulationMode -> "Permiso denegado"
                    !hasSensorPermission && !isSimulationMode -> "Esperando permiso..."
                    else -> "Esperando START..."
                },
                style = MaterialTheme.typography.body2,
                textAlign = TextAlign.Center
            )
        }
    }
}