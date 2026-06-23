// app/src/main/kotlin/com/ace/mobile/data/repository/BlockRepository.kt
package com.ace.mobile.data.repository

import android.content.Context
import android.util.Log
import com.ace.mobile.data.local.database.dao.BlockDao
import com.ace.mobile.data.local.database.entity.LocalBlockEntity
import com.ace.mobile.service.ExerciseSyncService
import com.ace.shared.enums.BlockStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "BlockRepository"

@Singleton
class BlockRepository @Inject constructor(
    private val blockDao: BlockDao,
    @ApplicationContext private val context: Context
) {

    suspend fun insertClosedBlock(block: LocalBlockEntity): String = withContext(Dispatchers.IO) {
        require(block.status == BlockStatus.PENDING) {
            "Block must be PENDING to be inserted as closed"
        }

        blockDao.insert(block)
        Log.i(TAG, "Block ${block.blockId} inserted (session=${block.sessionId}, xp=${block.xpCalculated})")

        // FIX #7: try/catch para que el bloque no se pierda si el servicio falla
        try {
            ExerciseSyncService.notifyBlockClosed(context, block.sessionId)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to notify sync service: ${e.message}")
        }

        return@withContext block.blockId
    }

    suspend fun insertClosedBlocks(blocks: List<LocalBlockEntity>): List<String> = withContext(Dispatchers.IO) {
        blocks.forEach { require(it.status == BlockStatus.PENDING) }

        blockDao.insertAll(blocks)
        Log.i(TAG, "${blocks.size} blocks inserted")

        blocks.firstOrNull()?.let { firstBlock ->
            try {
                ExerciseSyncService.notifyBlockClosed(context, firstBlock.sessionId)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to notify sync service: ${e.message}")
            }
        }

        return@withContext blocks.map { it.blockId }
    }

    suspend fun getPendingBlocks(limit: Int = 20): List<LocalBlockEntity> {
        return blockDao.getPendingBlocks(limit)
    }

    suspend fun getBlocksBySession(sessionId: String): List<LocalBlockEntity> {
        return blockDao.getBlocksBySession(sessionId)
    }

    suspend fun updateStatus(blockIds: List<String>, status: BlockStatus) {
        blockDao.updateStatus(blockIds, status.name)
    }

    suspend fun countByStatus(status: BlockStatus): Int {
        return blockDao.countByStatus(status.name)
    }

    suspend fun cleanupSyncedBlocks(olderThan: Long) {
        val deleted = blockDao.deleteSyncedOlderThan(olderThan)
        Log.d(TAG, "Deleted $deleted old synced blocks")
    }
}