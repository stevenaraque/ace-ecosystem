package com.ace.mobile.data.repository

import com.ace.shared.dto.ExerciseBlockDto
import com.ace.shared.dto.ExerciseSessionDto
import com.ace.shared.enums.BlockStatus
import com.ace.shared.enums.SessionStatus
import com.ace.shared.enums.SportType
import kotlinx.coroutines.flow.Flow

/**
 * Contrato de acceso a datos de sesiones y bloques de ejercicio.
 *
 * Sistema 2 — Sesión de Ejercicio (Apéndice S2)
 * Abstrae la fuente de datos (Room SQLite local).
 *
 * Implementación: [SessionRepositoryImpl]
 * Binding Hilt: [com.ace.mobile.di.RepositoryModule]
 */
interface SessionRepository {

    // ─── Sesiones ───

    /** Crea una nueva sesión en estado ACTIVE. */
    suspend fun createSession(
        sessionId: String,
        userId: String,
        sportType: SportType
    )

    /** Actualiza el estado de una sesión. */
    suspend fun updateSessionStatus(
        sessionId: String,
        status: SessionStatus
    )

    /** Cierra una sesión con timestamp de fin. */
    suspend fun completeSession(
        sessionId: String,
        timestampEnd: Long
    )

    /** Obtiene la sesión activa actual (si existe). Solo puede haber una. */
    suspend fun getActiveSession(): ExerciseSessionDto?

    /** Obtiene una sesión por ID. */
    suspend fun getSession(sessionId: String): ExerciseSessionDto?

    /** Flujo reactivo de la sesión activa. */
    fun observeActiveSession(): Flow<ExerciseSessionDto?>

    // ─── Bloques ───

    /** Inserta un bloque cerrado en estado PENDING. */
    suspend fun insertBlock(block: ExerciseBlockDto)

    /** Actualiza el estado de sincronización de un bloque. */
    suspend fun updateBlockStatus(
        blockId: String,
        status: BlockStatus
    )

    /** Obtiene bloques pendientes de sincronización (FIFO). */
    suspend fun getPendingBlocks(limit: Int = 20): List<ExerciseBlockDto>

    /** Obtiene todos los bloques de una sesión. */
    suspend fun getBlocksBySession(sessionId: String): List<ExerciseBlockDto>

    /** Cuenta bloques por estado. */
    suspend fun countBlocksByStatus(status: BlockStatus): Int
}