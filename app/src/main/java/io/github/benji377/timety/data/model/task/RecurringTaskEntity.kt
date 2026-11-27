package io.github.benji377.timety.data.model.task

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import io.github.benji377.timety.data.model.task.RecurringTaskEntity.Companion.LAST_ORDINAL
import java.time.Instant

/**
 * The calendar unit a recurring task repeats on. DAY covers custom gaps like "every 10 days" that
 * weeks and months can't express.
 */
enum class RecurrenceUnit {
    DAY,
    WEEK,
    MONTH,
    YEAR
}

/**
 * The unit of a recurring task's own task window, with the amounts it accepts. DAY and WEEK count
 * calendar days (0 days = only on the due day); HOUR counts from the due time and needs one.
 */
enum class TaskWindowUnit(val minAmount: Int, val maxAmount: Int) {
    HOUR(1, 48),
    DAY(0, 60),
    WEEK(1, 26);

    val amounts: IntRange get() = minAmount..maxAmount
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
 * The rule fields are a structured subset of RFC 5545 RRULE semantics:
 * - DAY: every [interval] days from the due date.
 * - WEEK: every [interval] weeks on the [daysOfWeek] weekdays (week-aligned to the due date).
 * - MONTH + [MonthlyMode.DAY_OF_MONTH]: every [interval] months on day [monthlyDay].
 * - MONTH + [MonthlyMode.NTH_WEEKDAY]: every [interval] months on the [monthlyOrdinal]th
 *   [monthlyWeekday] (ordinal [LAST_ORDINAL] = last).
 * - YEAR: every [interval] years on the due date's month, on day [monthlyDay].
 *
 * The intended day/ordinal and time of day are stored rather than re-derived from [dueDate], so
 * "on day 31" survives being clamped to a shorter month, Feb 29 comes back in leap years, and a
 * time inside a daylight-saving gap isn't shifted for good.
 */
@Entity(tableName = "recurring_tasks")
data class RecurringTaskEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String = "",
    val priority: Priority = Priority.MEDIUM,
    val size: TaskSize = TaskSize.MEDIUM,
    val location: String = "",
    /** Set only when [location] was picked from a search; a hand-typed location has no place. */
    val place: TaskPlace? = null,
    /** The next occurrence; advanced on completion. Also the anchor for week math. */
    val dueDate: Instant,
    /** False = due any time on the day; [dueDate] is then the local start of the day. */
    val hasDueTime: Boolean = true,
    /** Local time of day in minutes after midnight. Null = the due date's time of day. */
    val timeOfDayMinutes: Int? = null,
    val unit: RecurrenceUnit,
    val interval: Int = 1,
    /** ISO weekday numbers as `"[1,5]"` (1 = Monday), for WEEK. Empty/null = the due date's weekday. */
    val daysOfWeek: String? = null,
    val monthlyMode: MonthlyMode = MonthlyMode.DAY_OF_MONTH,
    /** 1..31, for MONTH + DAY_OF_MONTH and for YEAR. Null = the due date's day. */
    val monthlyDay: Int? = null,
    /** 1..4 or [LAST_ORDINAL], for MONTH + NTH_WEEKDAY. Null = derived from the due date. */
    val monthlyOrdinal: Int? = null,
    /** ISO weekday 1..7, for MONTH + NTH_WEEKDAY. Null = the due date's weekday. */
    val monthlyWeekday: Int? = null,
    /** Reminder offsets in minutes before the reminder anchor; empty = a single reminder at it. */
    val reminderOffsetsMinutes: List<Int> = emptyList(),
    /** How many [windowUnit]s before the due date the task shows in lists. Null = the global window. */
    val windowAmount: Int? = null,
    /** Null = the global upcoming task window. */
    val windowUnit: TaskWindowUnit? = null,
    val createdAt: Instant,
    /**
     * When the user pinned it, to keep it at the top of Home and the Recurring tasks page; null =
     * not pinned. It stays pinned across rounds. Several pinned tasks show oldest pin first.
     */
    val pinnedAt: Instant? = null,
) {
    companion object {
        /** [monthlyOrdinal] value meaning "the last such weekday of the month". */
        const val LAST_ORDINAL = -1
    }
}

/** What happened to one round of a recurring task. */
enum class OccurrenceStatus {
    COMPLETED,

    /** Passed over on purpose, e.g. on holiday: moves the due date on but never counts as done. */
    SKIPPED
}

/**
 * One logged round of a recurring task, completed or skipped; the habit-completion pattern applied
 * to tasks.
 */
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
    /** When the round was completed, or for a [OccurrenceStatus.SKIPPED] row when it was skipped. */
    val completedAt: Instant,
    val status: OccurrenceStatus = OccurrenceStatus.COMPLETED,
    /**
     * The task's due date before this completion advanced it, so unchecking can put it back.
     * Null for backdated entries, which never moved the due date, and for rows restored from
     * backups older than this field.
     */
    val previousDueDate: Instant? = null,
    /**
     * How many checklist items were ticked, and how many there were, when this round was
     * completed. Frozen then: editing the checklist later doesn't rewrite history. Both null when
     * the task had no checklist, for skipped rounds and backdated entries, and for rows from
     * before checklists.
     */
    val subtasksDone: Int? = null,
    val subtasksTotal: Int? = null,
)

/**
 * A recurring task paired with its logged rounds and checklist, for a Room `@Relation` query.
 * [log] holds skipped rounds too; anything that counts work done reads [completed].
 */
data class RecurringTaskWithOccurrences(
    @Embedded
    val task: RecurringTaskEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "recurringTaskId"
    )
    val log: List<RecurringOccurrenceEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "recurringTaskId"
    )
    val subtasks: List<RecurringSubtaskEntity> = emptyList(),
) {
    /** The rounds actually done, without skips. */
    val completed: List<RecurringOccurrenceEntity>
        get() = log.filter { it.status == OccurrenceStatus.COMPLETED }
}

/** The same task with its checklist in display order; Room returns relation rows unordered. */
fun RecurringTaskWithOccurrences.withSortedSubtasks(): RecurringTaskWithOccurrences =
    copy(subtasks = subtasks.sortedBy { it.sortOrder })
