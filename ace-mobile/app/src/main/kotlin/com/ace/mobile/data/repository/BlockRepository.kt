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

/**
 * Repositorio de bloques de ejercicio.
 * Orquesta el DAO y dispara sincronización cuando se cierra un bloque.
 *
 * v1.0.5: Al cerrar un bloque, dispara automáticamente SyncBlockWorker.
 */
@Singleton
class BlockRepository @Inject constructor(
    private val blockDao: BlockDao,
    @ApplicationContext private val context: Context
) {

    /**
     * Inserta un bloque cerrado en la base de datos y dispara sync.
     *
     * @param block Bloque cerrado con métricas agregadas y XP calculada
     * @return ID del bloque insertado
     */
    suspend fun insertClosedBlock(block: LocalBlockEntity): String = withContext(Dispatchers.IO) {
        require(block.status == BlockStatus.PENDING) {
            "Block must be PENDING to be inserted as closed"
        }

        blockDao.insert(block)
        Log.i(TAG, "Block ${block.blockId} inserted (session=${block.sessionId})")

        // v1.0.5: Disparar sync automáticamente
        ExerciseSyncService.notifyBlockClosed(context, block.sessionId)

        return@withContext block.blockId
    }

    /**
     * Inserta múltiples bloques y dispara un solo sync.
     */
    suspend fun insertClosedBlocks(blocks: List<LocalBlockEntity>): List<String> = withContext(Dispatchers.IO) {
        blocks.forEach { require(it.status == BlockStatus.PENDING) }

        blockDao.insertAll(blocks)
        Log.i(TAG, "${blocks.size} blocks inserted")

        // Disparar sync una sola vez para todo el batch
        blocks.firstOrNull()?.let {
            ExerciseSyncService.notifyBlockClosed(context, it.sessionId)
        }

        return@withContext blocks.map { it.blockId }
    }

    /**
     * Obtiene bloques pendientes para sync.
     */
    suspend fun getPendingBlocks(limit: Int = 20): List<LocalBlockEntity> {
        return blockDao.getPendingBlocks(limit)
    }

    /**
     * Actualiza estado de bloques (usado por SyncBlockWorker).
     */
    suspend fun updateStatus(blockIds: List<String>, status: BlockStatus) {
        blockDao.updateStatus(blockIds, status.name)
    }

    /**
     * Cuenta bloques por estado.
     */
    suspend fun countByStatus(status: BlockStatus): Int {
        return blockDao.countByStatus(status.name)
    }

    /**
     * Limpieza de bloques SYNCED antiguos.
     */
    suspend fun cleanupSyncedBlocks(olderThan: Long) {
        val deleted = blockDao.deleteSyncedOlderThan(olderThan)
        Log.d(TAG, "Deleted $deleted old synced blocks")
    }
}