package com.ace.mobile.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.ace.mobile.core.database.dao.*
import com.ace.mobile.core.database.entity.*

@Database(
    entities = [
        LocalUserEntity::class,
        LocalSessionEntity::class,
        LocalBlockEntity::class,
        LocalSessionHistoryEntity::class,
        LocalUserStatsEntity::class,
        LocalRankingCacheEntity::class,
        LocalXpFormulaEntity::class,
    ],
    version = 4, // ← INCREMENTADO por nuevo campo totalSessions
    exportSchema = false
)
@TypeConverters(SportTypeConverter::class, BlockStatusConverter::class)
abstract class AceDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun sessionDao(): SessionDao
    abstract fun blockDao(): BlockDao
    abstract fun xpFormulaDao(): XpFormulaDao
    abstract fun rankingCacheDao(): RankingCacheDao
    abstract fun statsDao(): StatsDao
    abstract fun sessionHistoryDao(): SessionHistoryDao
}

