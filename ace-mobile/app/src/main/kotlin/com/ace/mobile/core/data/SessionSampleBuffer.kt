package com.ace.mobile.core.data

import com.ace.mobile.core.model.HeartRateSample
import kotlinx.coroutines.flow.Flow

data class BlockSummary(
    val blockCount: Int,
    val xpGained: Double
)

interface SessionSampleBuffer {
    fun setActiveSessionId(sessionId: String?)
    fun getActiveSessionId(): String?
    fun addSample(sessionId: String, sample: HeartRateSample)
    fun getSamples(sessionId: String): List<HeartRateSample>
    fun clear(sessionId: String)
    fun observeSamples(): Flow<HeartRateSample>
    fun observeBlocks(): Flow<BlockSummary>
    fun getBlockCount(sessionId: String): Int
    fun pauseBlockTimer(sessionId: String)
    fun resumeBlockTimer(sessionId: String)
    fun isBlockTimerPaused(sessionId: String): Boolean
    suspend fun forceCloseBlock(sessionId: String): BlockSummary?
}