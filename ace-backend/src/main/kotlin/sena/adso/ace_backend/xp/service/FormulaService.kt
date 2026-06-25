package sena.adso.ace_backend.xp.service

import com.ace.shared.dto.XpFormulaDto
import mu.KotlinLogging
import org.springframework.stereotype.Service
import sena.adso.ace_backend.xp.repository.XpFormulaRepository

private val logger = KotlinLogging.logger {}

@Service
class FormulaService(
    private val xpFormulaRepository: XpFormulaRepository
) {

    fun getActiveFormulas(): Pair<List<XpFormulaDto>, Int> {
        val formulas = xpFormulaRepository.findAll()
            .filter { it.isActive }

        val maxVersion = formulas.maxOfOrNull { it.version.toInt() } ?: 1

        val dtos = formulas.map { formula ->
            XpFormulaDto(
                sportType = formula.sportType,
                minBpm = formula.minBpm.toInt(),
                xpPerMinute = formula.xpPerMinute.toInt(),
                maxXpPerBlock = formula.maxXpPerBlock,
                version = formula.version
            )
        }

        return dtos to maxVersion
    }
}