package io.github.benji377.timety.services

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import androidx.glance.appwidget.updateAll
import androidx.room.withTransaction
import io.github.benji377.timety.data.local.TimetyDatabase
import io.github.benji377.timety.data.model.habit.HabitWithCompletions
import io.github.benji377.timety.data.model.tag.TaggedItemType
import io.github.benji377.timety.data.repository.SettingsRepository
import io.github.benji377.timety.services.backup.dayRatingFromBackup
import io.github.benji377.timety.services.backup.goalFromBackup
import io.github.benji377.timety.services.backup.habitFromBackup
import io.github.benji377.timety.services.backup.modeFromBackup
import io.github.benji377.timety.services.backup.modeToBackupJson
import io.github.benji377.timety.services.backup.objects
import io.github.benji377.timety.services.backup.profileFromBackup
import io.github.benji377.timety.services.backup.quickHabitFromBackup
import io.github.benji377.timety.services.backup.recurringTaskFromBackup
import io.github.benji377.timety.services.backup.sessionFromBackup
import io.github.benji377.timety.services.backup.sessionToBackupJson
import io.github.benji377.timety.services.backup.tagsFromBackup
import io.github.benji377.timety.services.backup.taskFromBackup
import io.github.benji377.timety.services.backup.toBackupJson
import io.github.benji377.timety.util.ProfileImageStore
import io.github.benji377.timety.widget.HabitWidget
import io.github.benji377.timety.widget.TaskWidget
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant

/** The items whose reminders were scheduled before a restore replaced them. */
data class ReplacedReminders(
    val taskIds: List<String>,
    val recurringTaskIds: List<String>,
    val habitIds: List<String>,
    val quickHabitIds: List<String>,
)

/**
 * Exports the app database and preferences to a portable JSON payload and restores them back,
 * including payloads produced by legacy backups. [afterRestore] runs once a restore has fully
 * succeeded, to replace the old data's alarms, notifications and widgets.
 */
