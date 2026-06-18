package com.ace.mobile.data.wear

import com.ace.shared.constants.DataLayerPaths
import com.google.android.gms.wearable.MessageClient
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cliente de mensajería para enviar comandos de control al reloj Wear OS.
 *
 * Sistema 1 — Captura de Sensor (Apéndice S1 §2.1)
 * Usa [MessageClient] (fire-and-forget) para enviar START/STOP.
 * Si el reloj no está conectado, el mensaje se pierde; esto es aceptable
 * porque el usuario puede detener la sesión desde la UI del móvil.
 *
 * @param messageClient Cliente de mensajes de Google Play Services Wearable.
 */
@Singleton
class WearMessageClient @Inject constructor(
    private val messageClient: MessageClient
) {

    /**
     * Envía el comando START al reloj para iniciar la captura de FC.
     *
     * @param sessionId UUID de la sesión activa. Se usa para construir el path.
     * @param nodeId Identificador del nodo Wear OS destino. Si es null, se envía a todos.
     * @throws Exception si falla el envío (ej. reloj desconectado).
     */
    suspend fun sendStart(
        sessionId: String,
        nodeId: String? = null
    ) {
        val path = DataLayerPaths.sessionStatusPath(sessionId)
        val payload = DataLayerPaths.START_COMMAND.toByteArray()

        sendMessage(path, payload, nodeId)
    }

    /**
     * Envía el comando STOP al reloj para detener la captura de FC.
     *
     * @param sessionId UUID de la sesión activa.
     * @param nodeId Identificador del nodo Wear OS destino.
     * @throws Exception si falla el envío.
     */
    suspend fun sendStop(
        sessionId: String,
        nodeId: String? = null
    ) {
        val path = DataLayerPaths.sessionStatusPath(sessionId)
        val payload = DataLayerPaths.STOP_COMMAND.toByteArray()

        sendMessage(path, payload, nodeId)
    }

    private suspend fun sendMessage(
        path: String,
        payload: ByteArray,
        nodeId: String?
    ) {
        if (nodeId != null) {
            messageClient.sendMessage(nodeId, path, payload).await()
        } else {
            // Broadcast a todos los nodos conectados
            val nodes = messageClient.connectedNodes.await()
            nodes.forEach { node ->
                messageClient.sendMessage(node.id, path, payload).await()
            }
        }
    }
}