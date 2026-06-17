package com.ace.mobile.data.wear

import android.content.Context
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.Wearable
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WearMessageClient @Inject constructor(
    private val messageClient: MessageClient,
    @ApplicationContext private val context: Context
) {

    suspend fun sendMessage(path: String, data: ByteArray): Result<Int> {
        return try {
            val nodes = Wearable.getNodeClient(context)
                .connectedNodes
                .await()

            if (nodes.isEmpty()) {
                return Result.failure(Exception("No Wear OS device connected"))
            }

            nodes.forEach { node ->
                messageClient.sendMessage(node.id, path, data).await()
            }

            Result.success(nodes.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}