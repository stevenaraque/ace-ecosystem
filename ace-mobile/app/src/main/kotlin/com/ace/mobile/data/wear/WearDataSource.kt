package com.ace.mobile.data.wear

import android.content.Context
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton
import com.ace.mobile.data.wear.model.WearHeartRateSample

@Singleton
class WearDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataClient: DataClient = Wearable.getDataClient(context)

    fun observeHeartRate(): Flow<WearHeartRateSample> = callbackFlow {
        val listener = DataClient.OnDataChangedListener { dataEvents: DataEventBuffer ->
            dataEvents.forEach { event ->
                if (event.type == DataEvent.TYPE_CHANGED) {
                    val dataItem = event.dataItem
                    if (dataItem.uri.path?.startsWith("/ace/health/heart_rate") == true) {
                        val dataMap = DataMapItem.fromDataItem(dataItem).dataMap
                        val bpm = dataMap.getInt("bpm", 0)
                        val timestamp = dataMap.getLong("timestamp", 0L)

                        if (bpm > 0 && timestamp > 0) {
                            trySend(
                                WearHeartRateSample(
                                    bpm = bpm,
                                    timestamp = timestamp
                                )
                            )
                        }
                    }
                }
            }
        }

        dataClient.addListener(listener)
        awaitClose { dataClient.removeListener(listener) }
    }
}