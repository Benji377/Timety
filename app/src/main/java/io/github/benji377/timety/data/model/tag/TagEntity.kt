package io.github.benji377.timety.data.model.tag

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import io.github.benji377.timety.data.model.focus.FocusSessionEntity
import io.github.benji377.timety.data.model.goal.GoalEntity
import io.github.benji377.timety.data.model.habit.HabitEntity
import io.github.benji377.timety.data.model.task.RecurringTaskEntity
import io.github.benji377.timety.data.model.task.TaskEntity

/** A user-defined label shared by tasks, recurring tasks, habits, goals, and focus sessions. */
@Entity(
    tableName = "tags",
    indices = [Index(value = ["name"], unique = true)]
)
data class TagEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(collate = ColumnInfo.NOCASE)
    val name: String,
    val colorValue: Int
) {
    companion object {
        // Matches ui.theme.TaskColor; used when a tag is created without an explicit color
        // (legacy category names and session labels that have no color of their own).
        const val DEFAULT_COLOR_VALUE = 0xFF2563EB.toInt()

        /** Whether two tag names read as the same one: trimmed, ignoring case. */
        fun isSameName(a: String, b: String): Boolean = a.trim().equals(b.trim(), ignoreCase = true)
    }
}

/** The kinds of item a tag can be attached to, each backed by its own join table. */
enum class TaggedItemType {
    TASK,
    RECURRING_TASK,
    HABIT,
    GOAL,
    SESSION
}

/** A tag together with the id of the item it is attached to, for reading any join table. */
data class ItemTag(
    val itemId: String,
    @Embedded val tag: TagEntity
)

/** Attaches a [TagEntity] to a [TaskEntity]. */
@Entity(
    tableName = "task_tags",
    primaryKeys = ["taskId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("tagId")]
)
data class TaskTagEntity(
    val taskId: String,
    val tagId: String
)

/** Attaches a [TagEntity] to a [RecurringTaskEntity]. */
@Entity(
    tableName = "recurring_task_tags",
    primaryKeys = ["recurringTaskId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = RecurringTaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["recurringTaskId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("tagId")]
)
data class RecurringTaskTagEntity(
    val recurringTaskId: String,
    val tagId: String
)

/** Attaches a [TagEntity] to a [HabitEntity]. */
@Entity(
    tableName = "habit_tags",
    primaryKeys = ["habitId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("tagId")]
)
data class HabitTagEntity(
    val habitId: String,
    val tagId: String
)

/** Attaches a [TagEntity] to a [GoalEntity]. */
@Entity(
    tableName = "goal_tags",
    primaryKeys = ["goalId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = GoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["goalId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("tagId")]
)
data class GoalTagEntity(
    val goalId: String,
    val tagId: String
)

/** Attaches a [TagEntity] to a [FocusSessionEntity]; copied from its target when logged. */
@Entity(
    tableName = "session_tags",
    primaryKeys = ["sessionId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = FocusSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("tagId")]
)
data class SessionTagEntity(
    val sessionId: String,
    val tagId: String
)
