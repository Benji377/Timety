package io.github.benji377.timety.data.model.habit

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation
import java.time.Instant

/** How often a habit is expected to be completed. */
enum class HabitFrequency(val value: Int) {
    DAILY(0),
    WEEKLY_EXACT(1),
    WEEKLY_FLEXIBLE(2),
    MONTHLY_FLEXIBLE(3);

    /** Whether the habit has a target count per week or month instead of fixed days. */
    val isFlexible: Boolean get() = this == WEEKLY_FLEXIBLE || this == MONTHLY_FLEXIBLE
}

/** A user-defined habit with its recurrence rule, display, and optional stack placement. */
@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val frequency: HabitFrequency,
    // Days per week or month for flexible habits; the column keeps its original weekly-only name.
    @ColumnInfo(name = "targetDaysPerWeek")
    val targetCount: Int? = null,
    val targetWeekdays: String? = null, // Stored as a JSON string, e.g. "[1,3,5]".
    val reminderTimes: String? = null, // Minutes from midnight as a JSON string, e.g. "[480,1200]".
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
