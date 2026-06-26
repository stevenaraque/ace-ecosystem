package com.ace.mobile.feature.exercise.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import sena.adso.ace_mobile.BuildConfig // Importación corregida explícitamente
import com.ace.shared.enums.SportType
import java.util.Locale

@Composable
fun SessionScreen(
    userId: String,
    viewModel: SessionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val heartRate by viewModel.heartRate.collectAsState()
    val elapsedSeconds by viewModel.elapsedSeconds.collectAsState()
    val blockCount by viewModel.blockCount.collectAsState()
    val isConnected by viewModel.isConnected.collectAsState()
    val totalXp by viewModel.totalXp.collectAsState()

    // NUEVO: Observar el estado de simulación
    val isSimulating by viewModel.isSimulating.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (val state = uiState) {
            is SessionUiState.Idle -> {
                SessionIdleContent(onStartSession = { sport ->
                    viewModel.startSession(sport, userId)
                })
            }
            is SessionUiState.Loading -> {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(8.dp))
                Text("Initializing session...")
            }
            is SessionUiState.Active -> {
                SessionActiveContent(
                    session = state.session,
                    heartRate = heartRate,
                    elapsedSeconds = elapsedSeconds,
                    blockCount = blockCount,
                    totalXp = totalXp,
                    isConnected = isConnected,
                    isSimulating = isSimulating, // Pasar propiedad
                    onToggleSimulation = { viewModel.toggleSimulation() }, // Pasar callback
                    onPause = { viewModel.pauseSession() },
                    onStop = { viewModel.stopSession() }
                )
            }
            is SessionUiState.Paused -> {
                SessionPausedContent(
                    session = state.session,
                    heartRate = heartRate,
                    elapsedSeconds = elapsedSeconds,
                    blockCount = blockCount,
                    totalXp = totalXp,
                    isConnected = isConnected,
                    isAutoPaused = state.isAutoPaused,
                    isSimulating = isSimulating, // Pasar propiedad
                    onToggleSimulation = { viewModel.toggleSimulation() }, // Pasar callback
                    onResume = { viewModel.resumeSession() },
                    onStop = { viewModel.stopSession() }
                )
            }
            is SessionUiState.Stopping -> {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Closing final block & saving session...")
            }
            is SessionUiState.Completed -> {
                SessionCompletedContent(
                    session = state.session,
                    xpGained = state.xpGained,
                    blocksInSession = state.blocksInSession,
                    onReset = { viewModel.resetState() }
                )
            }
            is SessionUiState.Error -> {
                SessionErrorContent(
                    message = state.message,
                    onRetry = { viewModel.resetState() }
                )
            }
        }
    }
}

@Composable
fun SessionIdleContent(onStartSession: (SportType) -> Unit) {
    Text(text = "Start a New Workout", style = MaterialTheme.typography.headlineMedium)
    Spacer(modifier = Modifier.height(24.dp))
    Button(
        onClick = { onStartSession(SportType.RUNNING) },
        modifier = Modifier.fillMaxWidth(0.7f)
    ) {
        Text("RUNNING")
    }
    Spacer(modifier = Modifier.height(8.dp))
    Button(
        onClick = { onStartSession(SportType.CYCLING) },
        modifier = Modifier.fillMaxWidth(0.7f)
    ) {
        Text("CYCLING")
    }
}

@Composable
fun SessionActiveContent(
    session: com.ace.mobile.core.model.ExerciseSession,
    heartRate: Double,
    elapsedSeconds: Int,
    blockCount: Int,
    totalXp: Double,
    isConnected: Boolean,
    isSimulating: Boolean,
    onToggleSimulation: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit
) {
    Text(text = "Session Active", style = MaterialTheme.typography.headlineSmall, color = Color.Green)
    Text(text = "Sport: ${session.sportType.name}", style = MaterialTheme.typography.bodyLarge)

    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = if (isConnected) "Watch Connected" else "Searching Watch...",
        color = if (isConnected) Color.Green else Color.Gray,
        style = MaterialTheme.typography.bodyMedium
    )

    Spacer(modifier = Modifier.height(24.dp))
    Text(
        text = String.format(Locale.getDefault(), "%02d:%02d", elapsedSeconds / 60, elapsedSeconds % 60),
        style = MaterialTheme.typography.displayMedium
    )

    Spacer(modifier = Modifier.height(24.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        LiveDataCard(label = "Heart Rate", value = "${heartRate.toInt()}", unit = "BPM")
        LiveDataCard(label = "Blocks", value = "$blockCount", unit = "")
        LiveDataCard(label = "XP Accum.", value = String.format(Locale.getDefault(), "%.1f", totalXp), unit = "XP")
    }

    Spacer(modifier = Modifier.height(32.dp))

    // REQUISITO: Botón de simulación en Debug
    if (BuildConfig.DEBUG) {
        Button(
            onClick = onToggleSimulation,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isSimulating) Color.Red else MaterialTheme.colorScheme.tertiary
            ),
            modifier = Modifier.fillMaxWidth(0.7f)
        ) {
            Text(text = if (isSimulating) "Detener Simulación" else "Simular Wear OS")
        }
        Spacer(modifier = Modifier.height(12.dp))
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Button(onClick = onPause) { Text("PAUSE") }
        Button(
            onClick = onStop,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) { Text("STOP") }
    }
}

