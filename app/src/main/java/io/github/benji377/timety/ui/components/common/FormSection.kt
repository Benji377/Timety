package io.github.benji377.timety.ui.components.common

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.benji377.timety.ui.theme.AppTheme

/**
 * Uppercase, icon-led title that opens a group of fields on a detail screen, tinted with the item
 * type's [color]. Its top padding also separates it from the previous group. An [action] sits at
 * the end of the divider.
 */
@Composable
fun FormSectionHeader(
    title: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = AppTheme.spaceXLarge, bottom = AppTheme.spaceMedium),
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(AppTheme.iconSizeSmall))
        Spacer(Modifier.width(AppTheme.spaceSmall))
        Text(
            text = title.uppercase(),
            fontSize = AppTheme.fsBodySmall,
            fontWeight = AppTheme.fwBold,
            letterSpacing = AppTheme.lsWide,
            color = color,
        )
        Spacer(Modifier.width(AppTheme.spaceSmall))
        HorizontalDivider(modifier = Modifier.weight(1f))
        if (action != null) {
            Spacer(Modifier.width(AppTheme.spaceSmall))
            action()
        }
    }
}

/** Bold label above a control that has none of its own, such as a segmented selector or chips. */
@Composable
fun FieldLabel(text: String, modifier: Modifier = Modifier) {
    Text(text, fontWeight = AppTheme.fwBold, modifier = modifier.padding(bottom = AppTheme.spaceSmall))
}
