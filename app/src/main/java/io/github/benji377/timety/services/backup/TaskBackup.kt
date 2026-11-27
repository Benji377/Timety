package io.github.benji377.timety.services.backup

import io.github.benji377.timety.data.model.task.MonthlyMode
import io.github.benji377.timety.data.model.task.OccurrenceStatus
import io.github.benji377.timety.data.model.task.Priority
import io.github.benji377.timety.data.model.task.RecurrenceUnit
import io.github.benji377.timety.data.model.task.RecurringOccurrenceEntity
import io.github.benji377.timety.data.model.task.RecurringSubtaskEntity
import io.github.benji377.timety.data.model.task.RecurringTaskEntity
import io.github.benji377.timety.data.model.task.RecurringTaskWithOccurrences
import io.github.benji377.timety.data.model.task.SubtaskEntity
import io.github.benji377.timety.data.model.task.TaskEntity
import io.github.benji377.timety.data.model.task.TaskPlace
import io.github.benji377.timety.data.model.task.TaskSize
import io.github.benji377.timety.data.model.task.TaskWindowUnit
import io.github.benji377.timety.data.model.task.TaskWithSubtasks
import io.github.benji377.timety.util.habit.QuickHabitScheduling.MINUTES_PER_DAY
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

// Backup JSON for tasks and recurring tasks.

