// app/src/main/kotlin/com/ace/mobile/feature/wear/data/WearSimulationManager.kt
package com.ace.mobile.feature.wear.data

import sena.adso.ace_mobile.BuildConfig
import com.ace.mobile.feature.wear.data.model.WearHeartRateSample
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class WearSimulationManager @Inject constructor() {

    private val _isSimulationActive = MutableStateFlow(false)
    val isSimulationActive = _isSimulationActive.asStateFlow()

    private var lastBpm = 135.0

    fun setSimulationActive(active: Boolean) {
        if (BuildConfig.DEBUG) {
            _isSimulationActive.value = active
        }
    }

    fun observeSimulatedHeartRate(): Flow<WearHeartRateSample> = flow {
        while (true) {
            if (_isSimulationActive.value && BuildConfig.DEBUG) {
                // Variación realista dando pequeños pasos hacia arriba o abajo (-3 a +3 bpm)
                val step = Random.nextDouble(-3.0, 3.0)
                lastBpm = (lastBpm + step).coerceIn(120.0, 160.0)

                emit(
                    WearHeartRateSample(
                        bpm = lastBpm,
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
            delay(1000L)
        }
    }
}