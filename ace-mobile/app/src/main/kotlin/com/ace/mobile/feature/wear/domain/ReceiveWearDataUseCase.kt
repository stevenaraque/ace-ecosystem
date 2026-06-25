package com.ace.mobile.feature.wear.domain

import com.ace.mobile.feature.wear.data.WearDataSource
import com.ace.mobile.feature.wear.data.model.WearHeartRateSample
import com.ace.mobile.core.model.HeartRateSample
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ReceiveWearDataUseCase @Inject constructor(
    private val wearDataSource: WearDataSource
) {

    fun observeHeartRate(): Flow<HeartRateSample> {
        return wearDataSource.observeHeartRate().map { wearSample ->
            HeartRateSample(
                bpm = wearSample.bpm,
                timestamp = wearSample.timestamp
            )
        }
    }
}