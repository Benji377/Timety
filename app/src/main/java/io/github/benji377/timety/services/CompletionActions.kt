package io.github.benji377.timety.services

import android.content.Context
import androidx.glance.appwidget.updateAll
import io.github.benji377.timety.data.model.task.RecurringOccurrenceEntity
import io.github.benji377.timety.data.model.task.RecurringTaskEntity
import io.github.benji377.timety.data.repository.RecurringTaskRepository
import io.github.benji377.timety.data.repository.TaskRepository
import io.github.benji377.timety.util.task.RecurrenceUtils
import io.github.benji377.timety.widget.TaskWidget
import kotlinx.coroutines.sync.withLock
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
     * reschedules its reminders and refreshes the task widget. Reads the task from the DB and
     * returns null, changing nothing, when it is gone or its due date is no longer the one in
     * [task]: a second tap from a list that hasn't caught up yet must not complete it twice.
     */
    suspend fun completeRecurringOccurrence(
        context: Context,
        recurringTaskRepository: RecurringTaskRepository,
        task: RecurringTaskEntity,
    ): RecurringCompletionUndo? {
        val undo = recurringTaskRepository.completionMutex.withLock {
            val stored = recurringTaskRepository.getTaskById(task.id) ?: return null
            if (stored.dueDate != task.dueDate) return null
            val now = Instant.now()
            val occurrenceId = recurringTaskRepository.insertOccurrence(
                RecurringOccurrenceEntity(
                    recurringTaskId = stored.id,
                    completedAt = now,
                    previousDueDate = stored.dueDate,
                )
            )
            val advanced = stored.copy(dueDate = RecurrenceUtils.nextDueDate(stored, now))
            recurringTaskRepository.updateTask(advanced)
            RecurringCompletionUndo(occurrenceId, stored.dueDate, advanced)
        }
        rescheduleRecurring(context, undo.advancedTask)
        return undo
    }

    /**
     * Removes a logged occurrence. When it is the completion that last advanced the task and the
     * task is still exactly where that completion left it, the due date goes back too; otherwise
     * (a backdated entry, an older completion, or a task edited since) only the log entry goes,
     * so the schedule never lands somewhere the user didn't set.
     */
    suspend fun uncheckRecurringOccurrence(
        context: Context,
        recurringTaskRepository: RecurringTaskRepository,
        occurrence: RecurringOccurrenceEntity,
    ) {
        val restored = recurringTaskRepository.completionMutex.withLock {
            val task = recurringTaskRepository.getTaskById(occurrence.recurringTaskId)
            val previous = occurrence.previousDueDate
            val restorable = task != null && previous != null &&
                recurringTaskRepository.getLatestAdvancingOccurrenceId(task.id) == occurrence.id &&
                task.dueDate == RecurrenceUtils.nextDueDate(task.copy(dueDate = previous), occurrence.completedAt)
            recurringTaskRepository.deleteOccurrenceById(occurrence.id)
            if (restorable) task.copy(dueDate = previous).also { recurringTaskRepository.updateTask(it) } else null
        }
        if (restored != null) rescheduleRecurring(context, restored) else TaskWidget().updateAll(context)
    }

    private suspend fun rescheduleRecurring(context: Context, task: RecurringTaskEntity) {
        val scheduler = ReminderScheduler.create(context)
        scheduler.cancelRecurringTaskReminders(task.id)
        scheduler.scheduleRecurringTaskReminders(task)
        TaskWidget().updateAll(context)
    }
}
