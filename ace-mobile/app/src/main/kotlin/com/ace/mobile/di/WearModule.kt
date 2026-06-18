package com.ace.mobile.di

import android.content.Context
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.Wearable
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Módulo Hilt que provee las dependencias de Wear OS Data Layer API.
 *
 * Sistema 1 — Captura de Sensor (Apéndice S1)
 * Provee DataClient (datos del reloj) y MessageClient (comandos START/STOP).
 *
 * @see com.ace.mobile.data.wear.WearDataSource
 * @see com.ace.mobile.data.wear.WearMessageClient
 */
@Module
@InstallIn(SingletonComponent::class)
object WearModule {

    /**
     * Provee el [DataClient] de Google Play Services Wearable.
     * Usado por [WearDataSource] para recibir muestras de frecuencia cardíaca.
     */
    @Provides
    @Singleton
    fun provideDataClient(
        @ApplicationContext context: Context
    ): DataClient = Wearable.getDataClient(context)

    /**
     * Provee el [MessageClient] de Google Play Services Wearable.
     * Usado por [WearMessageClient] para enviar comandos START/STOP al reloj.
     */
    @Provides
    @Singleton
    fun provideMessageClient(
        @ApplicationContext context: Context
    ): MessageClient = Wearable.getMessageClient(context)
}