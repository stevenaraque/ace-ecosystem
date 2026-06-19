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
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wrapper de MeasureClient para Health Services API.
 */
@Singleton
open class HealthServicesManager @Inject constructor(
    private val healthServicesClient: HealthServicesClient
) {
    companion object {
        private const val TAG = "HealthServicesManager"
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
     * Inicia la captura de frecuencia cardiaca.
     */
    open fun startHeartRateMonitoring(): Boolean {
        Log.i(TAG, "Iniciando monitoreo de FC...")
        return try {
            measureClient.registerMeasureCallback(
                DataType.HEART_RATE_BPM,
                heartRateCallback
            )
            Log.i(TAG, "Solicitud de registro enviada")
            true
        } catch (e: SecurityException) {
            Log.e(TAG, "Permiso BODY_SENSORS no concedido", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error registrando callback", e)
            false
        }
    }

    /**
     * Detiene la captura de frecuencia cardiaca.
     */
    open suspend fun stopHeartRateMonitoring() {
        Log.i(TAG, "Deteniendo monitoreo de FC...")
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
        scope.cancel()
    }
}