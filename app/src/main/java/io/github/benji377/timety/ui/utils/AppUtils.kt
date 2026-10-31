package io.github.benji377.timety.ui.utils

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardDoubleArrowUp
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import io.github.benji377.timety.R
import io.github.benji377.timety.data.model.task.Priority
import io.github.benji377.timety.data.model.task.TaskSize
import io.github.benji377.timety.ui.theme.ErrorColor
import io.github.benji377.timety.ui.theme.TaskColor
import io.github.benji377.timety.ui.theme.WarningColor

/** Small shared UI helpers: size/priority glyphs and labels. */
object AppUtils {
    @StringRes
    fun priorityLabel(priority: Priority): Int = when (priority) {
        Priority.LOW -> R.string.taskPriorityLow
        Priority.MEDIUM -> R.string.taskPriorityMedium
        Priority.HIGH -> R.string.taskPriorityHigh
        Priority.VERY_HIGH -> R.string.taskPriorityVeryHigh
    }

    @StringRes
    fun sizeLabel(size: TaskSize): Int = when (size) {
        TaskSize.SMALL -> R.string.taskSizeSmall
        TaskSize.MEDIUM -> R.string.taskSizeMedium
        TaskSize.LARGE -> R.string.taskSizeLarge
        TaskSize.VERY_LARGE -> R.string.taskSizeVeryLarge
    }

    fun getSizeEmoji(size: TaskSize): String {
        return when (size) {
            TaskSize.SMALL -> "S"
            TaskSize.MEDIUM -> "M"
            TaskSize.LARGE -> "L"
            TaskSize.VERY_LARGE -> "XL"
        }
    }

    /**
     * Icon indicating a task's priority level, colored by severity by default. Pass [tint] to
     * override that color - e.g. a selected segment on a solid severity-colored fill needs a
     * single readable content color instead of clashing with its own background.
     */
    @Composable
    fun PriorityIcon(priority: Priority, tint: Color? = null) {
        val (icon, severityColor) = when (priority) {
            Priority.LOW -> Icons.Filled.KeyboardArrowDown to TaskColor
            Priority.MEDIUM -> Icons.Filled.DragHandle to WarningColor
            Priority.HIGH -> Icons.Filled.KeyboardArrowUp to ErrorColor
            Priority.VERY_HIGH -> Icons.Filled.KeyboardDoubleArrowUp to ErrorColor
        }
        Icon(imageVector = icon, contentDescription = null, tint = tint ?: severityColor)
    }
}
