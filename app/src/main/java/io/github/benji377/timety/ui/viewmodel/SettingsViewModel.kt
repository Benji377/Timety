package io.github.benji377.timety.ui.viewmodel

import androidx.lifecycle.viewModelScope
import io.github.benji377.timety.data.repository.SettingsRepository
import io.github.benji377.timety.data.repository.ThemeMode
import io.github.benji377.timety.services.ReminderScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Exposes user-configurable app settings as state and persists changes through [SettingsRepository]. */
class SettingsViewModel(
    private val application: android.app.Application,
    private val repository: SettingsRepository
) : androidx.lifecycle.AndroidViewModel(application) {
    val themePref = repository.themePrefFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ThemeMode.SYSTEM
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
    val keepScreenOnDuringFocus = repository.keepScreenOnDuringFocusFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        false
    )
    val autoDndEnabled = repository.autoDndEnabledFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        false
    )
    val autoDndLiftDuringBreaks = repository.autoDndLiftDuringBreaksFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        false
    )

    fun setThemePref(theme: ThemeMode) =
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
        ReminderScheduler.create(application)
            .scheduleDailyMotivation(time)
    }

    fun setEndOfDayCheckupTime(time: String) = viewModelScope.launch {
        repository.saveEndOfDayCheckupTime(time)
        ReminderScheduler.create(application)
            .scheduleEndOfDayCheckup(time)
    }

    fun setLocationApiEndpoint(endpoint: String) =
        viewModelScope.launch { repository.saveLocationApiEndpoint(endpoint) }

    fun setAppLocaleCode(code: String) = viewModelScope.launch {
        repository.saveAppLocaleCode(code)
        resyncNotifications()
    }

    fun setKeepScreenOnDuringFocus(keepOn: Boolean) =
        viewModelScope.launch { repository.saveKeepScreenOnDuringFocus(keepOn) }

    fun setAutoDndEnabled(enabled: Boolean) =
        viewModelScope.launch { repository.saveAutoDndEnabled(enabled) }

    fun setAutoDndLiftDuringBreaks(lift: Boolean) =
        viewModelScope.launch { repository.saveAutoDndLiftDuringBreaks(lift) }


    private suspend fun resyncNotifications() {
        ReminderScheduler.resyncAll(application)
    }

    /** Checks that [url] is a reachable Photon-compatible geocoding endpoint before it is saved. */
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
