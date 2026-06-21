package com.ace.mobile.domain.usecase.exercise

import android.util.Log
import com.ace.mobile.data.local.DeviceIdManager
import com.ace.mobile.data.local.database.dao.BlockDao
import com.ace.mobile.data.local.database.dao.UserDao
import com.ace.mobile.data.local.database.entity.LocalBlockEntity
import com.ace.mobile.data.remote.api.ExerciseApi
import com.ace.shared.constants.SyncConstants
import com.ace.shared.dto.ClientStatsDto
import com.ace.shared.dto.ExerciseBlockDto
import com.ace.shared.dto.SyncBatchRequestDto
import com.ace.shared.dto.XpAwardedResponseDto
import com.ace.shared.enums.BlockStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

private const val TAG = "SendPendingBlocksUC"

class SendPendingBlocksUseCase @Inject constructor(
    private val blockDao: BlockDao,
    private val userDao: UserDao,
    private val exerciseApi: ExerciseApi,
    private val deviceIdManager: DeviceIdManager
) {

    sealed class Result {
        data class Success(val syncedCount: Int, val errorCount: Int) : Result()
        data object NoPendingBlocks : Result()
        data class NetworkError(val message: String) : Result()
        data class AuthError(val message: String) : Result()
    }

    suspend operator fun invoke(sessionId: String? = null): Result = withContext(Dispatchers.IO) {

        val pendingBlocks = blockDao.getPendingBlocks(SyncConstants.BATCH_MAX_SIZE)

        if (pendingBlocks.isEmpty()) {
            Log.d(TAG, "No pending blocks to sync")
            return@withContext Result.NoPendingBlocks
        }

        Log.i(TAG, "Syncing ${pendingBlocks.size} pending blocks")

        val syncingIds = pendingBlocks.map { it.blockId }
        blockDao.updateStatus(syncingIds, BlockStatus.SYNCING.name)

        val blockDtos = pendingBlocks.map { it.toDto() }
        val deviceId = deviceIdManager.deviceId
        val user = userDao.getCurrentUser()

        // v1.0.5: Ahora podemos pasar sessionId, sentAt y schemaVersion
        val request = SyncBatchRequestDto(
            blocks = blockDtos,
            clientStats = buildClientStats(pendingBlocks, user?.userId),
            deviceId = deviceId,
            sessionId = sessionId ?: pendingBlocks.first().sessionId,
            sentAt = System.currentTimeMillis(),
            schemaVersion = SyncConstants.CURRENT_SCHEMA_VERSION
        )

        return@withContext try {
            val response = exerciseApi.syncBatch(request)
            resolveResponse(response, pendingBlocks)
        } catch (e: Exception) {
            Log.e(TAG, "Network error during sync", e)
            blockDao.updateStatus(syncingIds, BlockStatus.PENDING.name)
            Result.NetworkError(e.message ?: "Unknown network error")
        }
    }

    private suspend fun resolveResponse(
        response: retrofit2.Response<com.ace.shared.dto.SyncBatchResponseDto>,
        originalBlocks: List<LocalBlockEntity>
    ): Result {

        when (response.code()) {
            201 -> {
                val body = response.body()
                if (body == null) {
                    Log.w(TAG, "Empty response body")
                    blockDao.updateStatus(originalBlocks.map { it.blockId }, BlockStatus.PENDING.name)
                    return Result.NetworkError("Empty response body")
                }

                // v1.0.5: Usar xpDetails para granularidad por bloque
                val xpDetails = body.xpDetails.associateBy { it.blockId }
                val acceptedIds = body.acceptedBlocks.toSet()
                val rejectedIds = body.rejectedBlocks.map { it.blockId }.toSet()

                var syncedCount = 0
                var errorCount = 0

                for (block in originalBlocks) {
                    val xpDetail = xpDetails[block.blockId]

                    when {
                        block.blockId in acceptedIds -> {
                            blockDao.updateStatus(listOf(block.blockId), BlockStatus.SYNCED.name)
                            syncedCount++

                            // v1.0.5: Actualizar XP total local con balanceAfter
                            xpDetail?.let {
                                userDao.updateTotalXp(block.userId, it.balanceAfter.toInt())
                            }

                            Log.d(TAG, "Block ${block.blockId} synced, balance=${xpDetail?.balanceAfter}")
                        }

                        block.blockId in rejectedIds -> {
                            blockDao.updateStatus(listOf(block.blockId), BlockStatus.ERROR.name)
                            errorCount++

                            val rejected = body.rejectedBlocks.find { it.blockId == block.blockId }
                            Log.w(TAG, "Block ${block.blockId} rejected: ${rejected?.reason}")
                        }

                        else -> {
                            // No mencionado en respuesta: volver a PENDING
                            blockDao.updateStatus(listOf(block.blockId), BlockStatus.PENDING.name)
                            Log.w(TAG, "Block ${block.blockId} not mentioned in response")
                        }
                    }
                }

                // v1.0.5: Actualizar streak cache
                userDao.updateStreakCache(
                    originalBlocks.first().userId,
                    body.streakState.currentStreak,
                    body.streakState.bestStreak,
                    body.streakState.lastExerciseDate?.let { parseDateToMillis(it) }
                )

                // v1.0.5: Invalidar ranking cache si rankChanged
                if (body.rankChanged) {
                    Log.i(TAG, "Rank changed! Invalidating ranking cache")
                    // TODO: Invalidar cache de ranking
                }

                Log.i(TAG, "Sync resolved: $syncedCount synced, $errorCount errors")
                return Result.Success(syncedCount, errorCount)
            }

            401 -> {
                Log.e(TAG, "Auth error 401 after refresh attempt")
                blockDao.updateStatus(originalBlocks.map { it.blockId }, BlockStatus.PENDING.name)
                return Result.AuthError("Authentication failed")
            }

            422 -> {
                Log.e(TAG, "Batch rejected 422")
                blockDao.updateStatus(originalBlocks.map { it.blockId }, BlockStatus.ERROR.name)
                return Result.Success(0, originalBlocks.size)
            }

            else -> {
                Log.e(TAG, "Server error ${response.code()}: ${response.errorBody()?.string()}")
                blockDao.updateStatus(originalBlocks.map { it.blockId }, BlockStatus.PENDING.name)
                return Result.NetworkError("Server error ${response.code()}")
            }
        }
    }

    private fun buildClientStats(blocks: List<LocalBlockEntity>, userId: String?): ClientStatsDto {
        val totalXp = blocks.sumOf { (it.xpCalculated ?: 0).toLong() }
        val totalDuration = blocks.sumOf { it.durationSeconds.toLong() }
        val avgBpm = if (blocks.isNotEmpty()) {
            blocks.map { it.avgBpm }.average()
        } else 0.0

        // v1.0.5: Ahora podemos pasar userId, lastSyncAt y schemaVersion
        return ClientStatsDto(
            totalXp = totalXp,
            totalSessions = 1,
            totalBlocks = blocks.size,
            totalDurationSeconds = totalDuration,
            avgBpmAllTime = avgBpm,
            userId = userId,
            lastSyncAt = System.currentTimeMillis(),
            schemaVersion = SyncConstants.CURRENT_SCHEMA_VERSION
        )
    }

    private fun parseDateToMillis(dateString: String): Long? {
        return try {
            java.time.Instant.parse(dateString).toEpochMilli()
        } catch (e: Exception) {
            null
        }
    }
}

private fun LocalBlockEntity.toDto(): ExerciseBlockDto {
    return ExerciseBlockDto(
        blockId = this.blockId,
        sessionId = this.sessionId,
        userId = this.userId,
        deviceId = "", // Se pone en el SyncBatchRequestDto
        sportType = this.sportType,
        timestampStart = this.timestampStart,
        timestampEnd = this.timestampEnd,
        durationSeconds = this.durationSeconds,
        avgBpm = this.avgBpm,
        maxBpm = this.maxBpm,
        minBpm = this.minBpm,
        sampleCount = this.sampleCount,
        xpCalculated = this.xpCalculated ?: 0,
        schemaVersion = SyncConstants.CURRENT_SCHEMA_VERSION
    )
}