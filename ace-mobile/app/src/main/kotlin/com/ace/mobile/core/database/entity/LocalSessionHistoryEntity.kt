package com.ace.mobile.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "local_session_history")
data class LocalSessionHistoryEntity(
    @PrimaryKey
    val sessionId: String,
    val userId: String,
    val timestampStart: String,
    val timestampEnd: String?,
    val sportType: String,
    val durationSeconds: Int,
    val totalXp: Int,
    val totalBlocks: Int,
    val avgBpm: Double?
)