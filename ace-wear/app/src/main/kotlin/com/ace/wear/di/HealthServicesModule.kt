// ace-wear/app/src/main/kotlin/com/ace/wear/di/HealthServicesModule.kt
package com.ace.wear.di

import android.content.Context
import androidx.health.services.client.HealthServices
import androidx.health.services.client.HealthServicesClient
import com.ace.wear.data.health.HealthServicesManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Modulo de Hilt para proveer HealthServicesClient y HealthServicesManager.
 */
@Module
@InstallIn(SingletonComponent::class)
object HealthServicesModule {

    @Provides
    @Singleton
    fun provideHealthServicesClient(
        @ApplicationContext context: Context
    ): HealthServicesClient {
        return HealthServices.getClient(context)
    }

    @Provides
    @Singleton
    fun provideHealthServicesManager(
        healthServicesClient: HealthServicesClient
    ): HealthServicesManager {
        return HealthServicesManager(healthServicesClient)
    }
}