package io.github.benji377.timety.ui.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.benji377.timety.R
import io.github.benji377.timety.ui.theme.AppTheme
import io.github.benji377.timety.ui.theme.hairlineBorder
import io.github.benji377.timety.util.habit.HabitIcons

/**
 * Read-only outlined field that opens a picker on tap, showing the current selection
 * (an icon, a color swatch, …) as its leading [content]. Used on the detail screens'
 * appearance rows.
 */
@Composable
fun PickerField(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier) {
        // The field itself follows [enabled] so it picks up the normal enabled/disabled
        // styling (surface vs. transparent background); the overlay box catches taps.
        NeoOutlinedTextField(
            value = "",
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(label) },
            // [content] is a 40dp swatch or icon circle, far wider than the 24dp glyph M3 spaces
            // its leading icon for, so without this the label sits flush against it.
            leadingIcon = {
                Box(
                    modifier = Modifier.padding(start = AppTheme.spaceSmall, end = AppTheme.spaceSmall),
                    contentAlignment = Alignment.Center,
                ) { content() }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        // The tap target lies over the read-only field, so it carries the field's label itself.
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(enabled = enabled, role = Role.Button) { onClick() }
                .semantics { contentDescription = label },
        )
    }
}

/**
 * The icon and color fields side by side, as used by habits and goals: each shows its current
 * value in a bordered 40dp circle and opens its picker on tap.
 */
@Composable
fun AppearanceFields(
    iconIndex: Int?,
    color: Color,
    colorLabel: String,
    enabled: Boolean,
    onPickIcon: () -> Unit,
    onPickColor: () -> Unit,
    showIcon: Boolean = true,
    showColor: Boolean = true,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spaceLarge),
    ) {
        if (showIcon) PickerField(
            label = stringResource(R.string.detailLabelIcon),
            enabled = enabled,
            onClick = onPickIcon,
            modifier = Modifier.weight(1f),
        ) {
            Box(
                modifier = Modifier
                    .size(AppTheme.neoIconButtonSize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .hairlineBorder(),
                contentAlignment = Alignment.Center,
            ) {
                Icon(imageVector = HabitIcons.iconAt(iconIndex), contentDescription = null, tint = color)
            }
        }
        if (showColor) PickerField(
            label = colorLabel,
            enabled = enabled,
            onClick = onPickColor,
            modifier = Modifier.weight(1f),
        ) {
            Box(
                modifier = Modifier
                    .size(AppTheme.neoIconButtonSize)
                    .clip(CircleShape)
                    .background(color)
                    .hairlineBorder()
            )
        }
    }
}

/**
 * Dialog with a grid of the [HabitIcons] catalog; selecting an icon confirms and closes.
 * The current selection is highlighted with [accentColor].
 */
@Composable
fun IconPickerDialog(
    title: String,
    selectedIconIndex: Int?,
    accentColor: Color,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    NeoAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spaceLarge),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spaceLarge),
                modifier = Modifier.height(300.dp),
            ) {
                items(HabitIcons.availableIcons.size) { index ->
                    val isSelected = index == selectedIconIndex
                    val interactionSource = remember { MutableInteractionSource() }
                    // Selected state is a solid fill + border, not an alpha-blended tint - the
                    // same selected/unselected fill switch used by NeoFilterChip, so the picker
                    // reads consistently with the rest of the app's selector controls.
                    Box(
                        modifier = Modifier
                            .size(AppTheme.neoIconButtonSize)
                            .clip(CircleShape)
                            .background(if (isSelected) accentColor else Color.Transparent)
                            .hairlineBorder()
                            .selectable(
                                selected = isSelected,
                                interactionSource = interactionSource,
                                indication = ripple(),
                                role = Role.RadioButton,
                            ) {
                                onSelect(index)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = HabitIcons.availableIcons[index],
                            contentDescription = null,
                            tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        },
        confirmButton = {
            NeoTextButton(onClick = onDismiss) { Text(stringResource(R.string.commonLabelCancel)) }
        },
    )
}

/** Dialog with a [ColorSwatchGrid] of [colors]; selecting a swatch confirms and closes. */
@Composable
fun ColorPickerDialog(
    title: String,
    colors: List<Color>,
    selectedColor: Color,
    onSelect: (Color) -> Unit,
    onDismiss: () -> Unit,
) {
    NeoAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            ColorSwatchGrid(
                colors = colors,
                selectedColor = selectedColor,
                onSelect = onSelect,
                modifier = Modifier.height(220.dp),
                columns = 4,
                swatchSize = 40.dp,
                spacing = AppTheme.spaceLarge,
            )
        },
        confirmButton = {
            NeoTextButton(onClick = onDismiss) { Text(stringResource(R.string.commonLabelCancel)) }
        },
    )
}
