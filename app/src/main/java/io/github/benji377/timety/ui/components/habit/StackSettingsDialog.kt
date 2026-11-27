package io.github.benji377.timety.ui.components.habit

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import io.github.benji377.timety.R
import io.github.benji377.timety.data.repository.StackRenameResult
import io.github.benji377.timety.ui.components.common.NeoAlertDialog
import io.github.benji377.timety.ui.components.common.NeoButton
import io.github.benji377.timety.ui.components.common.NeoOutlinedTextField
import io.github.benji377.timety.ui.components.common.NeoTextButton
import io.github.benji377.timety.ui.theme.AppTheme
import io.github.benji377.timety.ui.theme.SuccessColor

/**
 * Pop-up for one stack's settings: its name and whether its habits are done in order. [onSave]
 * reports the result back through its callback, so a taken or empty name keeps the pop-up open
 * with an error on the field.
 */
@Composable
fun StackSettingsDialog(
    stackName: String,
    inOrder: Boolean,
    onSave: (name: String, inOrder: Boolean, onResult: (StackRenameResult) -> Unit) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(stackName) }
    var ordered by rememberSaveable { mutableStateOf(inOrder) }
    var error by rememberSaveable { mutableStateOf<StackRenameResult?>(null) }
    val errorText = when (error) {
        StackRenameResult.NAME_EMPTY -> stringResource(R.string.habitStackNameEmpty)
        StackRenameResult.NAME_TAKEN -> stringResource(R.string.habitStackNameTaken)
        StackRenameResult.SAVED, null -> null
    }

    NeoAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.habitStackEdit)) },
        text = {
            Column {
                NeoOutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        error = null
                    },
                    label = { Text(stringResource(R.string.habitStackName)) },
                    singleLine = true,
                    isError = errorText != null,
                    errorText = errorText,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(AppTheme.spaceMedium))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(value = ordered, role = Role.Switch, onValueChange = { ordered = it }),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.habitStackInOrder), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            stringResource(R.string.habitStackInOrderHint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(AppTheme.spaceMedium))
                    Switch(
                        checked = ordered,
                        onCheckedChange = null,
                        colors = SwitchDefaults.colors(checkedTrackColor = SuccessColor),
                    )
                }
            }
        },
        confirmButton = {
            NeoButton(onClick = {
                onSave(name, ordered) { result ->
                    if (result == StackRenameResult.SAVED) onDismiss() else error = result
                }
            }) { Text(stringResource(R.string.commonLabelSave)) }
        },
        dismissButton = {
            NeoTextButton(onClick = onDismiss) { Text(stringResource(R.string.commonLabelCancel)) }
        },
    )
}
