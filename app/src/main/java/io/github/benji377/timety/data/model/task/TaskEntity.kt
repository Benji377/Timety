package io.github.benji377.timety.data.model.task

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

/** Priority level of a task, from low to very high. */
enum class Priority(val value: Int) {
    LOW(0),
    MEDIUM(1),
    HIGH(2),
    VERY_HIGH(3)
}

/** Relative size/effort estimate for a task. */
enum class TaskSize(val value: Int) {
    SMALL(0),
    MEDIUM(1),
    LARGE(2),
    VERY_LARGE(3)
}

/** Available sort orders for the task list. */
enum class TaskSortOption {
    DUE_DATE,
    PRIORITY,
    SIZE,
    ALPHABETICAL
}

/** A single task with its scheduling and completion state. */
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String = "",
    val dueDate: Instant? = null,
    val location: String = "",
    val priority: Priority = Priority.MEDIUM,
    val size: TaskSize = TaskSize.MEDIUM,
    val isCompleted: Boolean = false,
    val completedAt: Instant? = null,
    val createdAt: Instant,
    val reminders: List<Instant> = emptyList(),
    // Set only when [location] was picked from a search; a hand-typed location has no place.
    val place: TaskPlace? = null,
)

/** A task paired with its subtasks, for a Room `@Relation` query. */
data class TaskWithSubtasks(
    @androidx.room.Embedded val task: TaskEntity,
    @androidx.room.Relation(
        parentColumn = "id",
        entityColumn = "taskId"
    )
    val subtasks: List<SubtaskEntity>
)

/** Room returns `@Relation` rows unordered, so readers that show or export subtasks sort them here. */
fun TaskWithSubtasks.withSortedSubtasks(): TaskWithSubtasks =
    copy(subtasks = subtasks.sortedBy { it.sortOrder })
