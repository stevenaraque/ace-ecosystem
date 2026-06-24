package sena.adso.ace_backend.xp.service

import com.ace.shared.dto.ExerciseBlockDto
import mu.KotlinLogging
import org.springframework.stereotype.Service
import sena.adso.ace_backend.xp.model.XpFormula
import java.time.Instant

private val logger = KotlinLogging.logger {}

@Service
class XpSanityValidator {

    /**
     * Resultado de la validación de sanidad de un bloque.
     */
    sealed class ValidationResult {
        data class Valid(val block: ExerciseBlockDto) : ValidationResult()
        data class Invalid(val blockId: String, val reason: String) : ValidationResult()
    }

    /**
     * Valida que la XP reportada por el móvil sea consistente con las métricas del bloque.
     * El backend NO recalcula XP desde cero; solo valida que no sea absurdo.
     * 
     * Reglas (Apéndice S5 §4.3):
     * 1. Rango fisiológico: avg_bpm entre 30 y 250
     * 2. Consistencia temporal: duration_seconds entre 270 y 330 (±10% de 300)
     * 3. Consistencia de muestras: sample_count coherente con duración (~1Hz)
     * 4. Consistencia de XP: xp_calculated ≤ fórmula teórica (con margen)
     * 5. Techo de bloque: xp_calculated no excede max_xp_per_block
     */
    fun validate(
        block: ExerciseBlockDto,
        formula: XpFormula? = null
    ): ValidationResult {
        
        // R1: Rango fisiológico
        if (block.avgBpm < MIN_BPM || block.avgBpm > MAX_BPM) {
            logger.warn { "Block ${block.blockId} rejected: avgBpm ${block.avgBpm} out of range [$MIN_BPM, $MAX_BPM]" }
            return ValidationResult.Invalid(block.blockId, "avgBpm out of range: ${block.avgBpm}")
        }

        // R2: Consistencia temporal (10-330s)
        // FIX: El mínimo era 30, lo que rechazaba bloques finales cortos (p.ej. 14s al
        // detener la sesión). Bajado a 10 para aceptarlos. El móvil también descarta
        // los < 10s, así que la frontera coincide en ambos lados.
        if (block.durationSeconds < MIN_DURATION || block.durationSeconds > MAX_DURATION) {
            logger.warn { "Block ${block.blockId} rejected: duration ${block.durationSeconds}s out of range [$MIN_DURATION, $MAX_DURATION]" }
            return ValidationResult.Invalid(block.blockId, "duration out of range: ${block.durationSeconds}s")
        }

        // R3: Consistencia de muestras (densidad ~1Hz, tolerancia ±20%)
        val expectedSamples = block.durationSeconds
        val sampleTolerance = (expectedSamples * SAMPLE_TOLERANCE_PERCENT / 100).toInt()
        if (block.sampleCount < expectedSamples - sampleTolerance || 
            block.sampleCount > expectedSamples + sampleTolerance) {
            logger.warn { "Block ${block.blockId} rejected: sampleCount ${block.sampleCount} inconsistent with duration ${block.durationSeconds}s" }
            return ValidationResult.Invalid(block.blockId, "sampleCount inconsistent: ${block.sampleCount}")
        }

        // R4: Techo de XP por bloque (anti-trampa)
        if (block.xpCalculated > MAX_XP_PER_BLOCK) {
            logger.warn { "Block ${block.blockId} rejected: xp ${block.xpCalculated} exceeds max $MAX_XP_PER_BLOCK" }
            return ValidationResult.Invalid(block.blockId, "xp exceeds max per block: ${block.xpCalculated}")
        }

        // R5: Consistencia de XP con fórmula (si está disponible)
        if (formula != null) {
            val maxTheoreticalXp = calculateMaxTheoreticalXp(block, formula)
            if (block.xpCalculated > maxTheoreticalXp + XP_TOLERANCE) {
                logger.warn { "Block ${block.blockId} rejected: xp ${block.xpCalculated} exceeds theoretical max $maxTheoreticalXp" }
                return ValidationResult.Invalid(block.blockId, "xp exceeds theoretical maximum")
            }
        }

        logger.debug { "Block ${block.blockId} passed sanity validation" }
        return ValidationResult.Valid(block)
    }

    /**
     * Calcula el máximo teórico de XP para un bloque según la fórmula activa.
     * Usado para validar que el móvil no reporte XP inflada.
     */
    private fun calculateMaxTheoreticalXp(block: ExerciseBlockDto, formula: XpFormula): Int {
        if (block.avgBpm < formula.minBpm) return 0
        
        val minutes = block.durationSeconds / 60.0
        val baseXp = (minutes * formula.xpPerMinute * formula.bonusMultiplier).toInt()
        return minOf(baseXp, formula.maxXpPerBlock)
    }

    companion object {
        const val MIN_BPM = 30.0
        const val MAX_BPM = 250.0
        const val MIN_DURATION = 10
        const val MAX_DURATION = 330
        const val SAMPLE_TOLERANCE_PERCENT = 20
        const val MAX_XP_PER_BLOCK = 50
        const val XP_TOLERANCE = 5  // Margen de tolerancia para fórmulas desactualizadas
    }
}