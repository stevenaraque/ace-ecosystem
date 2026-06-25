package com.ace.mobile.core.di

import android.content.Context
import androidx.work.WorkManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Módulo Hilt para WorkManager.
 *
 * NOTA: La configuración de WorkManager (Configuration.Provider) está en
 * MobileApplication.kt para evitar conflicto con HiltWorkerFactory.
 *
 * @see Apéndice S3 (Sync) · Apéndice S7 (Streaks)
 */
@Module
@InstallIn(SingletonComponent::class)
object WorkManagerModule {

    /**
     * Proporcione la instancia de WorkManager.
     * La configuración con HiltWorkerFactory se hace en MobileApplication.
     */
    @Provides
    @Singleton
    fun provideWorkManager(
        @ApplicationContext context: Context
    ): WorkManager {
        return WorkManager.getInstance(context)
    }
}