// ace-wear/app/src/main/kotlin/com/ace/wear/data/sync/WearDataClient.kt

package com.ace.wear.data.sync

import android.util.Log
import com.ace.shared.constants.DataLayerPaths
import com.ace.wear.data.health.model.HeartRateSample
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.PutDataRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cliente de datos del reloj.
 */
@Singleton
open class WearDataClient @Inject constructor(
    private val dataClient: DataClient
) {
    companion object {
        private const val TAG = "WearDataClient"
        private const val KEY_BPM = "bpm"
        private const val KEY_TIMESTAMP = "timestamp"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * Envia una muestra de FC al movil por DataClient.
     */
    open fun sendHeartRateSample(sample: HeartRateSample) {
        scope.launch {
            try {
                val path = "${DataLayerPaths.HEART_RATE}/${sample.timestamp}"
                val putDataMapRequest = PutDataMapRequest.create(path)
                putDataMapRequest.dataMap.apply {
                    putDouble(KEY_BPM, sample.bpm)
                    putLong(KEY_TIMESTAMP, sample.timestamp)
                }

                val request: PutDataRequest = putDataMapRequest.asPutDataRequest()
                request.setUrgent()

                dataClient.putDataItem(request).await()
                Log.d(TAG, "Muestra enviada: bpm=${sample.bpm}, ts=${sample.timestamp}")
            } catch (e: Exception) {
                Log.e(TAG, "Error enviando muestra al movil", e)
            }
        }
    }

    /**
     * Limpia recursos.
     */
    open fun cleanup() {
        scope.cancel()
    }
}