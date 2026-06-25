package com.ace.mobile.feature.wear.domain

import com.ace.mobile.feature.wear.data.WearMessageClient
import com.google.android.gms.wearable.DataMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class SendStartCommandUseCase @Inject constructor(
    private val wearMessageClient: WearMessageClient
) {
    sealed class Result {
        data class Success(val message: String) : Result()
        data class Error(val message: String) : Result()
    }

    companion object {
        private const val SESSION_STATUS_PATH = "/ace/session/%s/status"
        private const val START_COMMAND = "START"
    }

    suspend operator fun invoke(sessionId: String): Result = withContext(Dispatchers.IO) {
        try {
            val path = SESSION_STATUS_PATH.format(sessionId)

            val dataMap = DataMap().apply {
                putString("command", START_COMMAND)
                putString("sessionId", sessionId)
                putLong("timestamp", System.currentTimeMillis())
            }

            val sendResult = wearMessageClient.sendMessage(
                path = path,
                data = dataMap.toByteArray()
            )

            sendResult.fold(
                onSuccess = { nodeCount ->
                    Result.Success("Comando START enviado a $nodeCount dispositivo(s)")
                },
                onFailure = { e ->
                    Result.Error("Reloj no al alcance. ${e.message}")
                }
            )

        } catch (e: Exception) {
            Result.Error("Error inesperado. ${e.message}")
        }
    }
}