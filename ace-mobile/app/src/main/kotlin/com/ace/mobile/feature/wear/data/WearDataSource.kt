package com.ace.mobile.feature.wear.data

import android.content.Context
import android.util.Log
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
import com.ace.mobile.feature.wear.data.model.WearHeartRateSample

@Singleton
class WearDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "WearDataSource"
        private const val PATH_HEART_RATE = "/ace/health/heart_rate"
    }

    private val dataClient: DataClient = Wearable.getDataClient(context)

    fun observeHeartRate(): Flow<WearHeartRateSample> = callbackFlow {
        Log.i(TAG, "=== LISTENER REGISTRADO EN DATACLIENT ===")
        Log.i(TAG, "Esperando datos en path: $PATH_HEART_RATE")

        val listener = DataClient.OnDataChangedListener { dataEvents: DataEventBuffer ->
            Log.d(TAG, "onDataChanged: ${dataEvents.count} eventos recibidos")

            dataEvents.forEach { event ->
                Log.d(TAG, "Evento tipo: ${event.type}, dataItem: ${event.dataItem}")

                if (event.type == DataEvent.TYPE_CHANGED) {
                    val dataItem = event.dataItem
                    val path = dataItem.uri.path ?: "null"

                    Log.d(TAG, "DataItem path: $path")
                    Log.d(TAG, "¿path.startsWith($PATH_HEART_RATE)? = ${path.startsWith(PATH_HEART_RATE)}")

                    if (path.startsWith(PATH_HEART_RATE)) {
                        val dataMap = DataMapItem.fromDataItem(dataItem).dataMap
                        val bpm = dataMap.getDouble("bpm", 0.0)
                        val timestamp = dataMap.getLong("timestamp", 0L)

                        Log.i(TAG, "FC RECIBIDA DEL RELOJ: bpm=$bpm, timestamp=$timestamp")

                        if (bpm > 0 && timestamp > 0) {
                            val sample = WearHeartRateSample(bpm = bpm, timestamp = timestamp)
                            Log.i(TAG, "Enviando sample al flow: $sample")
                            trySend(sample)
                        } else {
                            Log.w(TAG, "FC invalida descartada: bpm=$bpm, timestamp=$timestamp")
                        }
                    } else {
                        Log.w(TAG, "Path no coincide con $PATH_HEART_RATE: $path")
                    }
                } else if (event.type == DataEvent.TYPE_DELETED) {
                    Log.d(TAG, "DataItem eliminado: ${event.dataItem.uri.path}")
                }
            }
        }

        dataClient.addListener(listener)
        Log.i(TAG, "Listener agregado a DataClient")

        awaitClose {
            Log.i(TAG, "=== LISTENER REMOVIDO DE DATACLIENT ===")
            dataClient.removeListener(listener)
        }
    }
}