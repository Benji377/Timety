package io.github.benji377.timety.ui.components.task

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.benji377.timety.R
import io.github.benji377.timety.ui.components.common.NeoButton as Button
import io.github.benji377.timety.ui.components.common.NeoFilterChip
import io.github.benji377.timety.ui.components.common.NeoOutlinedTextField as OutlinedTextField
import io.github.benji377.timety.ui.components.common.detailFieldColors
import io.github.benji377.timety.ui.theme.AppTheme
import io.github.benji377.timety.ui.theme.WarningColor
import io.github.benji377.timety.util.task.ReminderPreset


/**
 * A reminder as [ReminderField] shows it. [key] identifies it to the caller, [preset] is set when
 * it matches a [ReminderPreset], and [time] is the clock time shown beside the label, if any.
 */
data class ReminderEntry(
    val key: Long,
    val preset: ReminderPreset?,
    val label: String,
    val time: String? = null,
)

/**
 * Reminder editor shared by the task detail screens: a summary field that opens a bottom sheet of
 * one-tap preset chips plus any custom reminders. Tapping a selected chip removes it. Pass a null
 * [onAddCustom] when the screen only supports presets.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReminderField(
    entries: List<ReminderEntry>,
    isEditing: Boolean,
    sheetSubtitle: String,
    warning: String?,
    canAddPreset: (ReminderPreset) -> Boolean,
    onTogglePreset: (ReminderPreset) -> Unit,
    onRemoveCustom: (ReminderEntry) -> Unit,
    onAddCustom: (() -> Unit)?,
    customEnabled: Boolean = true,
) {
    if (!isEditing && entries.isEmpty()) return
    var showSheet by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isEditing) { showSheet = true }
    ) {
        OutlinedTextField(
            value = when (entries.size) {
                0 -> ""
                1 -> entries.first().label
                else -> pluralStringResource(
                    R.plurals.reminderFieldCount, entries.size, entries.size
                )
            },
            onValueChange = {},
            enabled = false,
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text(
                    stringResource(
                        if (entries.isEmpty()) R.string.reminderFieldAdd
                        else R.string.reminderFieldLabel
                    )
                )
            },
            leadingIcon = { Icon(Icons.Filled.Notifications, null) },
            trailingIcon = { if (isEditing) Icon(Icons.Filled.Edit, null) },
            colors = detailFieldColors(isEditing),
        )
    }

    if (entries.isNotEmpty()) {
        Spacer(Modifier.height(AppTheme.spaceSmall))
        Column(
            modifier = Modifier.padding(horizontal = AppTheme.spaceMedium),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spaceXSmall),
        ) {
            entries.forEach { entry ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(entry.label, fontSize = AppTheme.fsBodySmall)
                    entry.time?.let {
                        Text(
                            it,
                            fontSize = AppTheme.fsBodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }

    if (showSheet && isEditing) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = AppTheme.spaceLarge,
                        end = AppTheme.spaceLarge,
                        bottom = AppTheme.spaceXLarge,
                    ),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spaceMedium),
            ) {
                Column {
                    Text(
                        stringResource(R.string.reminderFieldLabel),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        sheetSubtitle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = AppTheme.fsBodySmall,
                    )
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(AppTheme.spaceSmall),
                    verticalArrangement = Arrangement.spacedBy(AppTheme.spaceSmall),
                ) {
                    ReminderPreset.entries.forEach { preset ->
                        val selected = entries.any { it.preset == preset }
                        val enabled = selected || canAddPreset(preset)
                        NeoFilterChip(
                            selected = selected,
                            onClick = { onTogglePreset(preset) },
                            label = reminderPresetLabel(preset),
                            enabled = enabled,
                            modifier = Modifier.alpha(if (enabled) 1f else 0.4f),
                        )
                    }
                    entries.filter { it.preset == null }.forEach { entry ->
                        NeoFilterChip(
                            selected = true,
                            onClick = { onRemoveCustom(entry) },
                            label = entry.label,
                        )
                    }
                    if (onAddCustom != null) {
                        NeoFilterChip(
                            selected = false,
                            onClick = onAddCustom,
                            label = stringResource(R.string.taskDetailsReminderOptionCustom),
                            enabled = customEnabled,
                            modifier = Modifier.alpha(if (customEnabled) 1f else 0.4f),
                            leadingIcon = {
                                Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp))
                            },
                        )
                    }
                }
                Text(
                    text = warning ?: stringResource(R.string.reminderSheetHint),
                    color = if (warning != null) WarningColor
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = AppTheme.fsBodySmall,
                )
                Button(onClick = { showSheet = false }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.commonLabelClose))
                }
            }
        }
    }
}


/** The user-facing label for a reminder preset. */
@Composable
@ReadOnlyComposable
fun reminderPresetLabel(preset: ReminderPreset): String = when (preset) {
    ReminderPreset.ON_TIME -> stringResource(R.string.taskDetailsReminderOptionOnce)
    ReminderPreset.MINUTES_30 -> stringResource(R.string.taskDetailsReminderOptionHalfHour)
    ReminderPreset.HOUR_1 -> stringResource(R.string.taskDetailsReminderOptionHour)
    ReminderPreset.DAY_1 -> stringResource(R.string.taskDetailsReminderOptionDay)
}
