package com.ace.mobile.core.data

import com.ace.shared.dto.ExerciseBlockDto
import com.ace.shared.dto.ExerciseSessionDto
import com.ace.shared.enums.BlockStatus
import com.ace.shared.enums.SessionStatus
import com.ace.shared.enums.SportType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementación PLACEHOLDER de [SessionRepository].
 *
 * ⚠️ ESTADO TEMPORAL: Usa almacenamiento en memoria (RAM) hasta que
 * los DAOs de Room estén implementados.
 *
 * TODO: Reemplazar por Room SQLite cuando existan:
 *   - SessionDao, BlockDao
 *   - LocalSessionEntity, LocalBlockEntity
 *   - AceDatabase
 */
@Singleton
class SessionRepositoryImpl @Inject constructor() : SessionRepository {

    // ─── Storage en memoria (PLACEHOLDER) ───

    private val sessions = mutableMapOf<String, ExerciseSessionDto>()
    private val blocks = mutableMapOf<String, ExerciseBlockDto>()
    private val blockStatuses = mutableMapOf<String, BlockStatus>()
    private val activeSessionFlow = MutableStateFlow<ExerciseSessionDto?>(null)

    // ─── Sesiones ───

    override suspend fun createSession(
        sessionId: String,
        userId: String,
        sportType: SportType
    ) {
        val session = ExerciseSessionDto(
            sessionId = sessionId,
            userId = userId,
            status = SessionStatus.ACTIVE,
            sportType = sportType,
            timestampStart = System.currentTimeMillis(),
            timestampEnd = null,
            totalBlocks = 0,
            totalXp = 0
        )
        sessions[sessionId] = session
        activeSessionFlow.value = session
    }

    override suspend fun updateSessionStatus(
        sessionId: String,
        status: SessionStatus
    ) {
        sessions[sessionId]?.let { current ->
            val updated = current.copy(status = status)
            sessions[sessionId] = updated
            if (status == SessionStatus.ACTIVE) {
                activeSessionFlow.value = updated
            } else if (activeSessionFlow.value?.sessionId == sessionId) {
                activeSessionFlow.value = null
            }
        }
    }

    override suspend fun completeSession(
        sessionId: String,
        timestampEnd: Long
    ) {
        sessions[sessionId]?.let { current ->
            val updated = current.copy(
                status = SessionStatus.COMPLETED,
                timestampEnd = timestampEnd
            )
            sessions[sessionId] = updated
            if (activeSessionFlow.value?.sessionId == sessionId) {
                activeSessionFlow.value = null
            }
        }
    }

    override suspend fun getActiveSession(): ExerciseSessionDto? {
        return sessions.values.find { it.status == SessionStatus.ACTIVE }
    }

    override suspend fun getSession(sessionId: String): ExerciseSessionDto? {
        return sessions[sessionId]
    }

    override fun observeActiveSession(): Flow<ExerciseSessionDto?> {
        return activeSessionFlow
    }

    // ─── Bloques ───

    override suspend fun insertBlock(block: ExerciseBlockDto) {
        blocks[block.blockId] = block
        blockStatuses[block.blockId] = BlockStatus.PENDING
    }

    override suspend fun updateBlockStatus(
        blockId: String,
        status: BlockStatus
    ) {
        blockStatuses[blockId] = status
    }

    override suspend fun getPendingBlocks(limit: Int): List<ExerciseBlockDto> {
        return blockStatuses
            .filter { it.value == BlockStatus.PENDING }
            .keys
            .take(limit)
            .mapNotNull { blocks[it] }
            .sortedBy { it.timestampStart }
    }

    override suspend fun getBlocksBySession(sessionId: String): List<ExerciseBlockDto> {
        return blocks.values
            .filter { it.sessionId == sessionId }
            .sortedBy { it.timestampStart }
    }

    override suspend fun countBlocksByStatus(status: BlockStatus): Int {
        return blockStatuses.count { it.value == status }
    }
}