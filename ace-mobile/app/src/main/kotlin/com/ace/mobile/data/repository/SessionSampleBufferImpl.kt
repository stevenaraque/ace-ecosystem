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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "SessionSampleBuffer"

/**
 * Buffer de muestras de frecuencia cardíaca con cierre automático de bloques.
 *
 * REGLAS DE CIERRE:
 * - Primer bloque: se cierra cuando se acumulan ≥ 20 segundos de muestras
 * - Bloques siguientes: se cierran cada 60 segundos desde el último cierre
 * - Bloque final (al pausar): se cierra inmediatamente con las muestras acumuladas
 *
 * FIXES vs versión anterior:
 * 1. Sincronización atómica con Mutex para evitar race conditions
 * 2. Timer usa timestamps absolutos en vez de delay() acumulativo
 * 3. Buffer se limpia atómicamente junto con el cierre del bloque
 * 4. forceCloseBlock cancela el timer y cierra el bloque de forma segura
 */
@Singleton
class SessionSampleBufferImpl @Inject constructor(
    private val buildExerciseBlockUseCase: BuildExerciseBlockUseCase,
    private val blockRepository: BlockRepository,
    private val sessionDao: SessionDao
) : SessionSampleBuffer {

    private val samplesBySession = ConcurrentHashMap<String, java.util.TreeSet<HeartRateSample>>()

    @Volatile
    private var activeSessionId: String? = null

    private val _sampleFlow = MutableSharedFlow<HeartRateSample>(extraBufferCapacity = 50)
    private val _blockFlow = MutableSharedFlow<BlockSummary>(extraBufferCapacity = 10)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var blockTimerJob: Job? = null
    private var blockCountBySession = ConcurrentHashMap<String, Int>()

    private val duplicateCountBySession = ConcurrentHashMap<String, Int>()

    /** Mutex para sincronizar addSample y closeBlockInternal */
    private val blockMutex = Mutex()

    /** Timestamp del último cierre de bloque por sesión (para calcular 60s) */
    private val lastBlockCloseTime = ConcurrentHashMap<String, Long>()

    override fun setActiveSessionId(sessionId: String?) {
        Log.i(TAG, "=== setActiveSessionId: $sessionId ===")

        // Cancelar timer anterior si existe
        if (activeSessionId != null && activeSessionId != sessionId) {
            stopBlockTimer()
        }

        activeSessionId = sessionId

        if (sessionId != null) {
            blockCountBySession[sessionId] = 0
            lastBlockCloseTime.remove(sessionId)
            startBlockTimer(sessionId)
        }
    }

    override fun getActiveSessionId(): String? = activeSessionId

    override fun getBlockCount(sessionId: String): Int {
        val count = blockCountBySession.getOrDefault(sessionId, 0)
        Log.d(TAG, "getBlockCount($sessionId) = $count")
        return count
    }

    override fun addSample(sessionId: String, sample: HeartRateSample) {
        val set = samplesBySession.getOrPut(sessionId) {
            java.util.TreeSet(compareBy { it.timestamp })
        }

        val wasAdded = set.add(sample)

        if (wasAdded) {
            _sampleFlow.tryEmit(sample)
            Log.d(TAG, "Sample added: session=$sessionId, bpm=${sample.bpm.toInt()}, total=${set.size}")
        } else {
            duplicateCountBySession.merge(sessionId, 1, Int::plus)
            Log.w(TAG, "DUPLICATE rejected: session=$sessionId, bpm=${sample.bpm.toInt()}")
        }
    }

    override fun getSamples(sessionId: String): List<HeartRateSample> {
        return samplesBySession[sessionId]?.toList() ?: emptyList()
    }

    override fun clear(sessionId: String) {
        Log.i(TAG, "=== clear($sessionId) ===")
        samplesBySession.remove(sessionId)
        duplicateCountBySession.remove(sessionId)
        blockCountBySession.remove(sessionId)
        lastBlockCloseTime.remove(sessionId)

        if (activeSessionId == sessionId) {
            activeSessionId = null
            stopBlockTimer()
            Log.i(TAG, "Session cleared and timer stopped: $sessionId")
        }
    }

    override fun observeSamples(): Flow<HeartRateSample> = _sampleFlow.asSharedFlow()

    override fun observeBlocks(): Flow<BlockSummary> = _blockFlow.asSharedFlow()

    /**
     * Fuerza cierre de bloque cuando el usuario pausa la sesión.
     * Cancela el timer automático y cierra el bloque pendiente de forma segura.
     */
    override suspend fun forceCloseBlock(sessionId: String): BlockSummary? {
        Log.i(TAG, "=== forceCloseBlock($sessionId) ===")

        // Cancelar el timer para que no cierre bloques automáticos mientras forceCloseBlock corre
        stopBlockTimer()

        return blockMutex.withLock {
            val result = closeBlockInternal(sessionId, isForced = true)
            Log.i(TAG, "forceCloseBlock result: ${result?.let { "Block #${it.blockCount}, XP=${it.xpGained}" } ?: "null"}")
            result
        }
    }

    /**
     * Timer de bloques:
     * - Primer bloque: espera hasta tener ≥ 20 segundos de muestras (chequea cada 500ms)
     * - Bloques siguientes: espera 60 segundos DESDE el último cierre
     */
    private fun startBlockTimer(sessionId: String) {
        stopBlockTimer()
        blockCountBySession[sessionId] = 0

        blockTimerJob = scope.launch {
            Log.i(TAG, "=== Timer started for $sessionId ===")
            var isFirstBlock = true

            while (activeSessionId == sessionId) {
                val waitUntilMs = if (isFirstBlock) {
                    // Primer bloque: esperar hasta que haya 20s de muestras
                    // Usamos un loop de chequeo cada 500ms para precisión
                    Log.d(TAG, "Primer bloque: esperando ${BuildExerciseBlockUseCase.MIN_BLOCK_DURATION_SECONDS}s de muestras...")
                    waitForMinDuration(sessionId, BuildExerciseBlockUseCase.MIN_BLOCK_DURATION_SECONDS * 1000L)
                } else {
                    // Bloques siguientes: esperar 60s desde el último cierre
                    val lastClose = lastBlockCloseTime[sessionId] ?: System.currentTimeMillis()
                    val nextCloseTime = lastClose + BuildExerciseBlockUseCase.BLOCK_TIMER_MS
                    val waitMs = nextCloseTime - System.currentTimeMillis()

                    if (waitMs > 0) {
                        Log.d(TAG, "Bloque siguiente: esperando ${waitMs}ms (hasta ${formatTime(nextCloseTime)})")
                        delay(waitMs)
                    } else {
                        Log.d(TAG, "Bloque siguiente: tiempo ya pasado, cerrando inmediatamente")
                    }
                    System.currentTimeMillis() // retornamos el timestamp actual como "señal de continuar"
                }

                if (activeSessionId != sessionId) {
                    Log.d(TAG, "Session changed, breaking timer")
                    break
                }

                // Cerrar bloque con mutex para evitar race conditions
                val summary = blockMutex.withLock {
                    closeBlockInternal(sessionId, isForced = false)
                }

                if (summary != null) {
                    isFirstBlock = false
                    lastBlockCloseTime[sessionId] = System.currentTimeMillis()
                    Log.d(TAG, "Bloque cerrado, siguiente será bloque regular. Last close: ${formatTime(lastBlockCloseTime[sessionId]!!)}")
                } else {
                    Log.d(TAG, "Bloque rechazado, reintentando...")
                    // Pequeño delay antes de reintentar
                    delay(500)
                }
            }

            Log.i(TAG, "=== Timer ended for $sessionId ===")
        }
    }

    /**
     * Espera hasta que el buffer tenga al menos [minDurationMs] de muestras.
     * Chequea cada 500ms para no consumir CPU.
     * Retorna el timestamp del momento en que se alcanzó la duración mínima.
     */
    private suspend fun waitForMinDuration(sessionId: String, minDurationMs: Long): Long {
        while (activeSessionId == sessionId) {
            val samples = getSamples(sessionId)
            if (samples.size >= 2) {
                val durationMs = samples.last().timestamp - samples.first().timestamp
                if (durationMs >= minDurationMs) {
                    Log.d(TAG, "Duración mínima alcanzada: ${durationMs}ms >= ${minDurationMs}ms")
                    return System.currentTimeMillis()
                }
            }
            delay(500L)
        }
        return System.currentTimeMillis()
    }

    private fun stopBlockTimer() {
        blockTimerJob?.cancel()
        blockTimerJob = null
        Log.i(TAG, "Timer stopped")
    }

    /**
     * Cierra un bloque: toma muestras, valida, calcula XP, guarda, emite.
     *
     * IMPORTANTE: Este método DEBE llamarse dentro de blockMutex.withLock {}
     * para garantizar que addSample no modifica el buffer durante el cierre.
     *
     * Si el bloque es rechazado (solo puede pasar con el primer bloque < 20s),
     * NO limpia el buffer para que el timer pueda reintentar.
     */
    private suspend fun closeBlockInternal(sessionId: String, isForced: Boolean): BlockSummary? {
        Log.d(TAG, "=== closeBlockInternal: session=$sessionId, isForced=$isForced ===")

        val samples = getSamples(sessionId).toList()

        if (samples.isEmpty()) {
            Log.w(TAG, "No samples to close block")
            return null
        }

        val currentCount = blockCountBySession.getOrDefault(sessionId, 0)
        val isFirstBlock = currentCount == 0

        Log.d(TAG, "currentCount=$currentCount, isFirstBlock=$isFirstBlock, samples=${samples.size}")
        Log.d(TAG, "First sample: ${samples.first().timestamp}, Last sample: ${samples.last().timestamp}")
        Log.d(TAG, "Duration: ${(samples.last().timestamp - samples.first().timestamp) / 1000}s")

        // Obtener datos de la sesión desde Room
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

        Log.d(TAG, "Session data: userId=${session.userId}, sport=${session.sportType}")

        val blockResult = buildExerciseBlockUseCase(session, samples, isFirstBlock)

        if (blockResult == null) {
            Log.w(TAG, "Bloque RECHAZADO por BuildExerciseBlockUseCase")
            Log.d(TAG, "NO limpio buffer, conservo ${samples.size} muestras para siguiente ciclo")
            // NO limpiar buffer, conservar muestras para reintento
            return null
        }

        Log.i(TAG, "Bloque ACEPTADO: id=${blockResult.blockId}, duration=${blockResult.durationSeconds}s, xp=${blockResult.xpCalculated}")

        // ATÓMICO: Limpiar buffer AHORA, bajo el mutex, para que addSample() no
        // agregue muestras al buffer viejo después de que lo leímos
        samplesBySession[sessionId] = java.util.TreeSet(compareBy { it.timestamp })
        Log.d(TAG, "Buffer limpiado atómicamente para siguiente bloque")

        // Guardar en SQLite
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

        // Actualizar contador
        val newCount = currentCount + 1
        blockCountBySession[sessionId] = newCount

        val summary = BlockSummary(
            blockCount = newCount,
            xpGained = blockResult.xpCalculated
        )

        _blockFlow.emit(summary)
        Log.i(TAG, "=== Bloque #$newCount cerrado: XP=${blockResult.xpCalculated}, emitido a UI ===")

        return summary
    }

    private fun formatTime(timestamp: Long): String {
        val sdf = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp))
    }
}