package io.github.benji377.timety.ui.viewmodel

import androidx.glance.appwidget.updateAll
import androidx.lifecycle.viewModelScope
import io.github.benji377.timety.data.model.task.SubtaskEntity
import io.github.benji377.timety.data.model.task.TaskCategoryEntity
import io.github.benji377.timety.data.model.task.TaskEntity
import io.github.benji377.timety.data.model.task.TaskWithSubtasks
import io.github.benji377.timety.data.repository.TaskRepository
import io.github.benji377.timety.services.ReminderScheduler
import io.github.benji377.timety.widget.TaskWidget
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID

/** Exposes tasks, subtasks, and categories, and applies reminder/widget side effects for changes to them. */
class TaskViewModel(
    private val application: android.app.Application,
    private val taskRepository: TaskRepository
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

    fun addTask(task: TaskEntity, subtasks: List<SubtaskEntity>) {
        launchToCompletion {
            taskRepository.insertTask(task)
            subtasks.forEach { taskRepository.insertSubtask(it) }
            scheduleTaskReminders(task)
            TaskWidget().updateAll(application)
        }
    }

    fun updateTask(task: TaskEntity) {
        launchToCompletion {
            taskRepository.updateTask(task)
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

    val allCategories: StateFlow<List<TaskCategoryEntity>> = taskRepository.allCategories
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun createCategory(
        name: String,
        colorValue: Int = TaskCategoryEntity.DEFAULT_COLOR_VALUE
    ) {
        viewModelScope.launch {
            taskRepository.createCategory(
                TaskCategoryEntity(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    colorValue = colorValue
                )
            )
        }
    }

    fun updateCategory(category: TaskCategoryEntity, newName: String, newColorValue: Int) {
        viewModelScope.launch {
            taskRepository.updateCategory(
                oldName = category.name,
                updated = category.copy(name = newName, colorValue = newColorValue)
            )
        }
    }

    fun deleteCategory(category: TaskCategoryEntity) {
        viewModelScope.launch {
            taskRepository.deleteCategory(category)
        }
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
