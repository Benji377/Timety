package io.github.benji377.timety.ui.components.common

import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.benji377.timety.R
import io.github.benji377.timety.ui.theme.AppTheme

/**
 * Neobrutalist floating action button: flat, outlined, tinted with the
 * owning section's accent color.
 */
@Composable
fun TimetyFab(
    onClick: () -> Unit,
    containerColor: Color,
    icon: ImageVector = Icons.Filled.Add,
    contentDescription: String = stringResource(R.string.commonLabelAdd),
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = Modifier.border(
            AppTheme.neoBorderWidth,
            MaterialTheme.colorScheme.outline,
            AppTheme.brNeo
        ),
        shape = AppTheme.brNeo,
        elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
        containerColor = containerColor,
        contentColor = Color.White
    ) {
        Icon(icon, contentDescription)
    }
}
