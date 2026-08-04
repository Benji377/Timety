package io.github.benji377.timety.ui.components.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import io.github.benji377.timety.ui.theme.AppTheme

/**
 * Flat neobrutalist replacement for Material3's `FilterChip`: a pill-shaped, bold-bordered toggle
 * chip built from scratch (not a wrapper) so the selected/unselected look is fully controlled.
 *
 * When [enabled] is false the chip becomes non-clickable, but a selected chip keeps its full-
 * contrast [selectedColor] fill instead of fading to Material's disabled alpha — matching the
 * detail screens' convention (see [detailFilterChipColors]) that a disabled-but-selected chip
 * must stay clearly readable in view mode.
 */
@Composable
fun NeoFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selectedColor: Color = MaterialTheme.colorScheme.primary,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    val shape: Shape = RoundedCornerShape(percent = 50)
    val containerColor = if (selected) selectedColor else Color.Transparent
    val contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = modifier
            .clip(shape)
            .background(containerColor, shape)
            .border(
                BorderStroke(AppTheme.listTileBorderWidth, MaterialTheme.colorScheme.outline),
                shape
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = AppTheme.spaceMedium, vertical = AppTheme.spaceSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) {
            leadingIcon()
            Spacer(Modifier.width(AppTheme.spaceXSmall))
        }
        Text(
            text = label,
            color = contentColor,
            fontWeight = if (selected) AppTheme.fwBold else AppTheme.fwMedium,
        )
    }
}
