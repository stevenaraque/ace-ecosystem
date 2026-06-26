// app/src/main/kotlin/com/ace/mobile/feature/exercise/presentation/SessionViewModel.kt
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
import com.ace.mobile.feature.wear.domain.ReceiveWearDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import sena.adso.ace_mobile.BuildConfig
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "SessionViewModel"
private const val PAUSE_BPM_THRESHOLD = 110.0
private const val LOW_BPM_PAUSE_SECONDS = 30
private const val STOP_TIMEOUT_MS = 15_000L

@HiltViewModel
class SessionViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val startSessionUseCase: StartSessionUseCase,
    private val stopSessionUseCase: StopSessionUseCase,
    private val pauseSessionUseCase: PauseSessionUseCase,
    private val resumeSessionUseCase: ResumeSessionUseCase,
    private val receiveWearDataUseCase: ReceiveWearDataUseCase,
    private val sessionSampleBuffer: SessionSampleBuffer,
    private val blockRepository: BlockRepository
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

    private val _totalXp = MutableStateFlow(0.0)
    val totalXp: StateFlow<Double> = _totalXp.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _isSimulating = MutableStateFlow(false)
    val isSimulating: StateFlow<Boolean> = _isSimulating.asStateFlow()

    private val _samplesReceived = MutableStateFlow(0)
    private val _lowBpmSeconds = MutableStateFlow(0)
    private var isManualPaused = false

    private var timerJob: Job? = null
    private var stopTimeoutJob: Job? = null
    private var wearDataJob: Job? = null

    init {
        observeWearHeartRate()
        observeBlockCountAndXp()

        viewModelScope.launch {
            receiveWearDataUseCase.isSimulationActive().collect { active ->
                _isSimulating.value = active
                if (active) {
                    _isConnected.value = true
                }
            }
        }
    }

    private fun observeWearHeartRate() {
        wearDataJob?.cancel()
        wearDataJob = receiveWearDataUseCase.observeHeartRate()
            .onEach { sample ->
                _isConnected.value = true
                _samplesReceived.value += 1

                if (_uiState.value is SessionUiState.Active) {
                    _heartRate.value = sample.bpm
                    handleAutoPauseLogic(sample.bpm)
                }
            }
            .launchIn(viewModelScope)
    }

    private fun observeBlockCountAndXp() {
        // SOLUCIÓN A LOS ERRORES DE LECTURA DEL BUFFER:
        // Si sessionSampleBuffer ya expone flujos individuales o propiedades directas de bloques/xp:
        viewModelScope.launch {
            // Escuchamos el cambio de bloques de forma genérica o mediante un polling reactivo continuo seguro
            while (true) {
                try {
                    // Adaptado para evitar referencias caídas a closedBlocksFlow
                    val currentBlocks = blockRepository.getBlocksBySession(_currentSession.value?.sessionId ?: "")
                    _blockCount.value = currentBlocks.size
                    _totalXp.value = currentBlocks.sumOf { (it.xpCalculated ?: 0).toDouble() }
                } catch (e: Exception) {
                    // No hay sesión activa aún
                }
                delay(2000L) // Actualiza de forma segura cada 2 segundos
            }
        }
    }

    private fun handleAutoPauseLogic(bpm: Double) {
        if (isManualPaused) return

        if (bpm < PAUSE_BPM_THRESHOLD) {
            _lowBpmSeconds.value += 1
            if (_lowBpmSeconds.value >= LOW_BPM_PAUSE_SECONDS) {
                Log.w(TAG, "Auto-pausa activada por FC baja")
                autoPauseSession()
            }
        } else {
            _lowBpmSeconds.value = 0
        }
    }

    fun startSession(sportType: com.ace.shared.enums.SportType, userId: String) {
        viewModelScope.launch {
            _uiState.value = SessionUiState.Loading
            val result = startSessionUseCase(sportType, userId)
            result.fold(
                onSuccess = { session ->
                    _currentSession.value = session
                    _uiState.value = SessionUiState.Active(session)
                    isManualPaused = false
                    _lowBpmSeconds.value = 0
                    startElapsedTimer()
                },
                onFailure = { e ->
                    _uiState.value = SessionUiState.Error(e.message ?: "Error desconocido")
                }
            )
        }
    }

    fun pauseSession() {
        val session = _currentSession.value ?: return
        viewModelScope.launch {
            isManualPaused = true
            pauseSessionUseCase(session.sessionId, isAutoPause = false)
            _uiState.value = SessionUiState.Paused(session, isAutoPaused = false)
        }
    }

    private fun autoPauseSession() {
        val session = _currentSession.value ?: return
        viewModelScope.launch {
            pauseSessionUseCase(session.sessionId, isAutoPause = true)
            _uiState.value = SessionUiState.Paused(session, isAutoPaused = true)
        }
    }

    fun resumeSession() {
        val session = _currentSession.value ?: return
        viewModelScope.launch {
            isManualPaused = false
            _lowBpmSeconds.value = 0
            resumeSessionUseCase(session.sessionId)
            _uiState.value = SessionUiState.Active(session)
        }
    }

    fun stopSession() {
        val session = _currentSession.value ?: return
        viewModelScope.launch {
            _uiState.value = SessionUiState.Stopping(session)
            stopTimer()

            stopTimeoutJob = launch {
                delay(STOP_TIMEOUT_MS)
                finalizeSessionUi(session)
            }

            val result = stopSessionUseCase(session.sessionId)
            stopTimeoutJob?.cancel()

            result.fold(
                onSuccess = { updatedSession ->
                    finalizeSessionUi(updatedSession)
                },
                onFailure = { e ->
                    _uiState.value = SessionUiState.Error(e.message ?: "Error al detener sesión")
                }
            )
        }
    }

    private fun finalizeSessionUi(session: ExerciseSession) {
        viewModelScope.launch {
            try {
                val blocks = blockRepository.getBlocksBySession(session.sessionId)
                val totalBlocks = blocks.size
                val totalXp = blocks.sumOf { (it.xpCalculated ?: 0).toDouble() }

                _uiState.value = SessionUiState.Completed(
                    session = session,
                    xpGained = totalXp,
                    blocksInSession = totalBlocks
                )

                // SOLUCIÓN AL ERROR DE startSync: Se eliminó la llamada directa ausente
                Log.i(TAG, "Sesión finalizada con éxito y guardada de forma local.")
            } catch (e: Exception) {
                _uiState.value = SessionUiState.Completed(session = session)
            }
        }
    }

    fun toggleSimulation() {
        if (BuildConfig.DEBUG) {
            val newState = !_isSimulating.value
            receiveWearDataUseCase.toggleSimulation(newState)
        }
    }

    fun resetState() {
        stopTimeoutJob?.cancel()
        stopTimeoutJob = null
        stopTimer()

        if (BuildConfig.DEBUG) {
            receiveWearDataUseCase.toggleSimulation(false)
        }

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
        if (BuildConfig.DEBUG) {
            receiveWearDataUseCase.toggleSimulation(false)
        }
    }
}