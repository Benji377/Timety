package io.github.benji377.timety.ui.viewmodel

import androidx.glance.appwidget.updateAll
import androidx.lifecycle.viewModelScope
import io.github.benji377.timety.data.model.task.RecurringOccurrenceEntity
import io.github.benji377.timety.data.model.task.RecurringTaskEntity
import io.github.benji377.timety.data.model.task.RecurringTaskWithOccurrences
import io.github.benji377.timety.data.repository.RecurringTaskRepository
import io.github.benji377.timety.data.repository.UserRepository
import io.github.benji377.timety.services.CompletionActions
import io.github.benji377.timety.services.RecurringCompletionUndo
import io.github.benji377.timety.services.ReminderScheduler
import io.github.benji377.timety.util.stats.ExperienceEngine
import io.github.benji377.timety.widget.TaskWidget
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant

/** Exposes recurring tasks with their occurrence logs, and applies XP/reminder side effects. */
class RecurringTaskViewModel(
    private val application: android.app.Application,
    private val recurringTaskRepository: RecurringTaskRepository,
    private val userRepository: UserRepository
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
     * reschedules its reminders, and awards XP. [onCompleted] receives the data needed to offer
     * an Undo action (see [undoCompleteOccurrence]).
     */
    fun completeOccurrence(
        task: RecurringTaskEntity,
        onCompleted: (RecurringCompletionUndo) -> Unit = {},
    ) {
        viewModelScope.launch {
            onCompleted(
                CompletionActions.completeRecurringOccurrence(
                    application, recurringTaskRepository, userRepository, task
                )
            )
        }
    }

    /** Reverts a completion: removes its occurrence, restores the due date, and takes back the XP. */
    fun undoCompleteOccurrence(undo: RecurringCompletionUndo) {
        viewModelScope.launch {
            recurringTaskRepository.deleteOccurrenceById(undo.occurrenceId)
            val restored = undo.advancedTask.copy(dueDate = undo.previousDueDate)
            recurringTaskRepository.updateTask(restored)
            scheduleReminders(restored)
            userRepository.addXp(-ExperienceEngine.XP_PER_TASK)
            updateWidgets()
        }
    }

    /** Logs a manually backdated occurrence and awards XP; the due date is left untouched. */
    fun addPastOccurrence(taskId: String, completedAt: Instant) {
        viewModelScope.launch {
            recurringTaskRepository.insertOccurrence(
                RecurringOccurrenceEntity(recurringTaskId = taskId, completedAt = completedAt)
            )
            userRepository.addXp(ExperienceEngine.XP_PER_TASK)
        }
    }

    /** Removes a logged occurrence and reverts the XP it had granted. */
    fun deleteOccurrence(occurrence: RecurringOccurrenceEntity) {
        viewModelScope.launch {
            recurringTaskRepository.deleteOccurrence(occurrence)
            userRepository.addXp(-ExperienceEngine.XP_PER_TASK)
        }
    }

    private suspend fun scheduleReminders(task: RecurringTaskEntity) {
        ReminderScheduler.create(application).scheduleRecurringTaskReminders(task)
    }
}
