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
    @TypeConverter
    fun fromSubtaskList(subtasks: List<io.github.benji377.timety.data.model.task.Subtask>?): String {
        if (subtasks == null) return "[]"
        val jsonArray = org.json.JSONArray()
        for (subtask in subtasks) {
            val obj = org.json.JSONObject()
            obj.put("id", subtask.id)
            obj.put("title", subtask.title)
            obj.put("isCompleted", subtask.isCompleted)
            jsonArray.put(obj)
        }
        return jsonArray.toString()
    }

    @TypeConverter
    fun toSubtaskList(data: String?): List<io.github.benji377.timety.data.model.task.Subtask> {
        if (data.isNullOrBlank()) return emptyList()
        val list = mutableListOf<io.github.benji377.timety.data.model.task.Subtask>()
        try {
            val jsonArray = org.json.JSONArray(data)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    io.github.benji377.timety.data.model.task.Subtask(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        isCompleted = obj.getBoolean("isCompleted")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }
}
