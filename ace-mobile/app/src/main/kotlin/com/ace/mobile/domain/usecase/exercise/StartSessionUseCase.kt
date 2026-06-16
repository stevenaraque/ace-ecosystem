package com.ace.mobile.domain.usecase.exercise

import com.ace.mobile.data.local.database.dao.SessionDao
import com.ace.mobile.data.local.database.entity.LocalSessionEntity
import com.ace.mobile.domain.model.ExerciseSession
import com.ace.mobile.domain.usecase.wear.SendStopCommandUseCase
import com.ace.shared.enums.SessionStatus
import com.ace.shared.enums.SportType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

class StartSessionUseCase @Inject constructor(
    private val sessionDao: SessionDao,
    private val sendStartCommandUseCase: SendStopCommandUseCase
) {

    suspend operator fun invoke(
        sportType: SportType,
        userId: String
    ): Result<ExerciseSession> = withContext(Dispatchers.IO) {
        try {
            // 1. Verificar si ya existe sesión ACTIVE y abortarla
            val activeSession = sessionDao.getActiveSession()
            activeSession?.let {
                sessionDao.updateSessionStatus(it.sessionId, SessionStatus.ABORTED.name)
            }

            // 2. Generar IDs locales
            val sessionId = UUID.randomUUID().toString()
            val deviceId = UUID.randomUUID().toString()

            // 3. Crear sesión en SQLite
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

            // 4. Enviar comando START al reloj
            val sendResult = sendStartCommandUseCase(sessionId)
            if (sendResult is SendStopCommandUseCase.Result.Error) {
                // Log pero no fallamos — la sesión existe localmente
            }

            // 5. Retornar modelo de dominio
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