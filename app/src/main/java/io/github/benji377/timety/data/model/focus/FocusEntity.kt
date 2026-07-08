package io.github.benji377.timety.data.model.focus

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/** Type of interruption logged against a focus session. */
enum class DistractionType {
    DISTRACTED,
    HYDRATED,
    STRETCHED,
    SNACK,
    RESTROOM
}

/** Timing behavior of a focus mode. */
enum class FocusModeType(val value: Int) {
    STOPWATCH(0),
    POMODORO(1),
    FLEXIBLE(2),
    CUSTOM(3)
}

/** Whether a session phase is a focus period or a rest period. */
enum class PhaseType(val value: Int) {
    FOCUS(0),
    REST(1)
}

/** A focus mode definition, either user-created or one of the built-in system modes. */
@Entity(tableName = "focus_modes")
data class FocusModeEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val type: FocusModeType,
    val isSystem: Boolean = false
) {
    companion object {
        // Fixed ids of the built-in modes seeded on first launch (FocusViewModel) and given
        // localized display names (ModeTimeline). Persisted in the DB - do not change.
        const val SYSTEM_STOPWATCH_ID = "system_stopwatch"
        const val SYSTEM_FLEXIBLE_ID = "system_flexible"
        const val SYSTEM_POMODORO_ID = "system_pomodoro"
    }
}

/** A single phase (focus or rest) within a focus mode's sequence. */
@Entity(
    tableName = "session_phases",
    foreignKeys = [
        ForeignKey(
            entity = FocusModeEntity::class,
            parentColumns = ["id"],
            childColumns = ["modeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("modeId")]
)
data class SessionPhaseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val modeId: String,
    val type: PhaseType,
    val durationMinutes: Int,
    val orderIndex: Int // Determines the phase's position within the mode.
)

/** What a focus session or tag selection is attributed to. */
enum class FocusTargetType(val value: Int) {
    TAG(0),
    TASK(1),
    HABIT(2)
}

/** A user-defined label for categorizing focus sessions. */
@Entity(tableName = "focus_tags")
data class FocusTagEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val colorValue: Int
)

/** A single recorded focus session, including its timing, completion state, and target. */
@Entity(
    tableName = "focus_sessions",
    foreignKeys = [
        ForeignKey(
            entity = FocusModeEntity::class,
            parentColumns = ["id"],
            childColumns = ["modeId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("modeId")]
)
data class FocusSessionEntity(
    @PrimaryKey
    val id: String,
    val modeId: String,
    val startTime: Instant,
    val endTime: Instant? = null,
    val totalSecondsFocused: Int = 0,
    val isCompleted: Boolean = false,
    val tagId: String? = null,
    val targetType: FocusTargetType = FocusTargetType.TAG,
    val targetId: String? = null,
    val targetLabel: String? = null
)

/** A logged interruption during a focus session. */
@Entity(
    tableName = "distractions",
    foreignKeys = [
        ForeignKey(
            entity = FocusSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId")]
)
data class DistractionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: String,
    val time: Instant,
    val type: DistractionType = DistractionType.DISTRACTED,
    val note: String = ""
)
