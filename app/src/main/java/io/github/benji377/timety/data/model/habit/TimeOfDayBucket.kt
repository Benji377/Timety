package io.github.benji377.timety.data.model.habit

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class TimeOfDayBucket(
    val label: String,
    val subtitle: String,
    val count: Int,
    val color: Color,
    val icon: ImageVector
)
