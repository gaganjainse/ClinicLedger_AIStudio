package com.villageclinicledger.data.converters

import androidx.room.TypeConverter
import java.util.Date

/**
 * Room TypeConverter that serializes java.util.Date to Long (epoch millis)
 * for storage in SQLite, and deserializes Long values back to Date.
 * This is necessary because Room cannot natively persist Date objects.
 */
class DateConverter {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}
