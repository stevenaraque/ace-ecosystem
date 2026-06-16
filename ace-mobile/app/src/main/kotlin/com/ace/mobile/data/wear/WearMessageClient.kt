package com.ace.mobile.data.wear

import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WearMessageClient @Inject constructor(
    private val messageClient: MessageClient
) {

    suspend fun sendMessage(path: String, data: ByteArray): Result<Int> {
        return try {
            // Obtener nodos conectados (el reloj)
            val nodes = Wearable.getNodeClient(messageClient.applicationContext)
                .connectedNodes
                .await()

            if (nodes.isEmpty()) {
                return Result.failure(Exception("No Wear OS device connected"))
            }

            // Enviar a todos los nodos (normalmente solo hay uno)
            nodes.forEach { node ->
                messageClient.sendMessage(node.id, path, data).await()
            }

            Result.success(nodes.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}