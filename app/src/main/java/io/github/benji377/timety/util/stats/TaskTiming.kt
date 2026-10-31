package io.github.benji377.timety.util.stats

import io.github.benji377.timety.data.model.task.RecurringTaskWithOccurrences
import io.github.benji377.timety.data.model.task.TaskEntity
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

/** Completions that had a due date, and how many of them met it. */
data class DueDateRate(val onTime: Int, val total: Int) {
    val percent: Int? get() = if (total == 0) null else (onTime * 100.0 / total).roundToInt()
}

/** When tasks get done relative to their due date and creation. */
object TaskTiming {
    /** When tasks got done: each completed one-off task and each logged recurring completion. */
    fun completions(tasks: List<TaskEntity>, recurring: List<RecurringTaskWithOccurrences>): List<Instant> =
        tasks.mapNotNull { task -> task.completedAt.takeIf { task.isCompleted } } +
            recurring.flatMap { item -> item.occurrences.map { it.completedAt } }

    private const val SECONDS_PER_DAY = 86_400.0

    /**
     * Completions in [range] against their due date, compared by calendar day. Recurring tasks count
     * per occurrence that recorded its due date; backdated occurrences have none and are left out.
     */
    fun dueDateRate(
        tasks: List<TaskEntity>,
        recurring: List<RecurringTaskWithOccurrences>,
        range: ClosedRange<LocalDate>,
        zone: ZoneId,
    ): DueDateRate {
        val taskPairs = tasks.mapNotNull { task ->
            val completedAt = task.completedAt.takeIf { task.isCompleted }
            val due = task.dueDate
            if (completedAt != null && due != null) completedAt to due else null
        }
        val occurrencePairs = recurring.flatMap { it.occurrences }.mapNotNull { occurrence ->
            occurrence.previousDueDate?.let { occurrence.completedAt to it }
        }
        val inRange = (taskPairs + occurrencePairs).map { (completedAt, due) ->
            completedAt.atZone(zone).toLocalDate() to due.atZone(zone).toLocalDate()
        }.filter { (completedOn, _) -> completedOn in range }
        return DueDateRate(inRange.count { (completedOn, dueOn) -> !completedOn.isAfter(dueOn) }, inRange.size)
    }

    /**
     * The median days from creation to completion of one-off tasks finished in [range], null when
     * there are none. Recurring tasks have no creation date per occurrence.
     */
    fun medianDaysToFinish(
        tasks: List<TaskEntity>,
        range: ClosedRange<LocalDate>,
        zone: ZoneId,
    ): Double? {
        val days = tasks.mapNotNull { task ->
            val completedAt: Instant = task.completedAt.takeIf { task.isCompleted } ?: return@mapNotNull null
            if (completedAt.atZone(zone).toLocalDate() !in range) return@mapNotNull null
            // A task completed with a backdated time can read as finished before it was created.
            Duration.between(task.createdAt, completedAt).seconds.coerceAtLeast(0) / SECONDS_PER_DAY
        }.sorted()
        if (days.isEmpty()) return null
        val middle = days.size / 2
        return if (days.size % 2 == 1) days[middle] else (days[middle - 1] + days[middle]) / 2
    }
}
