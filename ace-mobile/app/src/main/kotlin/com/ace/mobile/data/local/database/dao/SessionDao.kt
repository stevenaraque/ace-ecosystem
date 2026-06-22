// app/src/main/kotlin/com/ace/mobile/data/local/database/dao/SessionDao.kt
package com.ace.mobile.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.ace.mobile.data.local.database.entity.LocalSessionEntity

@Dao
interface SessionDao {

    @Insert
    suspend fun insertSession(session: LocalSessionEntity)

    @Query("SELECT * FROM local_sessions WHERE status = 'ACTIVE' LIMIT 1")
    suspend fun getActiveSession(): LocalSessionEntity?

    @Query("UPDATE local_sessions SET status = :newStatus WHERE sessionId = :sessionId")
    suspend fun updateSessionStatus(sessionId: String, newStatus: String)

    @Query("SELECT * FROM local_sessions WHERE sessionId = :sessionId")
    suspend fun getSessionById(sessionId: String): LocalSessionEntity?

    @Update
    suspend fun updateSession(session: LocalSessionEntity)

    /**
     * Finaliza una sesión: actualiza estado, timestamp de fin, totales de bloques y XP.
     * Usado por StopSessionUseCase al completar la sesión.
     */
    @Query("""
        UPDATE local_sessions 
        SET status = :status, 
            timestampEnd = :timestampEnd, 
            totalBlocks = :totalBlocks, 
            totalXp = :totalXp 
        WHERE sessionId = :sessionId
    """)
    suspend fun finalizeSession(
        sessionId: String,
        status: String,
        timestampEnd: Long,
        totalBlocks: Int,
        totalXp: Long
    )
}