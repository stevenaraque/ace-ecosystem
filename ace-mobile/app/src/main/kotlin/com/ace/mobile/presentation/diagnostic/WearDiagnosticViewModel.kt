package com.ace.mobile.presentation.diagnostic

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ace.mobile.data.wear.WearMessageClient
import com.ace.mobile.domain.usecase.wear.SendStartCommandUseCase
import com.ace.mobile.domain.usecase.wear.SendStopCommandUseCase
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.NodeClient
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import android.content.Intent
import androidx.core.content.ContextCompat
import com.ace.mobile.service.ExerciseSyncService

/**
 * ViewModel para el diagnostico de conexion Wear OS.
 *
 * Ahora conectado a SendStartCommandUseCase y SendStopCommandUseCase
 * para enviar comandos reales al reloj.
 */
@HiltViewModel
class WearDiagnosticViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val wearMessageClient: WearMessageClient,
    private val sendStartCommandUseCase: SendStartCommandUseCase,
    private val sendStopCommandUseCase: SendStopCommandUseCase
) : ViewModel() {

    companion object {
        private const val TAG = "WearDiagnosticVM"
        private const val MAX_LOGS = 50
        private const val TEST_PATH = "/ace/diagnostic/test"
    }

    private val nodeClient = Wearable.getNodeClient(context)
    private val dataClient = Wearable.getDataClient(context)

    private val _uiState = MutableStateFlow(WearDiagnosticUiState())
    val uiState: StateFlow<WearDiagnosticUiState> = _uiState.asStateFlow()

    // Session ID para las pruebas de START/STOP
    private var testSessionId: String? = null

    init {
        log("=== DIAGNOSTICO A.C.E WEAR INICIADO ===")
        log("applicationId: ${context.packageName}")
    }

    /**
     * Verifica el estado completo de conexion.
     */
    fun refreshStatus() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            log("Verificando nodos conectados...")

            try {
                val nodes = nodeClient.connectedNodes.await()
                val hasNodes = nodes.isNotEmpty()

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isConnected = hasNodes,
                    nodeCount = nodes.size,
                    nodeNames = nodes.map { "${it.displayName} (${it.id})" },
                    lastError = if (!hasNodes) "No hay relojes emparejados" else null
                )

                log("Nodos encontrados: ${nodes.size}")
                nodes.forEach { node ->
                    log("  -> ${node.displayName} | id=${node.id} | isNearby=${node.isNearby}")
                }

                if (!hasNodes) {
                    log("ERROR: No hay reloj conectado. Verifica:")
                    log("  1. App Wear OS empareja emuladores")
                    log("  2. Misma cuenta Google en ambos")
                    log("  3. applicationId coincide")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error verificando nodos", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isConnected = false,
                    nodeCount = 0,
                    lastError = e.message
                )
                log("ERROR: ${e.message}")
            }
        }
    }

    /**
     * Envia un mensaje de prueba al reloj via MessageClient.
     */
    fun sendTestMessage() {
        viewModelScope.launch {
            log("Enviando mensaje de prueba...")
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                val result = wearMessageClient.sendMessage(
                    path = TEST_PATH,
                    data = "TEST_MESSAGE".toByteArray()
                )

                result.fold(
                    onSuccess = { nodeCount ->
                        log("Mensaje enviado OK a $nodeCount nodo(s)")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            lastTestResult = "Enviado a $nodeCount nodo(s)"
                        )
                    },
                    onFailure = { e ->
                        log("FALLO: ${e.message}")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            lastError = e.message,
                            lastTestResult = "FALLO: ${e.message}"
                        )
                    }
                )

            } catch (e: Exception) {
                log("ERROR inesperado: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    lastError = e.message
                )
            }
        }
    }

    /**
     * Envia un DataItem de prueba al reloj via DataClient.
     */
    fun sendTestDataItem() {
        viewModelScope.launch {
            log("Enviando DataItem de prueba...")
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                val putDataMapRequest = PutDataMapRequest.create(TEST_PATH)
                putDataMapRequest.dataMap.apply {
                    putString("test_message", "Hello from mobile!")
                    putLong("timestamp", System.currentTimeMillis())
                }

                val request = putDataMapRequest.asPutDataRequest()
                request.setUrgent()

                dataClient.putDataItem(request).await()
                log("DataItem enviado OK al path: $TEST_PATH")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    lastTestResult = "DataItem enviado OK"
                )

            } catch (e: Exception) {
                log("ERROR DataItem: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    lastError = e.message,
                    lastTestResult = "FALLO DataItem: ${e.message}"
                )
            }
        }
    }

    /**
     * Envia comando START real al reloj Y inicia ExerciseSyncService en el mobile.
     */
    fun sendStartCommand() {
        viewModelScope.launch {
            // Generar un sessionId de prueba
            val sessionId = UUID.randomUUID().toString()
            testSessionId = sessionId

            log("Enviando START command real: sessionId=$sessionId")
            _uiState.value = _uiState.value.copy(isLoading = true)

            // 1. Iniciar ExerciseSyncService en el mobile PRIMERO
            // para que esté corriendo cuando el reloj envíe STOPPED
            log("Iniciando ExerciseSyncService en mobile...")
            val serviceIntent = Intent(context, ExerciseSyncService::class.java).apply {
                action = ExerciseSyncService.ACTION_START_SESSION
                putExtra(ExerciseSyncService.EXTRA_SESSION_ID, sessionId)
                putExtra(ExerciseSyncService.EXTRA_SPORT_TYPE, "RUNNING")
                putExtra(ExerciseSyncService.EXTRA_USER_ID, "diagnostic-user")
            }
            ContextCompat.startForegroundService(context, serviceIntent)
            log("ExerciseSyncService iniciado")

            // 2. Enviar START al reloj
            val result = sendStartCommandUseCase(sessionId)

            when (result) {
                is SendStartCommandUseCase.Result.Success -> {
                    log("START enviado OK: ${result.message}")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        lastTestResult = "START OK: ${result.message}",
                        activeSessionId = sessionId
                    )
                }
                is SendStartCommandUseCase.Result.Error -> {
                    log("START FALLO: ${result.message}")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        lastError = result.message,
                        lastTestResult = "START FALLO: ${result.message}"
                    )
                }
            }
        }
    }

    /**
     * Envia comando STOP real al reloj via SendStopCommandUseCase.
     */

    fun sendStopCommand() {
        viewModelScope.launch {
            val sessionId = testSessionId
            if (sessionId == null) {
                log("ERROR: No hay sesion activa para detener")
                _uiState.value = _uiState.value.copy(
                    lastTestResult = "STOP: No hay sesion activa"
                )
                return@launch
            }

            log("Enviando STOP command real: sessionId=$sessionId")
            _uiState.value = _uiState.value.copy(isLoading = true)

            // 1. Enviar STOP al reloj
            val result = sendStopCommandUseCase(sessionId)

            when (result) {
                is SendStopCommandUseCase.Result.Success -> {
                    log("STOP enviado OK: ${result.message}")
                }
                is SendStopCommandUseCase.Result.Error -> {
                    log("STOP FALLO: ${result.message}")
                }
            }

            // 2. Detener ExerciseSyncService en el mobile
            log("Deteniendo ExerciseSyncService en mobile...")
            val serviceIntent = Intent(context, ExerciseSyncService::class.java).apply {
                action = ExerciseSyncService.ACTION_STOP_SESSION
            }
            context.startService(serviceIntent)
            log("ExerciseSyncService detenido")

            testSessionId = null
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                lastTestResult = "STOP enviado y servicio detenido",
                activeSessionId = null
            )
        }
    }

    private fun log(message: String) {
        Log.d(TAG, message)
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val logLine = "[$timestamp] $message"

        val currentLogs = _uiState.value.logs.toMutableList()
        currentLogs.add(logLine)
        if (currentLogs.size > MAX_LOGS) {
            currentLogs.removeAt(0)
        }
        _uiState.value = _uiState.value.copy(logs = currentLogs.toList())
    }
}

/**
 * Estado UI del diagnostico.
 */
data class WearDiagnosticUiState(
    val isLoading: Boolean = false,
    val isConnected: Boolean = false,
    val nodeCount: Int = 0,
    val nodeNames: List<String> = emptyList(),
    val lastError: String? = null,
    val lastTestResult: String? = null,
    val logs: List<String> = emptyList(),
    val activeSessionId: String? = null
)