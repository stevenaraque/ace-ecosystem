// app/src/main/kotlin/com/ace/mobile/data/local/database/entity/LocalUserStatsEntity.kt
package com.ace.mobile.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "local_user_stats")
data class LocalUserStatsEntity(
    @PrimaryKey
    val userId: String,
    val totalXp: Long = 0L,
    val totalSessions: Int = 0,
    val totalBlocks: Int = 0,
    val totalDurationSeconds: Long = 0,
    val avgBpmAllTime: Double = 0.0,
    val lastSyncAt: Long? = null
)