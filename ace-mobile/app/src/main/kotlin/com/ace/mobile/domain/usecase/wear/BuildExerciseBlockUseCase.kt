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
        const val MIN_BLOCK_DURATION_SECONDS = 20      // Primer bloque mínimo
        const val MAX_BLOCK_DURATION_SECONDS = 300     // Tope máximo
        const val BLOCK_TIMER_MS = 60_000L             // Timer bloques post-primeros
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
        samples: List<HeartRateSample>,
        isFirstBlock: Boolean
    ): BlockResult? = withContext(Dispatchers.Default) {

        Log.d(TAG, "=== BUILD BLOCK START ===")
        Log.d(TAG, "isFirstBlock=$isFirstBlock, samples=${samples.size}")

        if (samples.isEmpty()) {
            Log.w(TAG, "No samples provided, aborting")
            return@withContext null
        }

        val timestampStart = samples.first().timestamp
        val timestampEnd = samples.last().timestamp

        // FIX: Calcular duración como (last - first) + intervalo_estimado
        // Si las muestras vienen a ~1Hz, el intervalo es ~1000ms
        // Usamos maxOf para no subestimar la duración
        val rawDurationMs = timestampEnd - timestampStart
        val estimatedIntervalMs = if (samples.size > 1) {
            rawDurationMs / (samples.size - 1)
        } else {
            1000L // Asumimos 1Hz por defecto
        }
        val durationMs = rawDurationMs + estimatedIntervalMs
        val durationSeconds = (durationMs / 1000).toInt()

        Log.d(TAG, "durationSeconds=$durationSeconds (raw=${rawDurationMs}ms + interval=${estimatedIntervalMs}ms)")
        Log.d(TAG, "first=${samples.first().bpm}, last=${samples.last().bpm}")

        // REGLA: Primer bloque debe durar ≥ 20 segundos
        if (isFirstBlock && durationSeconds < MIN_BLOCK_DURATION_SECONDS) {
            Log.w(TAG, "PRIMER BLOQUE RECHAZADO: $durationSeconds < $MIN_BLOCK_DURATION_SECONDS")
            Log.d(TAG, "=== BUILD BLOCK END (rejected) ===")
            return@withContext null
        }

        Log.d(TAG, "Primer bloque validado: $durationSeconds >= $MIN_BLOCK_DURATION_SECONDS")

        // Tope máximo
        if (durationSeconds > MAX_BLOCK_DURATION_SECONDS) {
            Log.w(TAG, "Bloque muy largo: $durationSeconds > $MAX_BLOCK_DURATION_SECONDS")
            Log.d(TAG, "=== BUILD BLOCK END (rejected) ===")
            return@withContext null
        }

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
        Log.d(TAG, "Formula from cache: ${formula?.sportType ?: "NULL"}")

        if (formula == null) {
            Log.w(TAG, "No formula cached, fetching from backend...")
            try {
                val cacheResult = cacheXpFormulasUseCase()
                cacheResult.onSuccess { count ->
                    Log.i(TAG, "Formulas fetched: $count")
                    formula = xpFormulaDao.getFormula(sportType.name)
                    Log.d(TAG, "Formula after fetch: ${formula?.sportType ?: "NULL"}")
                }.onFailure { error ->
                    Log.w(TAG, "Fetch failed: ${error.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception fetching: ${e.message}")
            }
        }

        if (formula == null) {
            Log.w(TAG, "No formula available, XP=0.0")
            return 0.0
        }

        Log.d(TAG, "Formula found: minBpm=${formula.minBpm}, xpPerMinute=${formula.xpPerMinute}, maxXp=${formula.maxXpPerBlock}")

        return calculateXpFromFormula(formula, durationSeconds, avgBpm)
    }

    private fun calculateXpFromFormula(
        formula: LocalXpFormulaEntity,
        durationSeconds: Int,
        avgBpm: Double
    ): Double {
        Log.d(TAG, "Calculando con formula: minBpm=${formula.minBpm}, xpPerMinute=${formula.xpPerMinute}, maxXp=${formula.maxXpPerBlock}")

        if (avgBpm < formula.minBpm) {
            Log.d(TAG, "avgBpm $avgBpm < min ${formula.minBpm}, XP=0.0")
            return 0.0
        }

        val minutes = durationSeconds / 60.0
        val rawXp = minutes * formula.xpPerMinute
        val cappedXp = minOf(rawXp, formula.maxXpPerBlock.toDouble())

        Log.d(TAG, "CALCULO DETALLADO:")
        Log.d(TAG, "  durationSeconds=$durationSeconds")
        Log.d(TAG, "  minutes=$durationSeconds/60.0 = $minutes")
        Log.d(TAG, "  rawXp=$minutes * ${formula.xpPerMinute} = $rawXp")
        Log.d(TAG, "  maxXpPerBlock=${formula.maxXpPerBlock}")
        Log.d(TAG, "  cappedXp=min($rawXp, ${formula.maxXpPerBlock}) = $cappedXp")

        return cappedXp
    }
}