package com.github.benji377.timety.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardDoubleArrowUp
import androidx.compose.material.icons.filled.Remove
import androidx.compose.ui.graphics.vector.ImageVector
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
    val reminders: List<Long> = emptyList(),
    val categoryId: Int? = null,
    val durationEst: Long? = null,
    val status: TaskStatus = TaskStatus.TODO,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val size: TaskSize = TaskSize.MEDIUM
)

enum class TaskStatus {
    TODO, DONE, OVERDUE
}

enum class TaskPriority(val label: String) {
    URGENT("Urgent"),
    HIGH("High"),
    MEDIUM("Medium"),
    LOW("Low");

    fun getIcon(): ImageVector = when (this) {
        URGENT -> Icons.Default.KeyboardDoubleArrowUp
        HIGH -> Icons.Default.KeyboardArrowUp
        MEDIUM -> Icons.Default.Remove
        LOW -> Icons.Default.KeyboardArrowDown
    }
}

enum class TaskSize(val label: String, val estimatedMinutes: Int, val badgeText: String) {
    TINY("Tiny", 15, "XS"),
    SMALL("Small", 30, "S"),
    MEDIUM("Medium", 60, "M"),
    LARGE("Large", 120, "L"),
    XLARGE("X-Large", 240, "XL");

    fun getIcon(): ImageVector = when (this) {
        TINY -> Icons.Default.Crop
        SMALL -> Icons.Default.CropSquare
        MEDIUM -> Icons.Default.Fullscreen
        LARGE -> Icons.Default.AspectRatio
        XLARGE -> Icons.Default.FitScreen
    }
}

