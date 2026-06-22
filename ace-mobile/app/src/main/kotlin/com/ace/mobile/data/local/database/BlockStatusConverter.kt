// app/src/main/kotlin/com/ace/mobile/data/local/database/BlockStatusConverter.kt
package com.ace.mobile.data.local.database

import androidx.room.TypeConverter
import com.ace.shared.enums.BlockStatus

class BlockStatusConverter {
    @TypeConverter
    fun fromString(value: String): BlockStatus = BlockStatus.valueOf(value)

    @TypeConverter
    fun toString(status: BlockStatus): String = status.name
}