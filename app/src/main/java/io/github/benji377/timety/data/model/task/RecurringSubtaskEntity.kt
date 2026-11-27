package io.github.benji377.timety.data.model.task

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A checklist item of a recurring task, shown in ascending [sortOrder]. The checklist is fixed:
 * the user ticks items during a round, and completing the round unticks all of them again. Kept
 * apart from [SubtaskEntity], whose rows point at one-off tasks only.
 */
@Entity(
    tableName = "recurring_subtasks",
    foreignKeys = [
        ForeignKey(
            entity = RecurringTaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["recurringTaskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("recurringTaskId")]
)
data class RecurringSubtaskEntity(
    @PrimaryKey
    val id: String,
    val recurringTaskId: String,
    val title: String,
    val isCompleted: Boolean = false,
    val sortOrder: Int = 0
) : java.io.Serializable