class BackupService(
    private val context: Context,
    private val database: TimetyDatabase,
    private val settingsRepository: SettingsRepository,
    private val afterRestore: suspend (ReplacedReminders) -> Unit = { refreshAfterRestore(context, it) },
) {
    private val taskDao get() = database.taskDao()
    private val recurringTaskDao get() = database.recurringTaskDao()
    private val habitDao get() = database.habitDao()
    private val quickHabitDao get() = database.quickHabitDao()
    private val goalDao get() = database.goalDao()
    private val focusDao get() = database.focusDao()
    private val userDao get() = database.userDao()
    private val dayRatingDao get() = database.dayRatingDao()
    private val tagDao get() = database.tagDao()

    fun suggestedFileName(): String = "Timety_Export_${System.currentTimeMillis()}.json"

    suspend fun exportToUri(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val json = buildPayload().toString(2)
            // "wt" truncates: plain "w" can leave an old, longer file's tail behind the new JSON.
            context.contentResolver.openOutputStream(uri, "wt")?.use { out ->
                out.write(json.toByteArray(Charsets.UTF_8))
            } ?: error("Unable to open output stream for $uri")
        }
    }

    suspend fun exportToShareUri(): Result<Uri> = withContext(Dispatchers.IO) {
        runCatching {
            val json = buildPayload().toString(2)
            val dir = File(context.cacheDir, "shared").apply { mkdirs() }
            val file = File(dir, suggestedFileName())
            file.writeText(json, Charsets.UTF_8)
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        }
    }

    suspend fun importFromUri(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val text = context.contentResolver.openInputStream(uri)?.use { input ->
                input.bufferedReader(Charsets.UTF_8).readText()
            } ?: error("Unable to open input stream for $uri")
            // The parser's own message quotes the whole file.
            val json = try {
                JSONObject(text)
            } catch (_: JSONException) {
                error("This file is not valid JSON. It may be incomplete or damaged.")
            }
            importFromJson(json).getOrThrow()
        }
    }

    /** Restores an already-parsed backup payload; also used when importing legacy backups. */
    suspend fun importFromJson(json: JSONObject): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching<Unit> {
            // Restoring clears every table before inserting, so an arbitrary JSON file that
            // merely parses must be rejected up front or it would wipe all data and "succeed".
            // Legacy backups carry no payloadType, so any recognized data key also passes.
            val declaredType = json.optString("payloadType", "")
            val looksLikeBackup = declaredType == PAYLOAD_TYPE ||
                    (declaredType.isEmpty() && KNOWN_DATA_KEYS.any { json.has(it) })
            if (!looksLikeBackup) {
                error("Not a Timety backup file.")
            }
            val schemaVersion = json.optInt("schemaVersion", SCHEMA_VERSION)
            if (schemaVersion > SCHEMA_VERSION) {
                error("This backup was created by a newer version of Timety and cannot be imported.")
            }
            // The running session is logged against a focus mode the restore would replace.
            if (FocusTimerManager.timerState.value.isSessionActive) {
                error("Stop the running focus session before importing a backup.")
            }
            // The data is restored in one transaction, so a broken file changes nothing; settings
            // and alarms are only replaced once that has committed.
            val preferences = json.optJSONObject("preferences")?.let { prefs ->
                prefs.keys().asSequence().associateWith { prefs.get(it) }
            }
            val replaced = scheduledReminders()
            database.withTransaction {
                restorePayload(json)
            }
            // The data is in from here on, so reporting "Import failed" would be wrong: a failed
            // settings step leaves the old settings, and the next app start re-arms every alarm.
            try {
                settingsRepository.restoreAll(preferences.orEmpty())
                afterRestore(replaced)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Backup data restored, but settings or reminders were not refreshed", e)
            }
        }
    }

    private suspend fun scheduledReminders() = ReplacedReminders(
        taskIds = taskDao.getAllTasks().first().map { it.task.id },
        recurringTaskIds = recurringTaskDao.getAllWithOccurrences().first().map { it.task.id },
        habitIds = habitDao.getAllHabits().first().map { it.id },
        quickHabitIds = quickHabitDao.getAll().first().map { it.id },
    )

    // Export.

    private suspend fun buildPayload(): JSONObject {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val habitCompletions = habitDao.getAllCompletions().first().groupBy { it.habitId }
        return JSONObject().apply {
            put("payloadType", PAYLOAD_TYPE)
            put("schemaVersion", SCHEMA_VERSION)
            put("appVersion", packageInfo.versionName ?: "")
            put("buildNumber", PackageInfoCompat.getLongVersionCode(packageInfo).toString())
            put("exportedAt", Instant.now().toString())
            put("preferences", JSONObject().apply {
                settingsRepository.exportAll().forEach { (key, value) -> if (value != null) put(key, value) }
            })
            put("userProfile", userDao.getUserProfileSynchronous()?.toBackupJson() ?: JSONObject.NULL)
            put("tags", JSONArray(tagDao.getAllTags().first().map { it.toBackupJson() }))
            val taskTags = tagIdsByItem(TaggedItemType.TASK)
            put("tasks", JSONArray(taskDao.getAllTasks().first().map {
                it.toBackupJson().withTagIds(taskTags, it.task.id)
            }))
            val recurringTags = tagIdsByItem(TaggedItemType.RECURRING_TASK)
            put("recurringTasks", JSONArray(recurringTaskDao.getAllWithOccurrences().first().map {
                it.toBackupJson().withTagIds(recurringTags, it.task.id)
            }))
            val habitTags = tagIdsByItem(TaggedItemType.HABIT)
            put("habits", JSONArray(habitDao.getAllHabits().first().map {
                HabitWithCompletions(it, habitCompletions[it.id].orEmpty()).toBackupJson()
                    .withTagIds(habitTags, it.id)
            }))
            put("quickHabits", JSONArray(quickHabitDao.getAll().first().map { it.toBackupJson() }))
            val goalTags = tagIdsByItem(TaggedItemType.GOAL)
            put("goals", JSONArray(goalDao.getAllGoalsWithEntries().first().map {
                it.toBackupJson().withTagIds(goalTags, it.goal.id)
            }))
            put("dayRatings", JSONArray(dayRatingDao.getAll().first().map { it.toBackupJson() }))
            put("focus", focusToJson())
        }
    }

    private suspend fun tagIdsByItem(type: TaggedItemType): Map<String, List<String>> =
        tagDao.getItemTags(type).first().groupBy({ it.itemId }, { it.tag.id })

    private fun JSONObject.withTagIds(tagIds: Map<String, List<String>>, itemId: String): JSONObject =
        put("tagIds", JSONArray(tagIds[itemId].orEmpty()))

    private suspend fun focusToJson(): JSONObject {
        val modes = focusDao.getAllModes().first().map { mode ->
            modeToBackupJson(mode, focusDao.getPhasesForMode(mode.id).first())
        }
        // One query for all distractions instead of one per session.
        val distractionsBySession = focusDao.getAllDistractions().first().groupBy { it.sessionId }
        val sessionTags = tagIdsByItem(TaggedItemType.SESSION)
        val sessions = focusDao.getAllSessions().first().map { session ->
            sessionToBackupJson(session, distractionsBySession[session.id].orEmpty())
                .withTagIds(sessionTags, session.id)
        }
        return JSONObject().apply {
            put("modes", JSONArray(modes))
            put("sessions", JSONArray(sessions))
        }
    }

    // Restore. Every table is cleared before its rows go in; missing arrays (older backups,
    // features added later) restore as empty.

    private fun restorePayload(json: JSONObject) {
        val restoredProfile = profileFromBackup(json.optJSONObject("userProfile"))
        // Backups hold only the avatar's file path. When that no longer resolves (a photo since
        // replaced, or another device), this device's current avatar is kept instead of cleared.
        userDao.insertUserProfile(
            if (restoredProfile.profileImagePath != null) restoredProfile
            else restoredProfile.copy(
                profileImagePath = ProfileImageStore.validOrNull(userDao.getUserProfileSynchronous()?.profileImagePath)
            )
        )

        taskDao.clearAll()
        val taskJson = LinkedHashMap<String, JSONObject>()
        json.objects("tasks").forEach { item ->
            val (task, subtasks) = taskFromBackup(item)
            taskJson[task.id] = item
            taskDao.insertTask(task)
            subtasks.forEach(taskDao::insertSubtask)
        }

        recurringTaskDao.clearAll()
        val recurringJson = LinkedHashMap<String, JSONObject>()
        json.objects("recurringTasks").forEach { item ->
            val (task, occurrences) = recurringTaskFromBackup(item)
            recurringJson[task.id] = item
            recurringTaskDao.insert(task)
            occurrences.forEach(recurringTaskDao::insertOccurrence)
        }

        habitDao.clearAll()
        val habitJson = LinkedHashMap<String, JSONObject>()
        json.objects("habits").forEach { item ->
            val (habit, completions) = habitFromBackup(item)
            habitJson[habit.id] = item
            habitDao.insertHabit(habit)
            completions.forEach(habitDao::insertCompletion)
        }

        quickHabitDao.clearAll()
        json.objects("quickHabits").map(::quickHabitFromBackup).forEach(quickHabitDao::insert)

        goalDao.clearAll()
        val goalJson = LinkedHashMap<String, JSONObject>()
        json.objects("goals").forEach { item ->
            val (goal, entries) = goalFromBackup(item)
            goalJson[goal.id] = item
            goalDao.insertGoal(goal)
            entries.forEach(goalDao::insertEntry)
        }

        dayRatingDao.clearAll()
        json.objects("dayRatings").mapNotNull(::dayRatingFromBackup).forEach(dayRatingDao::upsert)

        val sessionJson = restoreFocus(json.optJSONObject("focus") ?: JSONObject())

        restoreTags(
            json,
            mapOf(
                TaggedItemType.TASK to taskJson,
                TaggedItemType.RECURRING_TASK to recurringJson,
                TaggedItemType.HABIT to habitJson,
                TaggedItemType.GOAL to goalJson,
                TaggedItemType.SESSION to sessionJson,
            ),
        )
    }

    /** Must run after every tagged item is restored: attachments reference them. */
    private fun restoreTags(json: JSONObject, items: Map<TaggedItemType, Map<String, JSONObject>>) {
        val restored = tagsFromBackup(json, items)
        tagDao.clearAllTags()
        restored.tags.forEach(tagDao::insertTag)
        restored.attachments.forEach { (type, byItem) ->
            byItem.forEach { (itemId, tagIds) -> tagDao.setItemTags(type, itemId, tagIds) }
        }
    }

    /** Returns each restored session's JSON by its id. */
    private fun restoreFocus(focusJson: JSONObject): Map<String, JSONObject> {
        focusDao.clearAllSessions()
        focusDao.clearAllModes()
        focusJson.objects("modes").map(::modeFromBackup).forEach { (mode, phases) ->
            focusDao.insertMode(mode)
            if (phases.isNotEmpty()) focusDao.insertPhases(phases)
        }
        val sessionJson = LinkedHashMap<String, JSONObject>()
        focusJson.objects("sessions").forEach { item ->
            val (session, distractions) = sessionFromBackup(item)
            sessionJson[session.id] = item
            focusDao.insertSession(session)
            distractions.forEach(focusDao::insertDistraction)
        }
        return sessionJson
    }

    companion object {
        private const val TAG = "BackupService"
        private const val SCHEMA_VERSION = 2
        private const val PAYLOAD_TYPE = "user_data"

        /** Top-level keys that identify a payload (including legacy ones) as a Timety backup. */
        private val KNOWN_DATA_KEYS = listOf(
            "tags", "tasks", "taskCategories", "recurringTasks", "habits", "quickHabits",
            "goals", "dayRatings", "focus", "userProfile", "preferences",
        )
    }
}

/** Swaps the old data's reminders, notifications and widgets for the restored data's. */
private suspend fun refreshAfterRestore(context: Context, replaced: ReplacedReminders) {
    val scheduler = ReminderScheduler.create(context)
    replaced.taskIds.forEach { scheduler.cancelTaskReminders(it) }
    replaced.recurringTaskIds.forEach { scheduler.cancelRecurringTaskReminders(it) }
    replaced.habitIds.forEach { scheduler.cancelHabitReminder(it) }
    replaced.quickHabitIds.forEach { scheduler.cancelQuickHabit(it) }
    // Foreground-service notifications (a running focus session) survive cancelAll.
    NotificationManagerCompat.from(context).cancelAll()
    ReminderScheduler.resyncAll(context)
    TaskWidget().updateAll(context)
    HabitWidget().updateAll(context)
}
