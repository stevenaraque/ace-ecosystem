package com.ace.wear.presentation.session

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ace.wear.data.health.HealthServicesManager
import com.ace.wear.data.repository.WearHealthRepository
import com.ace.wear.data.sync.WearMessageClient
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
 * ViewModel de la sesion de ejercicio en el reloj con diagnostico completo.
 */
@HiltViewModel
class SessionViewModel @Inject constructor(
    private val healthServicesManager: HealthServicesManager,
    private val wearMessageClient: WearMessageClient,
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

    /** SessionId pendiente de iniciar (esperando permiso) */
    private var pendingSessionId: String? = null

    /** Callback para lanzar el dialogo de permiso */
    private var permissionLauncher: (() -> Unit)? = null

    init {
        // Escuchar muestras de FC del HealthServicesManager
        healthServicesManager.heartRateSamples
            .onEach { sample ->
                _state.value = _state.value.copy(bpm = sample.bpm)
                logDiag("FC recibida: ${sample.bpm.toInt()} bpm")
            }
            .launchIn(viewModelScope)

        // Escuchar disponibilidad del sensor
        healthServicesManager.availability
            .onEach { availability ->
                logDiag("Sensor disponibilidad: $availability")
            }
            .launchIn(viewModelScope)

        // Escuchar comandos del movil (START/STOP)
        wearMessageClient.commands
            .onEach { command ->
                when (command) {
                    is WearMessageClient.WearCommand.Start -> {
                        logDiag("COMANDO START recibido: sessionId=${command.sessionId}")
                        handleStartCommand(command.sessionId)
                    }
                    is WearMessageClient.WearCommand.Stop -> {
                        logDiag("COMANDO STOP recibido: sessionId=${command.sessionId}")
                        onSessionStopped(command.sessionId)
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    /**
     * Registra el launcher de permiso desde MainActivity.
     */
    fun setPermissionLauncher(launcher: () -> Unit) {
        permissionLauncher = launcher
    }

    /**
     * Llamado por MainActivity cuando el usuario responde al dialogo de permiso.
     */
    fun onPermissionResult(isGranted: Boolean) {
        _state.value = _state.value.copy(
            hasSensorPermission = isGranted,
            permissionDenied = !isGranted
        )

        if (isGranted) {
            logDiag("Permiso BODY_SENSORS concedido")
            // Si hay una sesion pendiente, iniciarla ahora
            pendingSessionId?.let { sessionId ->
                logDiag("Iniciando sesion pendiente: $sessionId")
                pendingSessionId = null
                startSessionInternal(sessionId)
            }
        } else {
            logDiag("Permiso BODY_SENSORS DENEGADO - no se puede monitorear FC")
        }
    }

    /**
     * Maneja el comando START: si no tiene permiso, lo pide primero.
     */
    private fun handleStartCommand(sessionId: String) {
        if (_state.value.hasSensorPermission) {
            // Ya tiene permiso, iniciar directamente
            startSessionInternal(sessionId)
        } else {
            // No tiene permiso, guardar sessionId y pedir permiso
            logDiag("START recibido pero falta permiso BODY_SENSORS - solicitando...")
            pendingSessionId = sessionId
            permissionLauncher?.invoke()
                ?: logDiag("ERROR: permissionLauncher no registrado")
        }
    }

    /**
     * Inicia la sesion real (monitoreo de FC + timer).
     * Solo llamar despues de confirmar que tiene permiso.
     */
    private fun startSessionInternal(sessionId: String) {
        currentSessionId = sessionId
        _state.value = _state.value.copy(
            isSessionActive = true,
            elapsedSeconds = 0L,
            bpm = null,
            lastError = null
        )
        logDiag("Sesion INICIADA: $sessionId")

        // Iniciar monitoreo de FC
        val success = healthServicesManager.startHeartRateMonitoring()
        if (!success) {
            logDiag("ERROR: HealthServicesManager no pudo iniciar")
            _state.value = _state.value.copy(
                lastError = "Fallo al iniciar sensor"
            )
        }

        // Iniciar timer
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

    fun initialize() {
        logDiag("=== INICIALIZANDO A.C.E WEAR ===")
        wearHealthRepository.initialize()
        checkConnectionStatus()
    }

    private fun checkConnectionStatus() {
        viewModelScope.launch {
            try {
                logDiag("Verificando nodos conectados...")
                val nodes = nodeClient.connectedNodes.await()
                val hasConnectedNode = nodes.isNotEmpty()

                _state.value = _state.value.copy(
                    isConnected = hasConnectedNode,
                    nodeCount = nodes.size,
                    lastError = if (!hasConnectedNode) "No hay nodos" else null
                )

                logDiag("Nodos encontrados: ${nodes.size}")
                nodes.forEach { node ->
                    logDiag("  -> ${node.displayName} (${node.id})")
                }

                if (!hasConnectedNode) {
                    logDiag("ADVERTENCIA: No hay movil conectado por DataLayer")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error verificando nodos", e)
                _state.value = _state.value.copy(
                    isConnected = false,
                    nodeCount = 0,
                    lastError = e.message ?: "Error desconocido"
                )
                logDiag("ERROR: ${e.message}")
            }
        }
    }

    private fun onSessionStopped(sessionId: String) {
        currentSessionId = null
        pendingSessionId = null
        stopTimer()
        _state.value = _state.value.copy(
            isSessionActive = false,
            bpm = null
        )
        logDiag("Sesion DETENIDA por movil: $sessionId")
    }

    fun onStopButtonClicked() {
        val sessionId = currentSessionId
        if (sessionId == null) {
            logDiag("ERROR: Boton DETENER presionado pero no hay sesion activa")
            return
        }

        logDiag("Boton DETENER presionado por usuario")
        logDiag("Enviando STOPPED al movil: $sessionId")

        // Detener UI INMEDIATAMENTE (no esperar al envio)
        currentSessionId = null
        pendingSessionId = null
        stopTimer()
        _state.value = _state.value.copy(
            isSessionActive = false,
            bpm = null,
            elapsedSeconds = 0L
        )
        logDiag("UI detenida localmente")

        // Enviar STOPPED al movil en background
        viewModelScope.launch {
            try {
                wearMessageClient.sendStoppedToMobile(sessionId)
                logDiag("STOPPED enviado al movil OK")
            } catch (e: Exception) {
                logDiag("ERROR enviando STOPPED: ${e.message}")
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
        stopExerciseUseCase()
        logDiag("Recursos liberados")
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    /**
     * Agrega un log de diagnostico al estado.
     */
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