package com.ace.mobile.service

import android.content.Intent
import android.util.Log
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
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Servicio que escucha datos y mensajes del reloj Wear OS.
 *
 * Google Play Services levanta este servicio automaticamente cuando:
 * - Llegan datos por DataClient (FC del reloj)
 * - Llegan mensajes por MessageClient (comando STOPPED del reloj)
 * - Cambia el estado de conexion del nodo Wear OS
 *
 * Este servicio es el PUENTE entre el Data Layer y el ExerciseSyncService.
 */
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

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "WearDataListenerService creado")
    }

    /**
     * Recibe datos del reloj (FC via DataClient).
     * Se activa cuando el reloj hace putDataItem().
     */
    override fun onDataChanged(dataEvents: DataEventBuffer) {
        Log.d(TAG, "onDataChanged: ${dataEvents.count} eventos")

        dataEvents.forEach { event ->
            if (event.type == DataEvent.TYPE_CHANGED) {
                val dataItem = event.dataItem
                val path = dataItem.uri.path ?: return@forEach

                Log.d(TAG, "DataItem recibido: path=$path")

                when {
                    path.startsWith(PATH_HEART_RATE) -> {
                        handleHeartRateData(dataItem)
                    }
                    else -> {
                        Log.w(TAG, "Path desconocido: $path")
                    }
                }
            }
        }
        dataEvents.release()
    }

    /**
     * Recibe mensajes del reloj (comandos via MessageClient).
     * Se activa cuando el reloj hace sendMessage().
     */
    override fun onMessageReceived(messageEvent: MessageEvent) {
        val path = messageEvent.path
        val data = messageEvent.data

        Log.d(TAG, "onMessageReceived: path=$path, dataSize=${data.size}")

        when {
            path.startsWith(PATH_SESSION_STATUS) -> {
                handleSessionStatusMessage(path, data)
            }
            else -> {
                Log.w(TAG, "Mensaje con path desconocido: $path")
            }
        }
    }

    /**
     * Se llama cuando un nodo Wear OS se conecta.
     */
    override fun onPeerConnected(node: Node) {
        Log.i(TAG, "Wear OS conectado: ${node.displayName} (${node.id})")
    }

    /**
     * Se llama cuando un nodo Wear OS se desconecta.
     */
    override fun onPeerDisconnected(node: Node) {
        Log.w(TAG, "Wear OS desconectado: ${node.displayName} (${node.id})")
    }

    // ─── Handlers privados ───

    private fun handleHeartRateData(dataItem: com.google.android.gms.wearable.DataItem) {
        try {
            val dataMap = DataMapItem.fromDataItem(dataItem).dataMap
            val bpm = dataMap.getDouble(KEY_BPM, 0.0)
            val timestamp = dataMap.getLong(KEY_TIMESTAMP, 0L)

            if (bpm > 0 && timestamp > 0) {
                Log.d(TAG, "FC recibida: bpm=$bpm, timestamp=$timestamp")

                // TODO: Enviar al ExerciseSyncService via broadcast o inyectar repositorio
                // Por ahora solo logueamos para verificar que llega
            } else {
                Log.w(TAG, "FC invalida: bpm=$bpm, timestamp=$timestamp")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error parseando FC", e)
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

                    // Enviar broadcast al ExerciseSyncService para que detenga la sesion
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