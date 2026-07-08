package io.github.benji377.timety.ui.components.common

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import io.github.benji377.timety.ui.theme.AppTheme


/** Row of a tinted icon followed by a bold title, used to head a list or section. */
@Composable
fun ListSectionHeader(
    title: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(
        horizontal = AppTheme.spaceLarge,
        vertical = AppTheme.spaceSmall,
    ),
    iconSize: Dp = AppTheme.iconSizeSmall,
    titleSize: TextUnit = AppTheme.fsBodyLarge,
) {
    Row(
        modifier = modifier.padding(padding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(iconSize))
        Spacer(Modifier.width(AppTheme.spaceSmall))
        Text(
            title,
            fontSize = titleSize,
            fontWeight = AppTheme.fwBold,
            color = color,
        )
    }
}
