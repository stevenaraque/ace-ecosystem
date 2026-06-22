// app/src/main/kotlin/com/ace/mobile/presentation/exercise/SessionUiState.kt
package com.ace.mobile.presentation.exercise

import com.ace.mobile.domain.model.ExerciseSession

sealed class SessionUiState {
    data object Idle : SessionUiState()
    data object Loading : SessionUiState()
    data class Active(val session: ExerciseSession) : SessionUiState()
    data class Stopping(val session: ExerciseSession) : SessionUiState()
    data class Completed(
        val session: ExerciseSession,
        val xpGained: Long = 0L,
        val blocksInSession: Int = 0
    ) : SessionUiState()
    data class Error(val message: String) : SessionUiState()
}