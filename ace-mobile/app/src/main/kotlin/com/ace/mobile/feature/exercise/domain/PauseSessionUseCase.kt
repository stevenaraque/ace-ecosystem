package com.ace.mobile.feature.exercise.domain

import android.util.Log
import com.ace.mobile.core.data.SessionSampleBuffer
import com.ace.mobile.core.database.dao.SessionDao
import com.ace.shared.enums.SessionStatus
import javax.inject.Inject

private const val TAG = "PauseSessionUseCase"

class PauseSessionUseCase @Inject constructor(
    private val sessionDao: SessionDao,
    private val sessionSampleBuffer: SessionSampleBuffer
) {

    suspend operator fun invoke(sessionId: String, isAutoPause: Boolean = false): Result<Unit> {
        return try {
            Log.i(TAG, "Pausing session=$sessionId, auto=$isAutoPause")

            // Pausar el timer del bloque (NO cerrarlo)
            sessionSampleBuffer.pauseBlockTimer(sessionId)

            // Actualizar estado en BD
            sessionDao.updateSessionStatus(sessionId, SessionStatus.PAUSED.name)

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing session", e)
            Result.failure(e)
        }
    }
}