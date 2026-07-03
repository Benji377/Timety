package io.github.benji377.timety.ui.components.stats

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * @deprecated Use StatCard with style: StatCardStyle.COMPACT_VERTICAL instead
 */
@Deprecated("Use StatCard with style = StatCardStyle.COMPACT_VERTICAL instead")
@Composable
fun CompactVerticalStatCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    StatCard(
        title = title,
        value = value,
        icon = icon,
        color = color,
        modifier = modifier,
        style = StatCardStyle.COMPACT_VERTICAL
    )
}
