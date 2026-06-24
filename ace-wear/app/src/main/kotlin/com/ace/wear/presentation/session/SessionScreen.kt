package com.ace.wear.presentation.session

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.ace.wear.presentation.WearScreenState

/**
 * Pantalla principal que gestiona los 3 estados: SPLASH, IDLE, ACTIVE.
 *
 * @param viewModel ViewModel inyectado desde MainActivity
 */
@Composable
fun SessionScreen(
    viewModel: SessionViewModel
) {
    val state by viewModel.state.collectAsState()

    when (state.screenState) {
        WearScreenState.SPLASH -> {
            SplashScreen()
        }

        WearScreenState.IDLE -> {
            IdleScreen(
                isConnected = state.isConnected,
                nodeCount = state.nodeCount,
                lastError = state.lastError,
                hasSensorPermission = state.hasSensorPermission,
                permissionDenied = state.permissionDenied
            )
        }

        WearScreenState.ACTIVE -> {
            ActiveSessionScreen(
                bpm = state.bpm,
                elapsedSeconds = state.elapsedSeconds,
                samplesSent = state.samplesSent,
                isConnected = state.isConnected,
                onStopClicked = { viewModel.onStopButtonClicked() }
            )
        }
    }
}