package com.github.benji377.timety.data

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromTaskStatus(status: TaskStatus): String = status.name

    @TypeConverter
    fun toTaskStatus(value: String): TaskStatus = TaskStatus.valueOf(value)

    @TypeConverter
    fun fromFocusRating(rating: FocusRating?): String? = rating?.name

    @TypeConverter
    fun toFocusRating(value: String?): FocusRating? = value?.let { FocusRating.valueOf(it) }

    @TypeConverter
    fun fromLongList(value: List<Long>): String = value.joinToString(",")

    @TypeConverter
    fun toLongList(value: String): List<Long> =
        if (value.isEmpty()) emptyList() else value.split(",").map { it.toLong() }

    @TypeConverter
    fun fromFocusStepList(value: List<FocusStep>): String = gson.toJson(value)

    @TypeConverter
    fun toFocusStepList(value: String): List<FocusStep> {
        val listType = object : TypeToken<List<FocusStep>>() {}.type
        return gson.fromJson(value, listType)
    }
}
