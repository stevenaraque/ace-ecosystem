// ace-wear/app/src/main/kotlin/com/ace/wear/presentation/session/SessionViewModel.kt

package com.ace.wear.presentation.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ace.wear.data.health.HealthServicesManager
import com.ace.wear.data.repository.WearHealthRepository
import com.ace.wear.data.sync.WearMessageClient
import com.ace.wear.domain.usecase.StopExerciseUseCase
import com.ace.wear.presentation.WearSessionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel de la sesion de ejercicio en el reloj.
 *
 * Responsabilidades:
 * - Exponer WearSessionState a la UI (SessionScreen)
 * - Escuchar muestras de FC del HealthServicesManager
 * - Contar el tiempo transcurrido de la sesion
 * - Manejar el boton DETENER (enviar STOPPED al movil)
 */
@HiltViewModel
class SessionViewModel @Inject constructor(
    private val healthServicesManager: HealthServicesManager,
    private val wearMessageClient: WearMessageClient,
    private val stopExerciseUseCase: StopExerciseUseCase,
    private val wearHealthRepository: WearHealthRepository
) : ViewModel() {

    companion object {
        private const val TAG = "SessionViewModel"
    }

    /** Estado expuesto a la UI */
    private val _state = MutableStateFlow(WearSessionState())
    val state: StateFlow<WearSessionState> = _state.asStateFlow()

    /** Job del timer para contar segundos */
    private var timerJob: Job? = null

    init {
        // Escuchar muestras de FC y actualizar el estado
        healthServicesManager.heartRateSamples
            .onEach { sample ->
                _state.value = _state.value.copy(
                    bpm = sample.bpm
                )
            }
            .launchIn(viewModelScope)

        // Escuchar disponibilidad del sensor (opcional, para debug)
        healthServicesManager.availability
            .onEach { availability ->
                // Se puede usar para mostrar estado del sensor en UI
            }
            .launchIn(viewModelScope)

        // Escuchar comandos del movil para saber si hay sesion activa
        wearMessageClient.commands
            .onEach { command ->
                when (command) {
                    is WearMessageClient.WearCommand.Start -> onSessionStarted(command.sessionId)
                    is WearMessageClient.WearCommand.Stop -> onSessionStopped(command.sessionId)
                }
            }
            .launchIn(viewModelScope)
    }

    /**
     * Inicializa el repositorio al arrancar la app.
     * Llama a esto desde MainActivity.onCreate().
     */
    fun initialize() {
        wearHealthRepository.initialize()
        _state.value = _state.value.copy(isConnected = true)
    }

    /**
     * Procesa inicio de sesion (START recibido del movil).
     */
    private fun onSessionStarted(sessionId: String) {
        _state.value = _state.value.copy(
            isSessionActive = true,
            elapsedSeconds = 0L,
            bpm = null
        )

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
     * Procesa fin de sesion (STOP recibido del movil).
     */
    private fun onSessionStopped(sessionId: String) {
        stopTimer()
        _state.value = _state.value.copy(
            isSessionActive = false,
            bpm = null
        )
    }

    /**
     * Usuario toca boton DETENER en el reloj.
     * Envia STOPPED al movil y limpia estado.
     */
    fun onStopButtonClicked() {
        val currentSessionId = "current" // TODO: Guardar sessionId real cuando llega START
        wearMessageClient.sendStoppedToMobile(currentSessionId)
        stopTimer()
        _state.value = _state.value.copy(
            isSessionActive = false,
            bpm = null
        )
    }

    /**
     * Limpia recursos al destruir el ViewModel.
     */
    override fun onCleared() {
        super.onCleared()
        stopTimer()
        stopExerciseUseCase()
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }
    fun dispose() {
        stopTimer()
        stopExerciseUseCase()
    }
}