package io.github.benji377.timety.ui.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.benji377.timety.R
import io.github.benji377.timety.ui.theme.AppTheme
import io.github.benji377.timety.util.habit.HabitIcons
import io.github.benji377.timety.ui.components.common.TimetyOutlinedTextField as OutlinedTextField


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
        OutlinedTextField(
            value = "",
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(label) },
            leadingIcon = { Box(contentAlignment = Alignment.Center) { content() } },
            modifier = Modifier.fillMaxWidth(),
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(enabled = enabled) { onClick() },
        )
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
    AlertDialog(
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
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = if (isSelected) accentColor.copy(alpha = 0.2f) else Color.Transparent,
                                shape = CircleShape,
                            )
                            .clickable { onSelect(index) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = HabitIcons.availableIcons[index],
                            contentDescription = null,
                            tint = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.commonLabelCancel)) }
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
    AlertDialog(
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
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.commonLabelCancel)) }
        },
    )
}
