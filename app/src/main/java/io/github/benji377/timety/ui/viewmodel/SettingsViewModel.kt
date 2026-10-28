package io.github.benji377.timety.ui.viewmodel

import android.net.Uri
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.viewModelScope
import io.github.benji377.timety.data.model.focus.DailyFocusTarget
import io.github.benji377.timety.data.repository.AccordionKey
import io.github.benji377.timety.data.repository.SettingsRepository
import io.github.benji377.timety.data.repository.ThemeMode
import io.github.benji377.timety.services.BackupService
import io.github.benji377.timety.services.ReminderScheduler
import io.github.benji377.timety.widget.TaskWidget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Result of the last backup restore, held until the settings UI has shown it. */
sealed interface RestoreOutcome {
    data object Success : RestoreOutcome

    /** [message] is the error's own English text: it varies too much to translate. */
    data class Failure(val message: String) : RestoreOutcome
}

private const val MAX_FAILURE_MESSAGE_LENGTH = 300

/** Exposes user-configurable app settings as state and persists changes through [SettingsRepository]. */
class SettingsViewModel(
    private val application: android.app.Application,
    private val repository: SettingsRepository,
    private val backupService: BackupService
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
    val askFocusOnTaskComplete = repository.askFocusOnTaskCompleteFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        true
    )
    val showHomeGreeting = repository.showHomeGreetingFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        true
    )
    val dailyFocusTarget = repository.dailyFocusTargetFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        DailyFocusTarget()
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

    private val _restoreOutcome = MutableStateFlow<RestoreOutcome?>(null)
    val restoreOutcome: StateFlow<RestoreOutcome?> = _restoreOutcome.asStateFlow()

    /**
     * Restores a backup in the view model's scope, so leaving the settings page mid-restore can't
     * cancel it halfway. The outcome waits in [restoreOutcome] until [consumeRestoreOutcome].
     */
    fun restoreBackup(uri: Uri) = viewModelScope.launch {
        _restoreOutcome.value = backupService.importFromUri(uri).fold(
            onSuccess = { RestoreOutcome.Success },
            onFailure = {
                // Capped: some messages quote the offending data at length.
                RestoreOutcome.Failure((it.message ?: it.javaClass.simpleName).take(MAX_FAILURE_MESSAGE_LENGTH))
            },
        )
    }

    fun consumeRestoreOutcome() {
        _restoreOutcome.value = null
    }

    fun setThemePref(theme: ThemeMode) =
        viewModelScope.launch { repository.saveThemePref(theme) }

    fun setUse24HourFormat(use24Hour: Boolean) = viewModelScope.launch {
        repository.saveUse24HourFormat(use24Hour)
        resyncNotifications()
        // The task widget shows due times, and only redraws on a data change otherwise.
        TaskWidget().updateAll(application)
    }

    fun setDateFormat(format: String) = viewModelScope.launch { repository.saveDateFormat(format) }
    fun setAutoCompleteFocus(autoComplete: Boolean) =
        viewModelScope.launch { repository.saveAutoCompleteFocus(autoComplete) }

    fun setAskFocusOnTaskComplete(ask: Boolean) =
        viewModelScope.launch { repository.saveAskFocusOnTaskComplete(ask) }

    fun setShowHomeGreeting(show: Boolean) =
        viewModelScope.launch { repository.saveShowHomeGreeting(show) }

    fun setDailyGoalMins(mins: Int) = viewModelScope.launch { repository.saveDailyGoalMins(mins) }

    fun toggleDailyGoalWeekday(day: Int) =
        viewModelScope.launch { repository.toggleDailyGoalWeekday(day) }

    fun setMaxStopwatchMins(mins: Int) =
        viewModelScope.launch { repository.saveMaxStopwatchMins(mins) }

    fun setMaxNodeMins(mins: Int) = viewModelScope.launch { repository.saveMaxNodeMins(mins) }
    fun setUpcomingTasksHorizon(days: Int) = viewModelScope.launch {
        repository.saveUpcomingTasksHorizon(days)
        TaskWidget().updateAll(application)
    }

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

    // Memoized per key: a plain function calling stateIn() on every call would spin up a new hot
    // flow each recomposition, so the same StateFlow instance is reused across calls for a key.
    private val accordionExpandedFlows = mutableMapOf<AccordionKey, StateFlow<Boolean>>()

    fun accordionExpanded(key: AccordionKey): StateFlow<Boolean> =
        accordionExpandedFlows.getOrPut(key) {
            repository.accordionExpandedFlow(key).stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                key.defaultExpanded
            )
        }

    fun setAccordionExpanded(key: AccordionKey, expanded: Boolean) =
        viewModelScope.launch { repository.saveAccordionExpanded(key, expanded) }

    private suspend fun resyncNotifications() {
        ReminderScheduler.resyncAll(application)
    }

    /**
     * Whether the exact-alarm prompt may show at launch: until the user grants the permission
     * (checked by the caller) or picks "Don't ask again". "Not now" asks again next launch.
     */
    suspend fun isExactAlarmPromptAllowed(): Boolean = !repository.exactAlarmPromptDeclinedFlow.first()

    fun declineExactAlarmPrompt() =
        viewModelScope.launch { repository.saveExactAlarmPromptDeclined(true) }

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
