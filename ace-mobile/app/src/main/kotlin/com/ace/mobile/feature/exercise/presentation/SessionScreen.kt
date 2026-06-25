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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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
    val samplesReceived by viewModel.samplesReceived.collectAsState()
    val totalXp by viewModel.totalXp.collectAsState()
    val lowBpmSeconds by viewModel.lowBpmSeconds.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (val state = uiState) {
            is SessionUiState.Idle -> SessionIdleContent(
                onStart = { sportType -> viewModel.startSession(sportType, userId) }
            )

            is SessionUiState.Loading -> {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Starting session...")
            }

            is SessionUiState.Active -> {
                val session = state.session
                SessionActiveContent(
                    session = session,
                    heartRate = heartRate,
                    elapsedSeconds = elapsedSeconds,
                    blockCount = blockCount,
                    isConnected = isConnected,
                    samplesReceived = samplesReceived,
                    totalXp = totalXp,
                    lowBpmSeconds = lowBpmSeconds,
                    onPause = { viewModel.pauseSession() },
                    onStop = { viewModel.stopSession() }
                )
            }

            is SessionUiState.Paused -> {
                val session = state.session
                SessionPausedContent(
                    session = session,
                    heartRate = heartRate,
                    elapsedSeconds = elapsedSeconds,
                    blockCount = blockCount,
                    isConnected = isConnected,
                    samplesReceived = samplesReceived,
                    totalXp = totalXp,
                    isAutoPaused = state.isAutoPaused,
                    onResume = { viewModel.resumeSession() },
                    onStop = { viewModel.stopSession() }
                )
            }

            is SessionUiState.Stopping -> {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Stopping session...",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Waiting for watch confirmation",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            is SessionUiState.Completed -> {
                val completed = uiState as SessionUiState.Completed
                SessionCompletedContent(
                    session = completed.session,
                    xpGained = completed.xpGained,
                    blocksInSession = completed.blocksInSession,
                    onReset = { viewModel.resetState() }
                )
            }

            is SessionUiState.Error -> {
                val message = (uiState as SessionUiState.Error).message
                SessionErrorContent(
                    message = message,
                    onRetry = { viewModel.resetState() }
                )
            }
        }
    }
}

@Composable
fun SessionIdleContent(
    onStart: (SportType) -> Unit
) {
    Text(
        text = "Start Exercise",
        style = MaterialTheme.typography.headlineMedium
    )

    Spacer(modifier = Modifier.height(24.dp))

    Text("Select activity type:", style = MaterialTheme.typography.bodyLarge)

    Spacer(modifier = Modifier.height(16.dp))

    Column {
        SportType.entries.forEach { sport ->
            Button(
                onClick = { onStart(sport) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Text(sport.name)
            }
        }
    }
}

@Composable
fun SessionActiveContent(
    session: com.ace.mobile.core.model.ExerciseSession,
    heartRate: Double,
    elapsedSeconds: Int,
    blockCount: Int,
    isConnected: Boolean,
    samplesReceived: Int,
    totalXp: Double,
    lowBpmSeconds: Int,
    onPause: () -> Unit,
    onStop: () -> Unit
) {
    Text(
        text = "Session Active!",
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.primary
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text("Sport: ${session.sportType.name}", style = MaterialTheme.typography.bodyLarge)
    Text("Session: ${session.sessionId.take(8)}...", style = MaterialTheme.typography.bodySmall)

    Spacer(modifier = Modifier.height(16.dp))

    // ← NUEVO: Warning de auto-pausa inminente
    if (heartRate > 0 && heartRate < 110 && lowBpmSeconds > 20) {
        Text(
            text = "⚠️ FC baja — pausa en ${30 - lowBpmSeconds}s",
            color = Color(0xFFFFA000),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
    }

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        val connectionColor = if (isConnected) Color.Green else Color.Red
        val connectionText = if (isConnected) "Watch connected" else "Watch disconnected"
        Text(
            text = "●",
            color = connectionColor,
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.padding(horizontal = 4.dp))
        Text(
            text = connectionText,
            style = MaterialTheme.typography.bodyMedium,
            color = connectionColor
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        LiveDataCard(
            label = "Heart Rate",
            value = if (heartRate > 0) "${heartRate.toInt()}" else "--",
            unit = "BPM"
        )
        LiveDataCard(
            label = "Time",
            value = "${elapsedSeconds / 60}:${String.format(Locale.getDefault(), "%02d", elapsedSeconds % 60)}",
            unit = ""
        )
        LiveDataCard(
            label = "Samples",
            value = "$samplesReceived",
            unit = "rcv"
        )
    }

    Spacer(modifier = Modifier.height(8.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        LiveDataCard(
            label = "Blocks",
            value = "$blockCount",
            unit = ""
        )
        LiveDataCard(
            label = "XP",
            value = String.format(Locale.getDefault(), "%.2f", totalXp),
            unit = ""
        )
    }

    Spacer(modifier = Modifier.height(24.dp))

    // ← NUEVO: Botones PAUSE + STOP
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onPause,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA000)) // Naranja
        ) {
            Text("PAUSE")
        }
        Button(
            onClick = onStop,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
        ) {
            Text("STOP")
        }
    }
}

