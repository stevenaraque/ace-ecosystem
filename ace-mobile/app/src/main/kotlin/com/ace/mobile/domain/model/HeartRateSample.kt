package com.ace.mobile.domain.model

import com.google.android.gms.wearable.DataMap

data class HeartRateSample(
    val bpm: Double,
    val timestamp: Long
) {
    fun bpmInt(): Int = bpm.toInt()


    fun isValid(): Boolean = bpm in 30.0..250.0

    companion object {

        fun fromDataMap(dataMap: DataMap): HeartRateSample? {
            return try {
                val dataType = dataMap.getString("data_type", "")
                if (dataType != "HEART_RATE_BPM") return null

                val value = dataMap.getDouble("value", -1.0)
                val timeStart = dataMap.getLong("time_interval_start", -1L)

                if (value < 0 || timeStart < 0) return null

                HeartRateSample(bpm = value, timestamp = timeStart)
            } catch (e: Exception) {
                null
            }
        }
    }
}
