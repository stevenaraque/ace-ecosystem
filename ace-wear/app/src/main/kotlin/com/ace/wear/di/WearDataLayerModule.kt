// ace-wear/app/src/main/kotlin/com/ace/wear/di/WearDataLayerModule.kt

package com.ace.wear.di

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
 * Modulo de Hilt para proveer clientes del Wear OS Data Layer.
 */
@Module
@InstallIn(SingletonComponent::class)
object WearDataLayerModule {

    @Provides
    @Singleton
    fun provideDataClient(
        @ApplicationContext context: Context
    ): DataClient {
        return Wearable.getDataClient(context)
    }

    @Provides
    @Singleton
    fun provideMessageClient(
        @ApplicationContext context: Context
    ): MessageClient {
        return Wearable.getMessageClient(context)
    }
}