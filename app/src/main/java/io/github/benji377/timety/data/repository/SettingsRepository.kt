package io.github.benji377.timety.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.benji377.timety.data.model.focus.DailyFocusTarget
import io.github.benji377.timety.util.habit.HabitUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * App settings. A settings file that can't be parsed is replaced with defaults rather than
 * crashing every launch; only preferences are lost, the Room database is untouched.
 */
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "settings",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() }
)

/** Display theme options, persisted using their [storageValue] string. */
enum class ThemeMode(val storageValue: String) {
    LIGHT("Light"),
    DARK("Dark"),
    SYSTEM("System Default");

    companion object {
        fun fromStorage(value: String?): ThemeMode =
            entries.firstOrNull { it.storageValue == value } ?: SYSTEM
    }
}

/** A collapsible accordion section with persisted state; [defaultExpanded] applies until toggled. */
enum class AccordionKey(val storageKey: String, val defaultExpanded: Boolean) {
    HOME_TASKS_DUE("home_tasks_due", true),
    HOME_HABITS_DUE("home_habits_due", false),
    HOME_TASKS_UPCOMING("home_tasks_upcoming", false),
    TASKS_OVERDUE("tasks_overdue", true),
    TASKS_DUE_TODAY("tasks_due_today", true),
    TASKS_UPCOMING("tasks_upcoming", true),
    TASKS_DONE("tasks_done", false),
    HABITS_DUE_TODAY("habits_due_today", true),
    HABITS_UPCOMING("habits_upcoming", false),
    HABITS_DONE("habits_done", false),
}

/** Repository for user preferences backed by DataStore, covering theme, formatting, and reminder-timing settings. */
class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    companion object {
        const val DEFAULT_LOCATION_API_ENDPOINT = "https://photon.komoot.io/api/"

        val THEME_PREF = stringPreferencesKey("theme")
        val USE_24_HOUR_FORMAT = booleanPreferencesKey("use24HourFormat")
        val DATE_FORMAT = stringPreferencesKey("dateFormat")
        val AUTO_COMPLETE_FOCUS = booleanPreferencesKey("autoCompleteFocus")
        val ASK_FOCUS_ON_TASK_COMPLETE = booleanPreferencesKey("askFocusOnTaskComplete")
        val SHOW_HOME_GREETING = booleanPreferencesKey("showHomeGreeting")
        val DAILY_GOAL_MINS = intPreferencesKey("dailyGoalMins")
        val DAILY_GOAL_WEEKDAYS = stringPreferencesKey("dailyGoalWeekdays")
        val MAX_STOPWATCH_MINS = intPreferencesKey("maxStopwatchMins")
        val MAX_NODE_MINS = intPreferencesKey("maxNodeMins")
        val UPCOMING_TASKS_HORIZON = intPreferencesKey("upcomingTasksHorizon")
        val DAILY_MOTIVATION_TIME = stringPreferencesKey("dailyMotivationTime")
        val END_OF_DAY_CHECKUP_TIME = stringPreferencesKey("endOfDayCheckupTime")
        val LOCATION_API_ENDPOINT = stringPreferencesKey("locationApiEndpoint")
        val APP_LOCALE_CODE = stringPreferencesKey("appLocaleCode")
        val KEEP_SCREEN_ON_DURING_FOCUS = booleanPreferencesKey("keepScreenOnDuringFocus")
        val AUTO_DND_ENABLED = booleanPreferencesKey("autoDndEnabled")
        val AUTO_DND_LIFT_DURING_BREAKS = booleanPreferencesKey("autoDndLiftDuringBreaks")
        val EXACT_ALARM_PROMPT_SHOWN = booleanPreferencesKey("exactAlarmPromptShown")

        // Not a user-facing setting: the interruption filter FocusDndController owned before it
        // applied Auto-DND, kept here (rather than in-memory) so a killed process can still
        // restore it on next start. Absent means Auto-DND currently owns nothing to restore.
        val STORED_INTERRUPTION_FILTER = intPreferencesKey("storedInterruptionFilter")

