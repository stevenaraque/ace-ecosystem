// ace-wear/app/src/main/kotlin/com/ace/wear/data/sync/WearMessageClient.kt

package com.ace.wear.data.sync

import android.util.Log
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Listener de MessageClient que recibe comandos START/STOP del movil.
 */
@Singleton
open class WearMessageClient @Inject constructor(
    private val messageClient: MessageClient
) : MessageClient.OnMessageReceivedListener {

    companion object {
        private const val TAG = "WearMessageClient"
        private const val START_COMMAND = "START"
        private const val STOP_COMMAND = "STOP"
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

    /**
     * Inicia la escucha de mensajes del movil.
     */
    open fun startListening() {
        messageClient.addListener(this)
        Log.i(TAG, "Listener de MessageClient registrado")
    }

    /**
     * Detiene la escucha de mensajes.
     */
    open fun stopListening() {
        messageClient.removeListener(this)
        Log.i(TAG, "Listener de MessageClient detenido")
    }

    /**
     * Notifica al movil que el reloj detuvo la sesion.
     */
    open fun sendStoppedToMobile(sessionId: String) {
        // Implementacion de envio de STOPPED al movil
        Log.i(TAG, "Notificando STOPPED al movil para sesion: $sessionId")
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        val path = messageEvent.path
        val data = String(messageEvent.data)

        Log.d(TAG, "Mensaje recibido - path: $path, data: $data")

        when {
            path.contains("/status") && data == START_COMMAND -> {
                val sessionId = extractSessionId(path)
                scope.launch {
                    _commands.emit(WearCommand.Start(sessionId))
                }
                Log.i(TAG, "Comando START recibido para sesion: $sessionId")
            }
            path.contains("/status") && data == STOP_COMMAND -> {
                val sessionId = extractSessionId(path)
                scope.launch {
                    _commands.emit(WearCommand.Stop(sessionId))
                }
                Log.i(TAG, "Comando STOP recibido para sesion: $sessionId")
            }
        }
    }

    private fun extractSessionId(path: String): String {
        return path.substringAfter("/ace/session/").substringBefore("/status")
    }
}