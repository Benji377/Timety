package io.github.benji377.timety.ui.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardDoubleArrowUp
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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


    /** Small color swatch identifying a task category (filter pills, category picker). */
    @Composable
    fun CategoryDot(colorValue: Int, size: Dp = 10.dp) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(Color(colorValue))
        )
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
