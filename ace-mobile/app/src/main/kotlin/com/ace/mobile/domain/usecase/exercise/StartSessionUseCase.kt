package com.ace.mobile.domain.usecase.exercise

import android.util.Log
import com.ace.mobile.data.local.database.dao.SessionDao
import com.ace.mobile.data.local.database.entity.LocalSessionEntity
import com.ace.mobile.domain.model.ExerciseSession
import com.ace.mobile.domain.usecase.wear.SendStartCommandUseCase
import com.ace.shared.enums.SessionStatus
import com.ace.shared.enums.SportType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

class StartSessionUseCase @Inject constructor(
    private val sessionDao: SessionDao,
    private val sendStartCommandUseCase: SendStartCommandUseCase
) {

    suspend operator fun invoke(
        sportType: SportType,
        userId: String
    ): Result<ExerciseSession> = withContext(Dispatchers.IO) {
        try {
            val activeSession = sessionDao.getActiveSession()
            activeSession?.let {
                sessionDao.updateSessionStatus(it.sessionId, SessionStatus.ABORTED.name)
            }

            val sessionId = UUID.randomUUID().toString()
            val deviceId = UUID.randomUUID().toString()

            val timestampStart = System.currentTimeMillis()
            val sessionEntity = LocalSessionEntity(
                sessionId = sessionId,
                userId = userId,
                deviceId = deviceId,
                status = SessionStatus.ACTIVE.name,
                sportType = sportType.name,
                timestampStart = timestampStart,
                timestampEnd = null,
                totalBlocks = 0,
                totalXp = 0
            )
            sessionDao.insertSession(sessionEntity)

            val sendResult = sendStartCommandUseCase(sessionId)
            if (sendResult is SendStartCommandUseCase.Result.Error) {
                Log.w("StartSessionUseCase", "Reloj no al alcance: ${sendResult.message}")
            }

            Result.success(
                ExerciseSession(
                    sessionId = sessionId,
                    userId = userId,
                    deviceId = deviceId,
                    status = SessionStatus.ACTIVE,
                    sportType = sportType,
                    timestampStart = timestampStart,
                    timestampEnd = null,
                    totalBlocks = 0,
                    totalXp = 0
                )
            )

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}