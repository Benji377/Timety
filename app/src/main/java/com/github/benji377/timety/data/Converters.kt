package com.github.benji377.timety.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromTaskStatus(status: TaskStatus): String = status.name

    @TypeConverter
    fun toTaskStatus(value: String): TaskStatus = TaskStatus.valueOf(value)

    @TypeConverter
    fun fromFocusRating(rating: FocusRating?): String? = rating?.name

    @TypeConverter
    fun toFocusRating(value: String?): FocusRating? = value?.let { FocusRating.valueOf(it) }
}
