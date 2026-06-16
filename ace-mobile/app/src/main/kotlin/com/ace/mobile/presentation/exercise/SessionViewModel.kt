package com.ace.mobile.presentation.exercise

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ace.mobile.domain.model.ExerciseSession
import com.ace.mobile.domain.usecase.exercise.StartSessionUseCase
import com.ace.mobile.domain.usecase.exercise.StopSessionUseCase
import com.ace.shared.enums.SportType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val startSessionUseCase: StartSessionUseCase,
    private val stopSessionUseCase: StopSessionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<SessionUiState>(SessionUiState.Idle)
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    private val _currentSession = MutableStateFlow<ExerciseSession?>(null)
    val currentSession: StateFlow<ExerciseSession?> = _currentSession.asStateFlow()

    fun startSession(sportType: SportType, userId: String) {
        viewModelScope.launch {
            _uiState.value = SessionUiState.Loading

            startSessionUseCase(sportType, userId)
                .onSuccess { session ->
                    _currentSession.value = session
                    _uiState.value = SessionUiState.Active(session)

                    // Iniciar foreground service
                    // TODO: Llamar a ExerciseSyncService.startSession()
                }
                .onFailure { error ->
                    _uiState.value = SessionUiState.Error(error.message ?: "Failed to start session")
                }
        }
    }

    fun stopSession() {
        val session = _currentSession.value ?: return

        viewModelScope.launch {
            stopSessionUseCase(session.sessionId)
                .onSuccess { completedSession ->
                    _currentSession.value = null
                    _uiState.value = SessionUiState.Completed(completedSession)
                }
                .onFailure { error ->
                    _uiState.value = SessionUiState.Error(error.message ?: "Failed to stop session")
                }
        }
    }

    fun resetState() {
        _uiState.value = SessionUiState.Idle
        _currentSession.value = null
    }
}