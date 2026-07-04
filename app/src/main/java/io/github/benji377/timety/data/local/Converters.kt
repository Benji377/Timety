package io.github.benji377.timety.data.local

import androidx.room.TypeConverter
import java.time.Instant

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Instant? {
        return value?.let { Instant.ofEpochMilli(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Instant?): Long? {
        return date?.toEpochMilli()
    }

    @TypeConverter
    fun fromInstantList(instants: List<Instant>?): String {
        return instants?.joinToString(",") { it.toEpochMilli().toString() } ?: ""
    }

    @TypeConverter
    fun toInstantList(data: String?): List<Instant> {
        if (data.isNullOrBlank()) return emptyList()
        return data.split(",").mapNotNull {
            try {
                Instant.ofEpochMilli(it.toLong())
            } catch (e: Exception) {
                null
            }
        }
    }

    @TypeConverter
    fun fromStringList(strings: List<String>?): String {
        return strings?.joinToString("|||") ?: ""
    }

    @TypeConverter
    fun toStringList(data: String?): List<String> {
        if (data.isNullOrBlank()) return emptyList()
        return data.split("|||")
    }
}
