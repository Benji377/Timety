package io.github.benji377.timety.ui.viewmodel

import androidx.glance.appwidget.updateAll
import androidx.lifecycle.viewModelScope
import io.github.benji377.timety.data.model.task.SubtaskEntity
import io.github.benji377.timety.data.model.task.TaskCategoryEntity
import io.github.benji377.timety.data.model.task.TaskEntity
import io.github.benji377.timety.data.model.task.TaskWithSubtasks
import io.github.benji377.timety.data.repository.TaskRepository
import io.github.benji377.timety.data.repository.UserRepository
import io.github.benji377.timety.services.ReminderScheduler
import io.github.benji377.timety.util.stats.ExperienceEngine
import io.github.benji377.timety.widget.TaskWidget
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID

/** Exposes tasks, subtasks, and categories, and applies XP/reminder/widget side effects for changes to them. */
class TaskViewModel(
    private val application: android.app.Application,
    private val taskRepository: TaskRepository,
    private val userRepository: UserRepository
) : androidx.lifecycle.AndroidViewModel(application) {

    val allTasks: StateFlow<List<TaskWithSubtasks>> = taskRepository.allTasks
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private fun updateWidgets() {
        viewModelScope.launch {
            TaskWidget().updateAll(application)
        }
    }

    fun addTask(task: TaskEntity, subtasks: List<SubtaskEntity>) {
        viewModelScope.launch {
            taskRepository.insertTask(task)
            subtasks.forEach { taskRepository.insertSubtask(it) }
            scheduleTaskReminders(task)
            updateWidgets()
        }
    }

    fun updateTask(task: TaskEntity) {
        viewModelScope.launch {
            taskRepository.updateTask(task)
            scheduleTaskReminders(task)
            updateWidgets()
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            taskRepository.deleteTask(task)
            cancelTaskReminders(task.id)
            updateWidgets()
        }
    }

    fun toggleTaskCompletion(task: TaskEntity) {
        val updatedTask = task.copy(
            isCompleted = !task.isCompleted,
            completedAt = if (!task.isCompleted) Instant.now() else null
        )
        viewModelScope.launch {
            taskRepository.updateTask(updatedTask)
            scheduleTaskReminders(updatedTask)
            updateWidgets()
            val xpAmount = ExperienceEngine.XP_PER_TASK
            if (updatedTask.isCompleted) {
                userRepository.addXp(xpAmount)
            } else {
                userRepository.addXp(-xpAmount)
            }
        }
    }

    /** Marks the task complete and awards XP; used by the focus auto-complete flow. */
    fun markTaskCompleted(taskId: String) {
        viewModelScope.launch {
            // Fetched from the DB, not the allTasks snapshot: the flow may not have
            // emitted yet when the focus auto-complete event fires.
            val task = taskRepository.getTaskById(taskId) ?: return@launch
            if (task.isCompleted) return@launch
            val updatedTask = task.copy(
                isCompleted = true,
                completedAt = Instant.now()
            )
            taskRepository.updateTask(updatedTask)
            scheduleTaskReminders(updatedTask)
            userRepository.addXp(ExperienceEngine.XP_PER_TASK)
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

    fun deleteSubtask(subtask: SubtaskEntity) {
        viewModelScope.launch {
            taskRepository.deleteSubtask(subtask)
        }
    }
}
