package com.ace.mobile.data.repository

import android.util.Log
import com.ace.mobile.data.local.database.dao.StatsDao
import com.ace.mobile.data.local.database.dao.UserDao
import com.ace.mobile.data.local.database.entity.LocalUserStatsEntity
import com.ace.mobile.data.remote.api.StatsApi
import com.ace.shared.dto.StatsReconcileRequestDto
import com.ace.shared.dto.StatsResponseDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StatsRepository @Inject constructor(
    private val statsApi: StatsApi,
    private val statsDao: StatsDao,
    private val userDao: UserDao
) {

    companion object {
        private const val TAG = "StatsRepository"
    }

    /**
     * Obtiene stats oficiales: primero local (offline), luego backend (refresh).
     */
    fun getStats(forceRefresh: Boolean = false): Flow<Result<StatsResponseDto>> = flow {
        val userId = userDao.getCurrentUser()?.userId
            ?: run {
                emit(Result.failure(IllegalStateException("No user logged in")))
                return@flow
            }

        // 1. Emitir cache local primero (offline-first)
        val localStats = statsDao.observeStats(userId).firstOrNull()
        if (localStats != null && !forceRefresh) {
            Log.d(TAG, "Emitting local stats for user $userId")
            emit(Result.success(localStats.toDto()))
        }

        // 2. Fetch del backend
        try {
            val response = statsApi.getStats()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    // Guardar en Room
                    statsDao.insert(body.toEntity(userId))
                    Log.d(TAG, "Stats refreshed from backend for user $userId: xp=${body.totalXp}")
                    emit(Result.success(body))
                } else {
                    emit(Result.failure(IllegalStateException("Empty response body")))
                }
            } else {
                Log.w(TAG, "Stats fetch failed: ${response.code()}")
                if (localStats == null) {
                    emit(Result.failure(IllegalStateException("HTTP ${response.code()}")))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Stats fetch exception", e)
            if (localStats == null) {
                emit(Result.failure(e))
            }
        }
    }

    /**
     * Reconcilia stats locales con el backend.
     */
    suspend fun reconcile(clientStats: com.ace.shared.dto.ClientStatsDto): Result<com.ace.shared.dto.StatsReconcileResponseDto> {
        return try {
            val response = statsApi.reconcile(StatsReconcileRequestDto(clientStats))
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Log.d(TAG, "Reconcile success: ${body.discrepancies.size} discrepancies")
                    Result.success(body)
                } else {
                    Result.failure(IllegalStateException("Empty reconcile response"))
                }
            } else {
                Log.w(TAG, "Reconcile failed: ${response.code()}")
                Result.failure(IllegalStateException("HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Reconcile exception", e)
            Result.failure(e)
        }
    }

    private fun LocalUserStatsEntity.toDto() = StatsResponseDto(
        totalXp = totalXp,
        totalSessions = totalSessions,
        totalBlocks = totalBlocks,
        totalDurationSeconds = totalDurationSeconds,
        avgBpmAllTime = avgBpmAllTime,
        currentRank = "UNKNOWN", // Se actualiza desde backend
        nextRank = null,
        xpToNextRank = null
    )

    private fun StatsResponseDto.toEntity(userId: String) = LocalUserStatsEntity(
        userId = userId,
        totalXp = totalXp,
        totalSessions = totalSessions,
        totalBlocks = totalBlocks,
        totalDurationSeconds = totalDurationSeconds,
        avgBpmAllTime = avgBpmAllTime,
        lastSyncAt = System.currentTimeMillis()
    )
}