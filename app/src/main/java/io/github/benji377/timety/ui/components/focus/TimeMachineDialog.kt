package io.github.benji377.timety.ui.components.focus

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.benji377.timety.R
import io.github.benji377.timety.data.model.focus.FocusModeEntity
import io.github.benji377.timety.data.model.focus.FocusTagEntity
import io.github.benji377.timety.ui.theme.AppTheme
import io.github.benji377.timety.ui.theme.ErrorColor
import io.github.benji377.timety.ui.theme.TaskColor
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * "Log a past session" dialog: pick a mode, an optional tag, and a start/end time, mirroring
 * `AppDialogs.showTimeMachineDialog` in `widgets/common/app_dialogs.dart`. This is focus-specific
 * (unlike the shared `ConfirmationDialog`/`TextInputDialog`), so it lives in this feature's
 * component package rather than the shared `AppDialogs`.
 *
 * Date/time formatting note: Flutter delegates to `SettingsProvider.getFormattedDateTime`. No
 * centralized Kotlin equivalent exists yet, so this uses `DateTimeFormatter` with the device
 * locale as a reasonable stand-in - the parent should centralize this later.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeMachineDialog(
    modes: List<FocusModeEntity>,
    tags: List<FocusTagEntity>,
    initialSelectedTagId: String?,
    onDismiss: () -> Unit,
    onLog: (mode: FocusModeEntity, startTime: Instant, endTime: Instant, tagId: String?) -> Unit,
) {
    var selectedMode by remember { mutableStateOf(modes.firstOrNull()) }
    var selectedTagId by remember { mutableStateOf(initialSelectedTagId) }
    var startDateTime by remember { mutableStateOf(Instant.now().minusSeconds(25L * 60)) }
    var endDateTime by remember { mutableStateOf(Instant.now()) }
    var errorText by remember { mutableStateOf<String?>(null) }

    var modeExpanded by remember { mutableStateOf(false) }
    var tagExpanded by remember { mutableStateOf(false) }

    // Date+time picker flow shared by both start/end fields (mirrors AppDatePickers.pickDateTime).
    var pickerTarget by remember { mutableStateOf<TimeMachineTarget?>(null) }
    var pickerStep by remember { mutableIntStateOf(0) } // 0 = none, 1 = date, 2 = time
    var pickedLocalDate by remember { mutableStateOf<LocalDate?>(null) }

    val endBeforeStartError = stringResource(R.string.dialogTimeMachineErrorEndBeforeStart)
    val zone = ZoneId.systemDefault()
    val locale = Locale.getDefault()
    val dateTimeFormatter = remember(locale) {
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).withLocale(locale).withZone(zone)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.History, contentDescription = null, tint = TaskColor)
                Spacer(modifier = Modifier.width(AppTheme.spaceSmall))
                Text(stringResource(R.string.dialogTimeMachineTitle))
            }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                ExposedDropdownMenuBox(expanded = modeExpanded, onExpandedChange = { modeExpanded = it }) {
                    OutlinedTextField(
                        value = selectedMode?.let { localizedFocusModeName(it) } ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.dialogTimeMachineMode)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modeExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                    )
                    ExposedDropdownMenu(expanded = modeExpanded, onDismissRequest = { modeExpanded = false }) {
                        modes.forEach { mode ->
                            DropdownMenuItem(
                                text = { Text(localizedFocusModeName(mode)) },
                                onClick = { selectedMode = mode; modeExpanded = false },
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(AppTheme.spaceLarge))

                val noTagLabel = stringResource(R.string.dialogTimeMachineNoTag)
                ExposedDropdownMenuBox(expanded = tagExpanded, onExpandedChange = { tagExpanded = it }) {
                    OutlinedTextField(
                        value = tags.firstOrNull { it.id == selectedTagId }?.name ?: noTagLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.dialogTimeMachineTag)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = tagExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                    )
                    ExposedDropdownMenu(expanded = tagExpanded, onDismissRequest = { tagExpanded = false }) {
                        DropdownMenuItem(text = { Text(noTagLabel) }, onClick = { selectedTagId = null; tagExpanded = false })
                        tags.forEach { tag ->
                            DropdownMenuItem(text = { Text(tag.name) }, onClick = { selectedTagId = tag.id; tagExpanded = false })
                        }
                    }
                }
                Spacer(modifier = Modifier.height(AppTheme.spaceLarge))

                TimeRow(
                    label = stringResource(R.string.dialogTimeMachineStartTime),
                    value = dateTimeFormatter.format(startDateTime),
                    onClick = {
                        pickerTarget = TimeMachineTarget.START
                        pickedLocalDate = startDateTime.atZone(zone).toLocalDate()
                        pickerStep = 1
                    },
                )
                Spacer(modifier = Modifier.height(AppTheme.spaceSmall))
                TimeRow(
                    label = stringResource(R.string.dialogTimeMachineEndTime),
                    value = dateTimeFormatter.format(endDateTime),
                    onClick = {
                        pickerTarget = TimeMachineTarget.END
                        pickedLocalDate = endDateTime.atZone(zone).toLocalDate()
                        pickerStep = 1
                    },
                )

                errorText?.let {
                    Spacer(modifier = Modifier.height(AppTheme.spaceSmall))
                    Text(it, color = ErrorColor, fontSize = AppTheme.fsBodySmall)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.commonLabelCancel)) }
        },
        confirmButton = {
            Button(onClick = {
                val mode = selectedMode
                if (mode == null) return@Button
                if (endDateTime.isBefore(startDateTime)) {
                    errorText = endBeforeStartError
                    return@Button
                }
                onLog(mode, startDateTime, endDateTime, selectedTagId)
            }) { Text(stringResource(R.string.commonLabelSave)) }
        },
    )

    if (pickerStep == 1) {
        val target = pickerTarget
        val initialInstant = if (target == TimeMachineTarget.START) startDateTime else endDateTime
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialInstant.toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { pickerStep = 0; pickerTarget = null },
            confirmButton = {
                TextButton(onClick = {
                    val millis = datePickerState.selectedDateMillis
                    if (millis != null) {
                        pickedLocalDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        pickerStep = 2
                    } else {
                        pickerStep = 0
                        pickerTarget = null
                    }
                }) { Text(stringResource(R.string.commonLabelConfirm)) }
            },
            dismissButton = {
                TextButton(onClick = { pickerStep = 0; pickerTarget = null }) { Text(stringResource(R.string.commonLabelCancel)) }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (pickerStep == 2 && pickedLocalDate != null) {
        val context = LocalContext.current
        val is24Hour = android.text.format.DateFormat.is24HourFormat(context)
        val referenceInstant = if (pickerTarget == TimeMachineTarget.START) startDateTime else endDateTime
        val referenceTime = referenceInstant.atZone(zone)
        val timePickerState = rememberTimePickerState(
            initialHour = referenceTime.hour,
            initialMinute = referenceTime.minute,
            is24Hour = is24Hour,
        )
        AlertDialog(
            onDismissRequest = { pickerStep = 0; pickerTarget = null },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(onClick = {
                    val date = pickedLocalDate!!
                    val instant = date.atTime(timePickerState.hour, timePickerState.minute).atZone(zone).toInstant()
                    if (pickerTarget == TimeMachineTarget.START) startDateTime = instant else endDateTime = instant
                    errorText = null
                    pickerStep = 0
                    pickerTarget = null
                }) { Text(stringResource(R.string.commonLabelConfirm)) }
            },
            dismissButton = {
                TextButton(onClick = { pickerStep = 0; pickerTarget = null }) { Text(stringResource(R.string.commonLabelCancel)) }
            },
        )
    }
}

private enum class TimeMachineTarget { START, END }

@Composable
private fun TimeRow(label: String, value: String, onClick: () -> Unit) {
    OutlinedCard(onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppTheme.spaceMedium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, fontSize = AppTheme.fsLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, fontWeight = FontWeight.Bold)
            }
            Icon(Icons.Filled.EditCalendar, contentDescription = null)
        }
    }
}
