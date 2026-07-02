package io.github.benji377.timety.ui.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import io.github.benji377.timety.data.model.task.Priority
import io.github.benji377.timety.data.model.task.TaskSize
import io.github.benji377.timety.ui.theme.ErrorColor
import io.github.benji377.timety.ui.theme.SuccessColor
import io.github.benji377.timety.ui.theme.TaskColor
import io.github.benji377.timety.ui.theme.WarningColor

object AppUtils {
    fun getSizeEmoji(size: TaskSize): String {
        return when (size) {
            TaskSize.SMALL -> "🌱"
            TaskSize.MEDIUM -> "🌿"
            TaskSize.LARGE -> "🌳"
            TaskSize.VERY_LARGE -> "🏔️"
        }
    }

    @Composable
    fun PriorityIcon(priority: Priority) {
        val (icon, color) = when (priority) {
            Priority.LOW -> Icons.Filled.ArrowDownward to SuccessColor
            Priority.MEDIUM -> Icons.Filled.Remove to TaskColor
            Priority.HIGH -> Icons.Filled.ArrowUpward to WarningColor
            Priority.VERY_HIGH -> Icons.Filled.PriorityHigh to ErrorColor
        }
        Icon(imageVector = icon, contentDescription = "Priority", tint = color)
    }
}
