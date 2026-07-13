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
import io.github.benji377.timety.data.model.goal.GoalEntity
import io.github.benji377.timety.data.model.goal.GoalEntryEntity
import io.github.benji377.timety.data.model.habit.HabitCompletionEntity
import io.github.benji377.timety.data.model.habit.HabitEntity
import io.github.benji377.timety.data.model.habit.HabitFrequency
import io.github.benji377.timety.data.model.habit.QuickHabitEntity
import io.github.benji377.timety.data.model.task.MonthlyMode
import io.github.benji377.timety.data.model.task.Priority
import io.github.benji377.timety.data.model.task.RecurrenceUnit
import io.github.benji377.timety.data.model.task.RecurringOccurrenceEntity
import io.github.benji377.timety.data.model.task.RecurringTaskEntity
import io.github.benji377.timety.data.model.task.SubtaskEntity
import io.github.benji377.timety.data.model.task.TaskCategoryEntity
import io.github.benji377.timety.data.model.task.TaskEntity
import io.github.benji377.timety.data.model.task.TaskSize
import io.github.benji377.timety.data.model.user.DayRating
import io.github.benji377.timety.data.model.user.DayRatingEntity
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
    private val recurringTaskDao get() = database.recurringTaskDao()
    private val habitDao get() = database.habitDao()
    private val quickHabitDao get() = database.quickHabitDao()
    private val goalDao get() = database.goalDao()
    private val focusDao get() = database.focusDao()
    private val userDao get() = database.userDao()
    private val dayRatingDao get() = database.dayRatingDao()


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
            put("recurringTasks", recurringTasksToJson())
            put("habits", habitsToJson())
            put("quickHabits", quickHabitsToJson())
            put("goals", goalsToJson())
            put("dayRatings", dayRatingsToJson())
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

    private suspend fun recurringTasksToJson(): JSONArray {
        val array = JSONArray()
        for (entry in recurringTaskDao.getAllWithOccurrences().first()) {
            val task = entry.task
            array.put(
                JSONObject().apply {
                    put("id", task.id)
                    put("title", task.title)
                    put("description", task.description)
                    put("category", task.category)
                    put("dueDate", task.dueDate.toString())
                    put("unit", task.unit.name)
                    put("interval", task.interval)
                    put("daysOfWeek", task.daysOfWeek ?: JSONObject.NULL)
                    put("monthlyMode", task.monthlyMode.name)
                    put("monthlyDay", task.monthlyDay ?: JSONObject.NULL)
                    put("monthlyOrdinal", task.monthlyOrdinal ?: JSONObject.NULL)
                    put("monthlyWeekday", task.monthlyWeekday ?: JSONObject.NULL)
                    put("reminderOffsetsMinutes", JSONArray(task.reminderOffsetsMinutes))
                    put("createdAt", task.createdAt.toString())
                    put(
                        "occurrences",
                        JSONArray(entry.occurrences.map { it.completedAt.toString() })
                    )
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

    private suspend fun goalsToJson(): JSONArray {
        val array = JSONArray()
        for (goalWithEntries in goalDao.getAllGoalsWithEntries().first()) {
            val goal = goalWithEntries.goal
            array.put(
                JSONObject().apply {
                    put("id", goal.id)
                    put("name", goal.name)
                    put("description", goal.description)
                    put("colorValue", goal.colorValue)
                    put("iconCodePoint", goal.iconCodePoint ?: JSONObject.NULL)
                    put("targetValue", goal.targetValue)
                    put("unitLabel", goal.unitLabel)
                    put("targetDate", goal.targetDate.toString())
                    put("createdAt", goal.createdAt.toString())
                    put("completedAt", goal.completedAt?.toString() ?: JSONObject.NULL)
                    put(
                        "entries",
                        JSONArray(goalWithEntries.entries.map { entry ->
                            JSONObject().apply {
                                put("value", entry.value)
                                put("timestamp", entry.timestamp.toString())
                            }
                        })
                    )
                }
            )
        }
        return array
    }

    private suspend fun dayRatingsToJson(): JSONArray {
        val array = JSONArray()
        for (rating in dayRatingDao.getAll().first()) {
            array.put(
                JSONObject().apply {
                    put("dayKey", rating.dayKey)
                    put("rating", rating.rating)
                    put("createdAt", rating.createdAt.toString())
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
        // Optional key: older and legacy backups simply carry no recurring tasks.
        restoreRecurringTasks(json.optJSONArray("recurringTasks") ?: JSONArray())
        restoreHabits(json.optJSONArray("habits") ?: JSONArray())
        restoreQuickHabits(json.optJSONArray("quickHabits") ?: JSONArray())
        // Optional key: backups from before the goals feature simply carry none.
        restoreGoals(json.optJSONArray("goals") ?: JSONArray())
        restoreDayRatings(json.optJSONArray("dayRatings") ?: JSONArray())
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

    private fun restoreRecurringTasks(recurringJson: JSONArray) {
        recurringTaskDao.clearAll()
        for (i in 0 until recurringJson.length()) {
            val json = recurringJson.getJSONObject(i)
            val id = readString(json, "id") ?: Instant.now().toEpochMilli().toString()
            recurringTaskDao.insert(
                RecurringTaskEntity(
                    id = id,
                    title = readString(json, "title") ?: "",
                    description = readString(json, "description") ?: "",
                    category = readString(json, "category") ?: "",
                    dueDate = readInstant(json, "dueDate") ?: Instant.now(),
                    unit = enumOrDefault(readString(json, "unit"), RecurrenceUnit.WEEK),
                    interval = json.optInt("interval", 1).coerceAtLeast(1),
                    daysOfWeek = readString(json, "daysOfWeek"),
                    monthlyMode = enumOrDefault(
                        readString(json, "monthlyMode"),
                        MonthlyMode.DAY_OF_MONTH
                    ),
                    monthlyDay = readOptInt(json, "monthlyDay"),
                    monthlyOrdinal = readOptInt(json, "monthlyOrdinal"),
                    monthlyWeekday = readOptInt(json, "monthlyWeekday"),
                    reminderOffsetsMinutes = readIntList(json, "reminderOffsetsMinutes"),
                    createdAt = readInstant(json, "createdAt") ?: Instant.now(),
                )
            )
            val occurrencesJson = json.optJSONArray("occurrences") ?: JSONArray()
            for (j in 0 until occurrencesJson.length()) {
                val instant = parseInstantFlexible(occurrencesJson.getString(j)) ?: continue
                recurringTaskDao.insertOccurrence(
                    RecurringOccurrenceEntity(recurringTaskId = id, completedAt = instant)
                )
            }
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


    private fun restoreGoals(goalsJson: JSONArray) {
        goalDao.clearAll()
        for (i in 0 until goalsJson.length()) {
            val goalJson = goalsJson.getJSONObject(i)
            val goalId = readString(goalJson, "id") ?: Instant.now().toEpochMilli().toString()
            goalDao.insertGoal(
                GoalEntity(
                    id = goalId,
                    name = readString(goalJson, "name") ?: "",
                    description = readString(goalJson, "description") ?: "",
                    colorValue = goalJson.optInt("colorValue", 0),
                    iconCodePoint = readOptInt(goalJson, "iconCodePoint"),
                    targetValue = goalJson.optInt("targetValue", 1).coerceAtLeast(1),
                    unitLabel = readString(goalJson, "unitLabel") ?: "",
                    targetDate = readInstant(goalJson, "targetDate") ?: Instant.now(),
                    createdAt = readInstant(goalJson, "createdAt") ?: Instant.now(),
                    completedAt = readInstant(goalJson, "completedAt"),
                )
            )
            val entriesJson = goalJson.optJSONArray("entries") ?: JSONArray()
            for (j in 0 until entriesJson.length()) {
                val entryJson = entriesJson.getJSONObject(j)
                val timestamp = readInstant(entryJson, "timestamp") ?: continue
                goalDao.insertEntry(
                    GoalEntryEntity(
                        goalId = goalId,
                        value = entryJson.optInt("value", 1).coerceAtLeast(1),
                        timestamp = timestamp,
                    )
                )
            }
        }
    }


    private fun restoreDayRatings(ratingsJson: JSONArray) {
        dayRatingDao.clearAll()
        for (i in 0 until ratingsJson.length()) {
            val json = ratingsJson.getJSONObject(i)
            val dayKey = readString(json, "dayKey") ?: continue
            // Skip ratings outside the known scale rather than importing garbage.
            val rating = DayRating.fromValue(json.optInt("rating")) ?: continue
            dayRatingDao.upsert(
                DayRatingEntity(
                    dayKey = dayKey,
                    rating = rating.value,
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

    private fun readIntList(json: JSONObject, key: String): List<Int> {
        val array = json.optJSONArray(key) ?: return emptyList()
        return (0 until array.length()).map { array.getInt(it) }
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

        /** Top-level keys that identify a payload (including legacy ones) as a Timety backup. */
        private val KNOWN_DATA_KEYS = listOf(
            "tasks", "taskCategories", "recurringTasks", "habits", "quickHabits",
            "goals", "dayRatings", "focus", "userProfile", "preferences",
        )
    }
}
