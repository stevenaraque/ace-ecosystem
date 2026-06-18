package com.ace.mobile.domain.usecase.wear

import com.ace.mobile.data.wear.WearDataSource
import com.ace.mobile.data.wear.model.WearHeartRateSample
import com.ace.mobile.domain.model.HeartRateSample
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