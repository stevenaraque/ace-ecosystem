// app/src/main/kotlin/com/ace/mobile/feature/wear/domain/ReceiveWearDataUseCase.kt
package com.ace.mobile.feature.wear.domain

import com.ace.mobile.feature.wear.data.WearDataSource
import com.ace.mobile.feature.wear.data.WearSimulationManager
import com.ace.mobile.core.model.HeartRateSample
import com.ace.mobile.core.data.SessionSampleBuffer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

class ReceiveWearDataUseCase @Inject constructor(
    private val wearDataSource: WearDataSource,
    private val wearSimulationManager: WearSimulationManager,
    private val sessionSampleBuffer: SessionSampleBuffer
) {

    fun observeHeartRate(): Flow<HeartRateSample> {
        // 1. Datos reales del reloj
        val realFlow = wearDataSource.observeHeartRate().map { wearSample ->
            HeartRateSample(bpm = wearSample.bpm, timestamp = wearSample.timestamp)
        }

        // 2. Datos simulados
        val simulatedFlow = wearSimulationManager.observeSimulatedHeartRate()
            .map { wearSample ->
                HeartRateSample(bpm = wearSample.bpm, timestamp = wearSample.timestamp)
            }
            .onEach { heartRateSample ->
                // SOLUCIÓN AQUÍ:
                // Verificamos si hay una sesión activa en el buffer
                val activeSessionId = sessionSampleBuffer.getActiveSessionId()
                if (!activeSessionId.isNullOrEmpty()) {
                    try {
                        // Usamos addSample pasándole el ID de la sesión y el objeto de la muestra envuelto
                        sessionSampleBuffer.addSample(activeSessionId, heartRateSample)
                    } catch (e: Exception) {
                        // Evita caídas si el buffer está ocupado en ese milisegundo
                    }
                }
            }

        return merge(realFlow, simulatedFlow)
    }

    fun isSimulationActive(): Flow<Boolean> = wearSimulationManager.isSimulationActive

    fun toggleSimulation(active: Boolean) {
        wearSimulationManager.setSimulationActive(active)
    }
}