package io.github.benji377.timety.services.backup

import io.github.benji377.timety.data.model.habit.HabitCompletionEntity
import io.github.benji377.timety.data.model.habit.HabitEntity
import io.github.benji377.timety.data.model.habit.HabitFrequency
import io.github.benji377.timety.data.model.habit.HabitWithCompletions
import io.github.benji377.timety.data.model.habit.QuickHabitEntity
import io.github.benji377.timety.util.habit.HabitUtils
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

// Backup JSON for habits and quick habits.

internal fun HabitWithCompletions.toBackupJson(): JSONObject = JSONObject().apply {
    put("id", habit.id)
    put("name", habit.name)
    put("frequency", habit.frequency.name)
    put("targetDaysPerWeek", orNull(habit.targetCount))
    put("targetWeekdays", orNull(habit.targetWeekdays))
    put("reminderTimes", orNull(habit.reminderTimes))
    put("createdAt", habit.createdAt.toString())
    put("colorValue", habit.colorValue)
    put("notes", orNull(habit.notes))
    put("iconCodePoint", orNull(habit.iconCodePoint))
    put("stackName", orNull(habit.stackName))
    put("stackOrder", orNull(habit.stackOrder))
    put("sortOrder", habit.sortOrder)
    put("completions", JSONArray(completions.map { it.completionDate.toString() }))
}

internal fun habitFromBackup(json: JSONObject): HabitWithCompletions {
    val id = json.stringOrNull("id") ?: newId()
    val habit = HabitEntity(
        id = id,
        name = json.stringOrNull("name") ?: "",
        frequency = json.enumOr("frequency", HabitFrequency.DAILY),
        targetCount = json.intOrNull("targetDaysPerWeek"),
        targetWeekdays = json.stringOrNull("targetWeekdays"),
        // Older backups carry a single `targetTimeMinutes` instead.
        reminderTimes = json.stringOrNull("reminderTimes")
            ?: json.intOrNull("targetTimeMinutes")?.let { HabitUtils.serializeReminderTimes(listOf(it)) },
        createdAt = json.instantOrNull("createdAt") ?: Instant.now(),
        colorValue = json.optInt("colorValue", 0),
        notes = json.stringOrNull("notes"),
        iconCodePoint = json.intOrNull("iconCodePoint"),
        stackName = json.stringOrNull("stackName"),
        stackOrder = json.intOrNull("stackOrder"),
        sortOrder = json.optInt("sortOrder", 0),
    )
    val completions = json.instantList("completions")
        .map { HabitCompletionEntity(habitId = id, completionDate = it) }
    return HabitWithCompletions(habit, completions)
}

internal fun QuickHabitEntity.toBackupJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("name", name)
    put("intervalMinutes", intervalMinutes)
    put("startMinuteOfDay", orNull(startMinuteOfDay))
    put("endMinuteOfDay", orNull(endMinuteOfDay))
    put("targetWeekdays", orNull(targetWeekdays))
    put("isEnabled", isEnabled)
    put("createdAt", createdAt.toString())
}

internal fun quickHabitFromBackup(json: JSONObject) = QuickHabitEntity(
    id = json.stringOrNull("id") ?: newId(),
    name = json.stringOrNull("name") ?: "",
    intervalMinutes = json.optInt("intervalMinutes", 60).coerceAtLeast(1),
    startMinuteOfDay = json.intOrNull("startMinuteOfDay"),
    endMinuteOfDay = json.intOrNull("endMinuteOfDay"),
    targetWeekdays = json.stringOrNull("targetWeekdays"),
    isEnabled = json.optBoolean("isEnabled", true),
    createdAt = json.instantOrNull("createdAt") ?: Instant.now(),
)
