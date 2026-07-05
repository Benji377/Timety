package io.github.benji377.timety.ui.viewmodel

import androidx.lifecycle.viewModelScope
import io.github.benji377.timety.data.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val application: android.app.Application,
    private val repository: SettingsRepository
) : androidx.lifecycle.AndroidViewModel(application) {
    val themePref = repository.themePrefFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        io.github.benji377.timety.data.repository.ThemeMode.SYSTEM
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

    fun setThemePref(theme: io.github.benji377.timety.data.repository.ThemeMode) =
        viewModelScope.launch { repository.saveThemePref(theme) }

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
        io.github.benji377.timety.services.ReminderScheduler.create(application)
            .scheduleDailyMotivation(time)
    }

    fun setEndOfDayCheckupTime(time: String) = viewModelScope.launch {
        repository.saveEndOfDayCheckupTime(time)
        io.github.benji377.timety.services.ReminderScheduler.create(application)
            .scheduleEndOfDayCheckup(time)
    }

    fun setLocationApiEndpoint(endpoint: String) =
        viewModelScope.launch { repository.saveLocationApiEndpoint(endpoint) }

    fun setAppLocaleCode(code: String) = viewModelScope.launch {
        repository.saveAppLocaleCode(code)
        resyncNotifications()
    }


    private suspend fun resyncNotifications() {
        io.github.benji377.timety.services.ReminderScheduler.resyncAll(application)
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
