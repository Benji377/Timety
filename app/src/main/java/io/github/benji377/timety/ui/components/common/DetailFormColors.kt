package io.github.benji377.timety.ui.components.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
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
 * - View mode ([isEditing] = false, the default): full-contrast value text (onSurface) so the
 *   screen stays readable, muted label/icons (onSurfaceVariant), and a recessed surfaceVariant
 *   container instead of the white one an editable field gets. The border stays solid rather than
 *   fading — per the reference sheet, "read-only does not mean soft-UI" — so the inactive state is
 *   carried by the fill and the missing shadow, not by a washed-out outline.
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
        // Bold outline border in both states - no thin/gray unfocused border, and no color shift
        // on focus - matching the reference sheet's "border is always bold, never soft" rule.
        focusedBorderColor = MaterialTheme.colorScheme.outline,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        errorBorderColor = MaterialTheme.colorScheme.error,
        // A recessed paper-alt fill rather than the white of an editable field, so a read-only form
        // is legible as read-only at a glance. It must stay opaque either way: the field's hard
        // shadow is painted behind it, and a transparent container would show that shadow through
        // and turn the field into a solid black block.
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
