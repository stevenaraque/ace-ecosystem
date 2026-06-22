// app/src/main/kotlin/com/ace/mobile/data/local/database/AceDatabase.kt
package com.ace.mobile.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.ace.mobile.data.local.database.dao.BlockDao
import com.ace.mobile.data.local.database.dao.SessionDao
import com.ace.mobile.data.local.database.dao.UserDao
import com.ace.mobile.data.local.database.dao.XpFormulaDao
import com.ace.mobile.data.local.database.entity.LocalBlockEntity
import com.ace.mobile.data.local.database.entity.LocalRankingCacheEntity
import com.ace.mobile.data.local.database.entity.LocalSessionEntity
import com.ace.mobile.data.local.database.entity.LocalSessionHistoryEntity
import com.ace.mobile.data.local.database.entity.LocalUserEntity
import com.ace.mobile.data.local.database.entity.LocalUserStatsEntity
import com.ace.mobile.data.local.database.entity.LocalXpFormulaEntity

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
    version = 2,
    exportSchema = false
)
@TypeConverters(SportTypeConverter::class, BlockStatusConverter::class)
abstract class AceDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun sessionDao(): SessionDao
    abstract fun blockDao(): BlockDao
    abstract fun xpFormulaDao(): XpFormulaDao
}