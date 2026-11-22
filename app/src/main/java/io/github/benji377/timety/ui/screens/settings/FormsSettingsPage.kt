package io.github.benji377.timety.ui.screens.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import io.github.benji377.timety.R
import io.github.benji377.timety.data.model.form.HabitInput
import io.github.benji377.timety.data.model.form.TaskInput
import io.github.benji377.timety.ui.components.common.NeoTabRow
import io.github.benji377.timety.ui.theme.AppTheme
import io.github.benji377.timety.ui.theme.HabitColor
import io.github.benji377.timety.ui.theme.TaskColor
import io.github.benji377.timety.ui.viewmodel.SettingsViewModel
import io.github.benji377.timety.ui.viewmodel.activityScopedViewModel

/** The editor a tab of the Forms page configures, in its item colour. */
private enum class FormTab(@param:StringRes val labelRes: Int, val color: Color) {
    TASKS(R.string.globalLabelTasks, TaskColor),
    HABITS(R.string.globalLabelHabits, HabitColor),
}

/** How an input's switch row reads. */
private class InputRow(@param:StringRes val titleRes: Int, @param:StringRes val subtitleRes: Int, val icon: ImageVector)

private fun TaskInput.row(): InputRow = when (this) {
    TaskInput.TAGS -> InputRow(R.string.globalLabelTags, R.string.settingsFormsTagsInfo, Icons.AutoMirrored.Outlined.Label)
    TaskInput.DESCRIPTION -> InputRow(
        R.string.taskDetailsLabelDescription, R.string.settingsFormsDescriptionInfo, Icons.AutoMirrored.Filled.Notes,
    )
    TaskInput.PRIORITY_EFFORT -> InputRow(
        R.string.taskDetailsSectionPriorityAndEffort, R.string.settingsFormsPriorityInfo, Icons.Filled.BarChart,
    )
    TaskInput.DUE -> InputRow(R.string.settingsFormsDueDate, R.string.settingsFormsDueInfo, Icons.Filled.CalendarToday)
    TaskInput.REMINDERS -> InputRow(
        R.string.settingsFormsReminders, R.string.settingsFormsTaskRemindersInfo, Icons.Outlined.Notifications,
    )
    TaskInput.LOCATION -> InputRow(
        R.string.taskDetailsSectionLocation, R.string.settingsFormsLocationInfo, Icons.Outlined.LocationOn,
    )
    TaskInput.CHECKLIST -> InputRow(
        R.string.taskDetailsSectionChecklist, R.string.settingsFormsChecklistInfo, Icons.Filled.Checklist,
    )
    TaskInput.WINDOW -> InputRow(R.string.recurringTaskWindowLabel, R.string.settingsFormsWindowInfo, Icons.Filled.Visibility)
}

private fun HabitInput.row(): InputRow = when (this) {
    HabitInput.TAGS -> InputRow(R.string.globalLabelTags, R.string.settingsFormsTagsInfo, Icons.AutoMirrored.Outlined.Label)
    HabitInput.NOTES -> InputRow(R.string.habitDetailLabelNotes, R.string.settingsFormsNotesInfo, Icons.AutoMirrored.Filled.Notes)
    HabitInput.STACK -> InputRow(R.string.habitDetailLabelStack, R.string.settingsFormsStackInfo, Icons.Filled.Layers)
    HabitInput.ICON -> InputRow(R.string.settingsFormsIcon, R.string.settingsFormsIconInfo, Icons.Outlined.EmojiEmotions)
    HabitInput.COLOR -> InputRow(R.string.habitDetailLabelColor, R.string.settingsFormsColorInfo, Icons.Outlined.Palette)
    HabitInput.TIMES_PER_DAY -> InputRow(
        R.string.settingsFormsTimesPerDay, R.string.settingsFormsTimesPerDayInfo, Icons.Outlined.Repeat,
    )
    HabitInput.REMINDERS -> InputRow(
        R.string.settingsFormsReminders, R.string.settingsFormsHabitRemindersInfo, Icons.Outlined.Notifications,
    )
}

/** Form settings: one switch per optional input of the task and habit editors, a tab per editor. */
@Composable
internal fun FormsSettings(settingsViewModel: SettingsViewModel = activityScopedViewModel()) {
    var tab by rememberSaveable { mutableStateOf(FormTab.TASKS) }
    val taskSkeleton by settingsViewModel.taskSkeleton.collectAsState()
    val habitSkeleton by settingsViewModel.habitSkeleton.collectAsState()

    Text(
        stringResource(R.string.settingsFormsHint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = AppTheme.spaceLarge, vertical = AppTheme.spaceMedium),
    )
    NeoTabRow(
        tabs = FormTab.entries.map { stringResource(it.labelRes) },
        selectedIndex = tab.ordinal,
        onTabSelected = { tab = FormTab.entries[it] },
        activeColor = tab.color,
        modifier = Modifier.padding(horizontal = AppTheme.spaceLarge),
    )
    Spacer(Modifier.height(AppTheme.spaceSmall))
    when (tab) {
        FormTab.TASKS -> TaskInput.entries.forEach { input ->
            // Reminders count from the due date, so they go when it does.
            val needsDue = input == TaskInput.REMINDERS && !taskSkeleton.isOn(TaskInput.DUE)
            InputSwitch(
                row = input.row(),
                color = tab.color,
                checked = !needsDue && taskSkeleton.isOn(input),
                enabled = !needsDue,
                subtitleOverride = if (needsDue) R.string.settingsFormsRemindersNeedDue else null,
                onCheckedChange = { settingsViewModel.setTaskInputOn(input, it) },
            )
        }
        FormTab.HABITS -> HabitInput.entries.forEach { input ->
            InputSwitch(
                row = input.row(),
                color = tab.color,
                checked = habitSkeleton.isOn(input),
                onCheckedChange = { settingsViewModel.setHabitInputOn(input, it) },
            )
        }
    }
}

@Composable
private fun InputSwitch(
    row: InputRow,
    color: Color,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    @StringRes subtitleOverride: Int? = null,
) {
    SwitchSettingsItem(
        title = stringResource(row.titleRes),
        subtitle = stringResource(subtitleOverride ?: row.subtitleRes),
        icon = row.icon,
        iconTint = color,
        checked = checked,
        enabled = enabled,
        onCheckedChange = onCheckedChange,
    )
}
