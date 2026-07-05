package io.github.benji377.timety.ui.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardDoubleArrowUp
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import io.github.benji377.timety.data.model.task.Priority
import io.github.benji377.timety.data.model.task.TaskSize
import io.github.benji377.timety.ui.theme.ErrorColor
import io.github.benji377.timety.ui.theme.TaskColor
import io.github.benji377.timety.ui.theme.WarningColor


object AppUtils {

    fun getSizeEmoji(size: TaskSize): String {
        return when (size) {
            TaskSize.SMALL -> "S"
            TaskSize.MEDIUM -> "M"
            TaskSize.LARGE -> "L"
            TaskSize.VERY_LARGE -> "XL"
        }
    }


    @Composable
    fun PriorityIcon(priority: Priority) {
        val (icon, color) = when (priority) {
            Priority.LOW -> Icons.Filled.KeyboardArrowDown to TaskColor
            Priority.MEDIUM -> Icons.Filled.DragHandle to WarningColor
            Priority.HIGH -> Icons.Filled.KeyboardArrowUp to ErrorColor
            Priority.VERY_HIGH -> Icons.Filled.KeyboardDoubleArrowUp to ErrorColor
        }
        Icon(imageVector = icon, contentDescription = null, tint = color)
    }
}
