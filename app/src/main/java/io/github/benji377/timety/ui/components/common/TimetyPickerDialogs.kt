package io.github.benji377.timety.ui.components.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.benji377.timety.R
import io.github.benji377.timety.ui.utils.LocalDateFormatSettings
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset


/**
 * Alert dialog wrapping a Material [TimePicker], confirming with the picked hour and minute.
 * The 24-hour setting follows the app's date-format preference.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetyTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit,
    title: (@Composable () -> Unit)? = null,
    confirmLabel: String? = null,
) {
    val state = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = LocalDateFormatSettings.current.use24HourFormat,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = title,
        text = {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TimePicker(state = state)
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour, state.minute) }) {
                Text(confirmLabel ?: stringResource(R.string.commonLabelConfirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.commonLabelCancel)) }
        },
    )
}


/**
 * Alert dialog wrapping a Material [TimeInput] used as an hours-and-minutes duration field,
 * confirming with the total minutes coerced into [minMinutes]..[maxMinutes]. Always 24-hour so
 * no AM/PM toggle appears - a duration is not a time of day.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetyDurationPickerDialog(
    initialMinutes: Int,
    minMinutes: Int,
    maxMinutes: Int,
    onConfirm: (minutes: Int) -> Unit,
    onDismiss: () -> Unit,
    title: (@Composable () -> Unit)? = null,
    confirmLabel: String? = null,
) {
    val initial = initialMinutes.coerceIn(minMinutes, maxMinutes)
    val state = rememberTimePickerState(
        initialHour = initial / 60,
        initialMinute = initial % 60,
        is24Hour = true,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = title,
        text = {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TimeInput(state = state)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm((state.hour * 60 + state.minute).coerceIn(minMinutes, maxMinutes))
            }) { Text(confirmLabel ?: stringResource(R.string.commonLabelConfirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.commonLabelCancel)) }
        },
    )
}


/**
 * Two-step date-then-time picker: a [DatePickerDialog] followed by a [TimetyTimePickerDialog],
 * confirming with the picked date, hour, and minute. The date is derived from the picker's UTC
 * millis; callers combine the parts into an Instant in their own zone. Cancelling either step
 * calls [onDismiss].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetyDateTimePickerDialog(
    initialDateMillis: Long,
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (date: LocalDate, hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit,
    timeTitle: String? = null,
    selectableDates: SelectableDates? = null,
) {
    var pickedDate by remember { mutableStateOf<LocalDate?>(null) }

    val date = pickedDate
    if (date == null) {
        val state = if (selectableDates == null) {
            rememberDatePickerState(initialSelectedDateMillis = initialDateMillis)
        } else {
            rememberDatePickerState(
                initialSelectedDateMillis = initialDateMillis,
                selectableDates = selectableDates,
            )
        }
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = {
                    val millis = state.selectedDateMillis
                    if (millis != null) {
                        pickedDate = Instant.ofEpochMilli(millis)
                            .atZone(ZoneOffset.UTC).toLocalDate()
                    } else {
                        onDismiss()
                    }
                }) { Text(stringResource(R.string.commonLabelConfirm)) }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.commonLabelCancel)) }
            },
        ) {
            DatePicker(state = state)
        }
    } else {
        TimetyTimePickerDialog(
            initialHour = initialHour,
            initialMinute = initialMinute,
            onConfirm = { hour, minute -> onConfirm(date, hour, minute) },
            onDismiss = onDismiss,
            title = timeTitle?.let { { Text(it) } },
        )
    }
}
