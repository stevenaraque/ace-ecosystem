package com.ace.mobile.presentation.exercise

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ace.shared.enums.SportType

@Composable
fun SessionScreen(
    userId: String,
    viewModel: SessionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentSession by viewModel.currentSession.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (uiState) {
            is SessionUiState.Idle -> SessionIdleContent(
                onStart = { sportType -> viewModel.startSession(sportType, userId) }
            )

            is SessionUiState.Loading -> {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Starting session...")
            }

            is SessionUiState.Active -> {
                val session = (uiState as SessionUiState.Active).session
                SessionActiveContent(
                    session = session,
                    onStop = { viewModel.stopSession() }
                )
            }

            is SessionUiState.Completed -> {
                val session = (uiState as SessionUiState.Completed).session
                SessionCompletedContent(
                    session = session,
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
    session: com.ace.mobile.domain.model.ExerciseSession,
    onStop: () -> Unit
) {
    Text(
        text = "🔥 Session Active!",
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.primary
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text("Sport: ${session.sportType.name}", style = MaterialTheme.typography.bodyLarge)
    Text("Session: ${session.sessionId.take(8)}...", style = MaterialTheme.typography.bodySmall)

    Spacer(modifier = Modifier.height(32.dp))

    Text("Waiting for heart rate data...", style = MaterialTheme.typography.bodyMedium)

    Spacer(modifier = Modifier.height(32.dp))

    Button(
        onClick = onStop,
        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
    ) {
        Text("STOP SESSION")
    }
}

@Composable
fun SessionCompletedContent(
    session: com.ace.mobile.domain.model.ExerciseSession,
    onReset: () -> Unit
) {
    Text(
        text = "✅ Session Completed!",
        style = MaterialTheme.typography.headlineMedium,
        color = Color.Green
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text("Sport: ${session.sportType.name}")
    Text("Blocks: ${session.totalBlocks}")
    Text("XP: ${session.totalXp}")

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
        text = "❌ Error",
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