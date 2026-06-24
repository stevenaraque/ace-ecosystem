package sena.adso.ace_backend.xp.service

import com.ace.shared.constants.XpConstants
import com.ace.shared.dto.ExerciseBlockDto
import mu.KotlinLogging
import org.springframework.stereotype.Service
import sena.adso.ace_backend.xp.model.XpFormula

private val logger = KotlinLogging.logger {}

@Service
class XpSanityValidator {

    sealed class ValidationResult {
        data class Valid(val block: ExerciseBlockDto) : ValidationResult()
        data class Invalid(val blockId: String, val reason: String) : ValidationResult()
    }

    /**
     * Valida que la XP reportada por el móvil sea consistente con las métricas del bloque.
     *
     * Reglas (Apéndice S5 §4.3, ajustado por decisión de equipo: bloques de 60s):
     * 1. Rango fisiológico: avg_bpm entre 30 y 250
     * 2. Consistencia temporal: duration_seconds entre 10 y 330
     * 3. Consistencia de muestras: densidad ≥ 0.5Hz (tolerancia Bluetooth)
     * 5. Techo de bloque: xp_calculated no excede max_xp_per_block de la fórmula
     * 5. Consistencia de XP con fórmula (si está disponible)
     */
    fun validate(
        block: ExerciseBlockDto,
        formula: XpFormula? = null
    ): ValidationResult {

        // R1: Rango fisiológico (usar XpConstants)
        if (block.avgBpm < XpConstants.MIN_BPM_PHYSIOLOGICAL || block.avgBpm > XpConstants.MAX_BPM_PHYSIOLOGICAL) {
            logger.warn { "Block ${block.blockId} rejected: avgBpm ${block.avgBpm} out of range [${XpConstants.MIN_BPM_PHYSIOLOGICAL}, ${XpConstants.MAX_BPM_PHYSIOLOGICAL}]" }
            return ValidationResult.Invalid(block.blockId, "avgBpm out of range: ${block.avgBpm}")
        }

        // R2: Consistencia temporal (decisión equipo: 10-330s para bloques cortos)
        if (block.durationSeconds < MIN_DURATION || block.durationSeconds > MAX_DURATION) {
            logger.warn { "Block ${block.blockId} rejected: duration ${block.durationSeconds}s out of range [$MIN_DURATION, $MAX_DURATION]" }
            return ValidationResult.Invalid(block.blockId, "duration out of range: ${block.durationSeconds}s")
        }

        // R3: Consistencia de muestras (M2: relajado a ≥0.5Hz para tolerar desconexiones Bluetooth)
        val minExpectedSamples = block.durationSeconds / 2
        if (block.sampleCount < minExpectedSamples) {
            logger.warn { "Block ${block.blockId} rejected: sampleCount ${block.sampleCount} too low for duration ${block.durationSeconds}s (min expected: $minExpectedSamples)" }
            return ValidationResult.Invalid(block.blockId, "sampleCount too low: ${block.sampleCount}")
        }

        // R4 ELIMINADO (M1): El techo arbitrario MAX_XP_PER_BLOCK=50 contradice el seed y es redundante.
        // R5 ya valida contra formula.maxXpPerBlock que es la fuente de verdad.

        // R5: Consistencia de XP con fórmula (techo real + cálculo teórico)
        if (formula != null) {
            if (block.xpCalculated > formula.maxXpPerBlock) {
                logger.warn { "Block ${block.blockId} rejected: xp ${block.xpCalculated} exceeds formula max ${formula.maxXpPerBlock}" }
                return ValidationResult.Invalid(block.blockId, "xp exceeds max per block: ${block.xpCalculated}")
            }

            val maxTheoreticalXp = calculateMaxTheoreticalXp(block, formula)
            if (block.xpCalculated > maxTheoreticalXp + XP_TOLERANCE) {
                logger.warn { "Block ${block.blockId} rejected: xp ${block.xpCalculated} exceeds theoretical max $maxTheoreticalXp" }
                return ValidationResult.Invalid(block.blockId, "xp exceeds theoretical maximum")
            }
        }

        logger.debug { "Block ${block.blockId} passed sanity validation" }
        return ValidationResult.Valid(block)
    }

    private fun calculateMaxTheoreticalXp(block: ExerciseBlockDto, formula: XpFormula): Int {
        if (block.avgBpm < formula.minBpm) return 0
        
        val minutes = block.durationSeconds / 60.0
        // C3 FIX: eliminado bonusMultiplier (no existe en :shared ni en apéndice)
        val baseXp = (minutes * formula.xpPerMinute).toInt()
        return minOf(baseXp, formula.maxXpPerBlock)
    }

    companion object {
        // Mantenidos: límites de duración por decisión de equipo (bloques de 60s, min 10s)
        const val MIN_DURATION = 10
        const val MAX_DURATION = 330
        const val XP_TOLERANCE = 5
    }
}