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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import io.github.benji377.timety.R
import io.github.benji377.timety.data.model.focus.FocusModeEntity
import io.github.benji377.timety.data.model.focus.FocusTagEntity
import io.github.benji377.timety.ui.components.common.NeoDateTimePickerDialog
import io.github.benji377.timety.ui.theme.AppTheme
import io.github.benji377.timety.ui.theme.ErrorColor
import io.github.benji377.timety.ui.theme.TaskColor
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import io.github.benji377.timety.ui.components.common.NeoButton as Button
import io.github.benji377.timety.ui.components.common.NeoOutlinedTextField as OutlinedTextField


/**
 * Dialog for logging a past focus session: pick a mode, an optional tag, and a start/end date
 * and time, then call [onLog]. Rejects an end time before the start time.
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

    // Date+time picker flow shared by both start/end fields: pick a date, then a time.
    var pickerTarget by remember { mutableStateOf<TimeMachineTarget?>(null) }

    val endBeforeStartError = stringResource(R.string.dialogTimeMachineErrorEndBeforeStart)
    val zone = ZoneId.systemDefault()
    val locale = LocalLocale.current.platformLocale
    val dateTimeFormatter = remember(locale) {
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
            .withLocale(locale).withZone(zone)
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
                ExposedDropdownMenuBox(
                    expanded = modeExpanded,
                    onExpandedChange = { modeExpanded = it }) {
                    OutlinedTextField(
                        value = selectedMode?.let { localizedFocusModeName(it) } ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.dialogTimeMachineMode)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modeExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = modeExpanded,
                        onDismissRequest = { modeExpanded = false }) {
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
                ExposedDropdownMenuBox(
                    expanded = tagExpanded,
                    onExpandedChange = { tagExpanded = it }) {
                    OutlinedTextField(
                        value = tags.firstOrNull { it.id == selectedTagId }?.name ?: noTagLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.dialogTimeMachineTag)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = tagExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = tagExpanded,
                        onDismissRequest = { tagExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text(noTagLabel) },
                            onClick = { selectedTagId = null; tagExpanded = false })
                        tags.forEach { tag ->
                            DropdownMenuItem(
                                text = { Text(tag.name) },
                                onClick = { selectedTagId = tag.id; tagExpanded = false })
                        }
                    }
                }
                Spacer(modifier = Modifier.height(AppTheme.spaceLarge))

                TimeRow(
                    label = stringResource(R.string.dialogTimeMachineStartTime),
                    value = dateTimeFormatter.format(startDateTime),
                    onClick = { pickerTarget = TimeMachineTarget.START },
                )
                Spacer(modifier = Modifier.height(AppTheme.spaceSmall))
                TimeRow(
                    label = stringResource(R.string.dialogTimeMachineEndTime),
                    value = dateTimeFormatter.format(endDateTime),
                    onClick = { pickerTarget = TimeMachineTarget.END },
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
                val mode = selectedMode ?: return@Button
                if (endDateTime.isBefore(startDateTime)) {
                    errorText = endBeforeStartError
                    return@Button
                }
                onLog(mode, startDateTime, endDateTime, selectedTagId)
            }) { Text(stringResource(R.string.commonLabelSave)) }
        },
    )

    pickerTarget?.let { target ->
        val referenceInstant = if (target == TimeMachineTarget.START) startDateTime else endDateTime
        val referenceTime = referenceInstant.atZone(zone)
        NeoDateTimePickerDialog(
            initialDateMillis = referenceInstant.toEpochMilli(),
            initialHour = referenceTime.hour,
            initialMinute = referenceTime.minute,
            onConfirm = { date, hour, minute ->
                val instant = date.atTime(hour, minute).atZone(zone).toInstant()
                if (target == TimeMachineTarget.START) startDateTime = instant
                else endDateTime = instant
                errorText = null
                pickerTarget = null
            },
            onDismiss = { pickerTarget = null },
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
                Text(
                    label,
                    fontSize = AppTheme.fsLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(value, fontWeight = FontWeight.Bold)
            }
            Icon(Icons.Filled.EditCalendar, contentDescription = null)
        }
    }
}
