package com.github.benji377.timety.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "focus_mode")
data class FocusMode(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val isCustom: Boolean = true,
    val steps: List<FocusStep>
)

data class FocusStep(
    val durationMins: Int,
    val type: FocusStepType,
    val behavior: FocusStepBehavior = FocusStepBehavior.COUNT_DOWN
)

enum class FocusStepType {
    START, FOCUS, REST, END, LOOP, STOPWATCH, FLEXIBLE
}

enum class FocusStepBehavior {
    COUNT_UP, COUNT_DOWN
}
