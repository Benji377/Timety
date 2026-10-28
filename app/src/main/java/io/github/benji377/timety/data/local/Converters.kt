package io.github.benji377.timety.data.local

import androidx.room.TypeConverter
import io.github.benji377.timety.data.model.task.TaskPlace
import org.json.JSONObject
import java.time.Instant

/** Room type converters for [Instant], [TaskPlace], and for lists of [Instant], [Int], and [String]. */
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
            } catch (_: Exception) {
                null
            }
        }
    }

    @TypeConverter
    fun fromIntList(values: List<Int>?): String {
        return values?.joinToString(",") ?: ""
    }

    @TypeConverter
    fun toIntList(data: String?): List<Int> {
        if (data.isNullOrBlank()) return emptyList()
        return data.split(",").mapNotNull { it.trim().toIntOrNull() }
    }

    @TypeConverter
    fun fromTaskPlace(place: TaskPlace?): String? {
        return place?.toJson()?.toString()
    }

    @TypeConverter
    fun toTaskPlace(data: String?): TaskPlace? {
        if (data.isNullOrBlank()) return null
        return try {
            TaskPlace.fromJson(JSONObject(data))
        } catch (_: Exception) {
            null
        }
    }
}
