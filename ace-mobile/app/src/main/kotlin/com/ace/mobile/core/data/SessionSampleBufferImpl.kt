package com.ace.mobile.core.data

import android.util.Log
import com.ace.mobile.core.database.dao.SessionDao
import com.ace.mobile.core.database.entity.LocalBlockEntity
import com.ace.mobile.core.model.HeartRateSample
import com.ace.mobile.core.model.ExerciseSession
import com.ace.mobile.feature.wear.domain.BuildExerciseBlockUseCase
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

    // ← NUEVO: Estado de pausa por sesión
    private val pausedSessions = ConcurrentHashMap<String, Boolean>()

    // ← NUEVO: Timestamp cuando se pausó (para calcular offset al reanudar)
    private val pauseTimestampBySession = ConcurrentHashMap<String, Long>()

    // ← NUEVO: Offset acumulado de tiempo pausado (ms)
    private val pausedOffsetBySession = ConcurrentHashMap<String, Long>()

    override fun setActiveSessionId(sessionId: String?) {
        Log.i(TAG, "=== setActiveSessionId: $sessionId ===")

        if (activeSessionId != null && activeSessionId != sessionId) {
            stopBlockTimer()
        }

        activeSessionId = sessionId

        if (sessionId != null) {
            blockCountBySession[sessionId] = 0
            pausedSessions[sessionId] = false
            pausedOffsetBySession[sessionId] = 0L
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
        pausedSessions.remove(sessionId)
        pauseTimestampBySession.remove(sessionId)
        pausedOffsetBySession.remove(sessionId)

        if (activeSessionId == sessionId) {
            activeSessionId = null
            stopBlockTimer()
            Log.i(TAG, "Session cleared: $sessionId")
        }
    }

    override fun observeSamples(): Flow<HeartRateSample> = _sampleFlow.asSharedFlow()

    override fun observeBlocks(): Flow<BlockSummary> = _blockFlow.asSharedFlow()

    // ← NUEVO: Pausar el timer del bloque SIN cerrarlo
    override fun pauseBlockTimer(sessionId: String) {
        if (activeSessionId != sessionId) {
            Log.w(TAG, "Cannot pause: $sessionId is not active")
            return
        }
        if (pausedSessions.getOrDefault(sessionId, false)) {
            Log.d(TAG, "Timer already paused for $sessionId")
            return
        }

        pausedSessions[sessionId] = true
        pauseTimestampBySession[sessionId] = System.currentTimeMillis()
        Log.i(TAG, "=== Timer PAUSED for $sessionId ===")
    }

    // ← NUEVO: Reanudar el timer del bloque
    override fun resumeBlockTimer(sessionId: String) {
        if (activeSessionId != sessionId) {
            Log.w(TAG, "Cannot resume: $sessionId is not active")
            return
        }
        if (!pausedSessions.getOrDefault(sessionId, false)) {
            Log.d(TAG, "Timer not paused for $sessionId")
            return
        }

        val pauseStart = pauseTimestampBySession[sessionId] ?: System.currentTimeMillis()
        val pauseDuration = System.currentTimeMillis() - pauseStart
        val currentOffset = pausedOffsetBySession.getOrDefault(sessionId, 0L)
        pausedOffsetBySession[sessionId] = currentOffset + pauseDuration

        pausedSessions[sessionId] = false
        pauseTimestampBySession.remove(sessionId)
        Log.i(TAG, "=== Timer RESUMED for $sessionId (paused for ${pauseDuration}ms, total offset=${pausedOffsetBySession[sessionId]}ms) ===")
    }

    // ← NUEVO: Verificar si el timer está pausado
    override fun isBlockTimerPaused(sessionId: String): Boolean {
        return pausedSessions.getOrDefault(sessionId, false)
    }

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

    // Timer basado en timestamps de muestras, con soporte de pausa
    private fun startBlockTimer(sessionId: String) {
        stopBlockTimer()
        blockCountBySession[sessionId] = 0

        blockTimerJob = scope.launch {
            Log.i(TAG, "=== Timer started for $sessionId ===")

            while (activeSessionId == sessionId) {
                // ← NUEVO: Si está pausado, solo esperar y continuar
                if (pausedSessions.getOrDefault(sessionId, false)) {
                    delay(500L)
                    continue
                }

                // Esperar a que haya al menos 2 muestras
                val samples = getSamples(sessionId)
                if (samples.size < 2) {
                    delay(500L)
                    continue
                }

                // Calcular duración basada en timestamps de las MUESTRAS (del reloj)
                // ← NUEVO: Restar el offset acumulado de tiempo pausado
                val firstTimestamp = samples.first().timestamp
                val lastTimestamp = samples.last().timestamp
                val rawDurationMs = lastTimestamp - firstTimestamp
                val pausedOffset = pausedOffsetBySession.getOrDefault(sessionId, 0L)
                val effectiveDurationMs = rawDurationMs - pausedOffset

                Log.d(TAG, "Checking: ${samples.size} samples, rawDuration=${rawDurationMs}ms, pausedOffset=${pausedOffset}ms, effective=${effectiveDurationMs}ms")

                // ¿Ya tenemos 60s efectivas de muestras?
                if (effectiveDurationMs >= BuildExerciseBlockUseCase.BLOCK_TIMER_MS) {
                    Log.d(TAG, "Effective duration reached ${BuildExerciseBlockUseCase.BLOCK_TIMER_MS}ms, closing block")

                    val summary = closeBlockInternal(sessionId)

                    if (summary != null) {
                        // ← NUEVO: Resetear offset al cerrar bloque
                        pausedOffsetBySession[sessionId] = 0L
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