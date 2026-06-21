package sena.adso.ace_backend.exercise.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import sena.adso.ace_backend.exercise.model.ExerciseBlock
import java.util.UUID

@Repository
interface ExerciseBlockRepository : JpaRepository<ExerciseBlock, UUID> {
    /**
     * Verifica si un bloque ya fue persistido.
     * Usado para idempotencia: ON CONFLICT (block_id) DO NOTHING.
     */
    fun existsByBlockId(blockId: UUID): Boolean
}