// ace-wear/app/src/main/kotlin/com/ace/wear/data/sync/WearMessageClient.kt

package com.ace.wear.data.sync

import android.util.Log
import com.ace.shared.constants.DataLayerPaths
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.NodeClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Listener de MessageClient que recibe y envia comandos DataMap al movil.
 */
@Singleton
open class WearMessageClient @Inject constructor(
    private val messageClient: MessageClient,
    private val nodeClient: NodeClient
) : MessageClient.OnMessageReceivedListener {

    companion object {
        private const val TAG = "WearMessageClient"
        private const val START_COMMAND = "START"
        private const val STOP_COMMAND = "STOP"
        private const val KEY_COMMAND = "command"
        private const val KEY_SESSION_ID = "sessionId"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    sealed class WearCommand {
        data class Start(val sessionId: String) : WearCommand()
        data class Stop(val sessionId: String) : WearCommand()
    }

    private val _commands = MutableSharedFlow<WearCommand>(
        extraBufferCapacity = 10
    )
    open val commands: SharedFlow<WearCommand> = _commands.asSharedFlow()

    open fun startListening() {
        messageClient.addListener(this)
        Log.i(TAG, "Listener de MessageClient registrado")
    }

    open fun stopListening() {
        messageClient.removeListener(this)
        Log.i(TAG, "Listener de MessageClient detenido")
    }

    /**
     * Notifica al movil que el reloj detuvo la sesion.
     * Envia DataMap con command=STOPPED.
     */
    open fun sendStoppedToMobile(sessionId: String) {
        scope.launch {
            try {
                val nodes = nodeClient.connectedNodes.await()
                if (nodes.isEmpty()) {
                    Log.w(TAG, "No hay nodos conectados para enviar STOPPED")
                    return@launch
                }

                val dataMap = DataMap().apply {
                    putString(KEY_COMMAND, "STOPPED")
                    putString(KEY_SESSION_ID, sessionId)
                    putLong("timestamp", System.currentTimeMillis())
                }

                val path = DataLayerPaths.sessionStatusPath(sessionId)

                nodes.forEach { node ->
                    messageClient.sendMessage(
                        node.id,
                        path,
                        dataMap.toByteArray()
                    ).await()
                }

                Log.i(TAG, "STOPPED enviado al movil: sessionId=$sessionId, nodos=${nodes.size}")

            } catch (e: Exception) {
                Log.e(TAG, "Error enviando STOPPED", e)
            }
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        val path = messageEvent.path
        val data = messageEvent.data

        Log.d(TAG, "Mensaje recibido - path=$path, dataSize=${data.size}")

        try {
            // 1. Intentar parsear como DataMap
            val dataMap = DataMap.fromByteArray(data)
            val command = dataMap.getString(KEY_COMMAND, "")
            val sessionId = dataMap.getString(KEY_SESSION_ID, "")
                .ifEmpty { extractSessionId(path) }

            Log.d(TAG, "DataMap parseado: command=$command, sessionId=$sessionId")

            when (command) {
                START_COMMAND -> {
                    scope.launch { _commands.emit(WearCommand.Start(sessionId)) }
                    Log.i(TAG, "START recibido: sessionId=$sessionId")
                }
                STOP_COMMAND -> {
                    scope.launch { _commands.emit(WearCommand.Stop(sessionId)) }
                    Log.i(TAG, "STOP recibido: sessionId=$sessionId")
                }
                else -> Log.w(TAG, "Comando desconocido: $command")
            }

        } catch (e: Exception) {
            // 2. Fallback a string plano (compatibilidad)
            Log.w(TAG, "No es DataMap, probando string plano")
            val dataStr = String(data)
            val sessionId = extractSessionId(path)

            when {
                path.contains("/status") && dataStr == START_COMMAND -> {
                    scope.launch { _commands.emit(WearCommand.Start(sessionId)) }
                    Log.i(TAG, "START (string) recibido: sessionId=$sessionId")
                }
                path.contains("/status") && dataStr == STOP_COMMAND -> {
                    scope.launch { _commands.emit(WearCommand.Stop(sessionId)) }
                    Log.i(TAG, "STOP (string) recibido: sessionId=$sessionId")
                }
                else -> Log.w(TAG, "Mensaje no reconocido: $dataStr")
            }
        }
    }

    private fun extractSessionId(path: String): String {
        return path.substringAfter("/ace/session/").substringBefore("/status")
    }
}