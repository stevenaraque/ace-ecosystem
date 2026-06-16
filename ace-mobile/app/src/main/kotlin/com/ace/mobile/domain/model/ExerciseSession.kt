package com.ace.mobile.domain.model

import com.ace.shared.enums.SessionStatus
import com.ace.shared.enums.SportType

data class ExerciseSession(
    val sessionId: String,
    val status: SessionStatus,
    val sportType: SportType,
    val timestampStart: Long,
    val timestampEnd: Long? = null,
    val totalBlocks: Int = 0,
    val totalXp: Int = 0
) {
    companion object {
        fun create(
            sportType: SportType,
            timestampStart: Long = System.currentTimeMillis()
        ): ExerciseSession = ExerciseSession(
            sessionId = java.util.UUID.randomUUID().toString(),
            status = SessionStatus.ACTIVE,
            sportType = sportType,
            timestampStart = timestampStart
        )
    }

    fun durationSeconds(): Long {
        val end = timestampEnd ?: System.currentTimeMillis()
        return (end - timestampStart) / 1000
    }

    fun isTerminal(): Boolean =
        status == SessionStatus.COMPLETED || status == SessionStatus.ABORTED

    fun canPause(): Boolean = status == SessionStatus.ACTIVE
    fun canResume(): Boolean = status == SessionStatus.PAUSED
    fun canStop(): Boolean =
        status == SessionStatus.ACTIVE || status == SessionStatus.PAUSED
}