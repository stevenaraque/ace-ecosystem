package com.ace.mobile.domain.model

import com.ace.shared.enums.SessionStatus
import com.ace.shared.enums.SportType

data class ExerciseSession(
    val sessionId: String,
    val userId: String,
    val deviceId: String,
    val status: SessionStatus,
    val sportType: SportType,
    val timestampStart: Long,
    val timestampEnd: Long?,
    val totalBlocks: Int,
    val totalXp: Int
)