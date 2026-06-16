package com.ace.mobile.domain.usecase.wear

import com.ace.mobile.data.wear.WearDataSource
import com.ace.mobile.domain.model.HeartRateSample
import com.google.android.gms.wearable.DataMap
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.filterNotNull
import javax.inject.Inject


class ReceiveWearDataUseCase @Inject constructor(
    private val wearDataSource: WearDataSource
) {
    operator fun invoke(): Flow<HeartRateSample> {
        return wearDataSource.heartRateFlow()
            .map { dataEvent ->
                // 1. Filtrar por path
                val path = dataEvent.dataItem.uri.path
                if (path != com.ace.shared.constants.DataLayerPaths.HEART_RATE) {
                    return@map null
                }

                // 2. Extraer DataMap y validar tipo
                val dataMap = DataMap.fromByteArray(dataEvent.dataItem.data)
                val dataType = dataMap.getString("data_type", "")
                if (dataType != "HEART_RATE_BPM") {
                    return@map null
                }

                // 3. Convertir a HeartRateSample
                HeartRateSample.fromDataMap(dataMap)
            }
            .filterNotNull()
            .map { sample ->
                // 4. Validar rango fisiológico
                if (sample.isValid()) sample else null
            }
            .filterNotNull()
    }
}