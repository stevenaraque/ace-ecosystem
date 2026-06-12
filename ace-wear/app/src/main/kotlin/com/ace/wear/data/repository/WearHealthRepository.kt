// ace-wear/app/src/main/kotlin/com/ace/wear/data/repository/WearHealthRepository.kt

package com.ace.wear.data.repository

import android.util.Log
import com.ace.wear.data.health.HealthServicesManager
import com.ace.wear.data.health.model.HeartRateSample
import com.ace.wear.data.sync.WearDataClient
import com.ace.wear.data.sync.WearMessageClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repositorio central del reloj.
 *
 * Orquesta el flujo completo S1:
 * 1. Recibe START del movil → activa HealthServicesManager
 * 2. HealthServicesManager emite muestras de FC
 * 3. WearDataClient envia cada muestra al movil
 * 4. Recibe STOP del movil → detiene HealthServicesManager
 *
 * El reloj no decide, no calcula, no persiste. Solo reacciona y transporta.
 */
@Singleton
class WearHealthRepository @Inject constructor(
    private val wearMessageClient: WearMessageClient,
    private val healthServicesManager: HealthServicesManager,
    private val wearDataClient: WearDataClient
) {
    companion object {
        private const val TAG = "WearHealthRepository"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * Estado actual de la sesion en el reloj.
     */
    private var isSessionActive = false

    /**
     * Inicia la escucha de comandos del movil.
     * Debe llamarse al arrancar la app (WearApplication o MainActivity).
     */
    fun initialize() {
        Log.i(TAG, "Inicializando WearHealthRepository...")

        // Escuchar comandos START/STOP del movil
        wearMessageClient.commands
            .onEach { command ->
                when (command) {
                    is WearMessageClient.WearCommand.Start -> onStartCommand(command.sessionId)
                    is WearMessageClient.WearCommand.Stop -> onStopCommand(command.sessionId)
                }
            }
            .launchIn(scope)

        // Escuchar muestras de FC y enviarlas al movil
        healthServicesManager.heartRateSamples
            .onEach { sample ->
                sendSampleToMobile(sample)
            }
            .launchIn(scope)

        // Iniciar escucha de mensajes del movil
        wearMessageClient.startListening()

        Log.i(TAG, "WearHealthRepository inicializado. Esperando START del movil.")
    }

    /**
     * Procesa comando START del movil.
     */
    private fun onStartCommand(sessionId: String) {
        if (isSessionActive) {
            Log.w(TAG, "Sesion ya activa, ignorando START")
            return
        }

        Log.i(TAG, "START recibido para sesion: $sessionId")
        isSessionActive = true

        scope.launch {
            try {
                healthServicesManager.startHeartRateMonitoring()
                Log.i(TAG, "Monitoreo de FC iniciado")
            } catch (e: Exception) {
                Log.e(TAG, "Error iniciando monitoreo de FC", e)
                isSessionActive = false
            }
        }
    }

    /**
     * Procesa comando STOP del movil.
     */
    private fun onStopCommand(sessionId: String) {
        if (!isSessionActive) {
            Log.w(TAG, "No hay sesion activa, ignorando STOP")
            return
        }

        Log.i(TAG, "STOP recibido para sesion: $sessionId")
        isSessionActive = false

        scope.launch {
            try {
                healthServicesManager.stopHeartRateMonitoring()
                Log.i(TAG, "Monitoreo de FC detenido")

                // Notificar al movil que el reloj detuvo
                wearMessageClient.sendStoppedToMobile(sessionId)
            } catch (e: Exception) {
                Log.e(TAG, "Error deteniendo monitoreo de FC", e)
            }
        }
    }

    /**
     * Envia una muestra de FC al movil.
     */
    private fun sendSampleToMobile(sample: HeartRateSample) {
        if (!isSessionActive) {
            Log.w(TAG, "Muestra recibida sin sesion activa, descartando")
            return
        }

        wearDataClient.sendHeartRateSample(sample)
    }

    /**
     * Limpia recursos al cerrar la app.
     */
    fun dispose() {
        Log.i(TAG, "Disponiendo WearHealthRepository...")

        scope.launch {
            if (isSessionActive) {
                healthServicesManager.stopHeartRateMonitoring()
            }
        }

        wearMessageClient.stopListening()
        healthServicesManager.cleanup()
        wearDataClient.cleanup()
        scope.cancel()

        Log.i(TAG, "WearHealthRepository dispuesto")
    }
}