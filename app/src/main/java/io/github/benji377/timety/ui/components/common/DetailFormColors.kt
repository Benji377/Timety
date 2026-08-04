package io.github.benji377.timety.ui.components.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButtonColors
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * App-wide palettes for detail-screen form controls, standardizing the view/edit distinction:
 * in view mode the *value* stays at full contrast (view mode is the primary reading surface),
 * while the *chrome* signals "not editable" — muted labels/icons, soft borders, transparent
 * containers.
 */

/**
 * Colors for [NeoOutlinedTextField]s on the detail screens.
 *
 * - View mode ([isEditing] = false, the default): full-contrast value text (onSurface), muted
 *   label/icons (onSurfaceVariant), a soft outlineVariant border, and a transparent container.
 * - Edit mode ([isEditing] = true): for fields that stay `enabled = false` while editing because
 *   they open a picker on tap (due dates, reminder times) — styled to look like a normal active
 *   field: surface container, outline border, full-contrast text.
 *
 * Plain fields that use `enabled = isEditing` get the view-mode branch automatically via
 * [NeoOutlinedTextField]'s default colors; only tap-to-pick fields need to pass
 * `detailFieldColors(isEditing)` explicitly.
 */
@Composable
fun detailFieldColors(isEditing: Boolean = false): TextFieldColors = if (isEditing) {
    OutlinedTextFieldDefaults.colors(
        disabledContainerColor = MaterialTheme.colorScheme.surface,
        disabledTextColor = MaterialTheme.colorScheme.onSurface,
        disabledBorderColor = MaterialTheme.colorScheme.outline,
        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
} else {
    OutlinedTextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.surface,
        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
        errorContainerColor = MaterialTheme.colorScheme.surface,
        // Disabled fields blend into the screen instead of looking like paper.
        disabledContainerColor = Color.Transparent,
        disabledTextColor = MaterialTheme.colorScheme.onSurface,
        disabledBorderColor = MaterialTheme.colorScheme.outlineVariant,
        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledSupportingTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/**
 * Colors for the detail screens' [androidx.compose.material3.SegmentedButton] rows, disabled
 * while viewing: the active segment keeps a visible container and full-contrast label instead
 * of Material's disabled fade, so the selection reads clearly in view mode.
 */
@Composable
fun detailSegmentedButtonColors(): SegmentedButtonColors = SegmentedButtonDefaults.colors(
    disabledActiveContainerColor = MaterialTheme.colorScheme.secondaryContainer,
    disabledActiveContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    disabledInactiveContainerColor = Color.Transparent,
    disabledInactiveContentColor = MaterialTheme.colorScheme.onSurface,
    disabledActiveBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
    disabledInactiveBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
)
