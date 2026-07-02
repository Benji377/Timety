package io.github.benji377.timety.data.model.focus

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

enum class FocusModeType(val value: Int) {
    STOPWATCH(0),
    POMODORO(1),
    FLEXIBLE(2),
    CUSTOM(3)
}

enum class PhaseType(val value: Int) {
    FOCUS(0),
    REST(1)
}

@Entity(tableName = "focus_modes")
data class FocusModeEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val type: FocusModeType,
    val isSystem: Boolean = false
)

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
    val orderIndex: Int // Important for maintaining the sequence
)

enum class FocusTargetType(val value: Int) {
    TAG(0),
    TASK(1),
    HABIT(2)
}

@Entity(tableName = "focus_tags")
data class FocusTagEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val colorValue: Int
)

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
    val note: String = ""
)
