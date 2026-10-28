package io.github.benji377.timety.services.backup

import io.github.benji377.timety.data.model.focus.DistractionEntity
import io.github.benji377.timety.data.model.focus.DistractionType
import io.github.benji377.timety.data.model.focus.FocusModeEntity
import io.github.benji377.timety.data.model.focus.FocusModeType
import io.github.benji377.timety.data.model.focus.FocusSessionEntity
import io.github.benji377.timety.data.model.focus.FocusTagEntity
import io.github.benji377.timety.data.model.focus.FocusTargetType
import io.github.benji377.timety.data.model.focus.PhaseType
import io.github.benji377.timety.data.model.focus.SessionPhaseEntity
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

// Backup JSON for focus modes with their phases, tags, and sessions with their distractions.

internal fun modeToBackupJson(mode: FocusModeEntity, phases: List<SessionPhaseEntity>): JSONObject =
    JSONObject().apply {
        put("id", mode.id)
        put("name", mode.name)
        put("type", mode.type.name)
        put("isSystem", mode.isSystem)
        put("phases", JSONArray(phases.sortedBy { it.orderIndex }.map { phase ->
            JSONObject().apply {
                put("type", phase.type.name)
                put("durationMinutes", phase.durationMinutes)
            }
        }))
    }

/** The mode and its phases, ordered as listed. */
internal fun modeFromBackup(json: JSONObject): Pair<FocusModeEntity, List<SessionPhaseEntity>> {
    val id = json.stringOrNull("id") ?: newId()
    val mode = FocusModeEntity(
        id = id,
        name = json.stringOrNull("name") ?: "",
        type = json.enumOr("type", FocusModeType.STOPWATCH),
        isSystem = json.optBoolean("isSystem", false),
    )
    val phases = json.objects("phases").mapIndexed { index, phase ->
        SessionPhaseEntity(
            modeId = id,
            type = phase.enumOr("type", PhaseType.FOCUS),
            durationMinutes = phase.optInt("durationMinutes", 0),
            orderIndex = index,
        )
    }
    return mode to phases
}

internal fun FocusTagEntity.toBackupJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("name", name)
    put("colorValue", colorValue)
}

internal fun tagFromBackup(json: JSONObject) = FocusTagEntity(
    id = json.stringOrNull("id") ?: newId(),
    name = json.stringOrNull("name") ?: "",
    colorValue = json.optInt("colorValue", 0),
)

internal fun sessionToBackupJson(session: FocusSessionEntity, distractions: List<DistractionEntity>): JSONObject =
    JSONObject().apply {
        put("id", session.id)
        put("modeId", session.modeId)
        put("startTime", session.startTime.toString())
        put("endTime", orNull(session.endTime?.toString()))
        put("totalSecondsFocused", session.totalSecondsFocused)
        put("isCompleted", session.isCompleted)
        put("tagId", orNull(session.tagId))
        put("targetType", session.targetType.name)
        put("targetId", orNull(session.targetId))
        put("targetLabel", orNull(session.targetLabel))
        put("distractions", JSONArray(distractions.sortedBy { it.time }.map { distraction ->
            JSONObject().apply {
                put("time", distraction.time.toString())
                put("type", distraction.type.name)
                put("note", distraction.note)
            }
        }))
    }

internal fun sessionFromBackup(json: JSONObject): Pair<FocusSessionEntity, List<DistractionEntity>> {
    val id = json.stringOrNull("id") ?: newId()
    val session = FocusSessionEntity(
        id = id,
        modeId = json.stringOrNull("modeId") ?: "",
        startTime = json.instantOrNull("startTime") ?: Instant.now(),
        endTime = json.instantOrNull("endTime"),
        totalSecondsFocused = json.optInt("totalSecondsFocused", 0),
        isCompleted = json.optBoolean("isCompleted", false),
        tagId = json.stringOrNull("tagId"),
        targetType = json.enumOr("targetType", FocusTargetType.TAG),
        targetId = json.stringOrNull("targetId"),
        targetLabel = json.stringOrNull("targetLabel"),
    )
    val distractions = json.objects("distractions").map { distraction ->
        DistractionEntity(
            sessionId = id,
            time = distraction.instantOrNull("time") ?: Instant.now(),
            type = distraction.enumOr("type", DistractionType.DISTRACTED),
            note = distraction.stringOrNull("note") ?: "",
        )
    }
    return session to distractions
}
