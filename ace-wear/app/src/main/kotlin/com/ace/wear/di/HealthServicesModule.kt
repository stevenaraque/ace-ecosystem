// ace-wear/app/src/main/kotlin/com/ace/wear/di/HealthServicesModule.kt

package com.ace.wear.di

import android.content.Context
import androidx.health.services.client.HealthServices
import androidx.health.services.client.HealthServicesClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Modulo de Hilt para proveer HealthServicesClient.
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
}