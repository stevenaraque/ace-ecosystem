package com.ace.mobile.domain.model

data class HeartRateAggregate(
    val avgBpm: Double,
    val maxBpm: Double,
    val minBpm: Double,
    val sampleCount: Int,
    val durationSeconds: Long,
    val timestampStart: Long,
    val timestampEnd: Long
) {
    companion object {

        fun fromSamples(samples: List<HeartRateSample>): HeartRateAggregate? {
            if (samples.isEmpty()) return null

            val bpms = samples.map { it.bpm }
            val timestamps = samples.map { it.timestamp }.sorted()

            return HeartRateAggregate(
                avgBpm = bpms.average(),
                maxBpm = bpms.maxOrNull() ?: 0.0,
                minBpm = bpms.minOrNull() ?: 0.0,
                sampleCount = samples.size,
                durationSeconds = (timestamps.last() - timestamps.first()) / 1000,
                timestampStart = timestamps.first(),
                timestampEnd = timestamps.last()
            )
        }
    }
}