package com.ace.mobile.domain.model

data class HeartRateAggregate(
    val avgBpm: Double,
    val maxBpm: Int,
    val minBpm: Int,
    val sampleCount: Int,
    val durationSeconds: Int,
    val timestampStart: Long,
    val timestampEnd: Long
)