package com.ace.mobile.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ace.shared.enums.BlockStatus
import com.ace.shared.enums.SportType

@Entity(tableName = "local_blocks")
data class LocalBlockEntity(
    @PrimaryKey val blockId: String,
    val sessionId: String,
    val userId: String,
    val timestampStart: Long,
    val timestampEnd: Long,
    val durationSeconds: Int,
    val avgBpm: Double,
    val maxBpm: Double,
    val minBpm: Double,
    val sampleCount: Int,
    val sportType: SportType,
    val xpCalculated: Int? = null,
    val status: BlockStatus = BlockStatus.PENDING
)