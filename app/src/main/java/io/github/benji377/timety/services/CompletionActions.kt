package io.github.benji377.timety.services

import android.content.Context
import androidx.glance.appwidget.updateAll
import io.github.benji377.timety.data.model.task.RecurringOccurrenceEntity
import io.github.benji377.timety.data.model.task.RecurringTaskEntity
import io.github.benji377.timety.data.repository.RecurringTaskRepository
import io.github.benji377.timety.data.repository.TaskRepository
import io.github.benji377.timety.util.task.RecurrenceUtils
import io.github.benji377.timety.widget.TaskWidget
import java.time.Instant

/** What [CompletionActions.completeRecurringOccurrence] changed, so an Undo action can revert it. */
data class RecurringCompletionUndo(
    val occurrenceId: Long,
    val previousDueDate: Instant,
    /** The task as stored after completion, i.e. with the advanced due date. */
    val advancedTask: RecurringTaskEntity,
)

/**
 * Completion side effects shared by every entry point that checks off a task or a recurring
 * task: the view models, the home-screen widget, and the focus auto-complete. Keeping one copy
 * means reminders and the task widget can't drift apart between them.
 */
object CompletionActions {
    /**
     * Marks the task complete, re-syncs its reminders and refreshes the task widget.
     * Reads the task from the DB rather than a UI snapshot, which may not have caught up yet.
     * Returns false, changing nothing, when the task is missing or already complete.
     */
    suspend fun completeTask(
        context: Context,
        taskRepository: TaskRepository,
        taskId: String,
    ): Boolean {
        val task = taskRepository.getTaskById(taskId) ?: return false
        if (task.isCompleted) return false
        val updated = task.copy(isCompleted = true, completedAt = Instant.now())
        taskRepository.updateTask(updated)
        ReminderScheduler.create(context).scheduleTaskReminders(updated)
        TaskWidget().updateAll(context)
        return true
    }

    /**
     * Logs a completed occurrence now, rolls the task's due date forward to the next occurrence,
     * reschedules its reminders and refreshes the task widget.
     */
    suspend fun completeRecurringOccurrence(
        context: Context,
        recurringTaskRepository: RecurringTaskRepository,
        task: RecurringTaskEntity,
    ): RecurringCompletionUndo {
        val now = Instant.now()
        val occurrenceId = recurringTaskRepository.insertOccurrence(
            RecurringOccurrenceEntity(recurringTaskId = task.id, completedAt = now)
        )
        val advanced = task.copy(dueDate = RecurrenceUtils.nextDueDate(task, now))
        recurringTaskRepository.updateTask(advanced)
        val scheduler = ReminderScheduler.create(context)
        scheduler.cancelRecurringTaskReminders(task.id)
        scheduler.scheduleRecurringTaskReminders(advanced)
        TaskWidget().updateAll(context)
        return RecurringCompletionUndo(occurrenceId, task.dueDate, advanced)
    }
}
