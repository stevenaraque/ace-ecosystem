package com.ace.mobile.core.model

data class HeartRateAggregate(
    val avgBpm: Double,
    val maxBpm: Int,
    val minBpm: Int,
    val sampleCount: Int,
    val durationSeconds: Int,
    val timestampStart: Long,
    val timestampEnd: Long
)