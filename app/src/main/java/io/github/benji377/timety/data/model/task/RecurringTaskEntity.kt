package io.github.benji377.timety.data.model.task

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import java.time.Instant

/**
 * The calendar unit a recurring task repeats on. Sub-weekly cadences are habit territory, so DAY
 * is deliberately not offered.
 */
enum class RecurrenceUnit {
    WEEK,
    MONTH,
    YEAR
}

/** How a MONTH-unit recurrence anchors inside the month. */
enum class MonthlyMode {
    /** The same day number each month, e.g. "on day 3"; clamped in shorter months. */
    DAY_OF_MONTH,

    /** An ordinal weekday each month, e.g. "on the second Friday" or "on the last Friday". */
    NTH_WEEKDAY
}

/**
 * A repeating task template, fully separate from one-off [TaskEntity] rows: it is never listed
 * with normal tasks and never becomes "completed". Completing it logs a
 * [RecurringOccurrenceEntity] and rolls [dueDate] forward to the next occurrence.
 *
 * The rule fields are a small structured subset of RFC 5545 RRULE semantics:
 * - WEEK: every [interval] weeks on the [daysOfWeek] weekdays (week-aligned to the due date).
 * - MONTH + [MonthlyMode.DAY_OF_MONTH]: every [interval] months on day [monthlyDay].
 * - MONTH + [MonthlyMode.NTH_WEEKDAY]: every [interval] months on the [monthlyOrdinal]th
 *   [monthlyWeekday] (ordinal [LAST_ORDINAL] = last).
 * - YEAR: every [interval] years on the due date's month/day.
 *
 * The intended day/ordinal is stored rather than re-derived from [dueDate], so "on day 31"
 * survives being clamped to a shorter month. The time of day always comes from [dueDate].
 */
@Entity(tableName = "recurring_tasks")
data class RecurringTaskEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String = "",
    /** Category name, like [TaskEntity.category]; feeds the stats breakdown and list filters. */
    val category: String = "",
    /** The next occurrence; advanced on completion. Also the anchor for week/time-of-day math. */
    val dueDate: Instant,
    val unit: RecurrenceUnit,
    val interval: Int = 1,
    /** ISO weekday numbers as `"[1,5]"` (1 = Monday), for WEEK. Empty/null = the due date's weekday. */
    val daysOfWeek: String? = null,
    val monthlyMode: MonthlyMode = MonthlyMode.DAY_OF_MONTH,
    /** 1..31, for MONTH + DAY_OF_MONTH. Null = the due date's day. */
    val monthlyDay: Int? = null,
    /** 1..4 or [LAST_ORDINAL], for MONTH + NTH_WEEKDAY. Null = derived from the due date. */
    val monthlyOrdinal: Int? = null,
    /** ISO weekday 1..7, for MONTH + NTH_WEEKDAY. Null = the due date's weekday. */
    val monthlyWeekday: Int? = null,
    /** Reminder offsets in minutes before [dueDate]; empty = a single reminder at the due time. */
    val reminderOffsetsMinutes: List<Int> = emptyList(),
    val createdAt: Instant,
) {
    companion object {
        /** [monthlyOrdinal] value meaning "the last such weekday of the month". */
        const val LAST_ORDINAL = -1
    }
}

/** One logged completion of a recurring task; the habit-completion pattern applied to tasks. */
@Entity(
    tableName = "recurring_occurrences",
    foreignKeys = [
        ForeignKey(
            entity = RecurringTaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["recurringTaskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("recurringTaskId")]
)
data class RecurringOccurrenceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val recurringTaskId: String,
    val completedAt: Instant
)

/** A recurring task paired with its completion records, for a Room `@Relation` query. */
data class RecurringTaskWithOccurrences(
    @Embedded
    val task: RecurringTaskEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "recurringTaskId"
    )
    val occurrences: List<RecurringOccurrenceEntity>
)
