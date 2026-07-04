package io.github.benji377.timety.data.model.habit

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation
import java.time.Instant

enum class HabitFrequency(val value: Int) {
    DAILY(0),
    WEEKLY_EXACT(1),
    WEEKLY_FLEXIBLE(2)
}

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val frequency: HabitFrequency,
    val targetDaysPerWeek: Int? = null,
    val targetWeekdays: String? = null, // Stored as JSON string "[1,3,5]" for simplicity
    val targetTimeMinutes: Int? = null, // Minutes from midnight
    val createdAt: Instant,
    val colorValue: Int,
    val notes: String? = null,
    val iconCodePoint: Int? = null,
    val stackName: String? = null,
    val stackOrder: Int? = null
)


data class HabitWithCompletions(
    @Embedded
    val habit: HabitEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "habitId"
    )
    val completions: List<HabitCompletionEntity>
)
