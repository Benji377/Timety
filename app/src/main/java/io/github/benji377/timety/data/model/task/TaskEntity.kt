package io.github.benji377.timety.data.model.task

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

enum class Priority(val value: Int) {
    LOW(0),
    MEDIUM(1),
    HIGH(2),
    VERY_HIGH(3)
}

enum class TaskSize(val value: Int) {
    SMALL(0),
    MEDIUM(1),
    LARGE(2),
    VERY_LARGE(3)
}

enum class TaskSortOption {
    DUE_DATE,
    PRIORITY,
    SIZE,
    ALPHABETICAL,
    CATEGORY
}

enum class ReminderOption {
    ON_TIME,
    MINUTES_30_BEFORE,
    HOUR_1_BEFORE,
    DAY_1_BEFORE,
    CUSTOM
}

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
    val reminders: List<Instant> = emptyList() // Needs TypeConverter
)

data class TaskWithSubtasks(
    @androidx.room.Embedded val task: TaskEntity,
    @androidx.room.Relation(
        parentColumn = "id",
        entityColumn = "taskId"
    )
    val subtasks: List<SubtaskEntity>
)
