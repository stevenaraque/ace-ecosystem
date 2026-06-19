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
 * Responsabilidades S1:
 * 1. Escuchar muestras de FC del HealthServicesManager y enviarlas al movil
 * 2. Iniciar/detener el sensor cuando el ViewModel lo ordene
 * 3. Notificar al movil cuando el usuario detiene la sesion
 *
 * El reloj no escucha comandos del movil. Eso es responsabilidad del SessionViewModel.
 * El reloj no decide, no calcula, no persiste. Solo reacciona y transporta.
 */
@Singleton
class WearHealthRepository @Inject constructor(
    private val healthServicesManager: HealthServicesManager,
    private val wearDataClient: WearDataClient,
    private val wearMessageClient: WearMessageClient
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
     * Inicializa el repositorio.
     * Escucha muestras de FC del sensor para enviarlas al movil.
     * NO escucha comandos del movil (eso lo hace SessionViewModel).
     */
    fun initialize() {
        Log.i(TAG, "Inicializando WearHealthRepository...")

        // Escuchar muestras de FC y enviarlas al movil
        healthServicesManager.heartRateSamples
            .onEach { sample ->
                sendSampleToMobile(sample)
            }
            .launchIn(scope)

        Log.i(TAG, "WearHealthRepository inicializado. Esperando orden de inicio desde ViewModel.")
    }

    /**
     * Inicia la sesion de ejercicio en el reloj.
     * Llama al sensor de FC y marca la sesion como activa.
     *
     * @param sessionId ID de la sesion (para validacion interna)
     */
    fun startSession(sessionId: String) {
        if (isSessionActive) {
            Log.w(TAG, "Sesion ya activa, ignorando startSession()")
            return
        }

        Log.i(TAG, "START ordenado por ViewModel para sesion: $sessionId")
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
     * Detiene la sesion activa en el reloj.
     * Detiene el sensor y notifica al movil que el reloj detuvo.
     *
     * @param sessionId ID de la sesion a detener
     */
    fun stopSession(sessionId: String) {
        if (!isSessionActive) {
            Log.w(TAG, "No hay sesion activa para detener")
            return
        }

        Log.i(TAG, "STOP ordenado por ViewModel para sesion: $sessionId")
        isSessionActive = false

        scope.launch {
            try {
                healthServicesManager.stopHeartRateMonitoring()
                Log.i(TAG, "Monitoreo de FC detenido")

                // Notificar al movil que el reloj detuvo
                wearMessageClient.sendStoppedToMobile(sessionId)
                Log.i(TAG, "STOPPED enviado al movil desde stopSession()")
            } catch (e: Exception) {
                Log.e(TAG, "Error deteniendo sesion", e)
            }
        }
    }

    /**
     * Envia una muestra de FC al movil.
     * Solo envia si hay una sesion activa.
     */
    private fun sendSampleToMobile(sample: HeartRateSample) {
        if (!isSessionActive) {
            // Normal al inicio: el sensor puede emitir una muestra antes de que
            // startSession() marque isSessionActive = true
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

        healthServicesManager.cleanup()
        wearDataClient.cleanup()
        scope.cancel()

        Log.i(TAG, "WearHealthRepository dispuesto")
    }
}