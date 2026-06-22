// ace-wear/app/src/main/kotlin/com/ace/wear/presentation/session/SessionViewModel.kt
package com.ace.wear.presentation.session

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ace.wear.data.health.HealthServicesManager
import com.ace.wear.data.repository.WearHealthRepository
import com.ace.wear.data.sync.WearMessageClient
import com.ace.wear.domain.usecase.StartExerciseUseCase
import com.ace.wear.domain.usecase.StopExerciseUseCase
import com.ace.wear.presentation.WearSessionState
import com.google.android.gms.wearable.NodeClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * ViewModel de la sesion de ejercicio en el reloj.
 *
 * Responsabilidades:
 * 1. UNICO escuchador de comandos START/STOP del movil via WearMessageClient
 * 2. Gestion de permisos BODY_SENSORS
 * 3. UI: timer, FC en vivo, estado de conexion, modo simulacion
 * 4. Ordena al use case iniciar/detener sensor (NO al repository directo)
 */
@HiltViewModel
class SessionViewModel @Inject constructor(
    private val healthServicesManager: HealthServicesManager,
    private val wearMessageClient: WearMessageClient,
    private val startExerciseUseCase: StartExerciseUseCase,
    private val stopExerciseUseCase: StopExerciseUseCase,
    private val wearHealthRepository: WearHealthRepository,
    private val nodeClient: NodeClient
) : ViewModel() {

    companion object {
        private const val TAG = "SessionViewModel"
        private const val MAX_LOGS = 20
    }

    private val _state = MutableStateFlow(WearSessionState())
    val state: StateFlow<WearSessionState> = _state.asStateFlow()

    private var timerJob: Job? = null
    private var currentSessionId: String? = null
    private var pendingSessionId: String? = null
    private var permissionLauncher: (() -> Unit)? = null

    init {
        // Escuchar muestras de FC del HealthServicesManager para UI
        healthServicesManager.heartRateSamples
            .onEach { sample ->
                _state.value = _state.value.copy(
                    bpm = sample.bpm
                )
                logDiag("FC recibida: ${sample.bpm.toInt()} bpm")
            }
            .launchIn(viewModelScope)

        // Escuchar disponibilidad del sensor
        healthServicesManager.availability
            .onEach { availability ->
                logDiag("Sensor: $availability")
            }
            .launchIn(viewModelScope)

        // Observar modo simulacion del repositorio
        wearHealthRepository.isSimulationMode
            .onEach { isSim ->
                _state.value = _state.value.copy(isSimulationMode = isSim)
                if (isSim) logDiag("=== MODO SIMULACION ACTIVO ===")
            }
            .launchIn(viewModelScope)

        // Observar contador de samples enviados
        wearHealthRepository.samplesSent
            .onEach { count ->
                _state.value = _state.value.copy(samplesSent = count)
            }
            .launchIn(viewModelScope)

        // UNICO escuchador de comandos del movil
        wearMessageClient.commands
            .onEach { command ->
                when (command) {
                    is WearMessageClient.WearCommand.Start -> {
                        logDiag("COMANDO START: sessionId=${command.sessionId}")
                        handleStartCommand(command.sessionId)
                    }
                    is WearMessageClient.WearCommand.Stop -> {
                        logDiag("COMANDO STOP: sessionId=${command.sessionId}")
                        handleStopFromMobile(command.sessionId)
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    fun initialize() {
        logDiag("=== INICIALIZANDO A.C.E WEAR ===")
        startExerciseUseCase()
        wearHealthRepository.initialize()
        checkConnectionStatus()
    }

    fun setPermissionLauncher(launcher: () -> Unit) {
        permissionLauncher = launcher
    }

    fun onPermissionResult(isGranted: Boolean) {
        _state.value = _state.value.copy(
            hasSensorPermission = isGranted,
            permissionDenied = !isGranted
        )

        if (isGranted) {
            logDiag("Permiso concedido")
            pendingSessionId?.let { sessionId ->
                logDiag("Iniciando pendiente: $sessionId")
                pendingSessionId = null
                startSessionInternal(sessionId)
            }
        } else {
            logDiag("Permiso DENEGADO - iniciando con simulacion...")
            pendingSessionId?.let { sessionId ->
                pendingSessionId = null
                startSessionInternal(sessionId)
            }
        }
    }

    private fun handleStartCommand(sessionId: String) {
        if (_state.value.hasSensorPermission) {
            startSessionInternal(sessionId)
        } else {
            logDiag("START sin permiso - solicitando...")
            pendingSessionId = sessionId
            permissionLauncher?.invoke()
                ?: run {
                    logDiag("No launcher, iniciando con simulacion")
                    startSessionInternal(sessionId)
                }
        }
    }

    private fun startSessionInternal(sessionId: String) {
        currentSessionId = sessionId
        _state.value = _state.value.copy(
            isSessionActive = true,
            elapsedSeconds = 0L,
            bpm = null,
            lastError = null,
            samplesSent = 0
        )
        logDiag("Sesion INICIADA: $sessionId")

        startExerciseUseCase.startSession(sessionId)

        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _state.value = _state.value.copy(
                    elapsedSeconds = _state.value.elapsedSeconds + 1
                )
            }
        }
    }

    private fun handleStopFromMobile(sessionId: String) {
        logDiag("STOP del movil: $sessionId")
        stopExerciseUseCase(sessionId)
        stopSessionInternal()
    }

    fun onStopButtonClicked() {
        val sessionId = currentSessionId
        if (sessionId == null) {
            logDiag("ERROR: DETENER sin sesion activa")
            return
        }

        logDiag("Boton DETENER presionado: $sessionId")
        stopSessionInternal()
        stopExerciseUseCase(sessionId)
    }

    private fun stopSessionInternal() {
        currentSessionId = null
        pendingSessionId = null
        stopTimer()
        _state.value = _state.value.copy(
            isSessionActive = false,
            bpm = null,
            elapsedSeconds = 0L,
            isSimulationMode = false,
            samplesSent = 0
        )
        logDiag("UI detenida")
    }

    private fun checkConnectionStatus() {
        viewModelScope.launch {
            try {
                logDiag("Verificando nodos...")
                val nodes = nodeClient.connectedNodes.await()
                val hasConnectedNode = nodes.isNotEmpty()

                _state.value = _state.value.copy(
                    isConnected = hasConnectedNode,
                    nodeCount = nodes.size,
                    lastError = if (!hasConnectedNode) "No hay nodos" else null
                )

                logDiag("Nodos: ${nodes.size}")
                nodes.forEach { node ->
                    logDiag("  -> ${node.displayName}")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error nodos", e)
                _state.value = _state.value.copy(
                    isConnected = false,
                    nodeCount = 0,
                    lastError = e.message
                )
                logDiag("ERROR: ${e.message}")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopTimer()
        logDiag("ViewModel destruido")
    }

    fun dispose() {
        stopTimer()
        stopExerciseUseCase.dispose()
        logDiag("Recursos liberados")
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    private fun logDiag(message: String) {
        Log.d(TAG, message)
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
            .format(java.util.Date())
        val logLine = "[$timestamp] $message"

        val currentLogs = _state.value.diagLogs.toMutableList()
        currentLogs.add(logLine)
        if (currentLogs.size > MAX_LOGS) {
            currentLogs.removeAt(0)
        }
        _state.value = _state.value.copy(diagLogs = currentLogs.toList())
    }
}