@Composable
fun SessionPausedContent(
    session: com.ace.mobile.core.model.ExerciseSession,
    heartRate: Double,
    elapsedSeconds: Int,
    blockCount: Int,
    isConnected: Boolean,
    samplesReceived: Int,
    totalXp: Double,
    isAutoPaused: Boolean,
    onResume: () -> Unit,
    onStop: () -> Unit
) {
    val pauseColor = if (isAutoPaused) Color(0xFFFFA000) else MaterialTheme.colorScheme.primary

    Text(
        text = if (isAutoPaused) "⏸ Auto-Paused" else "⏸ Paused",
        style = MaterialTheme.typography.headlineMedium,
        color = pauseColor
    )

    if (isAutoPaused) {
        Text(
            text = "FC < 110 BPM — reanudará automáticamente",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFFFFA000)
        )
    }

    Spacer(modifier = Modifier.height(8.dp))

    Text("Sport: ${session.sportType.name}", style = MaterialTheme.typography.bodyLarge)
    Text("Session: ${session.sessionId.take(8)}...", style = MaterialTheme.typography.bodySmall)

    Spacer(modifier = Modifier.height(16.dp))

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        val connectionColor = if (isConnected) Color.Green else Color.Red
        val connectionText = if (isConnected) "Watch connected" else "Watch disconnected"
        Text(
            text = "●",
            color = connectionColor,
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.padding(horizontal = 4.dp))
        Text(
            text = connectionText,
            style = MaterialTheme.typography.bodyMedium,
            color = connectionColor
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        LiveDataCard(
            label = "Heart Rate",
            value = if (heartRate > 0) "${heartRate.toInt()}" else "--",
            unit = "BPM"
        )
        LiveDataCard(
            label = "Time",
            value = "${elapsedSeconds / 60}:${String.format(Locale.getDefault(), "%02d", elapsedSeconds % 60)}",
            unit = ""
        )
        LiveDataCard(
            label = "Samples",
            value = "$samplesReceived",
            unit = "rcv"
        )
    }

    Spacer(modifier = Modifier.height(8.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        LiveDataCard(
            label = "Blocks",
            value = "$blockCount",
            unit = ""
        )
        LiveDataCard(
            label = "XP",
            value = String.format(Locale.getDefault(), "%.2f", totalXp),
            unit = ""
        )
    }

    Spacer(modifier = Modifier.height(24.dp))

    // ← NUEVO: Botones RESUME + STOP
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onResume,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)) // Verde
        ) {
            Text("RESUME")
        }
        Button(
            onClick = onStop,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
        ) {
            Text("STOP")
        }
    }
}

@Composable
fun LiveDataCard(
    label: String,
    value: String,
    unit: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )
        if (unit.isNotEmpty()) {
            Text(
                text = unit,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
fun SessionCompletedContent(
    session: com.ace.mobile.core.model.ExerciseSession,
    xpGained: Double,
    blocksInSession: Int,
    onReset: () -> Unit
) {
    Text(
        text = "Session Completed!",
        style = MaterialTheme.typography.headlineMedium,
        color = Color.Green
    )

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
        LiveDataCard(
            label = "Blocks",
            value = "$blocksInSession",
            unit = ""
        )
        LiveDataCard(
            label = "XP Gained",
            value = String.format(Locale.getDefault(), "%.2f", xpGained),
            unit = "XP"
        )
    }

    Spacer(modifier = Modifier.height(32.dp))

    Button(onClick = onReset) {
        Text("NEW SESSION")
    }
}

@Composable
fun SessionErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Text(
        text = "Error",
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.error
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text(message)

    Spacer(modifier = Modifier.height(32.dp))

    Button(onClick = onRetry) {
        Text("RETRY")
    }
}