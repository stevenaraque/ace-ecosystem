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
 * 3. UI: timer, FC en vivo, estado de conexion
 * 4. Ordena al use case iniciar/detener sensor (NO al repository directo)
 */
@HiltViewModel
class SessionViewModel @Inject constructor(
    private val healthServicesManager: HealthServicesManager,
    private val wearMessageClient: WearMessageClient,
    private val startExerciseUseCase: StartExerciseUseCase,
    private val stopExerciseUseCase: StopExerciseUseCase,
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
        // Escuchar muestras de FC del HealthServicesManager para UI
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

        // UNICO escuchador de comandos del movil
        wearMessageClient.commands
            .onEach { command ->
                when (command) {
                    is WearMessageClient.WearCommand.Start -> {
                        logDiag("COMANDO START recibido del movil: sessionId=${command.sessionId}")
                        handleStartCommand(command.sessionId)
                    }
                    is WearMessageClient.WearCommand.Stop -> {
                        logDiag("COMANDO STOP recibido del movil: sessionId=${command.sessionId}")
                        handleStopFromMobile(command.sessionId)
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    /**
     * Inicializa el ViewModel.
     * Llama al use case que inicializa el repositorio de salud.
     */
    fun initialize() {
        logDiag("=== INICIALIZANDO A.C.E WEAR ===")
        startExerciseUseCase()
        checkConnectionStatus()
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
     * Maneja el comando START del movil.
     * Si no tiene permiso, lo pide primero.
     */
    private fun handleStartCommand(sessionId: String) {
        if (_state.value.hasSensorPermission) {
            startSessionInternal(sessionId)
        } else {
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

        // Ordenar al use case que inicie el sensor
        startExerciseUseCase.startSession(sessionId)  // ← NUEVO

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

    /**
     * Maneja el comando STOP del movil.
     * Ordena al use case detener la sesion (sensor + notificar).
     */
    private fun handleStopFromMobile(sessionId: String) {
        logDiag("STOP recibido del movil para sesion: $sessionId")
        stopExerciseUseCase(sessionId)  // ← USE CASE: detiene sensor + notifica
        stopSessionInternal()
    }

    /**
     * Usuario presiona DETENER en el reloj.
     * Ordena al use case detener la sesion (sensor + notificar).
     */
    fun onStopButtonClicked() {
        val sessionId = currentSessionId
        if (sessionId == null) {
            logDiag("ERROR: Boton DETENER presionado pero no hay sesion activa")
            return
        }

        logDiag("Boton DETENER presionado por usuario")
        logDiag("Deteniendo sesion: $sessionId")

        // Detener UI INMEDIATAMENTE
        stopSessionInternal()

        // Ordenar al use case que detenga sensor y notifique al movil
        stopExerciseUseCase(sessionId)  // ← USE CASE: detiene sensor + notifica
    }

    /**
     * Detiene la UI de la sesion (timer, estado).
     * Llamado tanto por STOP del movil como por DETENER del usuario.
     */
    private fun stopSessionInternal() {
        currentSessionId = null
        pendingSessionId = null
        stopTimer()
        _state.value = _state.value.copy(
            isSessionActive = false,
            bpm = null,
            elapsedSeconds = 0L
        )
        logDiag("UI detenida localmente")
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

    override fun onCleared() {
        super.onCleared()
        stopTimer()
        logDiag("ViewModel destruido")
    }

    /**
     * Llamado por MainActivity.onDestroy().
     * Libera todos los recursos del repositorio.
     */
    fun dispose() {
        stopTimer()
        stopExerciseUseCase.dispose()  // ← Limpia todo al cerrar app
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