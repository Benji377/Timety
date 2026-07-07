package io.github.benji377.timety.migration

import android.content.Context
import android.util.Log
import io.github.benji377.timety.data.repository.SettingsRepository
import io.github.benji377.timety.data.repository.ThemeMode
import io.github.benji377.timety.di.AppContainer
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.Instant
import java.util.Locale

/**
 * TEMPORARY — one-shot migration of the old Flutter app's on-device data into the
 * Kotlin app, run on the first launch after the store update. See
 * docs/flutter-migration.md for the full design and removal instructions.
 *
 * Data (Hive boxes) and settings (shared_preferences) migrate independently, each
 * behind its own flag, so a settings failure never blocks or rolls back user data.
 */
object FlutterMigration {

    private const val TAG = "FlutterMigration"
    private const val FLAGS_PREFS = "flutter_migration"
    private const val KEY_DATA_MIGRATED = "dataMigrated"
    private const val KEY_SETTINGS_MIGRATED = "settingsMigrated"

    /** shared_preferences plugin file; keys are prefixed with "flutter.". */
    private const val FLUTTER_PREFS = "FlutterSharedPreferences"
    private const val FLUTTER_KEY_PREFIX = "flutter."

    suspend fun runIfNeeded(context: Context, container: AppContainer) {
        val flags = context.getSharedPreferences(FLAGS_PREFS, Context.MODE_PRIVATE)
        val dataMigrated = flags.getBoolean(KEY_DATA_MIGRATED, false)
        val settingsMigrated = flags.getBoolean(KEY_SETTINGS_MIGRATED, false)
        if (dataMigrated && settingsMigrated) return

        // Hive.initFlutter() stored boxes in the app documents dir: <dataDir>/app_flutter.
        val hiveDir = File(context.applicationInfo.dataDir, "app_flutter")
        val hasFlutterData = hiveDir.isDirectory &&
                hiveDir.listFiles()?.any { it.name.endsWith(".hive") } == true

        if (!hasFlutterData) {
            // Fresh install (or the old files are already gone): nothing to migrate, ever.
            flags.edit()
                .putBoolean(KEY_DATA_MIGRATED, true)
                .putBoolean(KEY_SETTINGS_MIGRATED, true)
                .apply()
            return
        }

        if (!dataMigrated) {
            try {
                migrateData(hiveDir, container)
                flags.edit().putBoolean(KEY_DATA_MIGRATED, true).apply()
                Log.i(TAG, "Flutter data migration completed")
            } catch (e: Exception) {
                // Leave the Hive files and the flag untouched: the migration retries on
                // the next launch. Once the user creates data in the new app, the
                // empty-database guard in migrateData() stops further attempts.
                Log.e(TAG, "Flutter data migration failed", e)
            }
        }

        if (!settingsMigrated) {
            try {
                migrateSettings(context, container.settingsRepository)
                Log.i(TAG, "Flutter settings migration completed")
            } catch (e: Exception) {
                // Settings are best-effort: losing them just means defaults. No retry,
                // otherwise a later attempt could overwrite settings the user has
                // meanwhile changed in the new app.
                Log.e(TAG, "Flutter settings migration failed", e)
            } finally {
                flags.edit().putBoolean(KEY_SETTINGS_MIGRATED, true).apply()
            }
        }
    }

    // --- DATA (Hive boxes → Room, via the backup import path) ---

    private suspend fun migrateData(hiveDir: File, container: AppContainer) {
        // The restore path wipes all tables first, so only skip when Room holds data
        // the user actually created. A bare default profile doesn't count: ViewModels
        // seed one (plus tags/system modes) as soon as the UI composes, and after a
        // failed attempt that must not block the retry on the next launch.
        val profile = container.userRepository.getUserProfileSnapshot()
        val hasUserData = profile != null && (
                profile.totalXp > 0 ||
                        container.taskRepository.allTasks.first().isNotEmpty() ||
                        container.habitRepository.allHabits.first().isNotEmpty() ||
                        container.focusRepository.allSessions.first().isNotEmpty()
                )
        if (hasUserData) {
            Log.w(TAG, "Room already has user data; skipping Flutter data migration")
            return
        }

        // Hive lowercases box names for the file name.
        fun box(name: String): List<Any?> {
            val file = File(hiveDir, "${name.lowercase(Locale.ROOT)}.hive")
            return if (file.isFile) HiveBoxReader.readBox(file) else emptyList()
        }

        // Same shape as the Flutter app's BackupService._buildPayload, minus
        // "preferences" (settings migrate separately, see migrateSettings).
        val payload = JSONObject().apply {
            put("payloadType", "user_data")
            put("schemaVersion", 1)
            put("appVersion", "flutter-hive-migration")
            put("exportedAt", Instant.now().toString())
            put("userProfile", box("userProfileBox").firstOrNull() ?: JSONObject.NULL)
            put("tasks", JSONArray(box("tasksBox")))
            put("habits", JSONArray(box("habitsBox")))
            put("focus", JSONObject().apply {
                put("modes", JSONArray(box("focusModesBox")))
                put("sessions", JSONArray(box("focusSessionsBox")))
                put("tags", JSONArray(box("focusTagsBox")))
            })
        }

        container.backupService.importFromJson(payload).getOrThrow()
    }

    // --- SETTINGS (shared_preferences → DataStore, best-effort) ---

    private suspend fun migrateSettings(context: Context, settings: SettingsRepository) {
        val raw = context.getSharedPreferences(FLUTTER_PREFS, Context.MODE_PRIVATE).all
        if (raw.isEmpty()) return
        val prefs = raw.mapKeys { it.key.removePrefix(FLUTTER_KEY_PREFIX) }

        // The Dart plugin stores Dart ints as Long.
        fun int(key: String): Int? = (prefs[key] as? Long)?.toInt()
        fun bool(key: String): Boolean? = prefs[key] as? Boolean
        fun string(key: String): String? = prefs[key] as? String
        fun time(hourKey: String, minKey: String): String? {
            val hour = int(hourKey) ?: return null
            val min = int(minKey) ?: 0
            return String.format(Locale.ROOT, "%02d:%02d", hour, min)
        }

        // Flutter stored ThemeMode.index: [system, light, dark].
        when (int("themeMode")) {
            1 -> settings.saveThemePref(ThemeMode.LIGHT)
            2 -> settings.saveThemePref(ThemeMode.DARK)
            else -> {}
        }
        time("notificationHour", "notificationMin")?.let { settings.saveDailyMotivationTime(it) }
        time("endOfDayHour", "endOfDayMin")?.let { settings.saveEndOfDayCheckupTime(it) }
        int("dailyGoalMins")?.let { settings.saveDailyGoalMins(it) }
        int("maxStopwatchMins")?.let { settings.saveMaxStopwatchMins(it) }
        int("maxNodeMins")?.let { settings.saveMaxNodeMins(it) }
        int("upcomingTasksDays")?.let { settings.saveUpcomingTasksHorizon(it) }
        bool("autoCompleteFocusTargetOnFinish")?.let { settings.saveAutoCompleteFocus(it) }
        bool("use24HourFormat")?.let { settings.saveUse24HourFormat(it) }
        string("locationApiEndpoint")?.let { settings.saveLocationApiEndpoint(it) }
        string("appLocaleCode")?.let { settings.saveAppLocaleCode(it) }
        string("dateFormatCode")?.let { settings.saveDateFormat(it) }
    }
}
