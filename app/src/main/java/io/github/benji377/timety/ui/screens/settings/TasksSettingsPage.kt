package io.github.benji377.timety.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.benji377.timety.R
import io.github.benji377.timety.ui.components.common.NeoAlertDialog
import io.github.benji377.timety.ui.components.common.NeoButton
import io.github.benji377.timety.ui.components.common.NeoSegmentedSelector
import io.github.benji377.timety.ui.components.common.NeoSlider
import io.github.benji377.timety.ui.components.common.NeoTextButton
import io.github.benji377.timety.ui.theme.AppTheme
import io.github.benji377.timety.ui.theme.TaskColor
import io.github.benji377.timety.ui.viewmodel.SettingsViewModel
import io.github.benji377.timety.ui.viewmodel.activityScopedViewModel

/** Task settings: the Home upcoming-task window and recurring auto-skip. */
@Composable
internal fun TasksSettings(
    settingsViewModel: SettingsViewModel = activityScopedViewModel(),
) {
    val upcomingTasksHorizon by settingsViewModel.upcomingTasksHorizon.collectAsState()
    val autoSkipHours by settingsViewModel.recurringAutoSkipHours.collectAsState()

    val upcomingTaskTitle = stringResource(R.string.settingsLabelUpcomingTask)
    val daysUnit = stringResource(R.string.settingsDialogUnitDays)
    var numberDialogSpec by remember { mutableStateOf<NumberDialogSpec?>(null) }
    var showAutoSkipDialog by rememberSaveable { mutableStateOf(false) }

    numberDialogSpec?.let { spec ->
        NumberPickerDialog(spec = spec, onDismiss = { numberDialogSpec = null })
    }
    if (showAutoSkipDialog) {
        AutoSkipDialog(
            currentHours = autoSkipHours,
            onSave = { settingsViewModel.setRecurringAutoSkipHours(it) },
            onDismiss = { showAutoSkipDialog = false },
        )
    }

    Spacer(modifier = Modifier.height(8.dp))
    NavSettingsItem(
        title = upcomingTaskTitle,
        subtitle = stringResource(R.string.settingsLabelUpcomingTaskSubtitle, upcomingTasksHorizon),
        icon = Icons.Outlined.Schedule,
        iconTint = TaskColor,
        onClick = {
            numberDialogSpec = NumberDialogSpec(
                title = upcomingTaskTitle,
                current = upcomingTasksHorizon, min = 1, max = 60, unit = daysUnit,
                onSave = { settingsViewModel.setUpcomingTasksHorizon(it) }
            )
        }
    )
    NavSettingsItem(
        title = stringResource(R.string.settingsLabelAutoSkip),
        subtitle = if (autoSkipHours == 0) {
            stringResource(R.string.settingsAutoSkipOff)
        } else {
            stringResource(R.string.settingsLabelAutoSkipSubtitle, autoSkipLabel(autoSkipHours))
        },
        icon = Icons.AutoMirrored.Outlined.Redo,
        iconTint = TaskColor,
        onClick = { showAutoSkipDialog = true },
    )
}

/** The unit the auto-skip window is picked in; stored as hours either way. */
private enum class AutoSkipUnit(val maxAmount: Int, val hoursPerUnit: Int) {
    HOURS(23, 1),
    DAYS(14, 24),
}

/** "6 hours" or "1 day"; whole days read as days. */
@Composable
@ReadOnlyComposable
private fun autoSkipLabel(hours: Int): String =
    if (hours % AutoSkipUnit.DAYS.hoursPerUnit == 0) {
        val days = hours / AutoSkipUnit.DAYS.hoursPerUnit
        pluralStringResource(R.plurals.settingsAutoSkipDays, days, days)
    } else {
        pluralStringResource(R.plurals.settingsAutoSkipHours, hours, hours)
    }

/** Picks the auto-skip window as an amount of hours or days; 0 turns it off. */
@Composable
private fun AutoSkipDialog(currentHours: Int, onSave: (Int) -> Unit, onDismiss: () -> Unit) {
    val startsInDays = currentHours % AutoSkipUnit.DAYS.hoursPerUnit == 0
    var unit by remember { mutableStateOf(if (startsInDays) AutoSkipUnit.DAYS else AutoSkipUnit.HOURS) }
    var amount by remember { mutableIntStateOf(currentHours / unit.hoursPerUnit) }
    val hours = amount * unit.hoursPerUnit
    val unitLabels = mapOf(
        AutoSkipUnit.HOURS to stringResource(R.string.recurringTaskWindowHours),
        AutoSkipUnit.DAYS to stringResource(R.string.recurringTaskWindowDays),
    )
    NeoAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settingsLabelAutoSkip)) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    stringResource(R.string.settingsAutoSkipHint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(AppTheme.spaceMedium))
                NeoSegmentedSelector(
                    values = AutoSkipUnit.entries,
                    selectedValue = unit,
                    isEditing = true,
                    onSelected = { picked ->
                        unit = picked
                        amount = amount.coerceAtMost(picked.maxAmount)
                    },
                    labelBuilder = { unitLabels.getValue(it) },
                    activeBgColor = TaskColor,
                )
                Spacer(Modifier.height(AppTheme.spaceMedium))
                Text(
                    if (hours == 0) stringResource(R.string.settingsAutoSkipOff) else autoSkipLabel(hours),
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                )
                NeoSlider(
                    value = amount,
                    onValueChange = { amount = it },
                    range = 0..unit.maxAmount,
                )
            }
        },
        confirmButton = {
            NeoButton(onClick = {
                onSave(hours)
                onDismiss()
            }) { Text(stringResource(R.string.commonLabelSave)) }
        },
        dismissButton = {
            NeoTextButton(onClick = onDismiss) { Text(stringResource(R.string.commonLabelCancel)) }
        },
    )
}
