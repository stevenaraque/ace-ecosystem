package com.ace.mobile.domain.usecase.wear

import com.ace.mobile.data.wear.WearMessageClient
import com.google.android.gms.wearable.DataMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class SendStopCommandUseCase @Inject constructor(
    private val wearMessageClient: WearMessageClient
) {
    sealed class Result {
        data class Success(val message: String) : Result()
        data class Error(val message: String) : Result()
    }

    companion object {
        private const val SESSION_STATUS_PATH = "/ace/session/%s/status"
        private const val STOP_COMMAND = "STOP"
    }

    suspend operator fun invoke(sessionId: String): Result = withContext(Dispatchers.IO) {
        try {
            val path = SESSION_STATUS_PATH.format(sessionId)

            val dataMap = DataMap().apply {
                putString("command", STOP_COMMAND)
                putString("sessionId", sessionId)
                putLong("timestamp", System.currentTimeMillis())
            }

            val sendResult = wearMessageClient.sendMessage(
                path = path,
                data = dataMap.toByteArray()
            )

            sendResult.fold(
                onSuccess = { nodeCount ->
                    Result.Success("Comando STOP enviado a $nodeCount dispositivo(s)")
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