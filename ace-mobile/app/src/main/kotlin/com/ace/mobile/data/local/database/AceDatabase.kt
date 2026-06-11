package com.ace.mobile.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ace.mobile.data.local.database.dao.UserDao
import com.ace.mobile.data.local.database.entity.LocalBlockEntity
import com.ace.mobile.data.local.database.entity.LocalRankingCacheEntity
import com.ace.mobile.data.local.database.entity.LocalSessionEntity
import com.ace.mobile.data.local.database.entity.LocalSessionHistoryEntity
import com.ace.mobile.data.local.database.entity.LocalUserEntity
import com.ace.mobile.data.local.database.entity.LocalUserStatsEntity

@Database(
    entities = [
        LocalUserEntity::class,
    ],
    version = 1,
    exportSchema = false
)
abstract class AceDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
}