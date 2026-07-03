package io.github.benji377.timety.data.model.focus

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class TargetTypeStat(
    val type: FocusTargetType,
    val label: String,
    val color: Color,
    val icon: ImageVector,
    val minutes: Int
)
