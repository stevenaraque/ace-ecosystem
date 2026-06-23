// app/src/main/kotlin/com/ace/mobile/data/repository/SessionSampleBufferImpl.kt
package com.ace.mobile.data.repository

import android.util.Log
import com.ace.mobile.data.local.database.dao.SessionDao
import com.ace.mobile.data.local.database.entity.LocalBlockEntity
import com.ace.mobile.domain.model.HeartRateSample
import com.ace.mobile.domain.model.ExerciseSession
import com.ace.mobile.domain.usecase.wear.BuildExerciseBlockUseCase
import com.ace.shared.enums.BlockStatus
import com.ace.shared.enums.SessionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "SessionSampleBuffer"

@Singleton
class SessionSampleBufferImpl @Inject constructor(
    private val buildExerciseBlockUseCase: BuildExerciseBlockUseCase,
    private val blockRepository: BlockRepository,
    private val sessionDao: SessionDao
) : SessionSampleBuffer {

    private val samplesBySession = ConcurrentHashMap<String, MutableSet<HeartRateSample>>()

    @Volatile
    private var activeSessionId: String? = null

    private val _sampleFlow = MutableSharedFlow<HeartRateSample>(extraBufferCapacity = 50)
    private val _blockFlow = MutableSharedFlow<BlockSummary>(extraBufferCapacity = 10)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var blockTimerJob: Job? = null
    private val blockCountBySession = ConcurrentHashMap<String, Int>()

    private val duplicateCountBySession = ConcurrentHashMap<String, Int>()
    private val bufferLock = Any()

    override fun setActiveSessionId(sessionId: String?) {
        Log.i(TAG, "=== setActiveSessionId: $sessionId ===")

        if (activeSessionId != null && activeSessionId != sessionId) {
            stopBlockTimer()
        }

        activeSessionId = sessionId

        if (sessionId != null) {
            blockCountBySession[sessionId] = 0
            startBlockTimer(sessionId)
        }
    }

    override fun getActiveSessionId(): String? = activeSessionId

    override fun getBlockCount(sessionId: String): Int {
        return blockCountBySession.getOrDefault(sessionId, 0)
    }

    override fun addSample(sessionId: String, sample: HeartRateSample) {
        synchronized(bufferLock) {
            val set = samplesBySession.getOrPut(sessionId) {
                java.util.Collections.synchronizedSet(
                    java.util.TreeSet(compareBy { it.timestamp })
                )
            }

            val wasAdded = set.add(sample)

            if (wasAdded) {
                _sampleFlow.tryEmit(sample)
                Log.d(TAG, "Sample added: session=$sessionId, bpm=${sample.bpm.toInt()}, total=${set.size}")
            } else {
                duplicateCountBySession.merge(sessionId, 1, Int::plus)
                Log.w(TAG, "DUPLICATE rejected: session=$sessionId")
            }
        }
    }

    override fun getSamples(sessionId: String): List<HeartRateSample> {
        return synchronized(bufferLock) {
            samplesBySession[sessionId]?.toList()?.sortedBy { it.timestamp } ?: emptyList()
        }
    }

    override fun clear(sessionId: String) {
        Log.i(TAG, "=== clear($sessionId) ===")
        synchronized(bufferLock) {
            samplesBySession.remove(sessionId)
        }
        duplicateCountBySession.remove(sessionId)
        blockCountBySession.remove(sessionId)

        if (activeSessionId == sessionId) {
            activeSessionId = null
            stopBlockTimer()
            Log.i(TAG, "Session cleared: $sessionId")
        }
    }

    override fun observeSamples(): Flow<HeartRateSample> = _sampleFlow.asSharedFlow()

    override fun observeBlocks(): Flow<BlockSummary> = _blockFlow.asSharedFlow()

    override suspend fun forceCloseBlock(sessionId: String): BlockSummary? {
        Log.i(TAG, "=== forceCloseBlock($sessionId) ===")
        stopBlockTimer()

        val samples = getSamples(sessionId)
        val currentCount = blockCountBySession.getOrDefault(sessionId, 0)
        val isFirstBlock = currentCount == 0

        // REGLA: Primer bloque necesita mínimo 20 segundos
        if (isFirstBlock && samples.size >= 2) {
            val durationMs = samples.last().timestamp - samples.first().timestamp
            val MIN_FIRST_BLOCK_MS = 20_000L
            if (durationMs < MIN_FIRST_BLOCK_MS) {
                Log.w(TAG, "PRIMER BLOQUE RECHAZADO: ${durationMs}ms < ${MIN_FIRST_BLOCK_MS}ms (mínimo 20s), descartando")
                synchronized(bufferLock) { samplesBySession.remove(sessionId) }
                return null
            }
        }

        // Bloque #2 en adelante: siempre válidos, cualquier duración
        val result = closeBlockInternal(sessionId)
        Log.i(TAG, "forceCloseBlock result: ${result?.let { "Block #${it.blockCount}, XP=${it.xpGained}" } ?: "null"}")
        return result
    }

    // 🔧 NUEVO: Timer basado en timestamps de muestras, no en System.currentTimeMillis()
    private fun startBlockTimer(sessionId: String) {
        stopBlockTimer()
        blockCountBySession[sessionId] = 0

        blockTimerJob = scope.launch {
            Log.i(TAG, "=== Timer started for $sessionId ===")

            while (activeSessionId == sessionId) {
                // Esperar a que haya al menos 2 muestras
                val samples = getSamples(sessionId)
                if (samples.size < 2) {
                    delay(500L)
                    continue
                }

                // Calcular duración basada en timestamps de las MUESTRAS (del reloj)
                val firstTimestamp = samples.first().timestamp
                val lastTimestamp = samples.last().timestamp
                val durationMs = lastTimestamp - firstTimestamp

                Log.d(TAG, "Checking: ${samples.size} samples, duration=${durationMs}ms")

                // ¿Ya tenemos 60s de muestras?
                if (durationMs >= BuildExerciseBlockUseCase.BLOCK_TIMER_MS) {
                    Log.d(TAG, "Duration reached ${BuildExerciseBlockUseCase.BLOCK_TIMER_MS}ms, closing block")

                    val summary = closeBlockInternal(sessionId)

                    if (summary != null) {
                        Log.d(TAG, "Block closed, starting next block")
                    } else {
                        Log.d(TAG, "Block close returned null, retrying in 1s...")
                        delay(1000)
                    }
                } else {
                    // No llegamos a 60s, esperar un poco y revisar
                    delay(500L)
                }

                if (activeSessionId != sessionId) {
                    Log.d(TAG, "Session changed, breaking timer")
                    break
                }
            }

            Log.i(TAG, "=== Timer ended for $sessionId ===")
        }
    }

    private fun stopBlockTimer() {
        blockTimerJob?.cancel()
        blockTimerJob = null
        Log.i(TAG, "Timer stopped")
    }

    private suspend fun closeBlockInternal(sessionId: String): BlockSummary? {
        Log.d(TAG, "=== closeBlockInternal: session=$sessionId ===")

        val samples = synchronized(bufferLock) {
            samplesBySession[sessionId]?.toList()?.sortedBy { it.timestamp } ?: emptyList()
        }

        if (samples.isEmpty()) {
            Log.w(TAG, "No samples to close block")
            return null
        }

        val currentCount = blockCountBySession.getOrDefault(sessionId, 0)

        Log.d(TAG, "currentCount=$currentCount, samples=${samples.size}")

        val sessionEntity = sessionDao.getSessionById(sessionId)
        if (sessionEntity == null) {
            Log.e(TAG, "Session $sessionId not found in DB!")
            return null
        }

        val session = ExerciseSession(
            sessionId = sessionEntity.sessionId,
            userId = sessionEntity.userId,
            deviceId = sessionEntity.deviceId,
            status = SessionStatus.ACTIVE,
            sportType = com.ace.shared.enums.SportType.valueOf(sessionEntity.sportType),
            timestampStart = sessionEntity.timestampStart,
            timestampEnd = null,
            totalBlocks = sessionEntity.totalBlocks,
            totalXp = sessionEntity.totalXp
        )

        // Ya no pasamos isForced, el use case solo calcula
        val blockResult = buildExerciseBlockUseCase(session, samples)

        if (blockResult == null) {
            Log.w(TAG, "Bloque RECHAZADO (no samples)")
            return null
        }

        Log.i(TAG, "Bloque ACEPTADO: id=${blockResult.blockId}, duration=${blockResult.durationSeconds}s, xp=${blockResult.xpCalculated}")

        val blockEntity = LocalBlockEntity(
            blockId = blockResult.blockId,
            sessionId = blockResult.sessionId,
            userId = blockResult.userId,
            timestampStart = blockResult.timestampStart,
            timestampEnd = blockResult.timestampEnd,
            durationSeconds = blockResult.durationSeconds,
            avgBpm = blockResult.avgBpm,
            maxBpm = blockResult.maxBpm,
            minBpm = blockResult.minBpm,
            sampleCount = blockResult.sampleCount,
            sportType = blockResult.sportType,
            xpCalculated = blockResult.xpCalculated.toInt(),
            status = BlockStatus.PENDING
        )

        blockRepository.insertClosedBlock(blockEntity)
        Log.i(TAG, "Bloque guardado en BD: ${blockResult.blockId}")

        synchronized(bufferLock) { samplesBySession.remove(sessionId) }
        Log.d(TAG, "Buffer limpiado para siguiente bloque")

        val newCount = currentCount + 1
        blockCountBySession[sessionId] = newCount

        val summary = BlockSummary(
            blockCount = newCount,
            xpGained = blockResult.xpCalculated
        )

        _blockFlow.emit(summary)
        Log.i(TAG, "=== Bloque #$newCount cerrado: XP=${blockResult.xpCalculated} ===")

        return summary
    }
}