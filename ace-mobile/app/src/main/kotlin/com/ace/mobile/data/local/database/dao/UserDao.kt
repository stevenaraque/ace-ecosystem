package com.ace.mobile.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ace.mobile.data.local.database.entity.LocalUserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: LocalUserEntity)

    @Update
    suspend fun update(user: LocalUserEntity)

    @Query("SELECT * FROM local_user LIMIT 1")
    suspend fun getCurrentUser(): LocalUserEntity?

    @Query("SELECT * FROM local_user LIMIT 1")
    fun observeCurrentUser(): Flow<LocalUserEntity?>

    @Query("DELETE FROM local_user")
    suspend fun clearUser()

    // ─── Tokens (S4) ───

    @Query("UPDATE local_user SET accessToken = :token, tokenExpiresAt = :expiresAt WHERE userId = :userId")
    suspend fun updateAccessToken(userId: String, token: String, expiresAt: Long)

    @Query("UPDATE local_user SET refreshToken = :token WHERE userId = :userId")
    suspend fun updateRefreshToken(userId: String, token: String)

    @Query("UPDATE local_user SET accessToken = NULL, refreshToken = NULL, tokenExpiresAt = NULL WHERE userId = :userId")
    suspend fun clearTokens(userId: String)

    // ─── Streak cache (S7) ───

    @Query("UPDATE local_user SET currentStreak = :streak, bestStreak = :best, lastExerciseDate = :date WHERE userId = :userId")
    suspend fun updateStreakCache(userId: String, streak: Int, best: Int, date: Long?)

    // ─── XP cache (S10) ───

    @Query("UPDATE local_user SET totalXp = :xp WHERE userId = :userId")
    suspend fun updateTotalXp(userId: String, xp: Int)
}