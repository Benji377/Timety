package io.github.benji377.timety.ui.components.focus

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.benji377.timety.R
import io.github.benji377.timety.data.model.focus.DistractionUIType
import io.github.benji377.timety.data.model.focus.FocusTagEntity
import io.github.benji377.timety.data.model.focus.FocusTargetSelection
import io.github.benji377.timety.data.model.focus.FocusTargetType
import io.github.benji377.timety.data.model.focus.icon
import io.github.benji377.timety.data.model.goal.GoalWithEntries
import io.github.benji377.timety.data.model.habit.HabitEntity
import io.github.benji377.timety.data.model.habit.HabitFrequency
import io.github.benji377.timety.data.model.habit.HabitWithCompletions
import io.github.benji377.timety.data.model.task.RecurringTaskWithOccurrences
import io.github.benji377.timety.data.model.task.TaskWithSubtasks
import io.github.benji377.timety.ui.components.common.NeoTabRow
import io.github.benji377.timety.ui.theme.AppTheme
import io.github.benji377.timety.ui.theme.SuccessColor
import io.github.benji377.timety.ui.theme.TaskColor
import io.github.benji377.timety.ui.utils.LocalDateFormatSettings
import io.github.benji377.timety.ui.viewmodel.AppViewModelProvider
import io.github.benji377.timety.ui.viewmodel.FocusViewModel
import io.github.benji377.timety.ui.viewmodel.GoalViewModel
import io.github.benji377.timety.ui.viewmodel.HabitViewModel
import io.github.benji377.timety.ui.viewmodel.RecurringTaskViewModel
import io.github.benji377.timety.ui.viewmodel.TaskViewModel
import io.github.benji377.timety.ui.viewmodel.activityScopedViewModel
import io.github.benji377.timety.util.datetime.AppDateFormatUtils
import io.github.benji377.timety.util.habit.HabitIcons
import io.github.benji377.timety.util.habit.HabitUtils
import java.time.LocalDate


/** Bottom sheet listing distraction event types; invokes [onEventSelected] when one is tapped. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DistractionBottomSheet(
    onDismissRequest: () -> Unit,
    onEventSelected: (DistractionUIType) -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(),
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.distractionSheetTitle),
                fontSize = AppTheme.fsHeadingSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(AppTheme.spaceLarge),
            )

            DistractionUIType.entries.forEach { type ->
                ListItem(
                    headlineContent = {
                        Text(
                            type.getLocalizedName(),
                            fontWeight = FontWeight.Medium
                        )
                    },
                    leadingContent = {
                        Icon(
                            type.icon,
                            contentDescription = null,
                            tint = type.color
                        )
                    },
                    modifier = Modifier.clickable { onEventSelected(type) },
                )
            }
        }
    }
}


/** Tabs of [TargetSelectorBottomSheet]; recurring tasks share the Tasks tab with one-off tasks. */
private enum class TargetTab(val labelRes: Int) {
    TAGS(R.string.globalLabelTags),
    TASKS(R.string.globalLabelTasks),
    HABITS(R.string.globalLabelHabits),
    GOALS(R.string.globalLabelGoals);

    companion object {
        fun of(type: FocusTargetType?): TargetTab = when (type) {
            null, FocusTargetType.TAG -> TAGS
            FocusTargetType.TASK, FocusTargetType.RECURRING_TASK -> TASKS
            FocusTargetType.HABIT -> HABITS
            FocusTargetType.GOAL -> GOALS
        }
    }
}

