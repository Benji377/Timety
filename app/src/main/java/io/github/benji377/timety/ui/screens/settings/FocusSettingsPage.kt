package io.github.benji377.timety.ui.screens.settings

import android.app.NotificationManager
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.FreeBreakfast
import androidx.compose.material.icons.filled.LinearScale
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.benji377.timety.R
import io.github.benji377.timety.data.model.focus.DailyFocusTarget
import io.github.benji377.timety.ui.components.common.ConfirmationDialog
import io.github.benji377.timety.ui.components.common.WeekdayToggleRow
import io.github.benji377.timety.ui.theme.AppTheme
import io.github.benji377.timety.ui.theme.FocusColor
import io.github.benji377.timety.ui.theme.TaskColor
import io.github.benji377.timety.ui.theme.WarningAccent
import io.github.benji377.timety.ui.utils.quantityString
import io.github.benji377.timety.ui.viewmodel.FocusViewModel
import io.github.benji377.timety.ui.viewmodel.SettingsViewModel
import io.github.benji377.timety.ui.viewmodel.activityScopedViewModel
import io.github.benji377.timety.util.datetime.AppDateFormatUtils

/** Focus settings: sessions (goal, limits, tags), linked tasks and habits, and Do Not Disturb. */
@Composable
internal fun FocusSettings(
    onNavigateToTags: () -> Unit,
    settingsViewModel: SettingsViewModel = activityScopedViewModel(),
    focusViewModel: FocusViewModel = activityScopedViewModel(),
) {
    val context = LocalContext.current
    val dailyFocusTarget by settingsViewModel.dailyFocusTarget.collectAsState()
    val maxStopwatchMins by settingsViewModel.maxStopwatchMins.collectAsState()
    val maxNodeMins by settingsViewModel.maxNodeMins.collectAsState()
    val keepScreenOnDuringFocus by settingsViewModel.keepScreenOnDuringFocus.collectAsState()
    val autoCompleteFocus by settingsViewModel.autoCompleteFocus.collectAsState()
    val askFocusOnTaskComplete by settingsViewModel.askFocusOnTaskComplete.collectAsState()
    val autoDndEnabled by settingsViewModel.autoDndEnabled.collectAsState()
    val autoDndLiftDuringBreaks by settingsViewModel.autoDndLiftDuringBreaks.collectAsState()
    val focusTags by focusViewModel.allTags.collectAsState()

    val focusGoalTitle = stringResource(R.string.settingsLabelFocusGoal)
    val focusStopwatchTitle = stringResource(R.string.settingsLabelFocusStopwatch)
    val focusNodeTimeTitle = stringResource(R.string.settingsLabelFocusNodeTime)

    var durationDialogSpec by remember { mutableStateOf<DurationDialogSpec?>(null) }
    var showDndPermissionDialog by rememberSaveable { mutableStateOf(false) }
    val systemNotificationManager = remember {
        context.getSystemService(NotificationManager::class.java)
    }

    // Enabling Auto-DND needs the Notification policy access special permission, which has no
    // runtime prompt - only a system settings toggle. Route through a rationale dialog first,
    // matching the c) wiring rules: the setting itself always saves, syncing/degrading silently
    // is FocusDndController's job once a session actually starts.
    fun requestAutoDndEnabled(enabled: Boolean) {
        if (enabled && systemNotificationManager?.isNotificationPolicyAccessGranted != true) {
            showDndPermissionDialog = true
        } else {
            settingsViewModel.setAutoDndEnabled(enabled)
        }
    }

    durationDialogSpec?.let { spec ->
        DurationPickerDialog(spec = spec, onDismiss = { durationDialogSpec = null })
    }

    // Notification policy access has no runtime prompt, only a system settings toggle - explain
    // why before sending the user there.
    ConfirmationDialog(
        visible = showDndPermissionDialog,
        title = stringResource(R.string.settingsDndPermissionDialogTitle),
        content = stringResource(R.string.settingsDndPermissionDialogContent),
        confirmLabel = stringResource(R.string.commonLabelOpenSettings),
        onConfirm = {
            settingsViewModel.setAutoDndEnabled(true)
            showDndPermissionDialog = false
            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
        },
        onDismiss = { showDndPermissionDialog = false }
    )

    SettingsHeader(stringResource(R.string.settingsGroupSessions))
    NavSettingsItem(
        title = focusGoalTitle,
        subtitle = listOf(
            if (dailyFocusTarget.isEnabled) {
                AppDateFormatUtils.formatMinutesCompact(dailyFocusTarget.minutes)
            } else {
                stringResource(R.string.settingsLabelFocusGoalOff)
            },
            stringResource(R.string.settingsLabelFocusGoalSubtitle)
        ).joinToString("\n"),
        icon = Icons.Filled.TrackChanges,
        iconTint = FocusColor,
        onClick = {
            durationDialogSpec = DurationDialogSpec(
                title = focusGoalTitle,
                current = dailyFocusTarget.minutes,
                min = 0,
                max = DailyFocusTarget.MAX_MINUTES,
                onSave = { settingsViewModel.setDailyGoalMins(it) }
            )
        }
    )
    if (dailyFocusTarget.isEnabled) {
        // Deselecting the last weekday is a no-op: zero minutes is the only "off".
        WeekdayToggleRow(
            selected = dailyFocusTarget.weekdays,
            onToggle = { day -> settingsViewModel.toggleDailyGoalWeekday(day) },
            selectedColor = FocusColor,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 56.dp, end = 16.dp, top = AppTheme.spaceXSmall, bottom = AppTheme.spaceLarge),
        )
    }
    NavSettingsItem(
        title = focusStopwatchTitle,
        subtitle = stringResource(R.string.settingsLabelFocusStopwatchSubtitle),
        icon = Icons.Outlined.Timer,
        iconTint = WarningAccent,
        onClick = {
            durationDialogSpec = DurationDialogSpec(
                title = focusStopwatchTitle,
                current = maxStopwatchMins, min = 30, max = 480,
                onSave = { settingsViewModel.setMaxStopwatchMins(it) }
            )
        }
    )
    NavSettingsItem(
        title = focusNodeTimeTitle,
        subtitle = stringResource(R.string.settingsLabelFocusNodeTimeSubtitle),
        icon = Icons.Filled.LinearScale,
        iconTint = TaskColor,
        onClick = {
            durationDialogSpec = DurationDialogSpec(
                title = focusNodeTimeTitle,
                current = maxNodeMins, min = 10, max = 480,
                onSave = { settingsViewModel.setMaxNodeMins(it) }
            )
        }
    )
    SwitchSettingsItem(
        title = stringResource(R.string.settingsLabelKeepScreenOn),
        subtitle = stringResource(R.string.settingsLabelKeepScreenOnSubtitle),
        icon = Icons.Outlined.Visibility,
        iconTint = FocusColor,
        checked = keepScreenOnDuringFocus,
        onCheckedChange = { settingsViewModel.setKeepScreenOnDuringFocus(it) }
    )
    NavSettingsItem(
        title = stringResource(R.string.settingsLabelTags),
        subtitle = quantityString(R.plurals.settingsTagsCount, focusTags.size, 0, focusTags.size),
        icon = Icons.Outlined.LocalOffer,
        iconTint = FocusColor,
        onClick = onNavigateToTags
    )

    SettingsGroupDivider()

    SettingsHeader(stringResource(R.string.settingsGroupLinkedItems))
    SwitchSettingsItem(
        title = stringResource(R.string.settingsLabelFocusAutocomplete),
        subtitle = stringResource(R.string.settingsLabelFocusAutocompleteSubtitle),
        icon = Icons.Filled.TaskAlt,
        iconTint = TaskColor,
        checked = autoCompleteFocus,
        onCheckedChange = { settingsViewModel.setAutoCompleteFocus(it) }
    )
    SwitchSettingsItem(
        title = stringResource(R.string.settingsLabelFocusAskOnTaskComplete),
        subtitle = stringResource(R.string.settingsLabelFocusAskOnTaskCompleteSubtitle),
        icon = Icons.Outlined.Timer,
        iconTint = FocusColor,
        checked = askFocusOnTaskComplete,
        onCheckedChange = { settingsViewModel.setAskFocusOnTaskComplete(it) }
    )

    SettingsGroupDivider()

    SettingsHeader(stringResource(R.string.settingsGroupDnd))
    SwitchSettingsItem(
        title = stringResource(R.string.settingsLabelAutoDnd),
        subtitle = stringResource(R.string.settingsLabelAutoDndSubtitle),
        icon = Icons.Filled.DoNotDisturbOn,
        iconTint = WarningAccent,
        checked = autoDndEnabled,
        onCheckedChange = { requestAutoDndEnabled(it) }
    )
    SwitchSettingsItem(
        title = stringResource(R.string.settingsLabelAutoDndLiftBreaks),
        subtitle = stringResource(R.string.settingsLabelAutoDndLiftBreaksSubtitle),
        icon = Icons.Filled.FreeBreakfast,
        iconTint = WarningAccent,
        checked = autoDndLiftDuringBreaks,
        enabled = autoDndEnabled,
        onCheckedChange = { settingsViewModel.setAutoDndLiftDuringBreaks(it) }
    )
}
