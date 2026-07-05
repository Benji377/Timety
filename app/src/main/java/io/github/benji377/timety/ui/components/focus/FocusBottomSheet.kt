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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import io.github.benji377.timety.R
import io.github.benji377.timety.data.model.focus.FocusTagEntity
import io.github.benji377.timety.data.model.focus.FocusTargetType
import io.github.benji377.timety.data.model.habit.HabitEntity
import io.github.benji377.timety.data.model.habit.HabitFrequency
import io.github.benji377.timety.data.model.habit.HabitWithCompletions
import io.github.benji377.timety.data.model.task.TaskEntity
import io.github.benji377.timety.data.model.task.TaskWithSubtasks
import io.github.benji377.timety.ui.theme.AppTheme
import io.github.benji377.timety.ui.theme.SuccessColor
import io.github.benji377.timety.ui.theme.TaskColor
import io.github.benji377.timety.util.habit.HabitIcons
import io.github.benji377.timety.util.habit.HabitUtils
import java.time.LocalDate


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DistractionBottomSheet(
    onDismissRequest: () -> Unit,
    onEventSelected: (io.github.benji377.timety.data.model.focus.DistractionUIType) -> Unit,
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

            io.github.benji377.timety.data.model.focus.DistractionUIType.entries.forEach { type ->
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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TargetSelectorBottomSheet(
    onDismissRequest: () -> Unit,
    tags: List<FocusTagEntity>,
    tasks: List<TaskWithSubtasks>,
    habitsWithCompletions: List<HabitWithCompletions>,
    selectedType: FocusTargetType,
    selectedId: String?,
    onTagSelected: (FocusTagEntity) -> Unit,
    onTaskSelected: (TaskEntity) -> Unit,
    onHabitSelected: (HabitEntity) -> Unit,
    onCreateNewTag: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
) {
    var tabIndex by remember { mutableIntStateOf(0) }

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
                TabRow(selectedTabIndex = tabIndex) {
                    Tab(
                        selected = tabIndex == 0,
                        onClick = { tabIndex = 0 },
                        text = { Text(stringResource(R.string.globalLabelTags)) })
                    Tab(
                        selected = tabIndex == 1,
                        onClick = { tabIndex = 1 },
                        text = { Text(stringResource(R.string.globalLabelTasks)) })
                    Tab(
                        selected = tabIndex == 2,
                        onClick = { tabIndex = 2 },
                        text = { Text(stringResource(R.string.globalLabelHabits)) })
                }
                Box(modifier = Modifier.fillMaxSize()) {
                    when (tabIndex) {
                        0 -> TagTab(
                            tags,
                            selectedType,
                            selectedId,
                            onTagSelected,
                            onCreateNewTag,
                            onDismissRequest
                        )

                        1 -> TaskTab(
                            tasks,
                            selectedType,
                            selectedId,
                            onTaskSelected,
                            onDismissRequest
                        )

                        else -> HabitTab(
                            habitsWithCompletions,
                            selectedType,
                            selectedId,
                            onHabitSelected,
                            onDismissRequest
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TagTab(
    tags: List<FocusTagEntity>,
    selectedType: FocusTargetType,
    selectedId: String?,
    onTagSelected: (FocusTagEntity) -> Unit,
    onCreateNewTag: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (tags.isEmpty()) {
            EmptyState(stringResource(R.string.focusTagsLabelEmpty), Modifier.weight(1f))
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(tags, key = { it.id }) { tag ->
                    val isSelected = selectedType == FocusTargetType.TAG && selectedId == tag.id
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
                        trailingContent = {
                            if (isSelected) Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                tint = SuccessColor
                            )
                        },
                        modifier = Modifier.clickable {
                            onTagSelected(tag)
                            onDismissRequest()
                        },
                    )
                    HorizontalDivider()
                }
            }
        }
        HorizontalDivider()
        ListItem(
            leadingContent = { Icon(Icons.Filled.AddCircleOutline, contentDescription = null) },
            headlineContent = { Text(stringResource(R.string.focusTagsLabelAdd)) },
            modifier = Modifier.clickable {
                onDismissRequest()
                onCreateNewTag()
            },
        )
    }
}

@Composable
private fun TaskTab(
    tasks: List<TaskWithSubtasks>,
    selectedType: FocusTargetType,
    selectedId: String?,
    onTaskSelected: (TaskEntity) -> Unit,
    onDismissRequest: () -> Unit,
) {
    if (tasks.isEmpty()) {
        EmptyState(stringResource(R.string.taskSheetEmpty), Modifier.fillMaxSize())
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(tasks, key = { it.task.id }) { taskWithSubtasks ->
            val task = taskWithSubtasks.task
            val isSelected = selectedType == FocusTargetType.TASK && selectedId == task.id
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
                trailingContent = {
                    if (isSelected) Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = SuccessColor
                    )
                },
                modifier = Modifier.clickable {
                    onTaskSelected(task)
                    onDismissRequest()
                },
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun HabitTab(
    habitsWithCompletions: List<HabitWithCompletions>,
    selectedType: FocusTargetType,
    selectedId: String?,
    onHabitSelected: (HabitEntity) -> Unit,
    onDismissRequest: () -> Unit,
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
            item {
                Text(
                    text = stackName.uppercase(),
                    fontSize = AppTheme.fsCaption,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = AppTheme.lsWide,
                    color = Color.Gray,
                    modifier = Modifier.padding(
                        start = AppTheme.spaceLarge,
                        end = AppTheme.spaceLarge,
                        top = AppTheme.spaceLarge,
                        bottom = AppTheme.spaceSmall,
                    ),
                )
            }
            itemsIndexed(sorted) { index, hwc ->
                val isDone = HabitUtils.isCompletedOn(hwc, today)
                val isPrevDone =
                    if (index > 0) HabitUtils.isCompletedOn(sorted[index - 1], today) else true
                val isLocked = HabitUtils.isHabitLocked(index, isDone, isPrevDone)
                HabitRow(hwc, isLocked, selectedType, selectedId, onHabitSelected, onDismissRequest)
            }
            item { HorizontalDivider() }
        }
        items(standalone, key = { it.habit.id }) { hwc ->
            HabitRow(
                hwc,
                isLocked = false,
                selectedType = selectedType,
                selectedId = selectedId,
                onHabitSelected = onHabitSelected,
                onDismissRequest = onDismissRequest
            )
        }
    }
}

@Composable
private fun HabitRow(
    hwc: HabitWithCompletions,
    isLocked: Boolean,
    selectedType: FocusTargetType,
    selectedId: String?,
    onHabitSelected: (HabitEntity) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val habit = hwc.habit
    val isSelected = selectedType == FocusTargetType.HABIT && selectedId == habit.id
    val statusText = if (habit.frequency == HabitFrequency.DAILY) {
        stringResource(R.string.habitLabelFreqDaily)
    } else {
        stringResource(R.string.habitLabelFreqWeekly)
    }

    ListItem(
        leadingContent = {
            Icon(
                if (isLocked) Icons.Outlined.Lock else HabitIcons.iconAt(habit.iconCodePoint),
                contentDescription = null,
                tint = if (isLocked) Color.Gray else Color(habit.colorValue),
            )
        },
        headlineContent = {
            Text(
                habit.name,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isLocked) Color.Gray else Color.Unspecified,
            )
        },
        supportingContent = {
            Text(
                statusText,
                color = if (isLocked) Color.Gray else MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent = {
            when {
                isLocked -> Icon(Icons.Filled.Lock, contentDescription = null, tint = Color.Gray)
                isSelected -> Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = SuccessColor
                )
            }
        },
        modifier = if (isLocked) {
            Modifier
        } else {
            Modifier.clickable {
                onHabitSelected(habit)
                onDismissRequest()
            }
        },
    )
}

@Composable
private fun EmptyState(text: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(text)
    }
}
