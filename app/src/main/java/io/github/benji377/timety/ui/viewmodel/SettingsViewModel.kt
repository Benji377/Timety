package io.github.benji377.timety.ui.viewmodel

import androidx.lifecycle.viewModelScope
import io.github.benji377.timety.data.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val application: android.app.Application,
    private val repository: SettingsRepository,
    private val taskRepository: io.github.benji377.timety.data.repository.TaskRepository,
    private val habitRepository: io.github.benji377.timety.data.repository.HabitRepository
) : androidx.lifecycle.AndroidViewModel(application) {
    val themePref = repository.themePrefFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        "System Default"
    )
    val use24HourFormat = repository.use24HourFormatFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        true
    )
    val dateFormat = repository.dateFormatFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        "System"
    )
    val autoCompleteFocus = repository.autoCompleteFocusFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        false
    )
    val dailyGoalMins = repository.dailyGoalMinsFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        90
    )
    val maxStopwatchMins = repository.maxStopwatchMinsFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        120
    )
    val maxNodeMins = repository.maxNodeMinsFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        240
    )
    val upcomingTasksHorizon = repository.upcomingTasksHorizonFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        7
    )
    val dailyMotivationTime = repository.dailyMotivationTimeFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        "08:00"
    )
    val endOfDayCheckupTime = repository.endOfDayCheckupTimeFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        "20:00"
    )
    val locationApiEndpoint = repository.locationApiEndpointFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        "https://photon.komoot.io/api/"
    )
    val appLocaleCode = repository.appLocaleCodeFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        "system"
    )

    fun setThemePref(theme: String) = viewModelScope.launch { repository.saveThemePref(theme) }
    fun setUse24HourFormat(use24Hour: Boolean) = viewModelScope.launch {
        repository.saveUse24HourFormat(use24Hour)
        resyncNotifications()
    }

    fun setDateFormat(format: String) = viewModelScope.launch { repository.saveDateFormat(format) }
    fun setAutoCompleteFocus(autoComplete: Boolean) =
        viewModelScope.launch { repository.saveAutoCompleteFocus(autoComplete) }

    fun setDailyGoalMins(mins: Int) = viewModelScope.launch { repository.saveDailyGoalMins(mins) }
    fun setMaxStopwatchMins(mins: Int) =
        viewModelScope.launch { repository.saveMaxStopwatchMins(mins) }

    fun setMaxNodeMins(mins: Int) = viewModelScope.launch { repository.saveMaxNodeMins(mins) }
    fun setUpcomingTasksHorizon(days: Int) =
        viewModelScope.launch { repository.saveUpcomingTasksHorizon(days) }

    fun setDailyMotivationTime(time: String) = viewModelScope.launch {
        repository.saveDailyMotivationTime(time)
        val parts = time.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 8
        val min = parts.getOrNull(1)?.toIntOrNull() ?: 0
        io.github.benji377.timety.services.NotificationService(application).scheduleDailyMotivation(
            hour, min,
            application.getString(io.github.benji377.timety.R.string.notificationMotivationTitle),
            application.getString(io.github.benji377.timety.R.string.notificationMotivationBody)
        )
    }

    fun setEndOfDayCheckupTime(time: String) = viewModelScope.launch {
        repository.saveEndOfDayCheckupTime(time)
        val parts = time.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 20
        val min = parts.getOrNull(1)?.toIntOrNull() ?: 0
        io.github.benji377.timety.services.NotificationService(application).scheduleEndOfDayCheckup(
            hour, min,
            application.getString(io.github.benji377.timety.R.string.notificationEveningTitle),
            application.getString(io.github.benji377.timety.R.string.notificationEveningBody)
        )
    }

    fun setLocationApiEndpoint(endpoint: String) =
        viewModelScope.launch { repository.saveLocationApiEndpoint(endpoint) }

    fun setAppLocaleCode(code: String) = viewModelScope.launch {
        repository.saveAppLocaleCode(code)
        resyncNotifications()
    }

    private suspend fun resyncNotifications() {
        val notificationService =
            io.github.benji377.timety.services.NotificationService(application)
        val tasks = taskRepository.allTasks.firstOrNull() ?: emptyList()
        val habits = habitRepository.allHabits.firstOrNull() ?: emptyList()

        tasks.forEach { taskWithSubtasks ->
            val task = taskWithSubtasks.task
            if (!task.isCompleted) {
                val baseId = task.id.hashCode()
                for (i in 0..10) notificationService.cancelNotification(baseId + i)

                var scheduledCount = 0
                val now = java.time.Instant.now()

                task.reminders.forEach { reminder ->
                    if (reminder.isAfter(now)) {
                        notificationService.scheduleTaskReminder(
                            notificationId = baseId + scheduledCount,
                            title = application.getString(
                                io.github.benji377.timety.R.string.reminderTaskTitle,
                                task.title
                            ),
                            body = task.category.ifBlank { application.getString(io.github.benji377.timety.R.string.globalLabelTask) },
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
        }

        habits.forEach { habit ->
            notificationService.cancelHabitReminder(habit.id)
            val mins = habit.targetTimeMinutes ?: return@forEach
            try {
                val hour = mins / 60
                val minute = mins % 60
                val targetWeekdaysList =
                    habit.targetWeekdays?.removePrefix("[")?.removeSuffix("]")?.split(",")
                        ?.mapNotNull { it.trim().toIntOrNull() }

                notificationService.scheduleHabitReminder(
                    habitId = habit.id,
                    title = application.getString(
                        io.github.benji377.timety.R.string.reminderHabitTitle,
                        habit.name
                    ),
                    body = application.getString(io.github.benji377.timety.R.string.globalLabelHabit),
                    hour = hour,
                    minute = minute,
                    targetWeekdays = targetWeekdaysList
                )
            } catch (e: Exception) {
                // Ignore parse errors
            }
        }
    }

    suspend fun validateLocationApiEndpoint(url: String): Boolean {
        if (url.isEmpty()) return false
        if (!url.startsWith("http://") && !url.startsWith("https://")) return false
        return kotlinx.coroutines.Dispatchers.IO.let { ioDispatcher ->
            kotlinx.coroutines.withContext(ioDispatcher) {
                try {
                    val formattedUrl = if (url.endsWith("/")) url else "$url/"
                    val testUrl = java.net.URL("${formattedUrl}?q=test&limit=1")
                    val connection = testUrl.openConnection() as java.net.HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.setRequestProperty(
                        "User-Agent",
                        "timety/1.0 (io.github.benji377.timety)"
                    )
                    connection.setRequestProperty("Accept", "application/json")
                    connection.connectTimeout = 3000
                    connection.readTimeout = 3000
                    connection.responseCode == 200
                } catch (e: Exception) {
                    false
                }
            }
        }
    }
}
