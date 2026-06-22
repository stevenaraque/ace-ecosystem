// app/src/main/kotlin/com/ace/mobile/domain/usecase/wear/BuildExerciseBlockUseCase.kt
package com.ace.mobile.domain.usecase.wear

import android.util.Log
import com.ace.mobile.data.local.database.dao.XpFormulaDao
import com.ace.mobile.data.local.database.entity.LocalXpFormulaEntity
import com.ace.mobile.domain.model.ExerciseSession
import com.ace.mobile.domain.model.HeartRateSample
import com.ace.mobile.domain.usecase.xp.CacheXpFormulasUseCase
import com.ace.shared.enums.SportType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

class BuildExerciseBlockUseCase @Inject constructor(
    private val xpFormulaDao: XpFormulaDao,
    private val cacheXpFormulasUseCase: CacheXpFormulasUseCase  // FIX BUG 2: Inyectar para fallback
) {

    companion object {
        private const val TAG = "BuildExerciseBlock"
        private const val BLOCK_DURATION_SECONDS = 20        // 5 minutos
        private const val BLOCK_DURATION_TOLERANCE_PERCENT = 300 // ±10%
    }

    data class BlockResult(
        val blockId: String,
        val sessionId: String,
        val userId: String,
        val deviceId: String,
        val sportType: SportType,
        val timestampStart: Long,
        val timestampEnd: Long,
        val durationSeconds: Int,
        val avgBpm: Double,
        val maxBpm: Double,
        val minBpm: Double,
        val sampleCount: Int,
        val xpCalculated: Int
    )

    suspend operator fun invoke(
        session: ExerciseSession,
        samples: List<HeartRateSample>
    ): BlockResult? = withContext(Dispatchers.Default) {

        if (samples.isEmpty()) {
            Log.w(TAG, "No samples provided for block build")
            return@withContext null
        }

        val timestampStart = samples.first().timestamp
        val timestampEnd = samples.last().timestamp
        val durationSeconds = ((timestampEnd - timestampStart) / 1000).toInt()

        val minDuration = BLOCK_DURATION_SECONDS -
                (BLOCK_DURATION_SECONDS * BLOCK_DURATION_TOLERANCE_PERCENT / 100)
        val maxDuration = BLOCK_DURATION_SECONDS +
                (BLOCK_DURATION_SECONDS * BLOCK_DURATION_TOLERANCE_PERCENT / 100)

        if (durationSeconds < minDuration || durationSeconds > maxDuration) {
            Log.w(TAG, "Duration $durationSeconds out of range [$minDuration, $maxDuration]")
            return@withContext null
        }

        val bpmValues = samples.map { it.bpm }
        val avgBpm = bpmValues.average()
        val maxBpm = bpmValues.maxOrNull() ?: 0.0
        val minBpm = bpmValues.minOrNull() ?: 0.0
        val sampleCount = samples.size

        // FIX BUG 2: Calcular XP con fallback de cacheo si no hay fórmula
        val xpCalculated = calculateXpWithFallback(
            sportType = session.sportType,
            durationSeconds = durationSeconds,
            avgBpm = avgBpm
        )

        BlockResult(
            blockId = UUID.randomUUID().toString(),
            sessionId = session.sessionId,
            userId = session.userId,
            deviceId = session.deviceId,
            sportType = session.sportType,
            timestampStart = timestampStart,
            timestampEnd = timestampEnd,
            durationSeconds = durationSeconds,
            avgBpm = avgBpm,
            maxBpm = maxBpm,
            minBpm = minBpm,
            sampleCount = sampleCount,
            xpCalculated = xpCalculated
        )
    }

    /**
     * FIX BUG 2: Intenta obtener fórmula de Room. Si no existe, intenta cachear del backend.
     * Si todo falla, retorna 0 XP (fallback conservador).
     */
    private suspend fun calculateXpWithFallback(
        sportType: SportType,
        durationSeconds: Int,
        avgBpm: Double
    ): Int {
        var formula = xpFormulaDao.getFormula(sportType.name)

        // FIX BUG 2: Si no hay fórmula, intentar cachear del backend
        if (formula == null) {
            Log.w(TAG, "No formula cached for $sportType, attempting to fetch from backend...")
            try {
                val cacheResult = cacheXpFormulasUseCase()
                cacheResult.onSuccess { count ->
                    Log.i(TAG, "XP formulas fetched on-demand: $count formulas")
                    // Reintentar obtener la fórmula después de cachear
                    formula = xpFormulaDao.getFormula(sportType.name)
                }.onFailure { error ->
                    Log.w(TAG, "Failed to fetch XP formulas on-demand: ${error.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception fetching XP formulas on-demand", e)
            }
        }

        if (formula == null) {
            Log.w(TAG, "No formula available for $sportType after fallback, XP=0")
            return 0
        }

        return calculateXpFromFormula(formula, durationSeconds, avgBpm)
    }

    private fun calculateXpFromFormula(
        formula: LocalXpFormulaEntity,
        durationSeconds: Int,
        avgBpm: Double
    ): Int {
        if (avgBpm < formula.minBpm) {
            Log.d(TAG, "avgBpm $avgBpm < min ${formula.minBpm}, XP=0")
            return 0
        }

        val minutes = durationSeconds / 60.0
        val rawXp = (minutes * formula.xpPerMinute).toInt()
        val cappedXp = minOf(rawXp, formula.maxXpPerBlock)

        Log.d(TAG,
            "XP calc: sport=${formula.sportType}, duration=${durationSeconds}s, " +
                    "minutes=$minutes, rawXp=$rawXp, cappedXp=$cappedXp, " +
                    "max=${formula.maxXpPerBlock}"
        )

        return cappedXp
    }
}