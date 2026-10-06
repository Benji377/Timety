package io.github.benji377.timety.util.reminder

import io.github.benji377.timety.data.model.habit.HabitWithCompletions
import io.github.benji377.timety.data.model.task.RecurringTaskEntity
import io.github.benji377.timety.data.model.task.TaskEntity
import io.github.benji377.timety.util.habit.HabitUtils
import java.time.Instant

/** One open item in the evening check-up. */
data class EveningItem(val name: String, val isTask: Boolean)

/** The first items of an [EveningSummary] that fit a notification, and how many were left out. */
data class EveningPreview(val items: List<EveningItem>, val hiddenCount: Int)

/** What the evening check-up still has open: overdue tasks, most overdue first, and today's unfinished habits. */
data class EveningSummary(val taskTitles: List<String>, val habitNames: List<String>) {
    val total: Int get() = taskTitles.size + habitNames.size
    val isAllDone: Boolean get() = total == 0

    /** Up to [max] items, overdue tasks before habits, with the count of those that didn't fit. */
    fun preview(max: Int): EveningPreview {
        val all = taskTitles.map { EveningItem(it, isTask = true) } +
                habitNames.map { EveningItem(it, isTask = false) }
        return EveningPreview(all.take(max), (all.size - max).coerceAtLeast(0))
    }
}

/** Collects the evening check-up's open items. Pure and unit-testable. */
object EveningSummaryBuilder {

    /**
     * Overdue tasks are incomplete ones whose due date has passed, recurring tasks included; habits
     * are those [HabitUtils.needsReminderToday] says still need doing.
     */
    fun build(
        habits: List<HabitWithCompletions>,
        tasks: List<TaskEntity>,
        recurringTasks: List<RecurringTaskEntity>,
        now: Instant,
    ): EveningSummary {
        val overdueTasks = tasks.mapNotNull { task ->
            task.dueDate
                ?.takeIf { !task.isCompleted && it.isBefore(now) }
                ?.let { it to task.title }
        }
        val overdueRecurring = recurringTasks
            .filter { it.dueDate.isBefore(now) }
            .map { it.dueDate to it.title }
        return EveningSummary(
            taskTitles = (overdueTasks + overdueRecurring).sortedBy { it.first }.map { it.second },
            habitNames = habits.filter { HabitUtils.needsReminderToday(it) }.map { it.habit.name },
        )
    }
}
