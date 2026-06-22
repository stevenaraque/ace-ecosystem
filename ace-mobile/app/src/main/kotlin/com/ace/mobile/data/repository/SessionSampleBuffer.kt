// app/src/main/kotlin/com/ace/mobile/data/repository/SessionSampleBuffer.kt
package com.ace.mobile.data.repository

import com.ace.mobile.domain.model.HeartRateSample
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

interface SessionSampleBuffer {
    fun setActiveSessionId(sessionId: String?)
    fun getActiveSessionId(): String?
    fun addSample(sessionId: String, sample: HeartRateSample)
    fun getSamples(sessionId: String): List<HeartRateSample>
    fun clear(sessionId: String)
    fun observeSamples(): Flow<HeartRateSample>
}