/**
 * Bottom sheet for picking the target of a focus session: a tag, a task or recurring task, a
 * habit, or an active goal, presented as tabs. Opens on the tab of [selected]. Passing null for
 * [onCreateNewTag] hides the "add tag" row.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TargetSelectorBottomSheet(
    onDismissRequest: () -> Unit,
    selected: FocusTargetSelection?,
    onTargetSelected: (FocusTargetSelection) -> Unit,
    onCreateNewTag: (() -> Unit)? = null,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    focusViewModel: FocusViewModel = activityScopedViewModel(),
    taskViewModel: TaskViewModel = viewModel(factory = AppViewModelProvider.Factory),
    recurringTaskViewModel: RecurringTaskViewModel = viewModel(factory = AppViewModelProvider.Factory),
    habitViewModel: HabitViewModel = viewModel(factory = AppViewModelProvider.Factory),
    goalViewModel: GoalViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val tags by focusViewModel.allTags.collectAsState()
    val tasks by taskViewModel.allTasks.collectAsState()
    val recurringTasks by recurringTaskViewModel.allRecurringTasks.collectAsState()
    val habitsWithCompletions by habitViewModel.habitsWithCompletions.collectAsState()
    val goals by goalViewModel.goalsWithEntries.collectAsState()
    var tab by remember { mutableStateOf(TargetTab.of(selected?.type)) }

    val select: (FocusTargetSelection) -> Unit = {
        onTargetSelected(it)
        onDismissRequest()
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    ) {
        Box(modifier = Modifier.fillMaxHeight(0.9f)) {
            Column(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = stringResource(R.string.targetSheetTitle),
                    fontSize = AppTheme.fsHeadingSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(
                        start = AppTheme.spaceLarge,
                        end = AppTheme.spaceLarge,
                        bottom = AppTheme.spaceMedium
                    ),
                )
                NeoTabRow(
                    tabs = TargetTab.entries.map { stringResource(it.labelRes) },
                    selectedIndex = tab.ordinal,
                    onTabSelected = { tab = TargetTab.entries[it] }
                )
                Box(modifier = Modifier.fillMaxSize()) {
                    when (tab) {
                        TargetTab.TAGS -> TagTab(
                            tags,
                            selected,
                            { select(FocusTargetSelection.tag(it)) },
                            onCreateNewTag?.let { create ->
                                {
                                    onDismissRequest()
                                    create()
                                }
                            },
                        )

                        TargetTab.TASKS -> TaskTab(recurringTasks, tasks, selected, select)
                        TargetTab.HABITS -> HabitTab(habitsWithCompletions, selected) {
                            select(FocusTargetSelection.of(FocusTargetType.HABIT, it.id, it.name))
                        }

                        TargetTab.GOALS -> GoalTab(goals, selected, select)
                    }
                }
            }
        }
    }
}

private fun FocusTargetSelection?.isTarget(type: FocusTargetType, id: String) =
    this?.type == type && this.id == id

/** Trailing check mark for the selected row. */
@Composable
private fun SelectedMark(isSelected: Boolean) {
    if (isSelected) Icon(Icons.Filled.Check, contentDescription = null, tint = SuccessColor)
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        fontSize = AppTheme.fsCaption,
        fontWeight = FontWeight.Bold,
        letterSpacing = AppTheme.lsWide,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(
            start = AppTheme.spaceLarge,
            end = AppTheme.spaceLarge,
            top = AppTheme.spaceLarge,
            bottom = AppTheme.spaceSmall,
        ),
    )
}

@Composable
private fun TagTab(
    tags: List<FocusTagEntity>,
    selected: FocusTargetSelection?,
    onTagSelected: (FocusTagEntity) -> Unit,
    onCreateNewTag: (() -> Unit)?,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (tags.isEmpty()) {
            EmptyState(stringResource(R.string.focusTagsLabelEmpty), Modifier.weight(1f))
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(tags, key = { it.id }) { tag ->
                    val isSelected = selected.isTarget(FocusTargetType.TAG, tag.id)
                    ListItem(
                        leadingContent = {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(tag.colorValue)),
                            )
                        },
                        headlineContent = {
                            Text(
                                tag.name,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        trailingContent = { SelectedMark(isSelected) },
                        modifier = Modifier.clickable { onTagSelected(tag) },
                    )
                    HorizontalDivider()
                }
            }
        }
        if (onCreateNewTag != null) {
            HorizontalDivider()
            ListItem(
                leadingContent = { Icon(Icons.Filled.AddCircleOutline, contentDescription = null) },
                headlineContent = { Text(stringResource(R.string.focusTagsLabelAdd)) },
                modifier = Modifier.clickable { onCreateNewTag() },
            )
        }
    }
}

