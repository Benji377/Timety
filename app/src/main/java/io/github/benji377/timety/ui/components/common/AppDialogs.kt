package io.github.benji377.timety.ui.components.common

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import io.github.benji377.timety.R
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
