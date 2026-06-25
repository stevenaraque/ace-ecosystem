package com.ace.mobile.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ace.mobile.core.database.entity.LocalSessionHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<LocalSessionHistoryEntity>)

    @Query("SELECT * FROM local_session_history WHERE userId = :userId ORDER BY timestampStart DESC LIMIT :limit")
    suspend fun getHistory(userId: String, limit: Int = 20): List<LocalSessionHistoryEntity>

    @Query("SELECT * FROM local_session_history WHERE userId = :userId ORDER BY timestampStart DESC LIMIT :limit")
    fun observeHistory(userId: String, limit: Int = 20): Flow<List<LocalSessionHistoryEntity>>

    @Query("DELETE FROM local_session_history WHERE userId = :userId")
    suspend fun clearHistory(userId: String)

    @Query("DELETE FROM local_session_history WHERE userId = :userId AND sessionId NOT IN (SELECT sessionId FROM local_session_history WHERE userId = :userId ORDER BY timestampStart DESC LIMIT :keep)")
    suspend fun trimHistory(userId: String, keep: Int = 5)
}