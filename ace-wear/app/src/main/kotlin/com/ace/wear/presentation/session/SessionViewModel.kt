// ace-wear/app/src/main/kotlin/com/ace/wear/presentation/session/SessionViewModel.kt

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
 * ViewModel de la sesion de ejercicio en el reloj.
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
    }

    private val _state = MutableStateFlow(WearSessionState())
    val state: StateFlow<WearSessionState> = _state.asStateFlow()

    private var timerJob: Job? = null
    private var currentSessionId: String? = null

    init {
        healthServicesManager.heartRateSamples
            .onEach { sample ->
                _state.value = _state.value.copy(bpm = sample.bpm)
            }
            .launchIn(viewModelScope)

        healthServicesManager.availability
            .onEach { }
            .launchIn(viewModelScope)

        wearMessageClient.commands
            .onEach { command ->
                when (command) {
                    is WearMessageClient.WearCommand.Start -> onSessionStarted(command.sessionId)
                    is WearMessageClient.WearCommand.Stop -> onSessionStopped(command.sessionId)
                }
            }
            .launchIn(viewModelScope)
    }

    fun initialize() {
        wearHealthRepository.initialize()
        checkConnectionStatus()
    }

    private fun checkConnectionStatus() {
        viewModelScope.launch {
            try {
                val nodes = nodeClient.connectedNodes.await()
                val hasConnectedNode = nodes.isNotEmpty()

                _state.value = _state.value.copy(isConnected = hasConnectedNode)

                Log.i(TAG, "Nodos conectados: ${nodes.size}")
                nodes.forEach { node ->
                    Log.i(TAG, "  - ${node.displayName} (${node.id})")
                }

                if (!hasConnectedNode) {
                    Log.w(TAG, "No hay movil conectado por DataLayer")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error verificando nodos", e)
                _state.value = _state.value.copy(isConnected = false)
            }
        }
    }

    private fun onSessionStarted(sessionId: String) {
        currentSessionId = sessionId
        _state.value = _state.value.copy(
            isSessionActive = true,
            elapsedSeconds = 0L,
            bpm = null
        )
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

    private fun onSessionStopped(sessionId: String) {
        currentSessionId = null
        stopTimer()
        _state.value = _state.value.copy(
            isSessionActive = false,
            bpm = null
        )
    }

    fun onStopButtonClicked() {
        currentSessionId?.let { sessionId ->
            wearMessageClient.sendStoppedToMobile(sessionId)
        }
        currentSessionId = null
        stopTimer()
        _state.value = _state.value.copy(
            isSessionActive = false,
            bpm = null
        )
    }

    override fun onCleared() {
        super.onCleared()
        stopTimer()
    }

    fun dispose() {
        stopTimer()
        stopExerciseUseCase()
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }
}