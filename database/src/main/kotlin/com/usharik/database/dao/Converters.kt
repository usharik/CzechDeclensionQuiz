package com.usharik.database.dao

import androidx.room.TypeConverter
import java.util.Date

class Converters {
    companion object {
        @TypeConverter @JvmStatic fun fromTimestamp(value: Long?): Date? = value?.let(::Date)
        @TypeConverter @JvmStatic fun dateToTimestamp(date: Date?): Long? = date?.time
    }
}
