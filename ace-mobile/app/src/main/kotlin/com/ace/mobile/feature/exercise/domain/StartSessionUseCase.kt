// app/src/main/kotlin/com/ace/mobile/domain/usecase/exercise/StartSessionUseCase.kt
package com.ace.mobile.feature.exercise.domain

import android.util.Log
import com.ace.mobile.core.database.dao.SessionDao
import com.ace.mobile.core.database.entity.LocalSessionEntity
import com.ace.mobile.core.data.SessionSampleBuffer
import com.ace.mobile.core.model.ExerciseSession
import com.ace.mobile.feature.wear.domain.SendStartCommandUseCase
import com.ace.mobile.feature.xp.domain.CacheXpFormulasUseCase
import com.ace.shared.enums.SessionStatus
import com.ace.shared.enums.SportType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

private const val TAG = "StartSessionUseCase"

class StartSessionUseCase @Inject constructor(
    private val sessionDao: SessionDao,
    private val sendStartCommandUseCase: SendStartCommandUseCase,
    private val sessionSampleBuffer: SessionSampleBuffer,
    private val cacheXpFormulasUseCase: CacheXpFormulasUseCase
) {

    suspend operator fun invoke(
        sportType: SportType,
        userId: String
    ): Result<ExerciseSession> = withContext(Dispatchers.IO) {
        try {
            // Cachear fórmulas de XP antes de iniciar sesión
            try {
                Log.d(TAG, "Caching XP formulas before session start...")
                val cacheResult = cacheXpFormulasUseCase()
                cacheResult.onSuccess { count ->
                    Log.i(TAG, "XP formulas cached: $count formulas")
                }.onFailure { error ->
                    Log.w(TAG, "Failed to cache XP formulas: ${error.message}. Will retry during block build.")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Exception caching XP formulas: ${e.message}")
            }

            val activeSession = sessionDao.getActiveSession()
            activeSession?.let {
                sessionDao.updateSessionStatus(it.sessionId, SessionStatus.ABORTED.name)
                sessionSampleBuffer.clear(it.sessionId)
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

            // FIX: Pasar datos completos de la sesión al buffer para que el timer de bloques funcione
            sessionSampleBuffer.setActiveSessionId(sessionId)
            Log.i(TAG, "Session $sessionId started, buffer activated")

            val sendResult = sendStartCommandUseCase(sessionId)
            if (sendResult is SendStartCommandUseCase.Result.Error) {
                Log.w(TAG, "Reloj no al alcance: ${sendResult.message}")
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
            Log.e(TAG, "Error starting session", e)
            Result.failure(e)
        }
    }
}