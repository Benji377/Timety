package io.github.benji377.timety.services.backup

import io.github.benji377.timety.data.model.task.MonthlyMode
import io.github.benji377.timety.data.model.task.Priority
import io.github.benji377.timety.data.model.task.RecurrenceUnit
import io.github.benji377.timety.data.model.task.RecurringOccurrenceEntity
import io.github.benji377.timety.data.model.task.RecurringTaskEntity
import io.github.benji377.timety.data.model.task.RecurringTaskWithOccurrences
import io.github.benji377.timety.data.model.task.SubtaskEntity
import io.github.benji377.timety.data.model.task.TaskCategoryEntity
import io.github.benji377.timety.data.model.task.TaskEntity
import io.github.benji377.timety.data.model.task.TaskPlace
import io.github.benji377.timety.data.model.task.TaskSize
import io.github.benji377.timety.data.model.task.TaskWithSubtasks
import io.github.benji377.timety.util.habit.QuickHabitScheduling.MINUTES_PER_DAY
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

// Backup JSON for tasks, their categories, and recurring tasks.

internal fun TaskWithSubtasks.toBackupJson(): JSONObject = JSONObject().apply {
    put("id", task.id)
    put("title", task.title)
    put("description", task.description)
    put("dueDate", orNull(task.dueDate?.toString()))
    put("location", task.location)
    put("place", orNull(task.place?.toJson()))
    put("priority", task.priority.name)
    put("category", task.category)
    put("size", task.size.name)
    put("isCompleted", task.isCompleted)
    put("completedAt", orNull(task.completedAt?.toString()))
    put("createdAt", task.createdAt.toString())
    put("reminders", JSONArray(task.reminders.map { it.toString() }))
    put("subtasks", JSONArray(subtasks.map { subtask ->
        JSONObject().apply {
            put("id", subtask.id)
            put("title", subtask.title)
            put("isCompleted", subtask.isCompleted)
        }
    }))
}

internal fun taskFromBackup(json: JSONObject): TaskWithSubtasks {
    val id = json.stringOrNull("id") ?: newId()
    val task = TaskEntity(
        id = id,
        title = json.stringOrNull("title") ?: "",
        description = json.stringOrNull("description") ?: "",
        dueDate = json.instantOrNull("dueDate"),
        location = json.stringOrNull("location") ?: "",
        place = json.optJSONObject("place")?.let { TaskPlace.fromJson(it) },
        priority = json.enumOr("priority", Priority.MEDIUM),
        category = json.stringOrNull("category") ?: "",
        size = json.enumOr("size", TaskSize.MEDIUM),
        isCompleted = json.optBoolean("isCompleted", false),
        completedAt = json.instantOrNull("completedAt"),
        createdAt = json.instantOrNull("createdAt") ?: Instant.now(),
        reminders = json.instantList("reminders"),
    )
    val subtasks = json.objects("subtasks").mapIndexed { index, subtask ->
        SubtaskEntity(
            id = subtask.stringOrNull("id") ?: "${id}_sub_$index",
            taskId = id,
            title = subtask.stringOrNull("title") ?: "",
            isCompleted = subtask.optBoolean("isCompleted", false),
        )
    }
    return TaskWithSubtasks(task, subtasks)
}

internal fun TaskCategoryEntity.toBackupJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("name", name)
    put("colorValue", colorValue)
}

/** Null for a category without a name, which can't be shown or picked. */
internal fun categoryFromBackup(json: JSONObject): TaskCategoryEntity? {
    val name = json.stringOrNull("name")?.takeIf { it.isNotBlank() } ?: return null
    return TaskCategoryEntity(
        id = json.stringOrNull("id") ?: newId(),
        name = name,
        colorValue = json.optInt("colorValue", TaskCategoryEntity.DEFAULT_COLOR_VALUE),
    )
}

internal fun RecurringTaskWithOccurrences.toBackupJson(): JSONObject = JSONObject().apply {
    put("id", task.id)
    put("title", task.title)
    put("description", task.description)
    put("category", task.category)
    put("dueDate", task.dueDate.toString())
    put("timeOfDayMinutes", orNull(task.timeOfDayMinutes))
    put("unit", task.unit.name)
    put("interval", task.interval)
    put("daysOfWeek", orNull(task.daysOfWeek))
    put("monthlyMode", task.monthlyMode.name)
    put("monthlyDay", orNull(task.monthlyDay))
    put("monthlyOrdinal", orNull(task.monthlyOrdinal))
    put("monthlyWeekday", orNull(task.monthlyWeekday))
    put("reminderOffsetsMinutes", JSONArray(task.reminderOffsetsMinutes))
    put("createdAt", task.createdAt.toString())
    put("occurrences", JSONArray(occurrences.map { it.completedAt.toString() }))
}

internal fun recurringTaskFromBackup(json: JSONObject): RecurringTaskWithOccurrences {
    val id = json.stringOrNull("id") ?: newId()
    val task = RecurringTaskEntity(
        id = id,
        title = json.stringOrNull("title") ?: "",
        description = json.stringOrNull("description") ?: "",
        category = json.stringOrNull("category") ?: "",
        dueDate = json.instantOrNull("dueDate") ?: Instant.now(),
        timeOfDayMinutes = json.intOrNull("timeOfDayMinutes")?.takeIf { it in 0 until MINUTES_PER_DAY },
        unit = json.enumOr("unit", RecurrenceUnit.WEEK),
        interval = json.optInt("interval", 1).coerceAtLeast(1),
        daysOfWeek = json.stringOrNull("daysOfWeek"),
        monthlyMode = json.enumOr("monthlyMode", MonthlyMode.DAY_OF_MONTH),
        monthlyDay = json.intOrNull("monthlyDay"),
        monthlyOrdinal = json.intOrNull("monthlyOrdinal"),
        monthlyWeekday = json.intOrNull("monthlyWeekday"),
        reminderOffsetsMinutes = json.intList("reminderOffsetsMinutes"),
        createdAt = json.instantOrNull("createdAt") ?: Instant.now(),
    )
    val occurrences = json.instantList("occurrences")
        .map { RecurringOccurrenceEntity(recurringTaskId = id, completedAt = it) }
    return RecurringTaskWithOccurrences(task, occurrences)
}
