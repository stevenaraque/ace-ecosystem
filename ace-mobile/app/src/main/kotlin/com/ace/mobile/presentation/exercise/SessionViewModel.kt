// app/src/main/kotlin/com/ace/mobile/presentation/exercise/SessionViewModel.kt
package com.ace.mobile.presentation.exercise

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ace.mobile.data.repository.BlockRepository
import com.ace.mobile.data.repository.SessionSampleBuffer
import com.ace.mobile.domain.model.ExerciseSession
import com.ace.mobile.domain.usecase.exercise.StartSessionUseCase
import com.ace.mobile.domain.usecase.exercise.StopSessionUseCase
import com.ace.mobile.domain.usecase.wear.SendStopCommandUseCase
import com.ace.mobile.service.ExerciseSyncService
import com.ace.shared.enums.SportType
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "SessionViewModel"
private const val STOP_TIMEOUT_MS = 15_000L

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val startSessionUseCase: StartSessionUseCase,
    private val stopSessionUseCase: StopSessionUseCase,
    private val sendStopCommandUseCase: SendStopCommandUseCase,
    private val blockRepository: BlockRepository,
    private val sessionSampleBuffer: SessionSampleBuffer,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<SessionUiState>(SessionUiState.Idle)
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    private val _currentSession = MutableStateFlow<ExerciseSession?>(null)
    val currentSession: StateFlow<ExerciseSession?> = _currentSession.asStateFlow()

    private val _heartRate = MutableStateFlow(0.0)
    val heartRate: StateFlow<Double> = _heartRate.asStateFlow()

    private val _elapsedSeconds = MutableStateFlow(0)
    val elapsedSeconds: StateFlow<Int> = _elapsedSeconds.asStateFlow()

    private val _blockCount = MutableStateFlow(0)
    val blockCount: StateFlow<Int> = _blockCount.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _totalXp = MutableStateFlow(0L)
    val totalXp: StateFlow<Long> = _totalXp.asStateFlow()

    private val _samplesReceived = MutableStateFlow(0)
    val samplesReceived: StateFlow<Int> = _samplesReceived.asStateFlow()

    private var stopTimeoutJob: Job? = null
    private var timerJob: Job? = null

    init {
        // Observar samples del reloj en tiempo real
        sessionSampleBuffer.observeSamples()
            .onEach { sample ->
                _heartRate.value = sample.bpm
                _samplesReceived.value = _samplesReceived.value + 1
                Log.d(TAG, "Sample received: ${sample.bpm.toInt()} BPM")
            }
            .launchIn(viewModelScope)
    }

    fun startSession(sportType: SportType, userId: String) {
        viewModelScope.launch {
            _uiState.value = SessionUiState.Loading

            startSessionUseCase(sportType, userId)
                .onSuccess { session ->
                    _currentSession.value = session
                    _uiState.value = SessionUiState.Active(session)
                    _elapsedSeconds.value = 0
                    _blockCount.value = 0
                    _totalXp.value = 0
                    _samplesReceived.value = 0
                    _heartRate.value = 0.0

                    ExerciseSyncService.startSession(context)
                    _isConnected.value = true
                    startElapsedTimer()
                    Log.i(TAG, "Session started: ${session.sessionId}")
                }
                .onFailure { error ->
                    Log.e(TAG, "Failed to start session", error)
                    _uiState.value = SessionUiState.Error(
                        error.message ?: "Failed to start session"
                    )
                }
        }
    }

    fun stopSession() {
        val session = _currentSession.value ?: run {
            Log.w(TAG, "stopSession() called but no active session")
            return
        }

        stopTimeoutJob?.cancel()

        viewModelScope.launch {
            _uiState.value = SessionUiState.Stopping(session)

            stopSessionUseCase(session.sessionId)
                .onSuccess { completedSession ->
                    Log.i(TAG, "Session stopped: ${completedSession.sessionId}")
                    finalizeStopSession(completedSession, forced = false)
                }
                .onFailure { error ->
                    Log.e(TAG, "StopSessionUseCase failed", error)
                    startStopTimeout(session.sessionId)
                }

            startStopTimeout(session.sessionId)
        }
    }

    private fun startStopTimeout(sessionId: String) {
        stopTimeoutJob?.cancel()
        stopTimeoutJob = viewModelScope.launch {
            delay(STOP_TIMEOUT_MS)
            if (_uiState.value is SessionUiState.Stopping) {
                Log.w(TAG, "Timeout waiting for STOP, forcing completion")
                forceCompleteSession(sessionId)
            }
        }
    }

    private fun forceCompleteSession(sessionId: String) {
        viewModelScope.launch {
            stopSessionUseCase(sessionId)
                .onSuccess { completedSession ->
                    finalizeStopSession(completedSession, forced = true)
                }
                .onFailure { error ->
                    Log.e(TAG, "Forced stop also failed", error)
                    _uiState.value = SessionUiState.Error(
                        error.message ?: "Failed to stop session (forced)"
                    )
                }
        }
    }

    private fun finalizeStopSession(session: ExerciseSession, forced: Boolean) {
        stopTimeoutJob?.cancel()
        stopTimeoutJob = null

        ExerciseSyncService.stopSession(context)

        _currentSession.value = null
        _heartRate.value = 0.0
        _elapsedSeconds.value = 0
        _blockCount.value = 0
        _isConnected.value = false

        viewModelScope.launch {
            try {
                val blocks = blockRepository.getBlocksBySession(session.sessionId)
                val xpGained = blocks.sumOf { (it.xpCalculated ?: 0).toLong() }
                val blockCount = blocks.size

                _uiState.value = SessionUiState.Completed(
                    session = session,
                    xpGained = xpGained,
                    blocksInSession = blockCount
                )
                Log.i(TAG, "Session completed: $blockCount blocks, $xpGained XP")
            } catch (e: Exception) {
                Log.e(TAG, "Error calculating session XP", e)
                _uiState.value = SessionUiState.Completed(session = session)
            }
        }

        if (forced) {
            Log.w(TAG, "Session finalized with forced=true")
        }
    }

    fun resetState() {
        stopTimeoutJob?.cancel()
        stopTimeoutJob = null
        stopTimer()

        _uiState.value = SessionUiState.Idle
        _currentSession.value = null
        _heartRate.value = 0.0
        _elapsedSeconds.value = 0
        _blockCount.value = 0
        _totalXp.value = 0
        _samplesReceived.value = 0
        _isConnected.value = false
    }

    private fun startElapsedTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_uiState.value is SessionUiState.Active) {
                delay(1_000L)
                if (_uiState.value is SessionUiState.Active) {
                    _elapsedSeconds.value += 1
                }
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopTimeoutJob?.cancel()
        stopTimer()
        Log.d(TAG, "ViewModel cleared")
    }
}