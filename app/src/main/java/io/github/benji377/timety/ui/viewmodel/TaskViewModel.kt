package io.github.benji377.timety.ui.viewmodel

import androidx.glance.appwidget.updateAll
import androidx.lifecycle.viewModelScope
import io.github.benji377.timety.data.model.task.SubtaskEntity
import io.github.benji377.timety.data.model.task.TaskEntity
import io.github.benji377.timety.data.model.task.TaskWithSubtasks
import io.github.benji377.timety.data.repository.TaskRepository
import io.github.benji377.timety.data.repository.UserRepository
import io.github.benji377.timety.services.ReminderScheduler
import io.github.benji377.timety.util.stats.ExperienceEngine
import io.github.benji377.timety.widget.TaskWidget
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant

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
                userRepository.addXp(xpAmount) // XP per task
            } else {
                userRepository.addXp(-xpAmount) // Revert XP
            }
        }
    }

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

    val allCategories: StateFlow<List<String>> = taskRepository.allTasks
        .map { tasks ->
            tasks.map { it.task.category }.filter { it.isNotBlank() }.distinct().sorted()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun renameCategory(oldName: String, newName: String) {
        viewModelScope.launch {
            taskRepository.renameCategory(oldName, newName)
        }
    }

    fun deleteCategory(categoryName: String) {
        viewModelScope.launch {
            taskRepository.renameCategory(categoryName, "")
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
