// app/src/main/kotlin/com/ace/mobile/di/RepositoryModule.kt
package com.ace.mobile.di

import com.ace.mobile.data.repository.SessionRepository
import com.ace.mobile.data.repository.SessionRepositoryImpl
import com.ace.mobile.data.repository.SessionSampleBuffer
import com.ace.mobile.data.repository.SessionSampleBufferImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindSessionRepository(
        impl: SessionRepositoryImpl
    ): SessionRepository

    @Binds
    abstract fun bindSessionSampleBuffer(
        impl: SessionSampleBufferImpl
    ): SessionSampleBuffer
}