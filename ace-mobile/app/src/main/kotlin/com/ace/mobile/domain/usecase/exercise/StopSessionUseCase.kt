// app/src/main/kotlin/com/ace/mobile/domain/usecase/exercise/StopSessionUseCase.kt
package com.ace.mobile.domain.usecase.exercise

import android.util.Log
import com.ace.mobile.data.local.database.dao.SessionDao
import com.ace.mobile.data.repository.BlockRepository
import com.ace.mobile.data.repository.SessionSampleBuffer
import com.ace.mobile.domain.model.ExerciseSession
import com.ace.mobile.domain.usecase.wear.SendStopCommandUseCase
import com.ace.shared.enums.SessionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

private const val TAG = "StopSessionUseCase"

class StopSessionUseCase @Inject constructor(
    private val sessionDao: SessionDao,
    private val sendStopCommandUseCase: SendStopCommandUseCase,
    private val sessionSampleBuffer: SessionSampleBuffer,
    private val blockRepository: BlockRepository
) {

    suspend operator fun invoke(sessionId: String): Result<ExerciseSession> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "=== STOP SESSION START: $sessionId ===")

            val sessionEntity = sessionDao.getSessionById(sessionId)
                ?: return@withContext Result.failure(Exception("Session not found"))

            // 1. Enviar STOP al reloj
            Log.d(TAG, "Sending STOP to watch...")
            sendStopCommandUseCase(sessionId)

            // 2. Forzar cierre del bloque final
            // El buffer cancela su timer interno y cierra el bloque de forma segura
            Log.d(TAG, "Forcing final block close...")
            val finalBlockSummary = sessionSampleBuffer.forceCloseBlock(sessionId)

            if (finalBlockSummary != null) {
                Log.i(TAG, "Final block: #${finalBlockSummary.blockCount}, XP=${finalBlockSummary.xpGained}")
            } else {
                Log.w(TAG, "No final block (no samples or rejected)")
            }

            // 3. Limpiar buffer
            Log.d(TAG, "Clearing buffer...")
            sessionSampleBuffer.clear(sessionId)

            // 4. Leer totales de BD
            val blocks = blockRepository.getBlocksBySession(sessionId)
            val totalBlocks = blocks.size
            val totalXp = blocks.sumOf { (it.xpCalculated ?: 0).toDouble() }

            Log.d(TAG, "Totals from DB: blocks=$totalBlocks, xp=$totalXp")

            // 5. Finalizar sesión
            val timestampEnd = System.currentTimeMillis()

            sessionDao.finalizeSession(
                sessionId = sessionId,
                status = SessionStatus.COMPLETED.name,
                timestampEnd = timestampEnd,
                totalBlocks = totalBlocks,
                totalXp = totalXp.toLong()
            )

            Log.i(TAG, "Session finalized: $totalBlocks blocks, $totalXp XP")

            // 6. Retornar
            Result.success(
                ExerciseSession(
                    sessionId = sessionEntity.sessionId,
                    userId = sessionEntity.userId,
                    deviceId = sessionEntity.deviceId,
                    status = SessionStatus.COMPLETED,
                    sportType = com.ace.shared.enums.SportType.valueOf(sessionEntity.sportType),
                    timestampStart = sessionEntity.timestampStart,
                    timestampEnd = timestampEnd,
                    totalBlocks = totalBlocks,
                    totalXp = totalXp.toLong()
                )
            ).also {
                Log.d(TAG, "=== STOP SESSION END ===")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error stopping session", e)
            Result.failure(e)
        }
    }
}