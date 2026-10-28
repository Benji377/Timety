package io.github.benji377.timety.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.benji377.timety.R
import io.github.benji377.timety.data.model.focus.FocusSessionEntity
import io.github.benji377.timety.data.model.focus.FocusTargetType
import io.github.benji377.timety.data.model.focus.color
import io.github.benji377.timety.data.model.habit.HabitWithCompletions
import io.github.benji377.timety.data.model.task.RecurringOccurrenceEntity
import io.github.benji377.timety.data.model.task.RecurringTaskEntity
import io.github.benji377.timety.data.model.task.TaskWithSubtasks
import io.github.benji377.timety.ui.components.common.NeoListTile
import io.github.benji377.timety.ui.components.common.StyledExpansionTile
import io.github.benji377.timety.ui.components.focus.EditSessionDialog
import io.github.benji377.timety.ui.components.focus.localizedFocusModeName
import io.github.benji377.timety.ui.theme.AppTheme
import io.github.benji377.timety.ui.theme.HabitColor
import io.github.benji377.timety.ui.theme.SuccessColor
import io.github.benji377.timety.ui.theme.TaskColor
import io.github.benji377.timety.ui.utils.AppUtils
import io.github.benji377.timety.ui.utils.LocalDateFormatSettings
import io.github.benji377.timety.ui.utils.rememberToday
import io.github.benji377.timety.ui.viewmodel.FocusViewModel
import io.github.benji377.timety.util.datetime.AppDateFormatUtils
import io.github.benji377.timety.util.habit.HabitUtils
import java.time.LocalDate
import java.time.ZoneId

// Accordion lists for the selected day.

@Composable
internal fun HabitsAccordion(
    habits: List<HabitWithCompletions>,
    selectedDate: LocalDate,
    onToggle: (HabitWithCompletions, Boolean) -> Unit,
    onHabitClick: (HabitWithCompletions) -> Unit
) {
    StyledExpansionTile(
        title = stringResource(R.string.calendarSectionHabits, habits.size),
        titleColor = HabitColor,
        initiallyExpanded = false
    ) {
        if (habits.isEmpty()) {
            Text(
                text = stringResource(R.string.calendarSectionHabitsEmpty),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            // A habit is done on the day itself, so future days can't be checked off ahead of
            // time, as in the habit history sheet. A stray future check can still be removed.
            val today by rememberToday()
            val isFutureDay = selectedDate.isAfter(today)
            habits.forEach { hwc ->
                val isCompleted = HabitUtils.isCompletedOn(hwc, selectedDate)
                NeoListTile(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AppTheme.listTileScreenMargin)
                        .clickable { onHabitClick(hwc) },
                    containerColor = MaterialTheme.colorScheme.background,
                    // Completion is already conveyed by the strikethrough + dimmed text below;
                    // the border stays a solid color rather than an alpha-faded one (reference
                    // sheet forbids soft borders) - falling back to the neutral outline reads as
                    // "no longer the active accent" without inventing a new faded tone.
                    borderColor = if (isCompleted) MaterialTheme.colorScheme.outline else HabitColor,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AppTheme.spaceMedium),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isCompleted,
                            onCheckedChange = { onToggle(hwc, isCompleted) },
                            enabled = isCompleted || !isFutureDay,
                            colors = CheckboxDefaults.colors(
                                checkedColor = SuccessColor,
                                uncheckedColor = HabitColor,
                                checkmarkColor = Color.White
                            )
                        )
                        Text(
                            text = hwc.habit.name,
                            textDecoration = if (isCompleted) TextDecoration.LineThrough else null,
                            color = if (isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(AppTheme.spaceSmall))
        }
    }
}

@Composable
internal fun TasksAccordion(
    tasks: List<TaskWithSubtasks>,
    recurringDue: List<RecurringTaskEntity>,
    recurringDone: List<Pair<RecurringTaskEntity, RecurringOccurrenceEntity>>,
    onToggle: (TaskWithSubtasks) -> Unit,
    onTaskClick: (TaskWithSubtasks) -> Unit,
    onCompleteRecurring: (RecurringTaskEntity) -> Unit,
    onUncheckRecurring: (RecurringOccurrenceEntity) -> Unit,
    onRecurringClick: (RecurringTaskEntity) -> Unit,
) {
    val totalCount = tasks.size + recurringDue.size + recurringDone.size
    StyledExpansionTile(
        title = stringResource(R.string.calendarSectionTasks, totalCount),
        titleColor = TaskColor,
        initiallyExpanded = false
    ) {
        if (totalCount == 0) {
            Text(
                text = stringResource(R.string.calendarSectionTasksEmpty),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            tasks.forEach { taskWithSubtasks ->
                val task = taskWithSubtasks.task
                DayTaskRow(
                    title = task.title,
                    isCompleted = task.isCompleted,
                    onToggle = { onToggle(taskWithSubtasks) },
                    onClick = { onTaskClick(taskWithSubtasks) },
                    trailing = { AppUtils.PriorityIcon(priority = task.priority) },
                )
            }
            recurringDue.forEach { task ->
                DayTaskRow(
                    title = task.title,
                    isCompleted = false,
                    onToggle = { onCompleteRecurring(task) },
                    onClick = { onRecurringClick(task) },
                    trailing = { RecurringMarkerIcon() },
                )
            }
            recurringDone.forEach { (task, occurrence) ->
                DayTaskRow(
                    title = task.title,
                    isCompleted = true,
                    onToggle = { onUncheckRecurring(occurrence) },
                    onClick = { onRecurringClick(task) },
                    trailing = { RecurringMarkerIcon() },
                )
            }
            Spacer(modifier = Modifier.height(AppTheme.spaceSmall))
        }
    }
}

/** Checkbox row card shared by normal and recurring tasks in the day-detail task accordion. */
@Composable
private fun DayTaskRow(
    title: String,
    isCompleted: Boolean,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit,
) {
    NeoListTile(
        modifier = Modifier
            .fillMaxWidth()
            .padding(AppTheme.listTileScreenMargin)
            .clickable { onClick() },
        containerColor = MaterialTheme.colorScheme.background,
        borderColor = if (isCompleted) SuccessColor else TaskColor,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppTheme.spaceMedium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isCompleted,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = SuccessColor,
                    uncheckedColor = TaskColor,
                    checkmarkColor = Color.White
                )
            )
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                textDecoration = if (isCompleted) TextDecoration.LineThrough else null
            )
            trailing()
        }
    }
}

