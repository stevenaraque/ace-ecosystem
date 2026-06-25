// app/src/main/kotlin/com/ace/mobile/service/WearDataListenerService.kt
package com.ace.mobile.feature.wear.service

import android.content.Intent
import android.util.Log
import com.ace.mobile.core.data.SessionSampleBuffer
import com.ace.mobile.core.model.HeartRateSample
import com.ace.mobile.feature.exercise.service.ExerciseSyncService
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.WearableListenerService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import javax.inject.Inject

@AndroidEntryPoint
class WearDataListenerService : WearableListenerService() {

    companion object {
        private const val TAG = "WearDataListenerService"
        private const val PATH_HEART_RATE = "/ace/health/heart_rate"
        private const val PATH_SESSION_STATUS = "/ace/session/"
        private const val KEY_BPM = "bpm"
        private const val KEY_TIMESTAMP = "timestamp"
        private const val KEY_COMMAND = "command"
        private const val KEY_SESSION_ID = "sessionId"
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Inject
    lateinit var sessionSampleBuffer: SessionSampleBuffer

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "WearDataListenerService creado")
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        var processedCount = 0
        var errorCount = 0

        try {
            dataEvents.forEach { event ->
                if (event.type == DataEvent.TYPE_CHANGED) {
                    val dataItem = event.dataItem
                    val path = dataItem.uri.path ?: return@forEach

                    when {
                        path.startsWith(PATH_HEART_RATE) -> {
                            if (handleHeartRateData(dataItem)) {
                                processedCount++
                            } else {
                                errorCount++
                            }
                        }
                        else -> {
                            Log.w(TAG, "Path desconocido: $path")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing data events", e)
        } finally {
            // FIX BUG 4: Asegurar que dataEvents siempre se libere
            dataEvents.release()
            Log.d(TAG, "onDataChanged: processed=$processedCount, errors=$errorCount, totalEvents=${dataEvents.count}")
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        val path = messageEvent.path
        val data = messageEvent.data

        when {
            path.startsWith(PATH_SESSION_STATUS) -> {
                handleSessionStatusMessage(path, data)
            }
            else -> {
                Log.w(TAG, "Mensaje con path desconocido: $path")
            }
        }
    }

    override fun onPeerConnected(node: Node) {
        Log.i(TAG, "Wear OS conectado: ${node.displayName} (${node.id})")
    }

    override fun onPeerDisconnected(node: Node) {
        Log.w(TAG, "Wear OS desconectado: ${node.displayName} (${node.id})")
    }

    /**
     * FIX BUG 4: Retorna true si el sample fue procesado correctamente
     */
    private fun handleHeartRateData(dataItem: com.google.android.gms.wearable.DataItem): Boolean {
        return try {
            val dataMap = DataMapItem.fromDataItem(dataItem).dataMap
            val bpm = dataMap.getDouble(KEY_BPM, 0.0)
            val timestamp = dataMap.getLong(KEY_TIMESTAMP, 0L)

            if (bpm <= 0 || timestamp <= 0) {
                Log.w(TAG, "FC invalida: bpm=$bpm, timestamp=$timestamp")
                return false
            }

            val sessionId = sessionSampleBuffer.getActiveSessionId()
            if (sessionId != null) {
                sessionSampleBuffer.addSample(
                    sessionId,
                    HeartRateSample(bpm = bpm, timestamp = timestamp)
                )
                true
            } else {
                Log.w(TAG, "FC recibida pero no hay sesión activa: bpm=$bpm")
                false
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error parseando FC", e)
            false
        }
    }

    private fun handleSessionStatusMessage(path: String, data: ByteArray) {
        try {
            val dataMap = com.google.android.gms.wearable.DataMap.fromByteArray(data)
            val command = dataMap.getString(KEY_COMMAND, "")
            val sessionId = dataMap.getString(KEY_SESSION_ID, "")

            Log.d(TAG, "Comando recibido: command=$command, sessionId=$sessionId")

            when (command) {
                "STOPPED" -> {
                    Log.i(TAG, "STOPPED recibido de reloj: sessionId=$sessionId")
                    val intent = Intent(this, ExerciseSyncService::class.java).apply {
                        action = ExerciseSyncService.ACTION_STOP_SESSION
                        putExtra(ExerciseSyncService.EXTRA_SESSION_ID, sessionId)
                    }
                    startService(intent)
                }
                else -> {
                    Log.w(TAG, "Comando desconocido: $command")
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error parseando mensaje de sesion", e)
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
        Log.i(TAG, "WearDataListenerService destruido")
    }
}