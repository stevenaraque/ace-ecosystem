package com.ace.mobile.feature.exercise.presentation

import com.ace.mobile.core.model.ExerciseSession

sealed class SessionUiState {
    data object Idle : SessionUiState()
    data object Loading : SessionUiState()
    data class Active(val session: ExerciseSession) : SessionUiState()
    data class Paused(
        val session: ExerciseSession,
        val isAutoPaused: Boolean = false
    ) : SessionUiState()
    data class Stopping(val session: ExerciseSession) : SessionUiState()
    data class Completed(
        val session: ExerciseSession,
        val xpGained: Double = 0.0,
        val blocksInSession: Int = 0
    ) : SessionUiState()
    data class Error(val message: String) : SessionUiState()
}