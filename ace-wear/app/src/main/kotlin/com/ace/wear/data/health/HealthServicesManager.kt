// ace-wear/app/src/main/kotlin/com/ace/wear/data/health/HealthServicesManager.kt
package com.ace.wear.data.health

import android.util.Log
import androidx.health.services.client.HealthServicesClient
import androidx.health.services.client.MeasureCallback
import androidx.health.services.client.data.Availability
import androidx.health.services.client.data.DataPointContainer
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.DeltaDataType
import androidx.health.services.client.data.SampleDataPoint
import androidx.health.services.client.unregisterMeasureCallback
import com.ace.wear.data.health.model.HeartRateSample
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wrapper de MeasureClient para Health Services API.
 * Incluye modo simulacion para testing sin sensor real.
 */
@Singleton
open class HealthServicesManager @Inject constructor(
    private val healthServicesClient: HealthServicesClient
) {
    companion object {
        private const val TAG = "HealthServicesManager"
        private const val SIMULATION_INTERVAL_MS = 1000L
        private const val SIMULATION_BPM_BASE = 120.0
        private const val SIMULATION_BPM_VARIANCE = 15.0
    }

    private val measureClient = healthServicesClient.measureClient
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _heartRateSamples = MutableSharedFlow<HeartRateSample>(
        extraBufferCapacity = 10
    )
    open val heartRateSamples: SharedFlow<HeartRateSample> = _heartRateSamples.asSharedFlow()

    private val _availability = MutableSharedFlow<Availability>(
        extraBufferCapacity = 1
    )
    open val availability: SharedFlow<Availability> = _availability.asSharedFlow()

    private var simulationJob: Job? = null
    private var isSimulationMode = false

    private val heartRateCallback = object : MeasureCallback {
        override fun onAvailabilityChanged(
            dataType: DeltaDataType<*, *>,
            availability: Availability
        ) {
            Log.d(TAG, "Disponibilidad de $dataType cambio a: $availability")
            scope.launch {
                _availability.emit(availability)
            }
        }

        override fun onDataReceived(data: DataPointContainer) {
            val samples = data.getData(DataType.HEART_RATE_BPM)
            for (sample in samples) {
                val heartRateSample = parseSampleDataPoint(sample)
                if (heartRateSample != null) {
                    scope.launch {
                        _heartRateSamples.emit(heartRateSample)
                    }
                }
            }
        }

        override fun onRegistered() {
            Log.i(TAG, "Callback registrado exitosamente")
        }

        override fun onRegistrationFailed(throwable: Throwable) {
            Log.e(TAG, "Fallo registro de callback", throwable)
        }
    }

    private fun parseSampleDataPoint(sample: SampleDataPoint<Double>): HeartRateSample? {
        return try {
            val bpm = sample.value
            val timestamp = sample.timeDurationFromBoot?.let { duration ->
                val bootTime = System.currentTimeMillis() - android.os.SystemClock.elapsedRealtime()
                bootTime + duration.toMillis()
            } ?: System.currentTimeMillis()

            HeartRateSample(bpm = bpm, timestamp = timestamp)
        } catch (e: Exception) {
            Log.e(TAG, "Error parseando SampleDataPoint", e)
            null
        }
    }

    /**
     * Inicia la captura de frecuencia cardiaca REAL.
     * Si falla por permisos, retorna false para que el caller active simulacion.
     */
    open fun startHeartRateMonitoring(): Boolean {
        Log.i(TAG, "Iniciando monitoreo de FC REAL...")
        return try {
            measureClient.registerMeasureCallback(
                DataType.HEART_RATE_BPM,
                heartRateCallback
            )
            Log.i(TAG, "Solicitud de registro enviada")
            true
        } catch (e: SecurityException) {
            Log.e(TAG, "Permiso BODY_SENSORS/READ_HEART_RATE no concedido", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error registrando callback", e)
            false
        }
    }

    /**
     * Inicia modo SIMULACION: genera FC fake cada segundo.
     * Usado para testing cuando no hay sensor o permisos.
     */
    open fun startSimulationMode() {
        if (isSimulationMode) {
            Log.w(TAG, "Simulacion ya activa")
            return
        }

        isSimulationMode = true
        Log.i(TAG, "=== MODO SIMULACION ACTIVADO ===")


        simulationJob = scope.launch {
            var sampleCount = 0
            while (isSimulationMode) {
                delay(SIMULATION_INTERVAL_MS)

                val variance = (Math.random() - 0.5) * 2 * SIMULATION_BPM_VARIANCE
                val bpm = SIMULATION_BPM_BASE + variance
                val timestamp = System.currentTimeMillis()

                val sample = HeartRateSample(bpm = bpm, timestamp = timestamp)
                _heartRateSamples.emit(sample)

                sampleCount++
                if (sampleCount % 5 == 0) {
                    Log.d(TAG, "Simulacion: $sampleCount muestras generadas, ultima BPM=${bpm.toInt()}")
                }
            }
        }
    }

    /**
     * Detiene la captura de frecuencia cardiaca (real o simulada).
     */
    open suspend fun stopHeartRateMonitoring() {
        Log.i(TAG, "Deteniendo monitoreo de FC...")

        // Detener simulacion si esta activa
        if (isSimulationMode) {
            isSimulationMode = false
            simulationJob?.cancel()
            simulationJob = null
            Log.i(TAG, "Modo simulacion detenido")
        }

        // Detener sensor real
        try {
            measureClient.unregisterMeasureCallback(
                DataType.HEART_RATE_BPM,
                heartRateCallback
            )
            Log.i(TAG, "Callback desregistrado")
        } catch (e: Exception) {
            Log.e(TAG, "Error desregistrando callback", e)
        }
    }

    /**
     * Limpia recursos.
     */
    open fun cleanup() {
        scope.launch {
            stopHeartRateMonitoring()
        }
        scope.cancel()
    }
}