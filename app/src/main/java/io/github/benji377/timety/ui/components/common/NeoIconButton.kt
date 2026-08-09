package io.github.benji377.timety.ui.components.common

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
 * Bordered icon-only button, replacing a bare Material `IconButton` wherever an icon would
 * otherwise float with no container. Built on [OutlinedIconButton].
 *
 * Stays at [AppTheme.borderHairline] rather than the card weight: these appear several to a row in
 * top bars, where a heavier border would make the header outweigh the content.
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
        border = AppTheme.hairlineStroke,
        interactionSource = interactionSource,
    ) {
        Icon(icon, contentDescription = contentDescription)
    }
}