@Composable
private fun TaskTab(
    recurringTasks: List<RecurringTaskWithOccurrences>,
    tasks: List<TaskWithSubtasks>,
    selected: FocusTargetSelection?,
    onSelected: (FocusTargetSelection) -> Unit,
) {
    if (recurringTasks.isEmpty() && tasks.isEmpty()) {
        EmptyState(stringResource(R.string.taskSheetEmpty), Modifier.fillMaxSize())
        return
    }
    val dateFmt = LocalDateFormatSettings.current
    val sortedRecurring = remember(recurringTasks) { recurringTasks.sortedBy { it.task.dueDate } }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        if (sortedRecurring.isNotEmpty()) {
            item { SectionLabel(stringResource(R.string.recurringTasksTitle)) }
            items(sortedRecurring, key = { "recurring-${it.task.id}" }) { entry ->
                val task = entry.task
                val isSelected = selected.isTarget(FocusTargetType.RECURRING_TASK, task.id)
                ListItem(
                    leadingContent = {
                        Icon(
                            FocusTargetType.RECURRING_TASK.icon,
                            contentDescription = null,
                            tint = TaskColor,
                        )
                    },
                    headlineContent = {
                        Text(
                            task.title,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        )
                    },
                    supportingContent = {
                        Text(
                            stringResource(
                                R.string.targetSheetRecurringNextDue,
                                AppDateFormatUtils.formatDate(task.dueDate, dateFmt.dateFormatCode)
                            )
                        )
                    },
                    trailingContent = { SelectedMark(isSelected) },
                    modifier = Modifier.clickable {
                        onSelected(
                            FocusTargetSelection.of(
                                FocusTargetType.RECURRING_TASK, task.id, task.title
                            )
                        )
                    },
                )
                HorizontalDivider()
            }
            if (tasks.isNotEmpty()) {
                item { SectionLabel(stringResource(R.string.globalLabelTasks)) }
            }
        }
        items(tasks, key = { it.task.id }) { taskWithSubtasks ->
            val task = taskWithSubtasks.task
            val isSelected = selected.isTarget(FocusTargetType.TASK, task.id)
            ListItem(
                leadingContent = {
                    Icon(
                        if (task.isCompleted) Icons.Filled.CheckCircle else Icons.Filled.TaskAlt,
                        contentDescription = null,
                        tint = if (task.isCompleted) SuccessColor else TaskColor,
                    )
                },
                headlineContent = {
                    Text(
                        task.title,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                    )
                },
                supportingContent = {
                    Text(
                        task.category.ifBlank {
                            if (task.isCompleted) stringResource(R.string.taskLabelCompleted) else stringResource(
                                R.string.globalLabelTask
                            )
                        },
                    )
                },
                trailingContent = { SelectedMark(isSelected) },
                modifier = Modifier.clickable {
                    onSelected(FocusTargetSelection.of(FocusTargetType.TASK, task.id, task.title))
                },
            )
            HorizontalDivider()
        }
    }
}

