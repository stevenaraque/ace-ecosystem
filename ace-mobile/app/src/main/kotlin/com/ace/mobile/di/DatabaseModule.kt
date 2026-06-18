package com.ace.mobile.di

import android.content.Context
import androidx.room.Room
import com.ace.mobile.data.local.database.AceDatabase
import com.ace.mobile.data.local.database.dao.BlockDao
import com.ace.mobile.data.local.database.dao.SessionDao
import com.ace.mobile.data.local.database.dao.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): AceDatabase {
        return Room.databaseBuilder(
            context,
            AceDatabase::class.java,
            "ace_database"
        ).build()
    }

    @Provides
    fun provideUserDao(database: AceDatabase): UserDao = database.userDao()

    @Provides
    fun provideSessionDao(database: AceDatabase): SessionDao = database.sessionDao()

    @Provides
    fun provideBlockDao(database: AceDatabase): BlockDao = database.blockDao()

}