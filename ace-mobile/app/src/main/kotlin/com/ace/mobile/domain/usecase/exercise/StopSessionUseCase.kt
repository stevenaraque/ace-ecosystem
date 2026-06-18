package com.ace.mobile.domain.usecase.exercise

import com.ace.mobile.data.local.database.dao.SessionDao
import com.ace.mobile.domain.model.ExerciseSession
import com.ace.mobile.domain.usecase.wear.SendStopCommandUseCase
import com.ace.shared.enums.SessionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class StopSessionUseCase @Inject constructor(
    private val sessionDao: SessionDao,
    private val sendStopCommandUseCase: SendStopCommandUseCase
) {

    suspend operator fun invoke(sessionId: String): Result<ExerciseSession> = withContext(Dispatchers.IO) {
        try {
            val sessionEntity = sessionDao.getSessionById(sessionId)
                ?: return@withContext Result.failure(Exception("Session not found"))

            // 1. Enviar STOP al reloj
            sendStopCommandUseCase(sessionId)

            // 2. Actualizar sesión a COMPLETED
            val timestampEnd = System.currentTimeMillis()
            sessionDao.updateSessionStatus(sessionId, SessionStatus.COMPLETED.name)

            // 3. Retornar sesión completada
            Result.success(
                ExerciseSession(
                    sessionId = sessionEntity.sessionId,
                    userId = sessionEntity.userId,
                    deviceId = sessionEntity.deviceId,
                    status = SessionStatus.COMPLETED,
                    sportType = com.ace.shared.enums.SportType.valueOf(sessionEntity.sportType),
                    timestampStart = sessionEntity.timestampStart,
                    timestampEnd = timestampEnd,
                    totalBlocks = sessionEntity.totalBlocks,
                    totalXp = sessionEntity.totalXp
                )
            )

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}