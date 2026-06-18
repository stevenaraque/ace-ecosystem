package com.ace.mobile.presentation.exercise

import com.ace.mobile.domain.model.ExerciseSession

sealed class SessionUiState {
    data object Idle : SessionUiState()
    data object Loading : SessionUiState()
    data class Active(val session: ExerciseSession) : SessionUiState()
    data class Completed(val session: ExerciseSession) : SessionUiState()
    data class Error(val message: String) : SessionUiState()
}