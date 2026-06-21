package sena.adso.ace_backend.exercise.service

import com.ace.shared.dto.ExerciseBlockDto
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import sena.adso.ace_backend.exercise.model.ExerciseBlock
import sena.adso.ace_backend.exercise.repository.ExerciseBlockRepository
import java.time.Instant
import java.util.UUID

private val logger = KotlinLogging.logger {}

@Service
class BlockPersistenceService(
    private val blockRepository: ExerciseBlockRepository
) {

    /**
     * Persiste un bloque si no existe ya (idempotencia).
     * Retorna true si fue persistido, false si ya existía.
     * 
     * v1.0.5: Ahora incluye sessionId si está disponible en el DTO.
     */
    @Transactional
    fun persistIfNotExists(dto: ExerciseBlockDto): Boolean {
        val blockId = UUID.fromString(dto.blockId)

        if (blockRepository.existsByBlockId(blockId)) {
            logger.debug { "Block $blockId already exists (idempotency skip)" }
            return false
        }

        val block = ExerciseBlock(
            blockId = blockId,
            sessionId = UUID.fromString(dto.sessionId),
            userId = UUID.fromString(dto.userId),
            deviceId = dto.deviceId,
            sportType = dto.sportType,
            timestampStart = Instant.ofEpochMilli(dto.timestampStart),
            timestampEnd = Instant.ofEpochMilli(dto.timestampEnd),
            durationSeconds = dto.durationSeconds,
            avgBpm = dto.avgBpm,
            maxBpm = dto.maxBpm,
            minBpm = dto.minBpm,
            sampleCount = dto.sampleCount,
            xpCalculated = dto.xpCalculated,
            schemaVersion = dto.schemaVersion
        )

        blockRepository.save(block)
        logger.debug { "Block $blockId persisted (xp=${dto.xpCalculated})" }
        return true
    }
}