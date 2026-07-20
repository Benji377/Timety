package io.github.benji377.timety.data.model.habit

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation
import java.time.Instant

/** How often a habit is expected to be completed. */
enum class HabitFrequency(val value: Int) {
    DAILY(0),
    WEEKLY_EXACT(1),
    WEEKLY_FLEXIBLE(2)
}

/** A user-defined habit with its recurrence rule, display, and optional stack placement. */
@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val frequency: HabitFrequency,
    val targetDaysPerWeek: Int? = null,
    val targetWeekdays: String? = null, // Stored as a JSON string, e.g. "[1,3,5]".
    val targetTimeMinutes: Int? = null, // Minutes from midnight.
    val createdAt: Instant,
    val colorValue: Int,
    val notes: String? = null,
    val iconCodePoint: Int? = null,
    val stackName: String? = null,
    val stackOrder: Int? = null,
    val sortOrder: Int = 0
)

/** A habit paired with all of its completion records, for a Room `@Relation` query. */
data class HabitWithCompletions(
    @Embedded
    val habit: HabitEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "habitId"
    )
    val completions: List<HabitCompletionEntity>
)
