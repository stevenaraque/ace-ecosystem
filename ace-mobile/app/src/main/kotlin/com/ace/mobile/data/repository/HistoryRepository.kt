package com.ace.mobile.data.repository

import android.util.Log
import com.ace.mobile.data.local.database.dao.SessionHistoryDao
import com.ace.mobile.data.local.database.dao.UserDao
import com.ace.mobile.data.local.database.entity.LocalSessionHistoryEntity
import com.ace.mobile.data.remote.api.HistoryApi
import com.ace.shared.dto.SessionHistoryEntryDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRepository @Inject constructor(
    private val historyApi: HistoryApi,
    private val historyDao: SessionHistoryDao,
    private val userDao: UserDao
) {

    companion object {
        private const val TAG = "HistoryRepository"
        private const val LOCAL_CACHE_SIZE = 5
    }

    /**
     * Obtiene historial: primero local (FIFO 5), luego backend (top 20).
     */
    fun getHistory(limit: Int = 20, forceRefresh: Boolean = false): Flow<Result<List<SessionHistoryEntryDto>>> = flow {
        val userId = userDao.getCurrentUser()?.userId
            ?: run {
                emit(Result.failure(IllegalStateException("No user logged in")))
                return@flow
            }

        // 1. Emitir cache local primero (FIFO 5)
        val localHistory = historyDao.observeHistory(userId, LOCAL_CACHE_SIZE).firstOrNull()
        if (!localHistory.isNullOrEmpty() && !forceRefresh) {
            Log.d(TAG, "Emitting ${localHistory.size} local history entries")
            emit(Result.success(localHistory.map { it.toDto() }))
        }

        // 2. Fetch del backend (top 20)
        try {
            val response = historyApi.getHistory(limit)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    // Guardar en Room (FIFO: mantener solo 5)
                    val entities = body.map { it.toEntity(userId) }
                    historyDao.insertAll(entities)
                    historyDao.trimHistory(userId, LOCAL_CACHE_SIZE)
                    Log.d(TAG, "History refreshed from backend: ${body.size} entries")
                    emit(Result.success(body))
                } else {
                    emit(Result.failure(IllegalStateException("Empty response body")))
                }
            } else {
                Log.w(TAG, "History fetch failed: ${response.code()}")
                if (localHistory.isNullOrEmpty()) {
                    emit(Result.failure(IllegalStateException("HTTP ${response.code()}")))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "History fetch exception", e)
            if (localHistory.isNullOrEmpty()) {
                emit(Result.failure(e))
            }
        }
    }

    private fun LocalSessionHistoryEntity.toDto() = SessionHistoryEntryDto(
        sessionId = sessionId,
        timestampStart = timestampStart,
        timestampEnd = timestampEnd,
        sportType = sportType,
        durationSeconds = durationSeconds,
        totalXp = totalXp,
        totalBlocks = totalBlocks,
        avgBpm = avgBpm
    )

    private fun SessionHistoryEntryDto.toEntity(userId: String) = LocalSessionHistoryEntity(
        sessionId = sessionId,
        userId = userId,
        timestampStart = timestampStart,
        timestampEnd = timestampEnd,
        sportType = sportType,
        durationSeconds = durationSeconds,
        totalXp = totalXp,
        totalBlocks = totalBlocks,
        avgBpm = avgBpm
    )
}