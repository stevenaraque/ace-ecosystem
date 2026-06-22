// app/src/main/kotlin/com/ace/mobile/data/local/database/entity/LocalSessionHistoryEntity.kt
package com.ace.mobile.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "local_session_history")
data class LocalSessionHistoryEntity(
    @PrimaryKey
    val sessionId: String,
    val timestampStart: Long,
    val timestampEnd: Long,
    val sportType: String,
    val durationSeconds: Int,
    val avgBpm: Double,
    val totalBlocks: Int,
    val totalXp: Long
)