/** Active goals only: a completed goal is done, so there is nothing left to focus on. */
@Composable
private fun GoalTab(
    goals: List<GoalWithEntries>,
    selected: FocusTargetSelection?,
    onSelected: (FocusTargetSelection) -> Unit,
) {
    val activeGoals = remember(goals) {
        goals.filter { it.goal.completedAt == null }.sortedBy { it.goal.targetDate }
    }
    if (activeGoals.isEmpty()) {
        EmptyState(stringResource(R.string.targetSheetGoalsEmpty), Modifier.fillMaxSize())
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(activeGoals, key = { it.goal.id }) { entry ->
            val goal = entry.goal
            val isSelected = selected.isTarget(FocusTargetType.GOAL, goal.id)
            ListItem(
                leadingContent = {
                    Icon(
                        HabitIcons.iconAt(goal.iconCodePoint),
                        contentDescription = null,
                        tint = Color(goal.colorValue),
                    )
                },
                headlineContent = {
                    Text(
                        goal.name,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    )
                },
                supportingContent = {
                    Text(
                        stringResource(
                            R.string.targetSheetGoalProgress,
                            entry.entries.sumOf { it.value },
                            goal.targetValue,
                            goal.unitLabel
                        )
                    )
                },
                trailingContent = { SelectedMark(isSelected) },
                modifier = Modifier.clickable {
                    onSelected(FocusTargetSelection.of(FocusTargetType.GOAL, goal.id, goal.name))
                },
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun HabitTab(
    habitsWithCompletions: List<HabitWithCompletions>,
    selected: FocusTargetSelection?,
    onHabitSelected: (HabitEntity) -> Unit,
) {
    if (habitsWithCompletions.isEmpty()) {
        EmptyState(stringResource(R.string.habitsSheetEmpty), Modifier.fillMaxSize())
        return
    }

    val grouped = LinkedHashMap<String, MutableList<HabitWithCompletions>>()
    val standalone = mutableListOf<HabitWithCompletions>()
    habitsWithCompletions.forEach { hwc ->
        val stackName = hwc.habit.stackName?.trim()
        if (!stackName.isNullOrEmpty()) {
            grouped.getOrPut(stackName) { mutableListOf() }.add(hwc)
        } else {
            standalone.add(hwc)
        }
    }
    val today = LocalDate.now()

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        grouped.forEach { (stackName, stackHabits) ->
            val sorted = stackHabits.sortedBy { it.habit.stackOrder ?: 99 }
            item { SectionLabel(stackName) }
            itemsIndexed(sorted) { index, hwc ->
                val isDone = HabitUtils.isCompletedOn(hwc, today)
                val isPrevDone =
                    if (index > 0) HabitUtils.isCompletedOn(sorted[index - 1], today) else true
                val isLocked = HabitUtils.isHabitLocked(index, isDone, isPrevDone)
                HabitRow(hwc, isLocked, selected, onHabitSelected)
            }
            item { HorizontalDivider() }
        }
        items(standalone, key = { it.habit.id }) { hwc ->
            HabitRow(hwc, isLocked = false, selected = selected, onHabitSelected = onHabitSelected)
        }
    }
}

@Composable
private fun HabitRow(
    hwc: HabitWithCompletions,
    isLocked: Boolean,
    selected: FocusTargetSelection?,
    onHabitSelected: (HabitEntity) -> Unit,
) {
    val habit = hwc.habit
    val isSelected = selected.isTarget(FocusTargetType.HABIT, habit.id)
    val statusText = when (habit.frequency) {
        HabitFrequency.DAILY -> stringResource(R.string.habitLabelFreqDaily)
        HabitFrequency.WEEKLY_EXACT, HabitFrequency.WEEKLY_FLEXIBLE ->
            stringResource(R.string.habitLabelFreqWeekly)

        HabitFrequency.MONTHLY_FLEXIBLE -> stringResource(R.string.habitLabelFreqMonthly)
    }

    ListItem(
        leadingContent = {
            Icon(
                if (isLocked) Icons.Outlined.Lock else HabitIcons.iconAt(habit.iconCodePoint),
                contentDescription = null,
                tint = if (isLocked) MaterialTheme.colorScheme.onSurfaceVariant else Color(habit.colorValue),
            )
        },
        headlineContent = {
            Text(
                habit.name,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isLocked) MaterialTheme.colorScheme.onSurfaceVariant else Color.Unspecified,
            )
        },
        supportingContent = {
            Text(
                statusText,
                color = if (isLocked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent = {
            when {
                isLocked -> Icon(
                    Icons.Filled.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )

                isSelected -> SelectedMark(true)
            }
        },
        modifier = if (isLocked) {
            Modifier
        } else {
            Modifier.clickable { onHabitSelected(habit) }
        },
    )
}

@Composable
private fun EmptyState(text: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(text)
    }
}
