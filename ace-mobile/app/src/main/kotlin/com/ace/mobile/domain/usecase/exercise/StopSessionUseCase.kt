// app/src/main/kotlin/com/ace/mobile/domain/usecase/exercise/StopSessionUseCase.kt
package com.ace.mobile.domain.usecase.exercise

import android.util.Log
import com.ace.mobile.data.local.database.dao.SessionDao
import com.ace.mobile.data.local.database.entity.LocalBlockEntity
import com.ace.mobile.data.repository.BlockRepository
import com.ace.mobile.data.repository.SessionSampleBuffer
import com.ace.mobile.domain.model.ExerciseSession
import com.ace.mobile.domain.model.HeartRateSample
import com.ace.mobile.domain.usecase.wear.BuildExerciseBlockUseCase
import com.ace.mobile.domain.usecase.wear.SendStopCommandUseCase
import com.ace.shared.enums.BlockStatus
import com.ace.shared.enums.SessionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

private const val TAG = "StopSessionUseCase"

class StopSessionUseCase @Inject constructor(
    private val sessionDao: SessionDao,
    private val sendStopCommandUseCase: SendStopCommandUseCase,
    private val buildExerciseBlockUseCase: BuildExerciseBlockUseCase,
    private val blockRepository: BlockRepository,
    private val sessionSampleBuffer: SessionSampleBuffer
) {

    suspend operator fun invoke(sessionId: String): Result<ExerciseSession> = withContext(Dispatchers.IO) {
        try {
            val sessionEntity = sessionDao.getSessionById(sessionId)
                ?: return@withContext Result.failure(Exception("Session not found"))

            // 1. Enviar STOP al reloj
            sendStopCommandUseCase(sessionId)

            // 2. Recuperar samples acumulados del buffer RAM
            val samples: List<HeartRateSample> = sessionSampleBuffer.getSamples(sessionId)

            // 3. Construir bloque con XP si hay samples
            val sessionForBuild = ExerciseSession(
                sessionId = sessionEntity.sessionId,
                userId = sessionEntity.userId,
                deviceId = sessionEntity.deviceId,
                status = SessionStatus.ACTIVE,
                sportType = com.ace.shared.enums.SportType.valueOf(sessionEntity.sportType),
                timestampStart = sessionEntity.timestampStart,
                timestampEnd = null,
                totalBlocks = sessionEntity.totalBlocks,
                totalXp = sessionEntity.totalXp
            )

            var blockXp = 0L
            var blockInserted = false

            if (samples.isNotEmpty()) {
                val blockResult = buildExerciseBlockUseCase(sessionForBuild, samples)

                if (blockResult != null) {
                    val blockEntity = LocalBlockEntity(
                        blockId = blockResult.blockId,
                        sessionId = blockResult.sessionId,
                        userId = blockResult.userId,
                        timestampStart = blockResult.timestampStart,
                        timestampEnd = blockResult.timestampEnd,
                        durationSeconds = blockResult.durationSeconds,
                        avgBpm = blockResult.avgBpm,
                        maxBpm = blockResult.maxBpm,
                        minBpm = blockResult.minBpm,
                        sampleCount = blockResult.sampleCount,
                        sportType = blockResult.sportType,
                        xpCalculated = blockResult.xpCalculated,
                        status = BlockStatus.PENDING
                    )

                    blockRepository.insertClosedBlock(blockEntity)
                    blockXp = blockResult.xpCalculated.toLong()
                    blockInserted = true
                    Log.i(TAG, "Block ${blockResult.blockId} inserted with XP=$blockXp")
                } else {
                    Log.w(TAG, "BuildExerciseBlockUseCase returned null (invalid duration/samples)")
                }
            } else {
                Log.w(TAG, "No samples in buffer for session $sessionId")
            }

            // 4. Limpiar buffer de esta sesión
            sessionSampleBuffer.clear(sessionId)

            // 5. Finalizar sesión en Room con totales reales
            val timestampEnd = System.currentTimeMillis()
            val newTotalBlocks = sessionEntity.totalBlocks + (if (blockInserted) 1 else 0)
            val newTotalXp = sessionEntity.totalXp + blockXp

            sessionDao.finalizeSession(
                sessionId = sessionId,
                status = SessionStatus.COMPLETED.name,
                timestampEnd = timestampEnd,
                totalBlocks = newTotalBlocks,
                totalXp = newTotalXp
            )

            // 6. Retornar sesión completada
            Result.success(
                ExerciseSession(
                    sessionId = sessionEntity.sessionId,
                    userId = sessionEntity.userId,
                    deviceId = sessionEntity.deviceId,
                    status = SessionStatus.COMPLETED,
                    sportType = com.ace.shared.enums.SportType.valueOf(sessionEntity.sportType),
                    timestampStart = sessionEntity.timestampStart,
                    timestampEnd = timestampEnd,
                    totalBlocks = newTotalBlocks,
                    totalXp = newTotalXp
                )
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error stopping session", e)
            Result.failure(e)
        }
    }
}