package com.ace.mobile.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ace.mobile.data.local.database.entity.LocalBlockEntity

@Dao
interface BlockDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(block: LocalBlockEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(blocks: List<LocalBlockEntity>)

    /**
     * Obtiene los bloques PENDING más antiguos (FIFO) para sincronización.
     * Límite: BATCH_MAX_SIZE (20)
     *
     * NOTA: Room usa los nombres de propiedad Kotlin como columnas.
     * LocalBlockEntity.timestampStart → columna "timestampStart"
     */
    @Query("""
        SELECT * FROM local_blocks 
        WHERE status = 'PENDING' 
        ORDER BY timestampStart ASC 
        LIMIT :limit
    """)
    suspend fun getPendingBlocks(limit: Int): List<LocalBlockEntity>

    /**
     * Actualiza el estado de múltiples bloques por sus IDs.
     *
     * NOTA: Room usa "blockId" (propiedad Kotlin), NO "block_id".
     */
    @Query("""
        UPDATE local_blocks 
        SET status = :status 
        WHERE blockId IN (:blockIds)
    """)
    suspend fun updateStatus(blockIds: List<String>, status: String)

    /**
     * Obtiene todos los bloques de una sesión.
     */
    @Query("SELECT * FROM local_blocks WHERE sessionId = :sessionId ORDER BY timestampStart ASC")
    suspend fun getBlocksBySession(sessionId: String): List<LocalBlockEntity>

    /**
     * Cuenta bloques por estado.
     */
    @Query("SELECT COUNT(*) FROM local_blocks WHERE status = :status")
    suspend fun countByStatus(status: String): Int

    /**
     * Elimina bloques SYNCED antiguos (mantenimiento).
     */
    @Query("DELETE FROM local_blocks WHERE status = 'SYNCED' AND timestampEnd < :olderThan")
    suspend fun deleteSyncedOlderThan(olderThan: Long): Int
}