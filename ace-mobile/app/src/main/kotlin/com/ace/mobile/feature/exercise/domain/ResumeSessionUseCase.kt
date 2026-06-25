package com.ace.mobile.feature.exercise.domain

import android.util.Log
import com.ace.mobile.core.data.SessionSampleBuffer
import com.ace.mobile.core.database.dao.SessionDao
import com.ace.shared.enums.SessionStatus
import javax.inject.Inject

private const val TAG = "ResumeSessionUseCase"

class ResumeSessionUseCase @Inject constructor(
    private val sessionDao: SessionDao,
    private val sessionSampleBuffer: SessionSampleBuffer
) {

    suspend operator fun invoke(sessionId: String): Result<Unit> {
        return try {
            Log.i(TAG, "Resuming session=$sessionId")

            // Reanudar el timer del bloque
            sessionSampleBuffer.resumeBlockTimer(sessionId)

            // Actualizar estado en BD
            sessionDao.updateSessionStatus(sessionId, SessionStatus.ACTIVE.name)

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error resuming session", e)
            Result.failure(e)
        }
    }
}