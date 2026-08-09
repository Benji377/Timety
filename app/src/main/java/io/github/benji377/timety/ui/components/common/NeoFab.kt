package io.github.benji377.timety.ui.components.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import io.github.benji377.timety.R
import io.github.benji377.timety.ui.theme.AppTheme
import io.github.benji377.timety.ui.theme.hairlineBorder

/**
 * Primary-action floating button, flat and tinted with the owning section's accent color. With no
 * elevation, the saturated [containerColor] is what makes it the most prominent thing on screen.
 */
@Composable
fun NeoFab(
    onClick: () -> Unit,
    containerColor: Color,
    icon: ImageVector = Icons.Filled.Add,
    contentDescription: String = stringResource(R.string.commonLabelAdd),
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = Modifier.hairlineBorder(AppTheme.brNeo),
        shape = AppTheme.brNeo,
        elevation = AppTheme.flatFabElevation,
        containerColor = containerColor,
        contentColor = Color.White,
    ) {
        Icon(icon, contentDescription)
    }
}

/** Extended (icon + label) FAB with the same treatment as [NeoFab]. */
@Composable
fun NeoExtendedFab(
    onClick: () -> Unit,
    text: String,
    containerColor: Color,
    icon: ImageVector = Icons.Filled.Add,
) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        modifier = Modifier.hairlineBorder(AppTheme.brNeo),
        shape = AppTheme.brNeo,
        elevation = AppTheme.flatFabElevation,
        containerColor = containerColor,
        contentColor = Color.White,
        icon = { Icon(icon, contentDescription = null) },
        text = { Text(text) },
    )
}
