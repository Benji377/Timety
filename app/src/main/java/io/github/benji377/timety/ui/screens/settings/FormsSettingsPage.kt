package io.github.benji377.timety.ui.screens.settings

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
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import io.github.benji377.timety.R
import io.github.benji377.timety.ui.components.common.NeoTabRow
import io.github.benji377.timety.ui.theme.AppTheme
import io.github.benji377.timety.ui.theme.HabitColor
import io.github.benji377.timety.ui.theme.TaskColor

// UI preview only: the switches aren't saved and the editors don't read them yet.
private enum class PreviewInput(
    val titleRes: Int,
    val subtitleRes: Int,
    val icon: ImageVector,
    val form: FormTab,
    val offOnNewInstall: Boolean = false,
) {
    TASK_TAGS(R.string.globalLabelTags, R.string.settingsFormsTagsInfo, Icons.AutoMirrored.Outlined.Label, FormTab.TASKS),
    TASK_DESCRIPTION(
        R.string.taskDetailsLabelDescription, R.string.settingsFormsDescriptionInfo,
        Icons.AutoMirrored.Filled.Notes, FormTab.TASKS,
    ),
    TASK_PRIORITY_EFFORT(
        R.string.taskDetailsSectionPriorityAndEffort, R.string.settingsFormsPriorityInfo,
        Icons.Filled.BarChart, FormTab.TASKS, offOnNewInstall = true,
    ),
    TASK_DUE(R.string.settingsFormsDueDate, R.string.settingsFormsDueInfo, Icons.Filled.CalendarToday, FormTab.TASKS),
    TASK_REMINDERS(
        R.string.settingsFormsReminders, R.string.settingsFormsTaskRemindersInfo,
        Icons.Outlined.Notifications, FormTab.TASKS,
    ),
    TASK_LOCATION(
        R.string.taskDetailsSectionLocation, R.string.settingsFormsLocationInfo,
        Icons.Outlined.LocationOn, FormTab.TASKS, offOnNewInstall = true,
    ),
    TASK_CHECKLIST(
        R.string.taskDetailsSectionChecklist, R.string.settingsFormsChecklistInfo,
        Icons.Filled.Checklist, FormTab.TASKS,
    ),
    TASK_WINDOW(
        R.string.recurringTaskWindowLabel, R.string.settingsFormsWindowInfo,
        Icons.Filled.Visibility, FormTab.TASKS, offOnNewInstall = true,
    ),
    HABIT_TAGS(R.string.globalLabelTags, R.string.settingsFormsTagsInfo, Icons.AutoMirrored.Outlined.Label, FormTab.HABITS),
    HABIT_NOTES(
        R.string.habitDetailLabelNotes, R.string.settingsFormsNotesInfo,
        Icons.AutoMirrored.Filled.Notes, FormTab.HABITS,
    ),
    HABIT_STACK(
        R.string.habitDetailLabelStack, R.string.settingsFormsStackInfo,
        Icons.Filled.Layers, FormTab.HABITS, offOnNewInstall = true,
    ),
    HABIT_ICON(R.string.settingsFormsIcon, R.string.settingsFormsIconInfo, Icons.Outlined.EmojiEmotions, FormTab.HABITS),
    HABIT_TIMES_PER_DAY(
        R.string.settingsFormsTimesPerDay, R.string.settingsFormsTimesPerDayInfo,
        Icons.Outlined.Repeat, FormTab.HABITS, offOnNewInstall = true,
    ),
    HABIT_REMINDERS(
        R.string.settingsFormsReminders, R.string.settingsFormsHabitRemindersInfo,
        Icons.Outlined.Notifications, FormTab.HABITS,
    ),
}

/** The editor a tab of the Forms page configures, in its item colour. */
private enum class FormTab(val labelRes: Int, val color: Color) {
    TASKS(R.string.globalLabelTasks, TaskColor),
    HABITS(R.string.globalLabelHabits, HabitColor),
}

/** Form settings: one switch per optional input of the task and habit editors, a tab per editor. */
@Composable
internal fun FormsSettings() {
    var tab by rememberSaveable { mutableStateOf(FormTab.TASKS) }
    val switchedOff = remember {
        mutableStateListOf(*PreviewInput.entries.filter { it.offOnNewInstall }.toTypedArray())
    }

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
    PreviewInput.entries.filter { it.form == tab }.forEach { input ->
        // Reminders count from the due date, so they go when it does.
        val needsDue = input == PreviewInput.TASK_REMINDERS && PreviewInput.TASK_DUE in switchedOff
        SwitchSettingsItem(
            title = stringResource(input.titleRes),
            subtitle = stringResource(if (needsDue) R.string.settingsFormsRemindersNeedDue else input.subtitleRes),
            icon = input.icon,
            iconTint = tab.color,
            checked = !needsDue && input !in switchedOff,
            enabled = !needsDue,
            onCheckedChange = { on -> if (on) switchedOff.remove(input) else switchedOff.add(input) },
        )
    }
}
