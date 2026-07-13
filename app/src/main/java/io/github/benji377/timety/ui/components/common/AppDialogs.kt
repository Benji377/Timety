package io.github.benji377.timety.ui.components.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.benji377.timety.R
import io.github.benji377.timety.ui.theme.AppTheme
import io.github.benji377.timety.ui.components.common.TimetyButton as Button
import io.github.benji377.timety.ui.components.common.TimetyOutlinedTextField as OutlinedTextField


/** Alert dialog with cancel/confirm actions; renders nothing while [visible] is false. */
@Composable
fun ConfirmationDialog(
    visible: Boolean,
    title: String,
    content: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmLabel: String? = null,
    confirmColor: Color? = null,
) {
    if (!visible) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(content) },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.commonLabelCancel)) }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = if (confirmColor == null) ButtonDefaults.buttonColors()
                else ButtonDefaults.buttonColors(
                    containerColor = confirmColor,
                    contentColor = Color.White
                ),
            ) {
                Text(confirmLabel ?: stringResource(R.string.commonLabelConfirm))
            }
        },
    )
}


/** Alert dialog with a single text field, confirming only if the trimmed input is non-empty. */
@Composable
fun TextInputDialog(
    visible: Boolean,
    title: String,
    labelText: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    initialValue: String = "",
    confirmLabel: String? = null,
    hintText: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    if (!visible) return
    var text by remember(visible) { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(labelText) },
                placeholder = hintText?.let { { Text(it) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.commonLabelCancel)) }
        },
        confirmButton = {
            TextButton(onClick = {
                val trimmed = text.trim()
                if (trimmed.isNotEmpty()) onConfirm(trimmed)
            }) {
                Text(confirmLabel ?: stringResource(R.string.commonLabelSave))
            }
        },
    )
}


/**
 * Dialog for creating or editing a named, colored item (focus tag, task category): a name
 * field plus a [ColorSwatchGrid]. Confirms only if the trimmed name is non-empty.
 */
@Composable
fun NamedColorEditDialog(
    title: String,
    nameLabel: String,
    colorLabel: String,
    confirmLabel: String,
    initialName: String,
    initialColor: Color,
    colors: List<Color>,
    onConfirm: (name: String, colorValue: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var selectedColor by remember { mutableStateOf(initialColor) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(nameLabel) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(AppTheme.spaceLarge))
                Text(colorLabel)
                Spacer(modifier = Modifier.height(AppTheme.spaceSmall))
                ColorSwatchGrid(
                    colors = colors,
                    selectedColor = selectedColor,
                    onSelect = { selectedColor = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.commonLabelCancel)) }
        },
        confirmButton = {
            Button(onClick = {
                val trimmed = name.trim()
                if (trimmed.isNotEmpty()) {
                    onConfirm(trimmed, selectedColor.toArgb())
                }
            }) {
                Text(confirmLabel)
            }
        },
    )
}
