package io.github.benji377.timety.ui.components.task

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.benji377.timety.R
import io.github.benji377.timety.data.model.task.ReminderOption
import io.github.benji377.timety.ui.theme.AppTheme
import io.github.benji377.timety.ui.components.common.TimetyButton as Button
import io.github.benji377.timety.ui.components.common.TimetyOutlinedTextField as OutlinedTextField


/**
 * Dropdown of [ReminderOption]s with an add button, used by the task detail screens to build
 * their reminder lists. Recurring tasks store relative offsets only, so they exclude
 * [ReminderOption.CUSTOM] via [includeCustom].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderOptionInput(
    selected: ReminderOption,
    onSelectedChange: (ReminderOption) -> Unit,
    onAdd: () -> Unit,
    addEnabled: Boolean = true,
    includeCustom: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.weight(1f)
        ) {
            OutlinedTextField(
                value = reminderOptionLabel(selected),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.taskDetailsLabelReminderSet)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                val options =
                    if (includeCustom) ReminderOption.entries
                    else ReminderOption.entries.filter { it != ReminderOption.CUSTOM }
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(reminderOptionLabel(option)) },
                        onClick = {
                            onSelectedChange(option)
                            expanded = false
                        }
                    )
                }
            }
        }
        Spacer(Modifier.width(AppTheme.spaceSmall))
        Button(
            onClick = onAdd,
            enabled = addEnabled
        ) { Text(stringResource(R.string.commonLabelAdd)) }
    }
}


/** The user-facing label for a reminder option. */
@Composable
fun reminderOptionLabel(option: ReminderOption): String = when (option) {
    ReminderOption.ON_TIME -> stringResource(R.string.taskDetailsReminderOptionOnce)
    ReminderOption.MINUTES_30_BEFORE -> stringResource(R.string.taskDetailsReminderOptionHalfHour)
    ReminderOption.HOUR_1_BEFORE -> stringResource(R.string.taskDetailsReminderOptionHour)
    ReminderOption.DAY_1_BEFORE -> stringResource(R.string.taskDetailsReminderOptionDay)
    ReminderOption.CUSTOM -> stringResource(R.string.taskDetailsReminderOptionCustom)
}
