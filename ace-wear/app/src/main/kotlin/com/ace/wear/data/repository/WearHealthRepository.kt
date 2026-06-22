// ace-wear/app/src/main/kotlin/com/ace/wear/data/repository/WearHealthRepository.kt
package com.ace.wear.data.repository

import android.util.Log
import com.ace.wear.data.health.HealthServicesManager
import com.ace.wear.data.health.model.HeartRateSample
import com.ace.wear.data.sync.WearDataClient
import com.ace.wear.data.sync.WearMessageClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "WearHealthRepository"
private const val SENSOR_TIMEOUT_MS = 2000L

@Singleton
class WearHealthRepository @Inject constructor(
    private val healthServicesManager: HealthServicesManager,
    private val wearDataClient: WearDataClient,
    private val wearMessageClient: WearMessageClient
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _isSimulationMode = MutableStateFlow(false)
    val isSimulationMode: StateFlow<Boolean> = _isSimulationMode.asStateFlow()

    private val _samplesSent = MutableStateFlow(0)
    val samplesSent: StateFlow<Int> = _samplesSent.asStateFlow()

    private var isSessionActive = false
    private var samplesSentCount = 0
    private var sensorFailed = false

    // FIX BUG 3: Job mutable para poder cancelar la suscripción anterior
    private var heartRateCollectionJob: Job? = null

    init {
        // FIX BUG 3: Iniciar la primera suscripción y guardar el Job
        heartRateCollectionJob = startHeartRateCollection()
    }

    fun initialize() {
        Log.i(TAG, "WearHealthRepository inicializado")
    }

    fun startSession(sessionId: String): Boolean {
        if (isSessionActive) {
            Log.w(TAG, "Sesion ya activa")
            return true
        }

        Log.i(TAG, "START para sesion: $sessionId")
        isSessionActive = true
        samplesSentCount = 0
        _samplesSent.value = 0
        sensorFailed = false

        val sensorStarted = healthServicesManager.startHeartRateMonitoring()
        Log.i(TAG, "Solicitud de sensor enviada: success=$sensorStarted")

        if (!sensorStarted) {
            Log.w(TAG, "Sensor rechazó inmediatamente. Activando simulación...")
            activateSimulation()
            return true
        }

        scope.launch {
            delay(SENSOR_TIMEOUT_MS)
            if (isSessionActive && !sensorFailed && _samplesSent.value == 0) {
                Log.w(TAG, "Timeout: no hay muestras del sensor. Activando simulación...")
                sensorFailed = true
                activateSimulation()
            }
        }

        return true
    }

    private fun activateSimulation() {
        if (_isSimulationMode.value) return

        Log.i(TAG, "=== ACTIVANDO MODO SIMULACION ===")
        healthServicesManager.startSimulationMode()
        _isSimulationMode.value = true

        // FIX BUG 3: Cancelar la suscripción anterior antes de crear una nueva
        heartRateCollectionJob?.cancel()
        heartRateCollectionJob = startHeartRateCollection()
    }

    fun stopSession(sessionId: String) {
        if (!isSessionActive) {
            Log.w(TAG, "No hay sesion activa")
            return
        }

        Log.i(TAG, "STOP para sesion: $sessionId")
        isSessionActive = false

        scope.launch {
            try {
                healthServicesManager.stopHeartRateMonitoring()
                Log.i(TAG, "Monitoreo detenido (simulacion=${_isSimulationMode.value})")

                wearMessageClient.sendStoppedToMobile(sessionId)
                Log.i(TAG, "STOPPED enviado al movil")

                _isSimulationMode.value = false
                sensorFailed = false
                samplesSentCount = 0
                _samplesSent.value = 0

            } catch (e: Exception) {
                Log.e(TAG, "Error deteniendo", e)
            }
        }
    }

    // FIX BUG 3: Extraer la suscripción a un método reutilizable
    private fun startHeartRateCollection(): Job {
        return healthServicesManager.heartRateSamples
            .onEach { sample ->
                if (isSessionActive) {
                    sendSampleToMobile(sample)
                }
            }
            .launchIn(scope)
    }

    private fun sendSampleToMobile(sample: HeartRateSample) {
        wearDataClient.sendHeartRateSample(sample)
        samplesSentCount++
        _samplesSent.value = samplesSentCount

        if (samplesSentCount % 10 == 0) {
            Log.d(TAG, "Total muestras enviadas: $samplesSentCount")
        }
    }

    fun dispose() {
        // FIX BUG 3: Cancelar el Job de colección antes de limpiar
        heartRateCollectionJob?.cancel()
        heartRateCollectionJob = null

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