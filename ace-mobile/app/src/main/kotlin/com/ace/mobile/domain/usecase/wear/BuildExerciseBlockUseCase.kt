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
    private val cacheXpFormulasUseCase: CacheXpFormulasUseCase
) {

    companion object {
        private const val TAG = "BuildExerciseBlock"
        // 🔧 CAMBIA ESTE NÚMERO PARA CAMBIAR LA DURACIÓN DEL BLOQUE
        const val BLOCK_TIMER_MS = 60_000L
        const val BLOCK_DURATION_SECONDS = (BLOCK_TIMER_MS / 1000).toInt()
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
        val xpCalculated: Double
    )

    suspend operator fun invoke(
        session: ExerciseSession,
        samples: List<HeartRateSample>
    ): BlockResult? = withContext(Dispatchers.Default) {

        Log.d(TAG, "=== BUILD BLOCK START ===")
        Log.d(TAG, "samples=${samples.size}")

        if (samples.isEmpty()) {
            Log.w(TAG, "No samples, aborting")
            return@withContext null
        }

        val timestampStart = samples.first().timestamp
        val timestampEnd = samples.last().timestamp

        // Duración basada en timestamps de las muestras (del reloj)
        val durationMs = timestampEnd - timestampStart
        val durationSeconds = (durationMs / 1000).toInt()

        Log.d(TAG, "durationSeconds=$durationSeconds (from sample timestamps)")

        val bpmValues = samples.map { it.bpm }
        val avgBpm = bpmValues.average()
        val maxBpm = bpmValues.maxOrNull() ?: 0.0
        val minBpm = bpmValues.minOrNull() ?: 0.0
        val sampleCount = samples.size

        Log.d(TAG, "BPM stats: avg=$avgBpm, max=$maxBpm, min=$minBpm, samples=$sampleCount")

        val xpCalculated = calculateXpWithFallback(
            sportType = session.sportType,
            durationSeconds = durationSeconds,
            avgBpm = avgBpm
        )

        Log.d(TAG, "XP FINAL: $xpCalculated")

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
        ).also {
            Log.d(TAG, "=== BUILD BLOCK END (success) ===")
        }
    }

    private suspend fun calculateXpWithFallback(
        sportType: SportType,
        durationSeconds: Int,
        avgBpm: Double
    ): Double {
        Log.d(TAG, "Calculando XP: sport=$sportType, duration=${durationSeconds}s, avgBpm=$avgBpm")

        var formula = xpFormulaDao.getFormula(sportType.name)

        if (formula == null) {
            Log.w(TAG, "No formula cached, fetching...")
            try {
                val cacheResult = cacheXpFormulasUseCase()
                cacheResult.onSuccess { count ->
                    Log.i(TAG, "Formulas fetched: $count")
                    formula = xpFormulaDao.getFormula(sportType.name)
                }.onFailure { error ->
                    Log.w(TAG, "Fetch failed: ${error.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception: ${e.message}")
            }
        }

        if (formula == null) {
            Log.w(TAG, "No formula, XP=0")
            return 0.0
        }

        return calculateXpFromFormula(formula, durationSeconds, avgBpm)
    }

    private fun calculateXpFromFormula(
        formula: LocalXpFormulaEntity,
        durationSeconds: Int,
        avgBpm: Double
    ): Double {
        if (avgBpm < formula.minBpm) {
            Log.d(TAG, "avgBpm $avgBpm < min ${formula.minBpm}, XP=0")
            return 0.0
        }

        val minutes = durationSeconds / 60.0
        val rawXp = minutes * formula.xpPerMinute
        val cappedXp = minOf(rawXp, formula.maxXpPerBlock.toDouble())

        Log.d(TAG, "XP: ${durationSeconds}s = ${minutes}min * ${formula.xpPerMinute} = $rawXp -> capped=$cappedXp")

        return cappedXp
    }
}