        // Not a user-facing setting: whether the per-weekday habit alarms of older versions were
        // cancelled on this device. Not backed up; a restore just runs the cleanup once more.
        val LEGACY_HABIT_ALARMS_CLEARED = booleanPreferencesKey("legacyHabitAlarmsCleared")

        private val RESTORABLE_BOOLEAN_KEYS = listOf(
            USE_24_HOUR_FORMAT, AUTO_COMPLETE_FOCUS, ASK_FOCUS_ON_TASK_COMPLETE, SHOW_HOME_GREETING,
            KEEP_SCREEN_ON_DURING_FOCUS, AUTO_DND_ENABLED, AUTO_DND_LIFT_DURING_BREAKS,
            EXACT_ALARM_PROMPT_SHOWN,
        )
        private val RESTORABLE_INT_KEYS = listOf(
            DAILY_GOAL_MINS, MAX_STOPWATCH_MINS, MAX_NODE_MINS, UPCOMING_TASKS_HORIZON,
        )
        private val RESTORABLE_STRING_KEYS = listOf(
            THEME_PREF, DATE_FORMAT, DAILY_GOAL_WEEKDAYS, DAILY_MOTIVATION_TIME,
            END_OF_DAY_CHECKUP_TIME, LOCATION_API_ENDPOINT, APP_LOCALE_CODE,
        )
    }

    val themePrefFlow: Flow<ThemeMode> =
        dataStore.data.map { ThemeMode.fromStorage(it[THEME_PREF]) }
    val use24HourFormatFlow: Flow<Boolean> = dataStore.data.map { it[USE_24_HOUR_FORMAT] ?: true }
    val dateFormatFlow: Flow<String> = dataStore.data.map { it[DATE_FORMAT] ?: "System" }
    val autoCompleteFocusFlow: Flow<Boolean> =
        dataStore.data.map { it[AUTO_COMPLETE_FOCUS] ?: false }
    val askFocusOnTaskCompleteFlow: Flow<Boolean> =
        dataStore.data.map { it[ASK_FOCUS_ON_TASK_COMPLETE] ?: true }
    val showHomeGreetingFlow: Flow<Boolean> =
        dataStore.data.map { it[SHOW_HOME_GREETING] ?: true }

    // Missing or empty weekdays mean every day.
    val dailyFocusTargetFlow: Flow<DailyFocusTarget> = dataStore.data.map { prefs ->
        DailyFocusTarget(
            minutes = prefs[DAILY_GOAL_MINS] ?: DailyFocusTarget.DEFAULT_MINUTES,
            weekdays = HabitUtils.parseWeekdays(prefs[DAILY_GOAL_WEEKDAYS])
                .filter { it in DailyFocusTarget.ALL_WEEKDAYS }
                .toSet()
                .ifEmpty { DailyFocusTarget.ALL_WEEKDAYS }
        )
    }
    val maxStopwatchMinsFlow: Flow<Int> = dataStore.data.map { it[MAX_STOPWATCH_MINS] ?: 120 }
    val maxNodeMinsFlow: Flow<Int> = dataStore.data.map { it[MAX_NODE_MINS] ?: 240 }
    val upcomingTasksHorizonFlow: Flow<Int> = dataStore.data.map { it[UPCOMING_TASKS_HORIZON] ?: 7 }
    val dailyMotivationTimeFlow: Flow<String> =
        dataStore.data.map { it[DAILY_MOTIVATION_TIME] ?: "08:00" }
    val endOfDayCheckupTimeFlow: Flow<String> =
        dataStore.data.map { it[END_OF_DAY_CHECKUP_TIME] ?: "20:00" }
    val locationApiEndpointFlow: Flow<String> =
        dataStore.data.map { it[LOCATION_API_ENDPOINT] ?: DEFAULT_LOCATION_API_ENDPOINT }

    // Per-app locale: "system" (follow OS) or one of "en"/"de"/"it"/"lld"/"zh-CN".
    val appLocaleCodeFlow: Flow<String> = dataStore.data.map { it[APP_LOCALE_CODE] ?: "system" }

    val keepScreenOnDuringFocusFlow: Flow<Boolean> =
        dataStore.data.map { it[KEEP_SCREEN_ON_DURING_FOCUS] ?: false }
    val autoDndEnabledFlow: Flow<Boolean> = dataStore.data.map { it[AUTO_DND_ENABLED] ?: false }
    val autoDndLiftDuringBreaksFlow: Flow<Boolean> =
        dataStore.data.map { it[AUTO_DND_LIFT_DURING_BREAKS] ?: false }
    val exactAlarmPromptShownFlow: Flow<Boolean> =
        dataStore.data.map { it[EXACT_ALARM_PROMPT_SHOWN] ?: false }

    /** Null means Auto-DND currently owns no filter to restore. */
    val storedInterruptionFilterFlow: Flow<Int?> =
        dataStore.data.map { it[STORED_INTERRUPTION_FILTER] }

    fun accordionExpandedFlow(key: AccordionKey): Flow<Boolean> =
        dataStore.data.map { it[booleanPreferencesKey(key.storageKey)] ?: key.defaultExpanded }

    suspend fun saveAccordionExpanded(key: AccordionKey, expanded: Boolean) {
        dataStore.edit { it[booleanPreferencesKey(key.storageKey)] = expanded }
    }

    suspend fun saveThemePref(theme: ThemeMode) {
        dataStore.edit { it[THEME_PREF] = theme.storageValue }
    }

    suspend fun saveUse24HourFormat(use24Hour: Boolean) {
        dataStore.edit { it[USE_24_HOUR_FORMAT] = use24Hour }
    }

    suspend fun saveDateFormat(format: String) {
        dataStore.edit { it[DATE_FORMAT] = format }
    }

    suspend fun saveAutoCompleteFocus(autoComplete: Boolean) {
        dataStore.edit { it[AUTO_COMPLETE_FOCUS] = autoComplete }
    }

    suspend fun saveAskFocusOnTaskComplete(ask: Boolean) {
        dataStore.edit { it[ASK_FOCUS_ON_TASK_COMPLETE] = ask }
    }

    suspend fun saveShowHomeGreeting(show: Boolean) {
        dataStore.edit { it[SHOW_HOME_GREETING] = show }
    }

    suspend fun saveDailyGoalMins(mins: Int) {
        dataStore.edit { it[DAILY_GOAL_MINS] = mins }
    }

    /**
     * Flips [day] in the target weekdays as one read-modify-write, so quick successive taps can't
     * overwrite each other. Deselecting the last day is ignored: zero minutes is the only "off".
     */
    suspend fun toggleDailyGoalWeekday(day: Int) {
        dataStore.edit { prefs ->
            val days = HabitUtils.parseWeekdays(prefs[DAILY_GOAL_WEEKDAYS])
                .filter { it in DailyFocusTarget.ALL_WEEKDAYS }
                .toSet()
                .ifEmpty { DailyFocusTarget.ALL_WEEKDAYS }
            val toggled = if (day in days) days - day else days + day
            if (toggled.isNotEmpty()) prefs[DAILY_GOAL_WEEKDAYS] = HabitUtils.serializeWeekdays(toggled)
        }
    }

    /** Saves the target weekdays; an empty set is rejected since it would silently mean "off". */
    suspend fun saveDailyGoalWeekdays(days: Set<Int>) {
        require(days.isNotEmpty()) { "A daily focus target needs at least one weekday" }
        dataStore.edit { it[DAILY_GOAL_WEEKDAYS] = HabitUtils.serializeWeekdays(days) }
    }

    suspend fun saveMaxStopwatchMins(mins: Int) {
        dataStore.edit { it[MAX_STOPWATCH_MINS] = mins }
    }

    suspend fun saveMaxNodeMins(mins: Int) {
        dataStore.edit { it[MAX_NODE_MINS] = mins }
    }

    suspend fun saveUpcomingTasksHorizon(days: Int) {
        dataStore.edit { it[UPCOMING_TASKS_HORIZON] = days }
    }

    suspend fun saveDailyMotivationTime(time: String) {
        dataStore.edit { it[DAILY_MOTIVATION_TIME] = time }
    }

    suspend fun saveEndOfDayCheckupTime(time: String) {
        dataStore.edit { it[END_OF_DAY_CHECKUP_TIME] = time }
    }

    suspend fun saveLocationApiEndpoint(endpoint: String) {
        dataStore.edit { it[LOCATION_API_ENDPOINT] = endpoint }
    }

    suspend fun saveAppLocaleCode(code: String) {
        dataStore.edit { it[APP_LOCALE_CODE] = code }
    }

    suspend fun saveKeepScreenOnDuringFocus(keepOn: Boolean) {
        dataStore.edit { it[KEEP_SCREEN_ON_DURING_FOCUS] = keepOn }
    }

    suspend fun saveAutoDndEnabled(enabled: Boolean) {
        dataStore.edit { it[AUTO_DND_ENABLED] = enabled }
    }

    suspend fun saveAutoDndLiftDuringBreaks(lift: Boolean) {
        dataStore.edit { it[AUTO_DND_LIFT_DURING_BREAKS] = lift }
    }

    suspend fun saveExactAlarmPromptShown(shown: Boolean) {
        dataStore.edit { it[EXACT_ALARM_PROMPT_SHOWN] = shown }
    }

    suspend fun saveStoredInterruptionFilter(filter: Int) {
        dataStore.edit { it[STORED_INTERRUPTION_FILTER] = filter }
    }

    suspend fun clearStoredInterruptionFilter() {
        dataStore.edit { it.remove(STORED_INTERRUPTION_FILTER) }
    }

    suspend fun isLegacyHabitAlarmsCleared(): Boolean =
        dataStore.data.first()[LEGACY_HABIT_ALARMS_CLEARED] ?: false

    suspend fun saveLegacyHabitAlarmsCleared() {
        dataStore.edit { it[LEGACY_HABIT_ALARMS_CLEARED] = true }
    }

    suspend fun exportAll(): Map<String, Any?> {
        val prefs = dataStore.data.first()
        // Device state, not user settings: a restored STORED_INTERRUPTION_FILTER would be stale
        // and nothing would ever restore it.
        val deviceStateKeys = setOf(STORED_INTERRUPTION_FILTER.name, LEGACY_HABIT_ALARMS_CLEARED.name)
        return prefs.asMap()
            .filterKeys { it.name !in deviceStateKeys }
            .mapKeys { it.key.name }
    }

    /**
     * Replaces every user setting with [map]'s values. Unknown keys and values of the wrong type are
     * dropped, so a hand-edited backup can't store a value the app later fails to read. The stored
     * interruption filter is kept: it belongs to a focus session that may still be running.
     */
    suspend fun restoreAll(map: Map<String, Any?>) {
        dataStore.edit { prefs ->
            val storedFilter = prefs[STORED_INTERRUPTION_FILTER]
            prefs.clear()
            storedFilter?.let { prefs[STORED_INTERRUPTION_FILTER] = it }
            restorableBooleanKeys().forEach { key -> (map[key.name] as? Boolean)?.let { prefs[key] = it } }
            RESTORABLE_INT_KEYS.forEach { key -> (map[key.name] as? Int)?.let { prefs[key] = it } }
            RESTORABLE_STRING_KEYS.forEach { key -> (map[key.name] as? String)?.let { prefs[key] = it } }
        }
    }

    private fun restorableBooleanKeys(): List<Preferences.Key<Boolean>> =
        RESTORABLE_BOOLEAN_KEYS + AccordionKey.entries.map { booleanPreferencesKey(it.storageKey) }
}
