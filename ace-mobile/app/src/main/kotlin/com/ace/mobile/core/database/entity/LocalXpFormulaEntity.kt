package com.ace.mobile.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "xp_formulas")
data class LocalXpFormulaEntity(
    @PrimaryKey
    val sportType: String,
    val minBpm: Double,
    val xpPerMinute: Double,
    val maxXpPerBlock: Int,
    val cachedAt: Long = System.currentTimeMillis()
)