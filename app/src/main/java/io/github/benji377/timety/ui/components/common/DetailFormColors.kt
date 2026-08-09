package io.github.benji377.timety.ui.components.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable

/**
 * Colors for [NeoOutlinedTextField]s on the detail screens. Values keep full contrast in both
 * modes; only the chrome signals editability.
 *
 * - View mode ([isEditing] = false): muted label/icons and a recessed surfaceVariant container.
 * - Edit mode ([isEditing] = true): for fields that stay `enabled = false` while editing because
 *   they open a picker on tap (due dates, reminder times), styled like a normal active field.
 *
 * Fields using `enabled = isEditing` get the view-mode branch automatically; only tap-to-pick
 * fields need to pass `detailFieldColors(isEditing)` explicitly.
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
        // Same hairline outline in both states - no color shift on focus, so the field's fill is
        // the only thing that changes between resting and focused.
        focusedBorderColor = MaterialTheme.colorScheme.outline,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        errorBorderColor = MaterialTheme.colorScheme.error,
        // A recessed paper-alt fill rather than the white of an editable field, so a read-only form
        // is legible as read-only at a glance. It must stay opaque: with the borders reduced to
        // hairlines, this fill is the whole of the read-only cue.
        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        disabledTextColor = MaterialTheme.colorScheme.onSurface,
        disabledBorderColor = MaterialTheme.colorScheme.outline,
        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledSupportingTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
