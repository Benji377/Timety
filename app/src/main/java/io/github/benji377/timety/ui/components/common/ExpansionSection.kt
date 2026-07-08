package io.github.benji377.timety.ui.components.common

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import io.github.benji377.timety.ui.theme.AppTheme


/** Collapsible section with a colored, icon-led header, built on [StyledExpansionTile]. */
@Composable
fun ExpansionSection(
    title: String,
    color: Color,
    initiallyExpanded: Boolean = true,
    icon: ImageVector = Icons.Filled.Circle,
    content: @Composable () -> Unit,
) {
    StyledExpansionTile(
        title = {
            ListSectionHeader(
                title = title,
                icon = icon,
                color = color,
                padding = PaddingValues(0.dp),
                iconSize = AppTheme.listSectionIconSize,
            )
        },
        iconColor = color,
        initiallyExpanded = initiallyExpanded,
        content = content,
    )
}