internal fun TaskWithSubtasks.toBackupJson(): JSONObject = JSONObject().apply {
    put("id", task.id)
    put("title", task.title)
    put("description", task.description)
    put("dueDate", orNull(task.dueDate?.toString()))
    put("hasDueTime", task.hasDueTime)
    put("location", task.location)
    put("place", orNull(task.place?.toJson()))
    put("priority", task.priority.name)
    put("size", task.size.name)
    put("isCompleted", task.isCompleted)
    put("completedAt", orNull(task.completedAt?.toString()))
    put("createdAt", task.createdAt.toString())
    put("reminders", JSONArray(task.reminders.map { it.toString() }))
    put("pinnedAt", orNull(task.pinnedAt?.toString()))
    // Array order is the subtask order.
    put("subtasks", JSONArray(subtasks.sortedBy { it.sortOrder }.map { subtask ->
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
        hasDueTime = json.optBoolean("hasDueTime", true),
        location = json.stringOrNull("location") ?: "",
        place = json.optJSONObject("place")?.let { TaskPlace.fromJson(it) },
        priority = json.enumOr("priority", Priority.MEDIUM),
        size = json.enumOr("size", TaskSize.MEDIUM),
        isCompleted = json.optBoolean("isCompleted", false),
        completedAt = json.instantOrNull("completedAt"),
        createdAt = json.instantOrNull("createdAt") ?: Instant.now(),
        reminders = json.instantList("reminders"),
        // A done task is never pinned; older backups restore unpinned.
        pinnedAt = json.instantOrNull("pinnedAt")?.takeIf { !json.optBoolean("isCompleted", false) },
    )
    val subtasks = json.objects("subtasks").mapIndexed { index, subtask ->
        SubtaskEntity(
            id = subtask.stringOrNull("id") ?: "${id}_sub_$index",
            taskId = id,
            title = subtask.stringOrNull("title") ?: "",
            isCompleted = subtask.optBoolean("isCompleted", false),
            sortOrder = index,
        )
    }
    return TaskWithSubtasks(task, subtasks)
}

internal fun RecurringTaskWithOccurrences.toBackupJson(): JSONObject = JSONObject().apply {
    put("id", task.id)
    put("title", task.title)
    put("description", task.description)
    put("priority", task.priority.name)
    put("size", task.size.name)
    put("location", task.location)
    put("place", orNull(task.place?.toJson()))
    put("dueDate", task.dueDate.toString())
    put("hasDueTime", task.hasDueTime)
    put("timeOfDayMinutes", orNull(task.timeOfDayMinutes))
    put("unit", task.unit.name)
    put("interval", task.interval)
    put("daysOfWeek", orNull(task.daysOfWeek))
    put("monthlyMode", task.monthlyMode.name)
    put("monthlyDay", orNull(task.monthlyDay))
    put("monthlyOrdinal", orNull(task.monthlyOrdinal))
    put("monthlyWeekday", orNull(task.monthlyWeekday))
    put("reminderOffsetsMinutes", JSONArray(task.reminderOffsetsMinutes))
    put("windowAmount", orNull(task.windowAmount))
    put("windowUnit", orNull(task.windowUnit?.name))
    put("createdAt", task.createdAt.toString())
    put("pinnedAt", orNull(task.pinnedAt?.toString()))
    // Objects since checklists; older backups hold bare completedAt strings.
    put("occurrences", JSONArray(log.sortedBy { it.completedAt }.map { occurrence ->
        JSONObject().apply {
            put("completedAt", occurrence.completedAt.toString())
            put("status", occurrence.status.name)
            put("previousDueDate", orNull(occurrence.previousDueDate?.toString()))
            put("subtasksDone", orNull(occurrence.subtasksDone))
            put("subtasksTotal", orNull(occurrence.subtasksTotal))
        }
    }))
    // Array order is the checklist order.
    put("subtasks", JSONArray(subtasks.sortedBy { it.sortOrder }.map { subtask ->
        JSONObject().apply {
            put("id", subtask.id)
            put("title", subtask.title)
            put("isCompleted", subtask.isCompleted)
        }
    }))
}

internal fun recurringTaskFromBackup(json: JSONObject): RecurringTaskWithOccurrences {
    val id = json.stringOrNull("id") ?: newId()
    // Kept only as a valid pair; anything else falls back to the global window.
    val windowUnit = json.stringOrNull("windowUnit")
        ?.let { name -> TaskWindowUnit.entries.firstOrNull { it.name == name } }
    val windowAmount = windowUnit?.let { unit -> json.intOrNull("windowAmount")?.takeIf { it in unit.amounts } }
    val task = RecurringTaskEntity(
        id = id,
        title = json.stringOrNull("title") ?: "",
        description = json.stringOrNull("description") ?: "",
        // Older backups have none of these four and get a new task's defaults.
        priority = json.enumOr("priority", Priority.MEDIUM),
        size = json.enumOr("size", TaskSize.MEDIUM),
        location = json.stringOrNull("location") ?: "",
        place = json.optJSONObject("place")?.let { TaskPlace.fromJson(it) },
        dueDate = json.instantOrNull("dueDate") ?: Instant.now(),
        hasDueTime = json.optBoolean("hasDueTime", true),
        timeOfDayMinutes = json.intOrNull("timeOfDayMinutes")?.takeIf { it in 0 until MINUTES_PER_DAY },
        unit = json.enumOr("unit", RecurrenceUnit.WEEK),
        interval = json.optInt("interval", 1).coerceAtLeast(1),
        daysOfWeek = json.stringOrNull("daysOfWeek"),
        monthlyMode = json.enumOr("monthlyMode", MonthlyMode.DAY_OF_MONTH),
        monthlyDay = json.intOrNull("monthlyDay"),
        monthlyOrdinal = json.intOrNull("monthlyOrdinal"),
        monthlyWeekday = json.intOrNull("monthlyWeekday"),
        reminderOffsetsMinutes = json.intList("reminderOffsetsMinutes"),
        windowAmount = windowAmount,
        windowUnit = windowUnit.takeIf { windowAmount != null },
        createdAt = json.instantOrNull("createdAt") ?: Instant.now(),
        pinnedAt = json.instantOrNull("pinnedAt"),
    )
    val occurrences = occurrencesFromBackup(json, id)
    val subtasks = json.objects("subtasks").mapIndexed { index, subtask ->
        RecurringSubtaskEntity(
            id = subtask.stringOrNull("id") ?: "${id}_sub_$index",
            recurringTaskId = id,
            title = subtask.stringOrNull("title") ?: "",
            isCompleted = subtask.optBoolean("isCompleted", false),
            sortOrder = index,
        )
    }
    return RecurringTaskWithOccurrences(task, occurrences, subtasks)
}

/**
 * The logged occurrences, as objects or, from older backups, bare completedAt strings (which were
 * all completions). Entries whose time doesn't parse are dropped, and a checklist snapshot is kept
 * only as a sane pair.
 */
private fun occurrencesFromBackup(json: JSONObject, taskId: String): List<RecurringOccurrenceEntity> {
    val array = json.optJSONArray("occurrences") ?: return emptyList()
    return (0 until array.length()).mapNotNull { index ->
        val entry = array.optJSONObject(index)
        if (entry == null) {
            val completedAt = parseInstantFlexible(array.optString(index)) ?: return@mapNotNull null
            return@mapNotNull RecurringOccurrenceEntity(recurringTaskId = taskId, completedAt = completedAt)
        }
        val completedAt = entry.instantOrNull("completedAt") ?: return@mapNotNull null
        val total = entry.intOrNull("subtasksTotal")?.takeIf { it > 0 }
        val done = total?.let { entry.intOrNull("subtasksDone")?.takeIf { done -> done in 0..it } }
        RecurringOccurrenceEntity(
            recurringTaskId = taskId,
            completedAt = completedAt,
            status = entry.enumOr("status", OccurrenceStatus.COMPLETED),
            previousDueDate = entry.instantOrNull("previousDueDate"),
            subtasksDone = done,
            subtasksTotal = total.takeIf { done != null },
        )
    }
}
