package com.ace.mobile.data.wear

import com.ace.shared.constants.DataLayerPaths
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.filter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fuente de datos que escucha el [DataClient] para recibir muestras de
 * frecuencia cardíaca del reloj Wear OS.
 *
 * Sistema 1 — Captura de Sensor (Apéndice S1 §4)
 * Filtra eventos por el path [DataLayerPaths.HEART_RATE].
 *
 * @param dataClient Cliente de datos de Google Play Services Wearable.
 */
@Singleton
class WearDataSource @Inject constructor(
    private val dataClient: DataClient
) {

    /**
     * Retorna un [Flow] de [DataEvent] filtrados por el path de heart rate.
     *
     * El consumidor (ej. [ReceiveWearDataUseCase]) debe deserializar el DataMap
     * y validar el tipo semántico `HEART_RATE_BPM`.
     *
     * @see com.ace.mobile.domain.usecase.wear.ReceiveWearDataUseCase
     */
    fun heartRateFlow(): Flow<DataEvent> = callbackFlow {
        val listener = DataClient.OnDataChangedListener { buffer: DataEventBuffer ->
            buffer.forEach { event ->
                trySend(event)
            }
            buffer.release()
        }

        dataClient.addListener(listener)

        awaitClose {
            dataClient.removeListener(listener)
        }
    }.filter { event ->
        event.dataItem.uri.path == DataLayerPaths.HEART_RATE
    }
}