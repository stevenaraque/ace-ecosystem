// app/src/main/kotlin/com/ace/mobile/data/local/database/entity/LocalSessionEntity.kt
package com.ace.mobile.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "local_sessions")
data class LocalSessionEntity(
    @PrimaryKey
    val sessionId: String,
    val userId: String,
    val deviceId: String,
    val status: String,
    val sportType: String,
    val timestampStart: Long,
    val timestampEnd: Long?,
    val totalBlocks: Int,
    val totalXp: Long
)