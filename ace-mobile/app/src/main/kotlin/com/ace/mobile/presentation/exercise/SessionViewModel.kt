package com.ace.mobile.presentation.exercise

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.core.content.ContextCompat
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

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

    // ─── Datos en vivo del servicio ───
    private val _heartRate = MutableStateFlow(0.0)
    val heartRate: StateFlow<Double> = _heartRate.asStateFlow()

    private val _elapsedSeconds = MutableStateFlow(0)
    val elapsedSeconds: StateFlow<Int> = _elapsedSeconds.asStateFlow()

    private val _blockCount = MutableStateFlow(0)
    val blockCount: StateFlow<Int> = _blockCount.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    // ─── Service Binding ───
    private var exerciseSyncService: ExerciseSyncService? = null
    private var isBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as ExerciseSyncService.LocalBinder
            exerciseSyncService = binder.getService()
            isBound = true
            android.util.Log.d("SessionViewModel", "ExerciseSyncService conectado")

            // Suscribirse a StateFlow del servicio
            observeServiceStateFlows()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            exerciseSyncService = null
            isBound = false
            android.util.Log.d("SessionViewModel", "ExerciseSyncService desconectado")
        }
    }

    fun startSession(sportType: SportType, userId: String) {
        viewModelScope.launch {
            _uiState.value = SessionUiState.Loading

            startSessionUseCase(sportType, userId)
                .onSuccess { session ->
                    _currentSession.value = session
                    _uiState.value = SessionUiState.Active(session)

                    // 1. Iniciar servicio como foreground
                    val serviceIntent = Intent(context, ExerciseSyncService::class.java).apply {
                        action = ExerciseSyncService.ACTION_START_SESSION
                        putExtra(ExerciseSyncService.EXTRA_SESSION_ID, session.sessionId)
                        putExtra(ExerciseSyncService.EXTRA_SPORT_TYPE, sportType.name)
                        putExtra(ExerciseSyncService.EXTRA_USER_ID, userId)
                    }
                    ContextCompat.startForegroundService(context, serviceIntent)

                    // 2. Vincularse al servicio para leer datos en vivo
                    val bindIntent = Intent(context, ExerciseSyncService::class.java)
                    context.bindService(bindIntent, serviceConnection, Context.BIND_AUTO_CREATE)
                }
                .onFailure { error ->
                    _uiState.value = SessionUiState.Error(error.message ?: "Failed to start session")
                }
        }
    }

    /**
     * Usuario presiona STOP en el mobile.
     * Envia STOP al reloj y espera confirmacion (STOPPED).
     * El servicio notificara via sessionStopped cuando llegue.
     */
    fun stopSession() {
        val session = _currentSession.value ?: return

        viewModelScope.launch {
            _uiState.value = SessionUiState.Stopping(session)

            // 1. Enviar STOP al reloj (el reloj respondera con STOPPED)
            sendStopCommandUseCase(session.sessionId)

            // 2. El servicio recibira STOPPED del reloj y emitira sessionStopped
            //    → observeServiceStateFlows() ya escucha eso
            //    → cuando llega, llama finalizeStopSession()

            // 3. Timeout de seguridad: si en 15s no llega STOPPED, forzar stop
            kotlinx.coroutines.delay(15000L)
            if (_uiState.value is SessionUiState.Stopping) {
                android.util.Log.w("SessionViewModel", "Timeout esperando STOPPED del reloj, forzando stop")
                finalizeStopSession(session.sessionId, forced = true)
            }
        }
    }

    private fun observeServiceStateFlows() {
        val service = exerciseSyncService ?: return

        // HeartRate
        service.heartRate
            .onEach { _heartRate.value = it }
            .launchIn(viewModelScope)

        // ElapsedSeconds
        service.elapsedSeconds
            .onEach { _elapsedSeconds.value = it }
            .launchIn(viewModelScope)

        // BlockCount
        service.blockCount
            .onEach { _blockCount.value = it }
            .launchIn(viewModelScope)

        // IsConnected
        service.isConnected
            .onEach { _isConnected.value = it }
            .launchIn(viewModelScope)

        // SessionStopped: cuando el reloj envia STOPPED
        service.sessionStopped
            .onEach { event ->
                android.util.Log.i("SessionViewModel", "SessionStopped recibido: $event")
                finalizeStopSession(event.sessionId, forced = false)
            }
            .launchIn(viewModelScope)
    }

    private fun finalizeStopSession(sessionId: String, forced: Boolean) {
        viewModelScope.launch {
            // Desvincular del servicio
            if (isBound) {
                context.unbindService(serviceConnection)
                isBound = false
            }

            // Detener el servicio (ya proceso el bloque final)
            val stopIntent = Intent(context, ExerciseSyncService::class.java)
            context.stopService(stopIntent)

            // Persistir en SQLite
            stopSessionUseCase(sessionId)
                .onSuccess { completedSession ->
                    _currentSession.value = null
                    _heartRate.value = 0.0
                    _elapsedSeconds.value = 0
                    _blockCount.value = 0
                    _isConnected.value = false
                    _uiState.value = SessionUiState.Completed(completedSession)
                }
                .onFailure { error ->
                    _uiState.value = SessionUiState.Error(
                        error.message ?: if (forced) "Forced stop failed" else "Failed to stop session"
                    )
                }
        }
    }

    fun resetState() {
        _uiState.value = SessionUiState.Idle
        _currentSession.value = null
        _heartRate.value = 0.0
        _elapsedSeconds.value = 0
        _blockCount.value = 0
        _isConnected.value = false

        if (isBound) {
            context.unbindService(serviceConnection)
            isBound = false
        }
        exerciseSyncService = null
    }

    override fun onCleared() {
        super.onCleared()
        if (isBound) {
            context.unbindService(serviceConnection)
            isBound = false
        }
    }
}