@Composable
fun SessionPausedContent(
    session: com.ace.mobile.core.model.ExerciseSession,
    heartRate: Double,
    elapsedSeconds: Int,
    blockCount: Int,
    totalXp: Double,
    isConnected: Boolean,
    isAutoPaused: Boolean,
    isSimulating: Boolean,
    onToggleSimulation: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit
) {
    Text(
        text = if (isAutoPaused) "Auto-Paused" else "Session Paused",
        style = MaterialTheme.typography.headlineSmall,
        color = Color.Yellow
    )
    Text(text = "Sport: ${session.sportType.name}")

    Spacer(modifier = Modifier.height(24.dp))
    Text(
        text = String.format(Locale.getDefault(), "%02d:%02d", elapsedSeconds / 60, elapsedSeconds % 60),
        style = MaterialTheme.typography.displayMedium,
        color = Color.Gray
    )

    Spacer(modifier = Modifier.height(24.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        LiveDataCard(label = "Last HR", value = "${heartRate.toInt()}", unit = "BPM")
        LiveDataCard(label = "Blocks", value = "$blockCount", unit = "")
        LiveDataCard(label = "XP Accum.", value = String.format(Locale.getDefault(), "%.1f", totalXp), unit = "XP")
    }

    Spacer(modifier = Modifier.height(32.dp))

    // REQUISITO: Botón de simulación también en Pausa si es Debug
    if (BuildConfig.DEBUG) {
        Button(
            onClick = onToggleSimulation,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isSimulating) Color.Red else MaterialTheme.colorScheme.tertiary
            ),
            modifier = Modifier.fillMaxWidth(0.7f)
        ) {
            Text(text = if (isSimulating) "Detener Simulación" else "Simular Wear OS")
        }
        Spacer(modifier = Modifier.height(12.dp))
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Button(onClick = onResume) { Text("RESUME") }
        Button(
            onClick = onStop,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) { Text("STOP") }
    }
}

@Composable
fun SessionCompletedContent(
    session: com.ace.mobile.core.model.ExerciseSession,
    xpGained: Double,
    blocksInSession: Int,
    onReset: () -> Unit
) {
    Text(text = "Session Completed!", style = MaterialTheme.typography.headlineMedium, color = Color.Green)
    Spacer(modifier = Modifier.height(16.dp))

    Text("Sport: ${session.sportType.name}")
    Text("Duration: ${session.timestampEnd?.let { end ->
        val duration = (end - session.timestampStart) / 1000
        "${duration / 60}m ${duration % 60}s"
    } ?: "Unknown"}")

    Spacer(modifier = Modifier.height(16.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        LiveDataCard(label = "Blocks", value = "$blocksInSession", unit = "")
        LiveDataCard(label = "XP Gained", value = String.format(Locale.getDefault(), "%.2f", xpGained), unit = "XP")
    }

    Spacer(modifier = Modifier.height(32.dp))
    Button(onClick = onReset) { Text("NEW SESSION") }
}

@Composable
fun SessionErrorContent(message: String, onRetry: () -> Unit) {
    Text(text = "Error", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.error)
    Spacer(modifier = Modifier.height(16.dp))
    Text(text = message, style = MaterialTheme.typography.bodyLarge)
    Spacer(modifier = Modifier.height(32.dp))
    Button(onClick = onRetry) { Text("RETRY") }
}

@Composable
fun LiveDataCard(label: String, value: String, unit: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(text = value, style = MaterialTheme.typography.titleLarge)
            if (unit.isNotEmpty()) {
                Spacer(modifier = Modifier.width(2.dp))
                Text(text = unit, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
        }
    }
}