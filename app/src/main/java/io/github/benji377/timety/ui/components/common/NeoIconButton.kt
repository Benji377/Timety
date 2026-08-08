package io.github.benji377.timety.ui.components.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import io.github.benji377.timety.ui.theme.AppTheme

/**
 * Bordered icon-only button - the neobrutalist replacement for a bare Material `IconButton`, which
 * the reference sheet flags app-wide (history icon, warning icon, settings gear, share icon, ...)
 * as "floating with no container weight." Built on [OutlinedIconButton] so the border and content
 * clipping come from the same M3 machinery.
 *
 * Deliberately flat, with a thin [AppTheme.borderThin] outline: these are 40dp targets that appear
 * several to a row in top bars, and giving each one a card's border weight and hard shadow made the
 * header heavier than the content underneath it. The container weight the reference sheet asks for
 * comes from the outline and the fill alone.
 *
 * @param size the button's fixed square footprint.
 */
@Composable
fun NeoIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    shape: Shape = AppTheme.brNeo,
    size: Dp = AppTheme.neoIconButtonSize,
) {
    val interactionSource = remember { MutableInteractionSource() }
    OutlinedIconButton(
        onClick = onClick,
        modifier = modifier.size(size),
        enabled = enabled,
        shape = shape,
        colors = IconButtonDefaults.outlinedIconButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor,
            disabledContentColor = contentColor.copy(alpha = AppTheme.OPACITY_MEDIUM),
        ),
        border = BorderStroke(AppTheme.borderThin, MaterialTheme.colorScheme.outline),
        interactionSource = interactionSource,
    ) {
        Icon(icon, contentDescription = contentDescription)
    }
}
