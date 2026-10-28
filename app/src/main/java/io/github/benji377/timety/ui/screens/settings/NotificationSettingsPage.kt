package io.github.benji377.timety.ui.screens.settings

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlarmOn
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import io.github.benji377.timety.R
import io.github.benji377.timety.ui.theme.HabitColor
import io.github.benji377.timety.ui.theme.WarningAccent
import io.github.benji377.timety.ui.viewmodel.SettingsViewModel
import io.github.benji377.timety.ui.viewmodel.activityScopedViewModel
import io.github.benji377.timety.util.datetime.AppDateFormatUtils
import io.github.benji377.timety.util.reminder.ExactAlarmAccess

/**
 * Notification settings: exact-alarm access while it is missing, background activity and the
 * daily reminder times.
 */
@Composable
internal fun NotificationSettings(
    settingsViewModel: SettingsViewModel = activityScopedViewModel(),
) {
    val context = LocalContext.current
    val use24HourFormat by settingsViewModel.use24HourFormat.collectAsState()
    val dailyMotivationTime by settingsViewModel.dailyMotivationTime.collectAsState()
    val endOfDayCheckupTime by settingsViewModel.endOfDayCheckupTime.collectAsState()

    val dailyMotivationTitle = stringResource(R.string.settingsLabelDailyMotivation)
    val eodCheckupTitle = stringResource(R.string.settingsLabelEodCheckup)
    var timeDialogSpec by remember { mutableStateOf<TimeDialogSpec?>(null) }
    // Re-checked on resume, so the row goes away once the user allows it in system settings.
    var exactAlarmsMissing by remember { mutableStateOf(ExactAlarmAccess.isMissing(context)) }
    LifecycleResumeEffect(Unit) {
        exactAlarmsMissing = ExactAlarmAccess.isMissing(context)
        onPauseOrDispose {}
    }

    timeDialogSpec?.let { spec ->
        TimePickerDialogRow(spec = spec, onDismiss = { timeDialogSpec = null })
    }

    Spacer(modifier = Modifier.height(8.dp))
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && exactAlarmsMissing) {
        NavSettingsItem(
            title = stringResource(R.string.settingsLabelExactAlarms),
            subtitle = stringResource(R.string.settingsLabelExactAlarmsSubtitle),
            icon = Icons.Filled.AlarmOn,
            iconTint = WarningAccent,
            trailing = SettingsTrailing.EXTERNAL,
            onClick = { context.startActivity(ExactAlarmAccess.settingsIntent(context)) }
        )
    }
    NavSettingsItem(
        title = stringResource(R.string.settingsLabelBackgroundActivity),
        subtitle = stringResource(R.string.settingsLabelBackgroundActivitySubtitle),
        icon = Icons.Filled.BatteryAlert,
        iconTint = WarningAccent,
        trailing = SettingsTrailing.EXTERNAL,
        onClick = {
            context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
    )
    NavSettingsItem(
        title = dailyMotivationTitle,
        subtitle = AppDateFormatUtils.formatTimeOfDay(dailyMotivationTime, use24HourFormat),
        icon = Icons.Filled.Schedule,
        iconTint = WarningAccent,
        trailing = SettingsTrailing.EDIT,
        onClick = {
            timeDialogSpec = TimeDialogSpec(
                title = dailyMotivationTitle,
                current = dailyMotivationTime,
                onSave = { settingsViewModel.setDailyMotivationTime(it) }
            )
        }
    )
    NavSettingsItem(
        title = eodCheckupTitle,
        subtitle = AppDateFormatUtils.formatTimeOfDay(endOfDayCheckupTime, use24HourFormat),
        icon = Icons.Filled.NightlightRound,
        iconTint = HabitColor,
        trailing = SettingsTrailing.EDIT,
        onClick = {
            timeDialogSpec = TimeDialogSpec(
                title = eodCheckupTitle,
                current = endOfDayCheckupTime,
                onSave = { settingsViewModel.setEndOfDayCheckupTime(it) }
            )
        }
    )
}
