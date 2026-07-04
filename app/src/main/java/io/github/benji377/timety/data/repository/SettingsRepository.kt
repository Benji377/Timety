package io.github.benji377.timety.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    companion object {
        const val DEFAULT_LOCATION_API_ENDPOINT = "https://photon.komoot.io/api/"

        val THEME_PREF = stringPreferencesKey("theme")
        val USE_24_HOUR_FORMAT = booleanPreferencesKey("use24HourFormat")
        val DATE_FORMAT = stringPreferencesKey("dateFormat")
        val AUTO_COMPLETE_FOCUS = booleanPreferencesKey("autoCompleteFocus")
        val DAILY_GOAL_MINS = intPreferencesKey("dailyGoalMins")
        val MAX_STOPWATCH_MINS = intPreferencesKey("maxStopwatchMins")
        val MAX_NODE_MINS = intPreferencesKey("maxNodeMins")
        val UPCOMING_TASKS_HORIZON = intPreferencesKey("upcomingTasksHorizon")
        val DAILY_MOTIVATION_TIME = stringPreferencesKey("dailyMotivationTime")
        val END_OF_DAY_CHECKUP_TIME = stringPreferencesKey("endOfDayCheckupTime")
        val LOCATION_API_ENDPOINT = stringPreferencesKey("locationApiEndpoint")
        val APP_LOCALE_CODE = stringPreferencesKey("appLocaleCode")
    }

    val themePrefFlow: Flow<String> = dataStore.data.map { it[THEME_PREF] ?: "System Default" }
    val use24HourFormatFlow: Flow<Boolean> = dataStore.data.map { it[USE_24_HOUR_FORMAT] ?: true }
    val dateFormatFlow: Flow<String> = dataStore.data.map { it[DATE_FORMAT] ?: "System" }
    val autoCompleteFocusFlow: Flow<Boolean> =
        dataStore.data.map { it[AUTO_COMPLETE_FOCUS] ?: false }
    val dailyGoalMinsFlow: Flow<Int> = dataStore.data.map { it[DAILY_GOAL_MINS] ?: 90 }
    val maxStopwatchMinsFlow: Flow<Int> = dataStore.data.map { it[MAX_STOPWATCH_MINS] ?: 120 }
    val maxNodeMinsFlow: Flow<Int> = dataStore.data.map { it[MAX_NODE_MINS] ?: 240 }
    val upcomingTasksHorizonFlow: Flow<Int> = dataStore.data.map { it[UPCOMING_TASKS_HORIZON] ?: 7 }
    val dailyMotivationTimeFlow: Flow<String> =
        dataStore.data.map { it[DAILY_MOTIVATION_TIME] ?: "08:00" }
    val endOfDayCheckupTimeFlow: Flow<String> =
        dataStore.data.map { it[END_OF_DAY_CHECKUP_TIME] ?: "20:00" }
    val locationApiEndpointFlow: Flow<String> =
        dataStore.data.map { it[LOCATION_API_ENDPOINT] ?: DEFAULT_LOCATION_API_ENDPOINT }

    // Per-app locale: "system" (follow OS) or one of "en"/"de"/"it"/"lld".
    val appLocaleCodeFlow: Flow<String> = dataStore.data.map { it[APP_LOCALE_CODE] ?: "system" }

    suspend fun saveThemePref(theme: String) {
        dataStore.edit { it[THEME_PREF] = theme }
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

    suspend fun saveDailyGoalMins(mins: Int) {
        dataStore.edit { it[DAILY_GOAL_MINS] = mins }
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

    suspend fun exportAll(): Map<String, Any?> {
        val prefs = dataStore.data.first()
        return prefs.asMap().mapKeys { it.key.name }
    }

    suspend fun restoreAll(map: Map<String, Any?>) {
        dataStore.edit { prefs ->
            prefs.clear()
            map.forEach { (key, value) ->
                when (value) {
                    is Boolean -> prefs[booleanPreferencesKey(key)] = value
                    is Int -> prefs[intPreferencesKey(key)] = value
                    is String -> prefs[stringPreferencesKey(key)] = value
                }
            }
        }
    }
}
