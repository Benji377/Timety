package io.github.benji377.timety.data.model.task

import androidx.room.Entity
import androidx.room.Index
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
    ALPHABETICAL,
    CATEGORY
}

/** Predefined offsets (or a custom one) for scheduling a task reminder relative to its due date. */
enum class ReminderOption {
    ON_TIME,
    MINUTES_30_BEFORE,
    HOUR_1_BEFORE,
    DAY_1_BEFORE,
    CUSTOM
}

/**
 * A user-defined category used to group tasks, identified by a unique name and display color.
 * Tasks reference categories by [name] ([TaskEntity.category]), not by id, so renaming a category
 * must also update every task's stored name (see `TaskDao.updateCategoryAndTasks`).
 */
@Entity(
    tableName = "task_categories",
    indices = [Index(value = ["name"], unique = true)]
)
data class TaskCategoryEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val colorValue: Int
) {
    companion object {
        // Matches ui.theme.TaskColor; used when a category is created without an explicit color
        // (task form quick-add, legacy backup import).
        const val DEFAULT_COLOR_VALUE = 0xFF2563EB.toInt()
    }
}

/** A single task with its scheduling, categorization, and completion state. */
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String = "",
    val dueDate: Instant? = null,
    val location: String = "",
    val priority: Priority = Priority.MEDIUM,
    val category: String = "",
    val size: TaskSize = TaskSize.MEDIUM,
    val isCompleted: Boolean = false,
    val completedAt: Instant? = null,
    val createdAt: Instant,
    val reminders: List<Instant> = emptyList(),
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
