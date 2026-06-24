package com.ace.mobile.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ace.mobile.data.local.database.entity.LocalUserStatsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StatsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(stats: LocalUserStatsEntity)

    @Query("SELECT * FROM local_user_stats WHERE userId = :userId")
    suspend fun getStats(userId: String): LocalUserStatsEntity?

    @Query("SELECT * FROM local_user_stats WHERE userId = :userId")
    fun observeStats(userId: String): Flow<LocalUserStatsEntity?>

    @Query("DELETE FROM local_user_stats WHERE userId = :userId")
    suspend fun clearStats(userId: String)
}