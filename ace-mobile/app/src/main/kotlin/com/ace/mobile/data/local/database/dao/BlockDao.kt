package com.ace.mobile.data.local.database.dao

import androidx.room.*
import com.ace.mobile.data.local.database.entity.LocalBlockEntity
import com.ace.shared.enums.BlockStatus

@Dao
interface BlockDao {
    @Insert
    suspend fun insert(block: LocalBlockEntity)

    @Update
    suspend fun update(block: LocalBlockEntity)

    @Query("SELECT * FROM local_blocks WHERE sessionId = :sessionId ORDER BY timestampStart ASC")
    suspend fun getBlocksBySession(sessionId: String): List<LocalBlockEntity>

    @Query("UPDATE local_blocks SET status = :status WHERE blockId = :blockId")
    suspend fun updateBlockStatus(blockId: String, status: BlockStatus)
}