package io.github.benji377.timety.ui.viewmodel

import androidx.glance.appwidget.updateAll
import androidx.lifecycle.viewModelScope
import io.github.benji377.timety.data.model.task.SubtaskEntity
import io.github.benji377.timety.data.model.tag.TaggedItemType
import io.github.benji377.timety.data.model.task.TaskEntity
import io.github.benji377.timety.data.model.task.TaskWithSubtasks
import io.github.benji377.timety.data.repository.TagRepository
import io.github.benji377.timety.data.repository.TaskRepository
import io.github.benji377.timety.services.ReminderScheduler
import io.github.benji377.timety.widget.TaskWidget
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant

/** Exposes tasks and subtasks, and applies reminder/widget side effects for changes to them. */
class TaskViewModel(
    private val application: android.app.Application,
    private val taskRepository: TaskRepository,
    private val tagRepository: TagRepository
) : androidx.lifecycle.AndroidViewModel(application) {
    val allTasks: StateFlow<List<TaskWithSubtasks>> = taskRepository.allTasks
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /** The task being viewed in the editor, without loading every other task. */
    fun observeTask(id: String): Flow<TaskWithSubtasks?> = taskRepository.observeTask(id)

    private fun updateWidgets() {
        viewModelScope.launch {
            TaskWidget().updateAll(application)
        }
    }

    fun addTask(task: TaskEntity, subtasks: List<SubtaskEntity>, tagIds: Set<String> = emptySet()) {
        launchToCompletion {
            taskRepository.insertTask(task)
            subtasks.forEach { taskRepository.insertSubtask(it) }
            tagRepository.setItemTags(TaggedItemType.TASK, task.id, tagIds)
            scheduleTaskReminders(task)
            TaskWidget().updateAll(application)
        }
    }

    /** Saves an edited task; null [tagIds] leaves its tags as they are. */
    fun updateTask(task: TaskEntity, tagIds: Set<String>? = null) {
        launchToCompletion {
            taskRepository.updateTask(task)
            tagIds?.let { tagRepository.setItemTags(TaggedItemType.TASK, task.id, it) }
            scheduleTaskReminders(task)
            TaskWidget().updateAll(application)
        }
    }

    fun deleteTask(task: TaskEntity) {
        launchToCompletion {
            taskRepository.deleteTask(task)
            cancelTaskReminders(task.id)
            TaskWidget().updateAll(application)
        }
    }

    fun toggleTaskCompletion(task: TaskEntity) {
        viewModelScope.launch {
            val updatedTask = task.copy(
                isCompleted = !task.isCompleted,
                completedAt = if (!task.isCompleted) Instant.now() else null
            )
            taskRepository.updateTask(updatedTask)
            scheduleTaskReminders(updatedTask)
            updateWidgets()
        }
    }

    private suspend fun scheduleTaskReminders(task: TaskEntity) {
        ReminderScheduler.create(application)
            .scheduleTaskReminders(task)
    }

    private suspend fun cancelTaskReminders(taskId: String) {
        ReminderScheduler.create(application)
            .cancelTaskReminders(taskId)
    }

    fun addSubtask(subtask: SubtaskEntity) {
        viewModelScope.launch {
            taskRepository.insertSubtask(subtask)
        }
    }

    fun updateSubtask(subtask: SubtaskEntity) {
        viewModelScope.launch {
            taskRepository.updateSubtask(subtask)
        }
    }

    fun updateSubtasks(subtasks: List<SubtaskEntity>) {
        viewModelScope.launch {
            taskRepository.updateSubtasks(subtasks)
        }
    }

    fun deleteSubtask(subtask: SubtaskEntity) {
        viewModelScope.launch {
            taskRepository.deleteSubtask(subtask)
        }
    }
}