@Composable
private fun RecurringMarkerIcon() {
    Icon(
        imageVector = Icons.Filled.Repeat,
        contentDescription = null,
        tint = TaskColor,
        modifier = Modifier.size(AppTheme.iconSizeSmall),
    )
}

@Composable
internal fun FocusSessionsAccordion(
    sessions: List<FocusSessionEntity>,
    focusViewModel: FocusViewModel
) {
    val modes by focusViewModel.allModes.collectAsState()
    val tags by focusViewModel.allTags.collectAsState()
    val zone = ZoneId.systemDefault()
    val use24Hour =
        LocalDateFormatSettings.current.use24HourFormat
    val ongoingLabel = stringResource(R.string.calendarLabelFocusOngoing)
    val untaggedLabel = stringResource(R.string.focusTargetUntagged)
    var editingSession by remember { mutableStateOf<FocusSessionEntity?>(null) }

    StyledExpansionTile(
        title = stringResource(R.string.calendarSectionFocus, sessions.size),
        titleColor = SuccessColor,
        initiallyExpanded = false
    ) {
        if (sessions.isEmpty()) {
            Text(
                text = stringResource(R.string.calendarSectionFocusEmpty),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            sessions.forEach { session ->
                val mode = modes.firstOrNull { it.id == session.modeId } ?: modes.firstOrNull()
                val tag = session.tagId?.let { tagId -> tags.firstOrNull { it.id == tagId } }

                var timeString =
                    AppDateFormatUtils.formatTime(
                        session.startTime,
                        use24Hour,
                        zone
                    )
                timeString += if (session.endTime != null) {
                    " - ${
                        AppDateFormatUtils.formatTime(
                            session.endTime,
                            use24Hour,
                            zone
                        )
                    }"
                } else {
                    " - $ongoingLabel"
                }

                val focusMins = session.totalSecondsFocused / 60

                NeoListTile(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AppTheme.listTileScreenMargin)
                        .clickable { editingSession = session },
                    containerColor = MaterialTheme.colorScheme.background,
                    // Focus rows take the same accent-border treatment as the habit and task rows
                    // above, using the color this screen already gives focus activity. The previous
                    // `outlineVariant` was the soft tan the general fixes ban outright.
                    borderColor = SuccessColor,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AppTheme.spaceLarge),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Circle,
                            contentDescription = null,
                            // Solid fallback instead of an alpha-faded tint -
                            // untagged sessions get the same neutral solid used for muted text
                            // elsewhere on this screen.
                            tint = tag?.let { Color(it.colorValue) }
                                ?: session.targetType.takeIf { it != FocusTargetType.TAG }?.color
                                ?: MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            // Sessions linked to anything but a tag carry no tag, so name them by
                            // the target they were logged against rather than calling them untagged.
                            val title = tag?.name
                                ?: session.targetLabel?.trim()?.takeIf { it.isNotEmpty() }
                                ?: untaggedLabel
                            Text(text = title, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            if (mode != null) {
                                Text(text = localizedFocusModeName(mode), fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                            }
                            Text(
                                text = timeString,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Text(
                            text = stringResource(R.string.focusMinutes, focusMins),
                            color = SuccessColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(AppTheme.spaceSmall))
        }
    }

    editingSession?.let { session ->
        EditSessionDialog(
            session = session,
            focusViewModel = focusViewModel,
            onDismiss = { editingSession = null },
        )
    }
}
