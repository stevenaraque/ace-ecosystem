// app/src/main/kotlin/com/ace/mobile/data/local/database/SportTypeConverter.kt
package com.ace.mobile.core.database

import androidx.room.TypeConverter
import com.ace.shared.enums.SportType

class SportTypeConverter {
    @TypeConverter
    fun fromString(value: String): SportType = SportType.valueOf(value)

    @TypeConverter
    fun toString(sportType: SportType): String = sportType.name
}