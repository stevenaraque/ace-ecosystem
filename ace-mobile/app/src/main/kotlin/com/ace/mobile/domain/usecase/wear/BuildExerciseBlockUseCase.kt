package com.ace.mobile.domain.usecase.wear

import com.ace.mobile.domain.model.ExerciseSession
import com.ace.mobile.domain.model.HeartRateSample
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

class BuildExerciseBlockUseCase @Inject constructor() {

    companion object {
        // Constantes hardcodeadas localmente (consistentes con arquitectura A.C.E)
        private const val BLOCK_DURATION_SECONDS = 300        // 5 minutos
        private const val BLOCK_DURATION_TOLERANCE_PERCENT = 10 // ±10%
    }

    data class BlockResult(
        val blockId: String,
        val sessionId: String,
        val userId: String,
        val deviceId: String,
        val sportType: String,
        val timestampStart: Long,
        val timestampEnd: Long,
        val durationSeconds: Int,
        val avgBpm: Double,
        val maxBpm: Int,
        val minBpm: Int,
        val sampleCount: Int
    )

    suspend operator fun invoke(
        session: ExerciseSession,
        samples: List<HeartRateSample>
    ): BlockResult? = withContext(Dispatchers.Default) {

        if (samples.isEmpty()) return@withContext null

        val timestampStart = samples.first().timestamp
        val timestampEnd = samples.last().timestamp
        val durationSeconds = ((timestampEnd - timestampStart) / 1000).toInt()

        // Validar duración: entre 270-330 segundos (±10% de 300)
        val minDuration = BLOCK_DURATION_SECONDS -
                (BLOCK_DURATION_SECONDS * BLOCK_DURATION_TOLERANCE_PERCENT / 100)
        val maxDuration = BLOCK_DURATION_SECONDS +
                (BLOCK_DURATION_SECONDS * BLOCK_DURATION_TOLERANCE_PERCENT / 100)

        if (durationSeconds < minDuration || durationSeconds > maxDuration) {
            return@withContext null
        }

        val bpmValues = samples.map { it.bpm }
        val avgBpm = bpmValues.average()
        val maxBpm = bpmValues.maxOrNull() ?: 0
        val minBpm = bpmValues.minOrNull() ?: 0
        val sampleCount = samples.size

        BlockResult(
            blockId = UUID.randomUUID().toString(),
            sessionId = session.sessionId,
            userId = session.userId,
            deviceId = session.deviceId,
            sportType = session.sportType.name,
            timestampStart = timestampStart,
            timestampEnd = timestampEnd,
            durationSeconds = durationSeconds,
            avgBpm = avgBpm,
            maxBpm = maxBpm,
            minBpm = minBpm,
            sampleCount = sampleCount
        )
    }
}