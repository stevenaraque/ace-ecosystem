// app/src/main/kotlin/com/ace/mobile/feature/wear/domain/ReceiveWearDataUseCase.kt
package com.ace.mobile.feature.wear.domain

import com.ace.mobile.feature.wear.data.WearDataSource
import com.ace.mobile.feature.wear.data.WearSimulationManager
import com.ace.mobile.core.model.HeartRateSample
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import javax.inject.Inject

class ReceiveWearDataUseCase @Inject constructor(
    private val wearDataSource: WearDataSource,
    private val wearSimulationManager: WearSimulationManager
) {

    fun observeHeartRate(): Flow<HeartRateSample> {
        val realFlow = wearDataSource.observeHeartRate().map { wearSample ->
            HeartRateSample(bpm = wearSample.bpm, timestamp = wearSample.timestamp)
        }

        val simulatedFlow = wearSimulationManager.observeSimulatedHeartRate().map { wearSample ->
            HeartRateSample(bpm = wearSample.bpm, timestamp = wearSample.timestamp)
        }

        // Retorna un merge de ambos canales. Si la simulación está apagada, simulatedFlow no emite nada.
        return merge(realFlow, simulatedFlow)
    }

    fun isSimulationActive(): Flow<Boolean> = wearSimulationManager.isSimulationActive

    fun toggleSimulation(active: Boolean) {
        wearSimulationManager.setSimulationActive(active)
    }
}