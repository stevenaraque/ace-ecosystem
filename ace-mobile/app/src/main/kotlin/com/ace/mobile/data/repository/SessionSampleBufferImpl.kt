// app/src/main/kotlin/com/ace/mobile/data/repository/SessionSampleBufferImpl.kt
package com.ace.mobile.data.repository

import android.util.Log
import com.ace.mobile.domain.model.HeartRateSample
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "SessionSampleBuffer"

@Singleton
class SessionSampleBufferImpl @Inject constructor() : SessionSampleBuffer {

    // FIX BUG 4: Usar LinkedHashSet con comparator por timestamp para deduplicar
    // y mantener orden cronológico. TreeSet ordena por timestamp automáticamente.
    private val samplesBySession = ConcurrentHashMap<String, java.util.TreeSet<HeartRateSample>>()

    @Volatile
    private var activeSessionId: String? = null

    // Flow para emitir samples en tiempo real a los observadores
    private val _sampleFlow = MutableSharedFlow<HeartRateSample>(extraBufferCapacity = 50)

    // FIX BUG 4: Métricas de deduplicación para debugging
    private val duplicateCountBySession = ConcurrentHashMap<String, Int>()

    override fun setActiveSessionId(sessionId: String?) {
        Log.i(TAG, "Active session set: $sessionId")
        activeSessionId = sessionId
    }

    override fun getActiveSessionId(): String? = activeSessionId

    override fun addSample(sessionId: String, sample: HeartRateSample) {
        // FIX BUG 4: Obtener o crear TreeSet ordenado por timestamp
        val set = samplesBySession.getOrPut(sessionId) {
            java.util.TreeSet(compareBy { it.timestamp })
        }

        // FIX BUG 4: add() retorna false si el elemento ya existe (mismo timestamp)
        val wasAdded = set.add(sample)

        if (wasAdded) {
            // Emitir al Flow solo si es un sample nuevo (no duplicado)
            _sampleFlow.tryEmit(sample)

            Log.d(
                TAG,
                "Sample added to $sessionId: bpm=${sample.bpm.toInt()}, " +
                        "timestamp=${sample.timestamp}, " +
                        "unique=${set.size}, " +
                        "duplicates=${duplicateCountBySession.getOrDefault(sessionId, 0)}"
            )
        } else {
            // FIX BUG 4: Track duplicados para debugging
            duplicateCountBySession.merge(sessionId, 1, Int::plus)
            Log.w(
                TAG,
                "DUPLICATE sample rejected for $sessionId: " +
                        "bpm=${sample.bpm.toInt()}, timestamp=${sample.timestamp}, " +
                        "duplicateCount=${duplicateCountBySession[sessionId]}"
            )
        }
    }

    override fun getSamples(sessionId: String): List<HeartRateSample> {
        // FIX BUG 4: TreeSet ya mantiene orden cronológico
        return samplesBySession[sessionId]?.toList() ?: emptyList()
    }

    override fun clear(sessionId: String) {
        val uniqueCount = samplesBySession[sessionId]?.size ?: 0
        val duplicateCount = duplicateCountBySession[sessionId] ?: 0
        val totalReceived = uniqueCount + duplicateCount

        samplesBySession.remove(sessionId)
        duplicateCountBySession.remove(sessionId)

        if (activeSessionId == sessionId) {
            activeSessionId = null
            Log.i(
                TAG,
                "Active session cleared: $sessionId | " +
                        "unique=$uniqueCount, duplicates=$duplicateCount, " +
                        "totalReceived=$totalReceived"
            )
        }
    }

    override fun observeSamples(): Flow<HeartRateSample> = _sampleFlow.asSharedFlow()

    // FIX BUG 4: Método de diagnóstico para ver estado del buffer
    fun getStats(sessionId: String): BufferStats {
        val unique = samplesBySession[sessionId]?.size ?: 0
        val duplicates = duplicateCountBySession[sessionId] ?: 0
        return BufferStats(uniqueCount = unique, duplicateCount = duplicates)
    }

    // FIX BUG 4: Data class para stats
    data class BufferStats(
        val uniqueCount: Int,
        val duplicateCount: Int
    ) {
        val totalReceived: Int get() = uniqueCount + duplicateCount
        val deduplicationRate: Double
            get() = if (totalReceived > 0) {
                duplicateCount.toDouble() / totalReceived
            } else 0.0
    }
}