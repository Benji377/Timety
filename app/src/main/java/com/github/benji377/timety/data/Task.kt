package com.github.benji377.timety.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "task")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String? = null,
    val iconName: String,
    val location: String? = null,
    val dueDate: Long? = null,
    val reminderTime: Long? = null,
    val durationEst: Long? = null,
    val status: TaskStatus = TaskStatus.TODO
)

enum class TaskStatus {
    TODO, DONE, OVERDUE
}
