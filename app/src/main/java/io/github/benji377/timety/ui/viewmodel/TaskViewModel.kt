package io.github.benji377.timety.ui.viewmodel

import androidx.glance.appwidget.updateAll
import androidx.lifecycle.viewModelScope
import io.github.benji377.timety.data.model.task.SubtaskEntity
import io.github.benji377.timety.data.model.task.TaskEntity
import io.github.benji377.timety.data.model.task.TaskWithSubtasks
import io.github.benji377.timety.data.repository.TaskRepository
import io.github.benji377.timety.data.repository.UserRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
            io.github.benji377.timety.widget.TaskWidget().updateAll(application)
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
            val xpAmount = io.github.benji377.timety.util.stats.ExperienceEngine.xpPerTask
            if (updatedTask.isCompleted) {
                userRepository.addXp(xpAmount) // XP per task
            } else {
                userRepository.addXp(-xpAmount) // Revert XP
            }
        }
    }

    fun markTaskCompleted(taskId: String) {
        val taskWithSubtasks = allTasks.value.find { it.task.id == taskId } ?: return
        if (taskWithSubtasks.task.isCompleted) return
        val updatedTask = taskWithSubtasks.task.copy(
            isCompleted = true,
            completedAt = Instant.now()
        )
        viewModelScope.launch {
            taskRepository.updateTask(updatedTask)
            scheduleTaskReminders(updatedTask)
            userRepository.addXp(io.github.benji377.timety.util.stats.ExperienceEngine.xpPerTask)
            updateWidgets()
        }
    }

    private fun scheduleTaskReminders(task: TaskEntity) {
        val notificationService =
            io.github.benji377.timety.services.NotificationService(application)
        val baseId = task.id.hashCode()
        for (i in 0..10) notificationService.cancelNotification(baseId + i)

        if (task.isCompleted) return

        val now = Instant.now()
        var scheduledCount = 0

        task.reminders.forEach { reminder ->
            if (reminder.isAfter(now)) {
                val diffMinutes =
                    java.time.Duration.between(reminder, task.dueDate ?: reminder).toMinutes()
                val bodyText = if (diffMinutes > 0) application.getString(
                    io.github.benji377.timety.R.string.homeSectionTasksDue,
                    diffMinutes
                ) else task.category.ifBlank { application.getString(io.github.benji377.timety.R.string.globalLabelTask) }

                notificationService.scheduleTaskReminder(
                    notificationId = baseId + scheduledCount,
                    title = application.getString(
                        io.github.benji377.timety.R.string.reminderTaskTitle,
                        task.title
                    ),
                    body = bodyText,
                    scheduledTime = reminder
                )
                scheduledCount++
            }
        }

        if (task.reminders.isEmpty() && task.dueDate != null && task.dueDate.isAfter(now)) {
            notificationService.scheduleTaskReminder(
                notificationId = baseId + scheduledCount,
                title = application.getString(
                    io.github.benji377.timety.R.string.reminderTaskTitle,
                    task.title
                ),
                body = task.category.ifBlank { application.getString(io.github.benji377.timety.R.string.globalLabelTask) },
                scheduledTime = task.dueDate
            )
        }
    }

    private fun cancelTaskReminders(taskId: String) {
        val notificationService =
            io.github.benji377.timety.services.NotificationService(application)
        val baseId = taskId.hashCode()
        for (i in 0..10) notificationService.cancelNotification(baseId + i)
    }

    fun getAllCategories(): List<String> {
        return allTasks.value.map { it.task.category }.filter { it.isNotBlank() }.distinct()
            .sorted()
    }

    fun renameCategory(oldName: String, newName: String) {
        viewModelScope.launch {
            allTasks.value.filter { it.task.category == oldName }.forEach { taskWithSubtasks ->
                taskRepository.updateTask(taskWithSubtasks.task.copy(category = newName))
            }
        }
    }

    fun deleteCategory(categoryName: String) {
        viewModelScope.launch {
            allTasks.value.filter { it.task.category == categoryName }.forEach { taskWithSubtasks ->
                taskRepository.updateTask(taskWithSubtasks.task.copy(category = ""))
            }
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
