package com.ace.mobile.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ace.mobile.data.local.database.entity.LocalRankingCacheEntity

@Dao
interface RankingCacheDao {

    @Query("SELECT * FROM local_ranking_cache WHERE type = :type LIMIT 1")
    suspend fun getByType(type: String): LocalRankingCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(cache: LocalRankingCacheEntity)

    @Query("DELETE FROM local_ranking_cache WHERE type = :type")
    suspend fun deleteByType(type: String)

    @Query("DELETE FROM local_ranking_cache")
    suspend fun clearAll()
}