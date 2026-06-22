package com.ace.mobile.domain.usecase.xp

import android.util.Log
import com.ace.mobile.data.local.database.dao.XpFormulaDao
import com.ace.mobile.data.local.database.entity.LocalXpFormulaEntity
import com.ace.shared.enums.SportType
import javax.inject.Inject

class CalculateBlockXpUseCase @Inject constructor(
    private val xpFormulaDao: XpFormulaDao
) {

    /**
     * Calcula la XP de un bloque usando la fórmula cacheada en Room.
     *
     * Fórmula: min((duraciónMinutos * xpPerMinute), maxXpPerBlock)
     * Solo si avgBpm >= minBpm
     *
     * @return XP calculada, o 0 si no hay fórmula o BPM insuficiente
     */
    suspend fun execute(
        sportType: SportType,
        durationSeconds: Int,
        avgBpm: Double
    ): Int {
        val formula = xpFormulaDao.getFormula(sportType.name)
            ?: run {
                Log.w("CalculateBlockXp", "No formula cached for $sportType")
                return 0
            }

        // Validar BPM mínimo
        if (avgBpm < formula.minBpm) {
            Log.d("CalculateBlockXp", "avgBpm $avgBpm < min ${formula.minBpm}")
            return 0
        }

        val minutes = durationSeconds / 60.0
        val rawXp = (minutes * formula.xpPerMinute).toInt()
        val cappedXp = minOf(rawXp, formula.maxXpPerBlock)

        Log.d("CalculateBlockXp",
            "sport=$sportType, duration=${durationSeconds}s, minutes=$minutes, " +
                    "rawXp=$rawXp, cappedXp=$cappedXp, max=${formula.maxXpPerBlock}"
        )

        return cappedXp
    }

    /**
     * Sobrecarga con formula explícita (para usar sin acceso a BD).
     */
    fun execute(
        formula: LocalXpFormulaEntity,
        durationSeconds: Int,
        avgBpm: Double
    ): Int {
        if (avgBpm < formula.minBpm) {
            Log.d("CalculateBlockXp", "avgBpm $avgBpm < min ${formula.minBpm}")
            return 0
        }

        val minutes = durationSeconds / 60.0
        val rawXp = (minutes * formula.xpPerMinute).toInt()
        return minOf(rawXp, formula.maxXpPerBlock)
    }
}