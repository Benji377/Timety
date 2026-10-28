package io.github.benji377.timety.ui.viewmodel

import androidx.glance.appwidget.updateAll
import androidx.lifecycle.viewModelScope
import io.github.benji377.timety.data.model.task.RecurringOccurrenceEntity
import io.github.benji377.timety.data.model.task.RecurringTaskEntity
import io.github.benji377.timety.data.model.task.RecurringTaskWithOccurrences
import io.github.benji377.timety.data.repository.RecurringTaskRepository
import io.github.benji377.timety.services.CompletionActions
import io.github.benji377.timety.services.RecurringCompletionUndo
import io.github.benji377.timety.services.ReminderScheduler
import io.github.benji377.timety.widget.TaskWidget
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant

/** Exposes recurring tasks with their occurrence logs, and applies reminder side effects. */
class RecurringTaskViewModel(
    private val application: android.app.Application,
    private val recurringTaskRepository: RecurringTaskRepository
) : androidx.lifecycle.AndroidViewModel(application) {
    val allRecurringTasks: StateFlow<List<RecurringTaskWithOccurrences>> =
        recurringTaskRepository.allRecurringTasks
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    /** The recurring task being viewed in the editor, without loading every other one. */
    fun observeTask(id: String): Flow<RecurringTaskWithOccurrences?> =
        recurringTaskRepository.observeTask(id)

    private fun updateWidgets() {
        viewModelScope.launch {
            TaskWidget().updateAll(application)
        }
    }

    fun addTask(task: RecurringTaskEntity) {
        viewModelScope.launch {
            recurringTaskRepository.insertTask(task)
            scheduleReminders(task)
            updateWidgets()
        }
    }

    fun updateTask(task: RecurringTaskEntity) {
        viewModelScope.launch {
            recurringTaskRepository.updateTask(task)
            scheduleReminders(task)
            updateWidgets()
        }
    }

    fun deleteTask(task: RecurringTaskEntity) {
        viewModelScope.launch {
            recurringTaskRepository.deleteTask(task)
            ReminderScheduler.create(application).cancelRecurringTaskReminders(task.id)
            updateWidgets()
        }
    }

    /**
     * Logs a completed occurrence now, rolls the task's due date forward to the next occurrence,
     * and reschedules its reminders. [onCompleted] receives the data needed to offer
     * an Undo action (see [undoCompleteOccurrence]); it isn't called when a repeated tap is ignored.
     */
    fun completeOccurrence(
        task: RecurringTaskEntity,
        onCompleted: (RecurringCompletionUndo) -> Unit = {},
    ) {
        viewModelScope.launch {
            CompletionActions.completeRecurringOccurrence(application, recurringTaskRepository, task)
                ?.let(onCompleted)
        }
    }

    /** Reverts a completion: removes its occurrence and restores the due date. */
    fun undoCompleteOccurrence(undo: RecurringCompletionUndo) {
        viewModelScope.launch {
            recurringTaskRepository.deleteOccurrenceById(undo.occurrenceId)
            val restored = undo.advancedTask.copy(dueDate = undo.previousDueDate)
            recurringTaskRepository.updateTask(restored)
            scheduleReminders(restored)
            updateWidgets()
        }
    }

    /** Logs a manually backdated occurrence; the due date is left untouched. */
    fun addPastOccurrence(taskId: String, completedAt: Instant) {
        viewModelScope.launch {
            recurringTaskRepository.insertOccurrence(
                RecurringOccurrenceEntity(recurringTaskId = taskId, completedAt = completedAt)
            )
        }
    }

    /**
     * Unchecks a completion shown as done, putting the due date back when it is safe to; see
     * [CompletionActions.uncheckRecurringOccurrence].
     */
    fun uncheckOccurrence(occurrence: RecurringOccurrenceEntity) {
        viewModelScope.launch {
            CompletionActions.uncheckRecurringOccurrence(application, recurringTaskRepository, occurrence)
        }
    }

    /** Removes a logged occurrence from the history; the due date is left untouched. */
    fun deleteOccurrence(occurrence: RecurringOccurrenceEntity) {
        viewModelScope.launch {
            recurringTaskRepository.deleteOccurrence(occurrence)
        }
    }

    private suspend fun scheduleReminders(task: RecurringTaskEntity) {
        ReminderScheduler.create(application).scheduleRecurringTaskReminders(task)
    }
}
