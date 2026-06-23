// app/src/main/kotlin/com/ace/mobile/data/repository/SessionSampleBuffer.kt
package com.ace.mobile.data.repository

import com.ace.mobile.domain.model.HeartRateSample
import kotlinx.coroutines.flow.Flow

data class BlockSummary(
    val blockCount: Int,
    val xpGained: Double
)

interface SessionSampleBuffer {
    fun setActiveSessionId(sessionId: String?)
    fun getActiveSessionId(): String?
    fun addSample(sessionId: String, sample: HeartRateSample)  // ← NO suspend
    fun getSamples(sessionId: String): List<HeartRateSample>
    fun clear(sessionId: String)
    fun observeSamples(): Flow<HeartRateSample>
    fun observeBlocks(): Flow<BlockSummary>
    fun getBlockCount(sessionId: String): Int
    suspend fun forceCloseBlock(sessionId: String): BlockSummary?
}