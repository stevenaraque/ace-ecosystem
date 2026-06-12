package com.ace.mobile.domain.usecase.wear

import com.ace.mobile.data.wear.WearMessageClient
import com.ace.shared.constants.DataLayerPaths
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

    suspend operator fun invoke(sessionId: String): Result = withContext(Dispatchers.IO) {
        try {
            val path = DataLayerPaths.SESSION_STATUS.format(sessionId)

            val dataMap = DataMap().apply {
                putString("command", DataLayerPaths.START_COMMAND)
                putString("sessionId", sessionId)
                putLong("timestamp", System.currentTimeMillis())
            }

            wearMessageClient.sendMessage(
                path = path,
                data = dataMap.toByteArray()
            )

            Result.Success("Comando START enviado al reloj")

        } catch (e: Exception) {
            Result.Error("Reloj no al alcance. ${e.message}")
        }
    }
}