package io.github.benji377.timety.ui.components.common

import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.benji377.timety.R
import io.github.benji377.timety.ui.theme.AppTheme

/**
 * Neobrutalist floating action button: the app's primary action, so it gets the heaviest border
 * in the border scale ([AppTheme.borderThick]), a full-size [neoPressShadow], and is tinted with
 * the owning section's accent color - per the reference sheet, the FAB must be the most visually
 * dominant thing on screen.
 */
@Composable
fun NeoFab(
    onClick: () -> Unit,
    containerColor: Color,
    icon: ImageVector = Icons.Filled.Add,
    contentDescription: String = stringResource(R.string.commonLabelAdd),
    shadowColor: Color = NeoShadowDefaults.color,
) {
    val interactionSource = remember { MutableInteractionSource() }
    FloatingActionButton(
        onClick = onClick,
        modifier = Modifier
            .neoPressShadow(interactionSource = interactionSource, color = shadowColor)
            .border(
                AppTheme.borderThick,
                MaterialTheme.colorScheme.outline,
                AppTheme.brNeo
            ),
        shape = AppTheme.brNeo,
        elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
        containerColor = containerColor,
        contentColor = Color.White,
        interactionSource = interactionSource,
    ) {
        Icon(icon, contentDescription)
    }
}

/**
 * Neobrutalist extended (icon + label) FAB: same dominant, thick-bordered, hard-shadowed
 * treatment as [NeoFab] for actions that need a text label.
 */
@Composable
fun NeoExtendedFab(
    onClick: () -> Unit,
    text: String,
    containerColor: Color,
    icon: ImageVector = Icons.Filled.Add,
    shadowColor: Color = NeoShadowDefaults.color,
) {
    val interactionSource = remember { MutableInteractionSource() }
    ExtendedFloatingActionButton(
        onClick = onClick,
        modifier = Modifier
            .neoPressShadow(interactionSource = interactionSource, color = shadowColor)
            .border(
                AppTheme.borderThick,
                MaterialTheme.colorScheme.outline,
                AppTheme.brNeo
            ),
        shape = AppTheme.brNeo,
        elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
        containerColor = containerColor,
        contentColor = Color.White,
        icon = { Icon(icon, contentDescription = null) },
        text = { Text(text) },
        interactionSource = interactionSource,
    )
}
