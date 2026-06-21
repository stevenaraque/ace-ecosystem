package com.ace.mobile.presentation.exercise

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "SessionViewModel"
private const val STOP_TIMEOUT_MS = 15_000L

/**
 * ViewModel de la pantalla de sesión de ejercicio.
 *
 * Arquitectura corregida (S3 funcional):
 * - ExerciseSyncService: solo foreground + notificación persistente (no binder, no StateFlows).
 * - Datos en vivo del reloj: se reciben via WearDataListenerService (S1) y se persisten en Room.
 *   El ViewModel puede observar BlockRepository si necesita conteo de bloques.
 * - STOP: envía comando al reloj, espera confirmación via callback del use case o timeout.
 *
 * NOTA: Este ViewModel NO hace bindService() porque ExerciseSyncService no expone IBinder.
 */
@HiltViewModel
class SessionViewModel @Inject constructor(
    private val startSessionUseCase: StartSessionUseCase,
    private val stopSessionUseCase: StopSessionUseCase,
    private val sendStopCommandUseCase: SendStopCommandUseCase,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<SessionUiState>(SessionUiState.Idle)
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    private val _currentSession = MutableStateFlow<ExerciseSession?>(null)
    val currentSession: StateFlow<ExerciseSession?> = _currentSession.asStateFlow()

    // ─── Datos en vivo (mock/placeholder hasta S1 exponga flows reales) ───
    private val _heartRate = MutableStateFlow(0.0)
    val heartRate: StateFlow<Double> = _heartRate.asStateFlow()

    private val _elapsedSeconds = MutableStateFlow(0)
    val elapsedSeconds: StateFlow<Int> = _elapsedSeconds.asStateFlow()

    private val _blockCount = MutableStateFlow(0)
    val blockCount: StateFlow<Int> = _blockCount.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    // ─── Control de timeout ───
    private var stopTimeoutJob: Job? = null

    /**
     * Inicia una nueva sesión de ejercicio.
     * 1. Persiste sesión en Room (ACTIVE).
     * 2. Envía START al reloj.
     * 3. Levanta ExerciseSyncService como foreground (notificación persistente).
     */
    fun startSession(sportType: SportType, userId: String) {
        viewModelScope.launch {
            _uiState.value = SessionUiState.Loading

            startSessionUseCase(sportType, userId)
                .onSuccess { session ->
                    _currentSession.value = session
                    _uiState.value = SessionUiState.Active(session)
                    _elapsedSeconds.value = 0
                    _blockCount.value = 0

                    // Iniciar foreground service (sin bind — el servicio no expone binder)
                    ExerciseSyncService.startSession(context)

                    // TODO(S1): Suscribirse a datos en vivo del reloj via WearDataListenerService
                    //  cuando esté implementado. Por ahora los valores se mantienen en 0.
                    _isConnected.value = true

                    // Iniciar contador de tiempo transcurrido
                    startElapsedTimer()
                }
                .onFailure { error ->
                    android.util.Log.e(TAG, "Failed to start session", error)
                    _uiState.value = SessionUiState.Error(
                        error.message ?: "Failed to start session"
                    )
                }
        }
    }

    /**
     * Usuario presiona STOP en el mobile.
     * Envía STOP al reloj y espera confirmación o timeout.
     *
     * NOTA: StopSessionUseCase YA envía sendStopCommandUseCase internamente.
     *       No duplicamos la llamada.
     */
    fun stopSession() {
        val session = _currentSession.value ?: run {
            android.util.Log.w(TAG, "stopSession() called but no active session")
            return
        }

        // Cancelar timeout previo si existe
        stopTimeoutJob?.cancel()

        viewModelScope.launch {
            _uiState.value = SessionUiState.Stopping(session)

            // 1. Llamar al use case que: envía STOP al reloj + completa la sesión en Room
            stopSessionUseCase(session.sessionId)
                .onSuccess { completedSession ->
                    android.util.Log.i(TAG, "Session stopped successfully: ${completedSession.sessionId}")
                    finalizeStopSession(completedSession, forced = false)
                }
                .onFailure { error ->
                    android.util.Log.e(TAG, "StopSessionUseCase failed", error)
                    // Si falla, intentamos timeout de gracia
                    startStopTimeout(session.sessionId)
                }

            // 2. Timeout de seguridad: si en 15s no se completó, forzar
            startStopTimeout(session.sessionId)
        }
    }

    /**
     * Timeout de seguridad para el stop.
     * Si el use case no responde en 15s, forzamos la finalización.
     */
    private fun startStopTimeout(sessionId: String) {
        stopTimeoutJob?.cancel()
        stopTimeoutJob = viewModelScope.launch {
            delay(STOP_TIMEOUT_MS)
            if (_uiState.value is SessionUiState.Stopping) {
                android.util.Log.w(TAG, "Timeout esperando STOP, forzando finalización")
                // Forzar completación local sin esperar al reloj
                forceCompleteSession(sessionId)
            }
        }
    }

    /**
     * Forzar completación de sesión cuando el reloj no responde.
     */
    private fun forceCompleteSession(sessionId: String) {
        viewModelScope.launch {
            stopSessionUseCase(sessionId)
                .onSuccess { completedSession ->
                    finalizeStopSession(completedSession, forced = true)
                }
                .onFailure { error ->
                    android.util.Log.e(TAG, "Forced stop also failed", error)
                    _uiState.value = SessionUiState.Error(
                        error.message ?: "Failed to stop session (forced)"
                    )
                }
        }
    }

    /**
     * Limpia estado y detiene el foreground service.
     */
    private fun finalizeStopSession(session: ExerciseSession, forced: Boolean) {
        // Cancelar timeout
        stopTimeoutJob?.cancel()
        stopTimeoutJob = null

        // Detener foreground service
        ExerciseSyncService.stopSession(context)

        // Resetear flows
        _currentSession.value = null
        _heartRate.value = 0.0
        _elapsedSeconds.value = 0
        _blockCount.value = 0
        _isConnected.value = false

        _uiState.value = SessionUiState.Completed(session)

        if (forced) {
            android.util.Log.w(TAG, "Session finalized with forced=true")
        }
    }

    /**
     * Resetea el estado del ViewModel a Idle.
     * Útil cuando el usuario cierra la pantalla de completado.
     */
    fun resetState() {
        stopTimeoutJob?.cancel()
        stopTimeoutJob = null

        _uiState.value = SessionUiState.Idle
        _currentSession.value = null
        _heartRate.value = 0.0
        _elapsedSeconds.value = 0
        _blockCount.value = 0
        _isConnected.value = false
    }

    /**
     * Contador interno de segundos transcurridos.
     * Se detiene automáticamente cuando la sesión ya no está activa.
     */
    private fun startElapsedTimer() {
        viewModelScope.launch {
            while (_uiState.value is SessionUiState.Active) {
                delay(1_000L)
                if (_uiState.value is SessionUiState.Active) {
                    _elapsedSeconds.value += 1
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopTimeoutJob?.cancel()
        android.util.Log.d(TAG, "ViewModel cleared")
    }
}