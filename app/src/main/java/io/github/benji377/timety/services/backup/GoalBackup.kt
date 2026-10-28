package io.github.benji377.timety.services.backup

import io.github.benji377.timety.data.model.goal.GoalEntity
import io.github.benji377.timety.data.model.goal.GoalEntryEntity
import io.github.benji377.timety.data.model.goal.GoalWithEntries
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

// Backup JSON for goals and their logged entries.

internal fun GoalWithEntries.toBackupJson(): JSONObject = JSONObject().apply {
    put("id", goal.id)
    put("name", goal.name)
    put("description", goal.description)
    put("colorValue", goal.colorValue)
    put("iconCodePoint", orNull(goal.iconCodePoint))
    put("targetValue", goal.targetValue)
    put("unitLabel", goal.unitLabel)
    put("targetDate", goal.targetDate.toString())
    put("createdAt", goal.createdAt.toString())
    put("completedAt", orNull(goal.completedAt?.toString()))
    put("entries", JSONArray(entries.map { entry ->
        JSONObject().apply {
            put("value", entry.value)
            put("timestamp", entry.timestamp.toString())
        }
    }))
}

internal fun goalFromBackup(json: JSONObject): GoalWithEntries {
    val id = json.stringOrNull("id") ?: newId()
    val goal = GoalEntity(
        id = id,
        name = json.stringOrNull("name") ?: "",
        description = json.stringOrNull("description") ?: "",
        colorValue = json.optInt("colorValue", 0),
        iconCodePoint = json.intOrNull("iconCodePoint"),
        targetValue = json.optInt("targetValue", 1).coerceAtLeast(1),
        unitLabel = json.stringOrNull("unitLabel") ?: "",
        targetDate = json.instantOrNull("targetDate") ?: Instant.now(),
        createdAt = json.instantOrNull("createdAt") ?: Instant.now(),
        completedAt = json.instantOrNull("completedAt"),
    )
    val entries = json.objects("entries").mapNotNull { entry ->
        val timestamp = entry.instantOrNull("timestamp") ?: return@mapNotNull null
        GoalEntryEntity(goalId = id, value = entry.optInt("value", 1).coerceAtLeast(1), timestamp = timestamp)
    }
    return GoalWithEntries(goal, entries)
}
