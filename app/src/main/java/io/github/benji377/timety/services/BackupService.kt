package io.github.benji377.timety.services

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import androidx.room.withTransaction
import io.github.benji377.timety.data.local.TimetyDatabase
import io.github.benji377.timety.data.model.focus.DistractionEntity
import io.github.benji377.timety.data.model.focus.FocusModeEntity
import io.github.benji377.timety.data.model.focus.FocusModeType
import io.github.benji377.timety.data.model.focus.FocusSessionEntity
import io.github.benji377.timety.data.model.focus.FocusTagEntity
import io.github.benji377.timety.data.model.focus.FocusTargetType
import io.github.benji377.timety.data.model.focus.PhaseType
import io.github.benji377.timety.data.model.focus.SessionPhaseEntity
import io.github.benji377.timety.data.model.habit.HabitCompletionEntity
import io.github.benji377.timety.data.model.habit.HabitEntity
import io.github.benji377.timety.data.model.habit.HabitFrequency
import io.github.benji377.timety.data.model.habit.QuickHabitEntity
import io.github.benji377.timety.data.model.task.Priority
import io.github.benji377.timety.data.model.task.SubtaskEntity
import io.github.benji377.timety.data.model.task.TaskCategoryEntity
import io.github.benji377.timety.data.model.task.TaskEntity
import io.github.benji377.timety.data.model.task.TaskSize
import io.github.benji377.timety.data.model.user.UserProfileEntity
import io.github.benji377.timety.data.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant


/**
 * Exports the app database and preferences to a portable JSON payload and restores them back,
 * including payloads produced by legacy backups.
 */
