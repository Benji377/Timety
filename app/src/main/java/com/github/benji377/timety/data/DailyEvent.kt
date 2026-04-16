package com.github.benji377.timety.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_event")
data class DailyEvent(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long,
    val type: String, // "FOCUS", "REST", "DISTRACTED", "STRETCH", "DRINK", etc.
    val description: String? = null
)
