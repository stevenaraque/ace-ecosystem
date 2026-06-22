// xp/dto/XpFormulaResponse.kt
package sena.adso.ace_backend.xp.dto

import com.ace.shared.enums.SportType

data class XpFormulaResponse(
    val formulas: List<FormulaItem>
) {
    data class FormulaItem(
        val sportType: SportType,
        val minBpm: Double,
        val xpPerMinute: Double,
        val bonusMultiplier: Double,
        val maxXpPerBlock: Int
    )
}

