package com.ace.mobile.feature.exercise.presentation

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ace.mobile.core.data.BlockRepository
import com.ace.mobile.core.data.SessionSampleBuffer
import com.ace.mobile.core.model.ExerciseSession
import com.ace.mobile.feature.exercise.domain.PauseSessionUseCase
import com.ace.mobile.feature.exercise.domain.ResumeSessionUseCase
import com.ace.mobile.feature.exercise.domain.StartSessionUseCase
import com.ace.mobile.feature.exercise.domain.StopSessionUseCase
import com.ace.mobile.feature.wear.domain.SendStopCommandUseCase
import com.ace.mobile.feature.exercise.service.ExerciseSyncService
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

// Constantes de auto-pausa
private const val PAUSE_BPM_THRESHOLD = 110.0
private const val LOW_BPM_PAUSE_SECONDS = 30
private const val STOP_TIMEOUT_MS = 15_000L

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val startSessionUseCase: StartSessionUseCase,
    private val stopSessionUseCase: StopSessionUseCase,
    private val pauseSessionUseCase: PauseSessionUseCase,
    private val resumeSessionUseCase: ResumeSessionUseCase,
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

    private val _totalXp = MutableStateFlow(0.0)
    val totalXp: StateFlow<Double> = _totalXp.asStateFlow()

    private val _samplesReceived = MutableStateFlow(0)
    val samplesReceived: StateFlow<Int> = _samplesReceived.asStateFlow()

    // ← NUEVO: Contador de segundos con FC baja para auto-pausa
    private val _lowBpmSeconds = MutableStateFlow(0)
    val lowBpmSeconds: StateFlow<Int> = _lowBpmSeconds.asStateFlow()

    // ← NUEVO: Flag para distinguir pausa manual de auto-pausa
    private var isManualPaused = false

    private var stopTimeoutJob: Job? = null
    private var timerJob: Job? = null
    private var autoPauseWatcherJob: Job? = null

    init {
        sessionSampleBuffer.observeSamples()
            .onEach { sample ->
                _heartRate.value = sample.bpm
                _samplesReceived.value += 1
                Log.d(TAG, "Sample: ${sample.bpm.toInt()} BPM")
            }
            .launchIn(viewModelScope)

        sessionSampleBuffer.observeBlocks()
            .onEach { blockSummary ->
                _blockCount.value = blockSummary.blockCount
                _totalXp.value += blockSummary.xpGained
                Log.i(TAG, "UI: Block #${blockSummary.blockCount}, +${blockSummary.xpGained} XP, total=${_totalXp.value}")
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
                    _totalXp.value = 0.0
                    _samplesReceived.value = 0
                    _heartRate.value = 0.0
                    _lowBpmSeconds.value = 0
                    isManualPaused = false

                    ExerciseSyncService.startSession(context)
                    _isConnected.value = true
                    startElapsedTimer()
                    startAutoPauseWatcher()
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

    // ← NUEVO: Pausa manual
    fun pauseSession() {
        val session = _currentSession.value ?: return
        if (_uiState.value !is SessionUiState.Active) return

        viewModelScope.launch {
            pauseSessionUseCase(session.sessionId, isAutoPause = false)
                .onSuccess {
                    isManualPaused = true
                    _uiState.value = SessionUiState.Paused(session, isAutoPaused = false)
                    stopAutoPauseWatcher()
                    Log.i(TAG, "Session manually paused")
                }
                .onFailure { error ->
                    Log.e(TAG, "Failed to pause session", error)
                }
        }
    }

    // ← NUEVO: Reanudar (manual o auto)
    fun resumeSession() {
        val session = _currentSession.value ?: return
        if (_uiState.value !is SessionUiState.Paused) return

        viewModelScope.launch {
            resumeSessionUseCase(session.sessionId)
                .onSuccess {
                    isManualPaused = false
                    _uiState.value = SessionUiState.Active(session)
                    _lowBpmSeconds.value = 0
                    startAutoPauseWatcher()
                    Log.i(TAG, "Session resumed")
                }
                .onFailure { error ->
                    Log.e(TAG, "Failed to resume session", error)
                }
        }
    }

    fun stopSession() {
        val session = _currentSession.value ?: run {
            Log.w(TAG, "stopSession() called but no active session")
            return
        }

        stopTimeoutJob?.cancel()
        stopAutoPauseWatcher()

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

    // ← NUEVO: Watcher de FC para auto-pausa
    private fun startAutoPauseWatcher() {
        autoPauseWatcherJob?.cancel()
        autoPauseWatcherJob = viewModelScope.launch {
            Log.i(TAG, "Auto-pause watcher started (threshold=${PAUSE_BPM_THRESHOLD} BPM, ${LOW_BPM_PAUSE_SECONDS}s)")

            while (true) {
                delay(1000L)

                val currentState = _uiState.value
                if (currentState !is SessionUiState.Active && currentState !is SessionUiState.Paused) {
                    break // Salir si la sesión ya no está activa/pausada
                }

                val bpm = _heartRate.value

                if (currentState is SessionUiState.Active) {
                    if (bpm > 0 && bpm < PAUSE_BPM_THRESHOLD) {
                        val newLowSeconds = _lowBpmSeconds.value + 1
                        _lowBpmSeconds.value = newLowSeconds
                        Log.d(TAG, "Low BPM: ${bpm.toInt()} for ${newLowSeconds}s")

                        if (newLowSeconds >= LOW_BPM_PAUSE_SECONDS) {
                            // Auto-pausa
                            val session = _currentSession.value ?: break
                            pauseSessionUseCase(session.sessionId, isAutoPause = true)
                                .onSuccess {
                                    _uiState.value = SessionUiState.Paused(session, isAutoPaused = true)
                                    Log.i(TAG, "Auto-pause triggered: FC < $PAUSE_BPM_THRESHOLD for ${LOW_BPM_PAUSE_SECONDS}s")
                                }
                            break // Salir del watcher, se reanuda cuando FC sube
                        }
                    } else {
                        if (_lowBpmSeconds.value > 0) {
                            _lowBpmSeconds.value = 0
                            Log.d(TAG, "BPM recovered: ${bpm.toInt()}, reset low counter")
                        }
                    }
                }

                // Auto-reanudación SOLO si fue auto-pausa (no manual)
                if (currentState is SessionUiState.Paused && !isManualPaused) {
                    if (bpm >= PAUSE_BPM_THRESHOLD) {
                        val session = _currentSession.value ?: break
                        resumeSessionUseCase(session.sessionId)
                            .onSuccess {
                                _uiState.value = SessionUiState.Active(session)
                                _lowBpmSeconds.value = 0
                                Log.i(TAG, "Auto-resume triggered: FC >= $PAUSE_BPM_THRESHOLD")
                            }
                        // Continuar el loop para seguir vigilando
                    }
                }
            }

            Log.i(TAG, "Auto-pause watcher stopped")
        }
    }

    private fun stopAutoPauseWatcher() {
        autoPauseWatcherJob?.cancel()
        autoPauseWatcherJob = null
        _lowBpmSeconds.value = 0
        Log.d(TAG, "Auto-pause watcher cancelled")
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
        stopAutoPauseWatcher()

        ExerciseSyncService.stopSession(context)

        _currentSession.value = null
        _heartRate.value = 0.0
        _elapsedSeconds.value = 0
        _blockCount.value = 0
        _isConnected.value = false
        _lowBpmSeconds.value = 0
        isManualPaused = false

        viewModelScope.launch {
            try {
                val blocks = blockRepository.getBlocksBySession(session.sessionId)
                val xpGained = blocks.sumOf { (it.xpCalculated ?: 0).toDouble() }
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
        stopAutoPauseWatcher()

        _uiState.value = SessionUiState.Idle
        _currentSession.value = null
        _heartRate.value = 0.0
        _elapsedSeconds.value = 0
        _blockCount.value = 0
        _totalXp.value = 0.0
        _samplesReceived.value = 0
        _lowBpmSeconds.value = 0
        isManualPaused = false
        _isConnected.value = false
    }

    private fun startElapsedTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_uiState.value is SessionUiState.Active || _uiState.value is SessionUiState.Paused) {
                delay(1_000L)
                // Solo contar tiempo cuando está ACTIVE (no cuando está PAUSED)
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
        stopAutoPauseWatcher()
        Log.d(TAG, "ViewModel cleared")
    }
}