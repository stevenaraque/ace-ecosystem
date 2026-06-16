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
        // Path hardcodeado como string literal (consistente con :shared)
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

            wearMessageClient.sendMessage(
                path = path,
                data = dataMap.toByteArray()
            )

            Result.Success("Comando STOP enviado al reloj")

        } catch (e: Exception) {
            Result.Error("Reloj no al alcance. ${e.message}")
        }
    }
}