class BackupService(
    private val context: Context,
    private val database: TimetyDatabase,
    private val settingsRepository: SettingsRepository,
) {
    private val taskDao get() = database.taskDao()
    private val habitDao get() = database.habitDao()
    private val quickHabitDao get() = database.quickHabitDao()
    private val focusDao get() = database.focusDao()
    private val userDao get() = database.userDao()


    fun suggestedFileName(): String = "Timety_Export_${System.currentTimeMillis()}.json"


    suspend fun exportToUri(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val json = buildPayload().toString(2)
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(json.toByteArray(Charsets.UTF_8))
            } ?: error("Unable to open output stream for $uri")
        }
    }


    suspend fun exportToShareUri(): Result<Uri> = withContext(Dispatchers.IO) {
        runCatching {
            val json = buildPayload().toString(2)
            val dir = java.io.File(context.cacheDir, "shared").apply { mkdirs() }
            val file = java.io.File(dir, suggestedFileName())
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
            importFromJson(JSONObject(text)).getOrThrow()
        }
    }

    /** Restores an already-parsed backup payload; also used when importing legacy backups. */
    suspend fun importFromJson(json: JSONObject): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val schemaVersion = json.optInt("schemaVersion", SCHEMA_VERSION)
            if (schemaVersion > SCHEMA_VERSION) {
                error("This backup was created by a newer version of Timety and cannot be imported.")
            }
            database.withTransaction {
                restorePayload(json)
            }
        }
    }

    // Export.

    private suspend fun buildPayload(): JSONObject {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val appVersion = packageInfo.versionName ?: ""
        val buildNumber = PackageInfoCompat.getLongVersionCode(packageInfo).toString()

        return JSONObject().apply {
            put("payloadType", PAYLOAD_TYPE)
            put("schemaVersion", SCHEMA_VERSION)
            put("appVersion", appVersion)
            put("buildNumber", buildNumber)
            put("exportedAt", Instant.now().toString())
            put("preferences", preferencesToJson(settingsRepository.exportAll()))
            put(
                "userProfile",
                userProfileToJson(userDao.getUserProfileSynchronous()) ?: JSONObject.NULL
            )
            put("tasks", tasksToJson())
            put("taskCategories", taskCategoriesToJson())
            put("habits", habitsToJson())
            put("quickHabits", quickHabitsToJson())
            put("focus", focusToJson())
        }
    }

    private fun preferencesToJson(map: Map<String, Any?>): JSONObject {
        val obj = JSONObject()
        for ((key, value) in map) {
            when (value) {
                null -> {}
                else -> obj.put(key, value)
            }
        }
        return obj
    }

    private suspend fun tasksToJson(): JSONArray {
        val array = JSONArray()
        val tasksWithSubtasks = taskDao.getAllTasks().first()
        for (entry in tasksWithSubtasks) {
            val task = entry.task
            val taskObj = JSONObject().apply {
                put("id", task.id)
                put("title", task.title)
                put("description", task.description)
                put("dueDate", task.dueDate?.toString() ?: JSONObject.NULL)
                put("location", task.location)
                put("priority", task.priority.name)
                put("category", task.category)
                put("size", task.size.name)
                put("isCompleted", task.isCompleted)
                put("completedAt", task.completedAt?.toString() ?: JSONObject.NULL)
                put("createdAt", task.createdAt.toString())
                put("reminders", JSONArray(task.reminders.map { it.toString() }))
                put(
                    "subtasks",
                    JSONArray(entry.subtasks.map { subtask ->
                        JSONObject().apply {
                            put("id", subtask.id)
                            put("title", subtask.title)
                            put("isCompleted", subtask.isCompleted)
                        }
                    })
                )
            }
            array.put(taskObj)
        }
        return array
    }

    private suspend fun taskCategoriesToJson(): JSONArray {
        val array = JSONArray()
        for (category in taskDao.getAllCategories().first()) {
            array.put(
                JSONObject().apply {
                    put("id", category.id)
                    put("name", category.name)
                    put("colorValue", category.colorValue)
                }
            )
        }
        return array
    }

    private suspend fun habitsToJson(): JSONArray {
        val array = JSONArray()
        val habits = habitDao.getAllHabits().first()
        val allCompletions = habitDao.getAllCompletions().first().groupBy { it.habitId }
        for (habit in habits) {
            val completions = allCompletions[habit.id].orEmpty()
            val habitObj = JSONObject().apply {
                put("id", habit.id)
                put("name", habit.name)
                put("frequency", habit.frequency.name)
                put("targetDaysPerWeek", habit.targetDaysPerWeek ?: JSONObject.NULL)
                put("targetWeekdays", habit.targetWeekdays ?: JSONObject.NULL)
                put("targetTimeMinutes", habit.targetTimeMinutes ?: JSONObject.NULL)
                put("createdAt", habit.createdAt.toString())
                put("colorValue", habit.colorValue)
                put("notes", habit.notes ?: JSONObject.NULL)
                put("iconCodePoint", habit.iconCodePoint ?: JSONObject.NULL)
                put("stackName", habit.stackName ?: JSONObject.NULL)
                put("stackOrder", habit.stackOrder ?: JSONObject.NULL)
                put("completions", JSONArray(completions.map { it.completionDate.toString() }))
            }
            array.put(habitObj)
        }
        return array
    }

    private suspend fun quickHabitsToJson(): JSONArray {
        val array = JSONArray()
        for (quickHabit in quickHabitDao.getAll().first()) {
            array.put(
                JSONObject().apply {
                    put("id", quickHabit.id)
                    put("name", quickHabit.name)
                    put("intervalMinutes", quickHabit.intervalMinutes)
                    put("startMinuteOfDay", quickHabit.startMinuteOfDay ?: JSONObject.NULL)
                    put("endMinuteOfDay", quickHabit.endMinuteOfDay ?: JSONObject.NULL)
                    put("targetWeekdays", quickHabit.targetWeekdays ?: JSONObject.NULL)
                    put("isEnabled", quickHabit.isEnabled)
                    put("createdAt", quickHabit.createdAt.toString())
                }
            )
        }
        return array
    }

    private suspend fun focusToJson(): JSONObject {
        val modes = focusDao.getAllModes().first()
        val modesArray = JSONArray()
        for (mode in modes) {
            val phases = focusDao.getPhasesForMode(mode.id).first().sortedBy { it.orderIndex }
            modesArray.put(
                JSONObject().apply {
                    put("id", mode.id)
                    put("name", mode.name)
                    put("type", mode.type.name)
                    put("isSystem", mode.isSystem)
                    put(
                        "phases",
                        JSONArray(phases.map { phase ->
                            JSONObject().apply {
                                put("type", phase.type.name)
                                put("durationMinutes", phase.durationMinutes)
                            }
                        })
                    )
                }
            )
        }

        val tagsArray = JSONArray()
        for (tag in focusDao.getAllTags().first()) {
            tagsArray.put(
                JSONObject().apply {
                    put("id", tag.id)
                    put("name", tag.name)
                    put("colorValue", tag.colorValue)
                }
            )
        }

        val sessions = focusDao.getAllSessions().first()
        val sessionsArray = JSONArray()
        for (session in sessions) {
            val distractions = focusDao.getDistractionsForSession(session.id).first()
            sessionsArray.put(
                JSONObject().apply {
                    put("id", session.id)
                    put("modeId", session.modeId)
                    put("startTime", session.startTime.toString())
                    put("endTime", session.endTime?.toString() ?: JSONObject.NULL)
                    put("totalSecondsFocused", session.totalSecondsFocused)
                    put("isCompleted", session.isCompleted)
                    put("tagId", session.tagId ?: JSONObject.NULL)
                    put("targetType", session.targetType.name)
                    put("targetId", session.targetId ?: JSONObject.NULL)
                    put("targetLabel", session.targetLabel ?: JSONObject.NULL)
                    put(
                        "distractions",
                        JSONArray(distractions.map { distraction ->
                            JSONObject().apply {
                                put("time", distraction.time.toString())
                                put("note", distraction.note)
                            }
                        })
                    )
                }
            )
        }

        return JSONObject().apply {
            put("modes", modesArray)
            put("tags", tagsArray)
            put("sessions", sessionsArray)
        }
    }

    private fun userProfileToJson(profile: UserProfileEntity?): JSONObject? {
        if (profile == null) return null
        return JSONObject().apply {
            put("name", profile.name)
            put("profileImagePath", profile.profileImagePath ?: JSONObject.NULL)
            put("accountCreated", profile.accountCreated.toString())
            put("unlockedAchievements", JSONArray(profile.unlockedAchievements))
            put("totalXp", profile.totalXp)
        }
    }

    // Restore.

    private suspend fun restorePayload(json: JSONObject) {
        json.optJSONObject("preferences")?.let { prefsJson ->
            settingsRepository.restoreAll(jsonToPreferencesMap(prefsJson))
        }

        val userProfileJson = json.optJSONObject("userProfile")
        userDao.insertUserProfile(userProfileFromJson(userProfileJson))

        restoreTasks(json.optJSONArray("tasks") ?: JSONArray())
        restoreTaskCategories(json.optJSONArray("taskCategories") ?: JSONArray())
        restoreHabits(json.optJSONArray("habits") ?: JSONArray())
        restoreQuickHabits(json.optJSONArray("quickHabits") ?: JSONArray())
        json.optJSONObject("focus")?.let { restoreFocus(it) }
    }

    private fun jsonToPreferencesMap(obj: JSONObject): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        val keys = obj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            map[key] = obj.get(key)
        }
        return map
    }

    private fun userProfileFromJson(json: JSONObject?): UserProfileEntity {
        if (json == null) {
            return UserProfileEntity(name = "Bobert", accountCreated = Instant.now())
        }
        return UserProfileEntity(
            name = readString(json, "name") ?: "Bobert",
            profileImagePath = readString(json, "profileImagePath"),
            accountCreated = readInstant(json, "accountCreated") ?: Instant.now(),
            unlockedAchievements = readStringList(json, "unlockedAchievements"),
            totalXp = json.optInt("totalXp", 0),
        )
    }

    private fun restoreTasks(tasksJson: JSONArray) {
        taskDao.clearAll()
        for (i in 0 until tasksJson.length()) {
            val taskJson = tasksJson.getJSONObject(i)
            val taskId = readString(taskJson, "id") ?: Instant.now().toEpochMilli().toString()
            taskDao.insertTask(
                TaskEntity(
                    id = taskId,
                    title = readString(taskJson, "title") ?: "",
                    description = readString(taskJson, "description") ?: "",
                    dueDate = readInstant(taskJson, "dueDate"),
                    location = readString(taskJson, "location") ?: "",
                    priority = enumOrDefault(readString(taskJson, "priority"), Priority.MEDIUM),
                    category = readString(taskJson, "category") ?: "",
                    size = enumOrDefault(readString(taskJson, "size"), TaskSize.MEDIUM),
                    isCompleted = taskJson.optBoolean("isCompleted", false),
                    completedAt = readInstant(taskJson, "completedAt"),
                    createdAt = readInstant(taskJson, "createdAt") ?: Instant.now(),
                    reminders = readInstantList(taskJson, "reminders"),
                )
            )
            val subtasksJson = taskJson.optJSONArray("subtasks") ?: JSONArray()
            for (j in 0 until subtasksJson.length()) {
                val subtaskJson = subtasksJson.getJSONObject(j)
                taskDao.insertSubtask(
                    SubtaskEntity(
                        id = readString(subtaskJson, "id") ?: "${taskId}_sub_$j",
                        taskId = taskId,
                        title = readString(subtaskJson, "title") ?: "",
                        isCompleted = subtaskJson.optBoolean("isCompleted", false),
                    )
                )
            }
        }
    }

    /** Must run after [restoreTasks]: missing entries are derived from the restored tasks. */
    private fun restoreTaskCategories(categoriesJson: JSONArray) {
        taskDao.clearAllCategories()
        for (i in 0 until categoriesJson.length()) {
            val categoryJson = categoriesJson.getJSONObject(i)
            val name = readString(categoryJson, "name")?.takeIf { it.isNotBlank() } ?: continue
            taskDao.insertCategoryIfAbsent(
                TaskCategoryEntity(
                    id = readString(categoryJson, "id")
                        ?: java.util.UUID.randomUUID().toString(),
                    name = name,
                    colorValue = categoryJson.optInt(
                        "colorValue",
                        TaskCategoryEntity.DEFAULT_COLOR_VALUE
                    ),
                )
            )
        }
        // Legacy backups (and pre-category backups) carry no taskCategories array: fill the
        // table from the category names on the restored tasks.
        for (name in taskDao.getDistinctTaskCategoryNames()) {
            taskDao.insertCategoryIfAbsent(
                TaskCategoryEntity(
                    id = java.util.UUID.randomUUID().toString(),
                    name = name,
                    colorValue = TaskCategoryEntity.DEFAULT_COLOR_VALUE,
                )
            )
        }
    }

    private fun restoreHabits(habitsJson: JSONArray) {
        habitDao.clearAll()
        for (i in 0 until habitsJson.length()) {
            val habitJson = habitsJson.getJSONObject(i)
            val habitId = readString(habitJson, "id") ?: Instant.now().toEpochMilli().toString()
            habitDao.insertHabit(
                HabitEntity(
                    id = habitId,
                    name = readString(habitJson, "name") ?: "",
                    frequency = enumOrDefault(
                        readString(habitJson, "frequency"),
                        HabitFrequency.DAILY
                    ),
                    targetDaysPerWeek = if (habitJson.has("targetDaysPerWeek") && !habitJson.isNull(
                            "targetDaysPerWeek"
                        )
                    ) habitJson.optInt("targetDaysPerWeek") else null,
                    targetWeekdays = readString(habitJson, "targetWeekdays"),
                    targetTimeMinutes = if (habitJson.has("targetTimeMinutes") && !habitJson.isNull(
                            "targetTimeMinutes"
                        )
                    ) habitJson.optInt("targetTimeMinutes") else null,
                    createdAt = readInstant(habitJson, "createdAt") ?: Instant.now(),
                    colorValue = habitJson.optInt("colorValue", 0),
                    notes = readString(habitJson, "notes"),
                    iconCodePoint = if (habitJson.has("iconCodePoint") && !habitJson.isNull("iconCodePoint")) habitJson.optInt(
                        "iconCodePoint"
                    ) else null,
                    stackName = readString(habitJson, "stackName"),
                    stackOrder = if (habitJson.has("stackOrder") && !habitJson.isNull("stackOrder")) habitJson.optInt(
                        "stackOrder"
                    ) else null,
                )
            )
            val completionsJson = habitJson.optJSONArray("completions") ?: JSONArray()
            for (j in 0 until completionsJson.length()) {
                val instant = parseInstantFlexible(completionsJson.getString(j)) ?: continue
                habitDao.insertCompletion(
                    HabitCompletionEntity(
                        habitId = habitId,
                        completionDate = instant
                    )
                )
            }
        }
    }


    private fun restoreQuickHabits(quickHabitsJson: JSONArray) {
        quickHabitDao.clearAll()
        for (i in 0 until quickHabitsJson.length()) {
            val json = quickHabitsJson.getJSONObject(i)
            val id = readString(json, "id") ?: Instant.now().toEpochMilli().toString()
            quickHabitDao.insert(
                QuickHabitEntity(
                    id = id,
                    name = readString(json, "name") ?: "",
                    intervalMinutes = json.optInt("intervalMinutes", 60).coerceAtLeast(1),
                    startMinuteOfDay = readOptInt(json, "startMinuteOfDay"),
                    endMinuteOfDay = readOptInt(json, "endMinuteOfDay"),
                    targetWeekdays = readString(json, "targetWeekdays"),
                    isEnabled = json.optBoolean("isEnabled", true),
                    createdAt = readInstant(json, "createdAt") ?: Instant.now(),
                )
            )
        }
    }


    private fun restoreFocus(focusJson: JSONObject) {
        focusDao.clearAllSessions()
        focusDao.clearAllModes()
        focusDao.clearAllTags()

        val modesJson = focusJson.optJSONArray("modes") ?: JSONArray()
        for (i in 0 until modesJson.length()) {
            val modeJson = modesJson.getJSONObject(i)
            val modeId = readString(modeJson, "id") ?: Instant.now().toEpochMilli().toString()
            focusDao.insertMode(
                FocusModeEntity(
                    id = modeId,
                    name = readString(modeJson, "name") ?: "",
                    type = enumOrDefault(readString(modeJson, "type"), FocusModeType.STOPWATCH),
                    isSystem = modeJson.optBoolean("isSystem", false),
                )
            )
            val phasesJson = modeJson.optJSONArray("phases") ?: JSONArray()
            val phases = (0 until phasesJson.length()).map { j ->
                val phaseJson = phasesJson.getJSONObject(j)
                SessionPhaseEntity(
                    modeId = modeId,
                    type = enumOrDefault(readString(phaseJson, "type"), PhaseType.FOCUS),
                    durationMinutes = phaseJson.optInt("durationMinutes", 0),
                    orderIndex = j,
                )
            }
            if (phases.isNotEmpty()) focusDao.insertPhases(phases)
        }

        val tagsJson = focusJson.optJSONArray("tags") ?: JSONArray()
        for (i in 0 until tagsJson.length()) {
            val tagJson = tagsJson.getJSONObject(i)
            focusDao.insertTag(
                FocusTagEntity(
                    id = readString(tagJson, "id") ?: Instant.now().toEpochMilli().toString(),
                    name = readString(tagJson, "name") ?: "",
                    colorValue = tagJson.optInt("colorValue", 0),
                )
            )
        }

        val sessionsJson = focusJson.optJSONArray("sessions") ?: JSONArray()
        for (i in 0 until sessionsJson.length()) {
            val sessionJson = sessionsJson.getJSONObject(i)
            val sessionId = readString(sessionJson, "id") ?: Instant.now().toEpochMilli().toString()
            focusDao.insertSession(
                FocusSessionEntity(
                    id = sessionId,
                    modeId = readString(sessionJson, "modeId") ?: "",
                    startTime = readInstant(sessionJson, "startTime") ?: Instant.now(),
                    endTime = readInstant(sessionJson, "endTime"),
                    totalSecondsFocused = sessionJson.optInt("totalSecondsFocused", 0),
                    isCompleted = sessionJson.optBoolean("isCompleted", false),
                    tagId = readString(sessionJson, "tagId"),
                    targetType = enumOrDefault(
                        readString(sessionJson, "targetType"),
                        FocusTargetType.TAG
                    ),
                    targetId = readString(sessionJson, "targetId"),
                    targetLabel = readString(sessionJson, "targetLabel"),
                )
            )
            val distractionsJson = sessionJson.optJSONArray("distractions") ?: JSONArray()
            for (j in 0 until distractionsJson.length()) {
                val distractionJson = distractionsJson.getJSONObject(j)
                focusDao.insertDistraction(
                    DistractionEntity(
                        sessionId = sessionId,
                        time = readInstant(distractionJson, "time") ?: Instant.now(),
                        note = readString(distractionJson, "note") ?: "",
                    )
                )
            }
        }
    }

    // JSON read helpers.

    private fun readString(json: JSONObject, key: String): String? =
        if (json.has(key) && !json.isNull(key)) json.optString(key) else null

    private fun readOptInt(json: JSONObject, key: String): Int? =
        if (json.has(key) && !json.isNull(key)) json.optInt(key) else null

    private fun readInstant(json: JSONObject, key: String): Instant? {
        val raw = readString(json, key) ?: return null
        return parseInstantFlexible(raw)
    }

    private fun readInstantList(json: JSONObject, key: String): List<Instant> {
        val array = json.optJSONArray(key) ?: return emptyList()
        return (0 until array.length()).mapNotNull { i ->
            parseInstantFlexible(array.getString(i))
        }
    }


    private fun parseInstantFlexible(raw: String): Instant? =
        runCatching { Instant.parse(raw) }.getOrNull()
            ?: runCatching { java.time.OffsetDateTime.parse(raw).toInstant() }.getOrNull()
            ?: runCatching {
                java.time.LocalDateTime.parse(raw)
                    .atZone(java.time.ZoneId.systemDefault())
                    .toInstant()
            }.getOrNull()

    private fun readStringList(json: JSONObject, key: String): List<String> {
        val array = json.optJSONArray(key) ?: return emptyList()
        return (0 until array.length()).map { array.getString(it) }
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(name: String?, default: T): T {
        if (name == null) return default
        // To support importing from legacy app versions, try converting camelCase to UPPER_SNAKE_CASE.
        val normalizedName = name.replace(Regex("([a-z])([A-Z]+)"), "$1_$2").uppercase()
        return try {
            enumValueOf<T>(normalizedName)
        } catch (e: IllegalArgumentException) {
            try {
                enumValueOf<T>(name)
            } catch (e2: IllegalArgumentException) {
                default
            }
        }
    }

    companion object {
        private const val SCHEMA_VERSION = 1
        private const val PAYLOAD_TYPE = "user_data"
    }
}
