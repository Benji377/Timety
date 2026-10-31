package io.github.benji377.timety.ui.screens.task

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.benji377.timety.R
import io.github.benji377.timety.data.model.tag.TagEntity
import io.github.benji377.timety.data.model.task.RecurringTaskEntity
import io.github.benji377.timety.data.model.task.TaskEntity
import io.github.benji377.timety.data.model.task.TaskSortOption
import io.github.benji377.timety.data.model.task.TaskWithSubtasks
import io.github.benji377.timety.data.repository.AccordionKey
import io.github.benji377.timety.ui.components.common.ExpansionSection
import io.github.benji377.timety.ui.components.common.NeoFab
import io.github.benji377.timety.ui.components.common.NeoIconButton
import io.github.benji377.timety.ui.components.common.NeoOutlinedTextField
import io.github.benji377.timety.ui.components.common.NeoTopBar
import io.github.benji377.timety.ui.components.common.TagFilterRow
import io.github.benji377.timety.ui.components.task.RecurringTaskListTile
import io.github.benji377.timety.ui.components.task.TaskListTile
import io.github.benji377.timety.ui.components.task.rememberRecurringCompleter
import io.github.benji377.timety.ui.components.task.rememberTaskCompletionToggle
import io.github.benji377.timety.ui.theme.AppTheme
import io.github.benji377.timety.ui.theme.ErrorColor
import io.github.benji377.timety.ui.theme.LocalSnackbarHostState
import io.github.benji377.timety.ui.theme.SuccessColor
import io.github.benji377.timety.ui.theme.TaskColor
import io.github.benji377.timety.ui.theme.WarningColor
import io.github.benji377.timety.ui.viewmodel.AppViewModelProvider
import io.github.benji377.timety.ui.viewmodel.RecurringTaskViewModel
import io.github.benji377.timety.ui.viewmodel.SettingsViewModel
import io.github.benji377.timety.ui.viewmodel.TagViewModel
import io.github.benji377.timety.ui.viewmodel.TaskViewModel
import io.github.benji377.timety.ui.viewmodel.activityScopedViewModel
import io.github.benji377.timety.util.tag.TagFilter
import io.github.benji377.timety.util.task.RecurrenceUtils
import io.github.benji377.timety.util.task.RecurringStatus
import io.github.benji377.timety.util.task.TaskFilterEngine
import kotlinx.coroutines.flow.drop
import java.time.Instant
import java.time.ZoneId

/**
 * Shows all tasks grouped into overdue, due-today, upcoming, and completed sections, with search,
 * tag filtering, and sorting controls.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    viewModel: TaskViewModel = viewModel(factory = AppViewModelProvider.Factory),
    recurringViewModel: RecurringTaskViewModel = viewModel(factory = AppViewModelProvider.Factory),
    settingsViewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory),
    onNavigateToTaskDetail: (String?) -> Unit,
    onNavigateToRecurring: () -> Unit = {},
    onNavigateToRecurringDetail: (String) -> Unit = {},
    tagViewModel: TagViewModel = activityScopedViewModel(),
) {
    val tasks by viewModel.allTasks.collectAsState()
    val taskTags by tagViewModel.taskTags.collectAsState()
    val recurringTags by tagViewModel.recurringTaskTags.collectAsState()
    val recurringItems by recurringViewModel.allRecurringTasks.collectAsState()
    val horizonDays by settingsViewModel.upcomingTasksHorizon.collectAsState()
    val completeRecurring =
        rememberRecurringCompleter(recurringViewModel, LocalSnackbarHostState.current)
    val toggleTaskCompletion = rememberTaskCompletionToggle(viewModel)
    val rowActions = TaskRowActions(
        onToggleCompleted = toggleTaskCompletion,
        onOpenTask = onNavigateToTaskDetail,
        onDeleteTask = viewModel::deleteTask,
        onCompleteRecurring = completeRecurring,
        onOpenRecurring = onNavigateToRecurringDetail,
    )

    val isOverdueExpanded by settingsViewModel.accordionExpanded(AccordionKey.TASKS_OVERDUE)
        .collectAsState()
    val isDueTodayExpanded by settingsViewModel.accordionExpanded(AccordionKey.TASKS_DUE_TODAY)
        .collectAsState()
    val isUpcomingExpanded by settingsViewModel.accordionExpanded(AccordionKey.TASKS_UPCOMING)
        .collectAsState()
    val isDoneExpanded by settingsViewModel.accordionExpanded(AccordionKey.TASKS_DONE)
        .collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedTagIds by remember { mutableStateOf(emptySet<String>()) }
    var sortOption by remember { mutableStateOf(TaskSortOption.DUE_DATE) }
    var isAscending by remember { mutableStateOf(true) }
    var sortMenuExpanded by remember { mutableStateOf(false) }

    // A changed result starts from the top. Otherwise the list stays anchored on its first visible
    // row, leaving sections that reappear above it (like Overdue) scrolled out of view.
    val listState = rememberLazyListState()
    LaunchedEffect(listState) {
        snapshotFlow { selectedTagIds to searchQuery }
            .drop(1)
            .collect { listState.requestScrollToItem(0) }
    }

    // Recurring tasks join the list only while actionable (overdue, due today, or within the
    // horizon) and are never shown as done: completing one just moves its due date forward.
    val actionableRecurring = remember(recurringItems, horizonDays) {
        val now = Instant.now()
        recurringItems.map { it.task }
            .sortedBy { it.dueDate }
            .groupBy { RecurrenceUtils.statusOf(it, now, horizonDays) }
            .filterKeys { it != RecurringStatus.SCHEDULED }
    }

    // Pills come from what this list can show, so each one matches something on its own.
    val usedTags = remember(tasks, actionableRecurring, taskTags, recurringTags) {
        (TagFilter.usedTags(tasks.map { it.task.id }, taskTags) +
                TagFilter.usedTags(actionableRecurring.values.flatten().map { it.id }, recurringTags))
            .distinctBy { it.id }
            .sortedBy { it.name.lowercase() }
    }
    val activeTagIds = TagFilter.activeIds(selectedTagIds, usedTags)

    val filteredRecurring = remember(actionableRecurring, searchQuery, activeTagIds, recurringTags) {
        actionableRecurring.mapValues { (_, grouped) ->
            grouped.filter { task ->
                TagFilter.matches(recurringTags[task.id].orEmpty(), activeTagIds) &&
                        (searchQuery.isEmpty() ||
                                task.title.lowercase().contains(searchQuery.lowercase()) ||
                                task.description.lowercase().contains(searchQuery.lowercase()))
            }
        }
    }
    val processedTasks =
        remember(tasks, searchQuery, activeTagIds, taskTags, sortOption, isAscending) {
            val engine = TaskFilterEngine(
                searchQuery = searchQuery,
                sortOption = sortOption,
                isAscending = isAscending
            )
            engine.process(TagFilter.filter(tasks, activeTagIds, taskTags) { it.task.id }) { it.task }
        }

    Scaffold(
        topBar = {
            NeoTopBar(
                title = stringResource(R.string.taskListTitle),
                actions = {
                    // Bordered tap target instead of a bare floating icon, so it carries the
                    // same container weight as a text button.
                    NeoIconButton(
                        onClick = onNavigateToRecurring,
                        icon = Icons.Filled.Repeat,
                        contentDescription = stringResource(R.string.recurringTasksTitle),
                        modifier = Modifier.padding(end = AppTheme.spaceSmall),
                    )
                }
            )
        },
        floatingActionButton = {
            NeoFab(onClick = { onNavigateToTaskDetail(null) }, containerColor = TaskColor)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search field and sort controls.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = AppTheme.spaceSmall,
                        top = AppTheme.spaceSmall,
                        end = AppTheme.spaceSmall,
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NeoOutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(stringResource(R.string.taskListSearchHint)) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    shape = AppTheme.brNeo
                )
                Spacer(modifier = Modifier.width(AppTheme.spaceSmall))

                // Sort Dropdown. Bordered tap target (NeoIconButton) instead of a bare floating
                // icon.
                Box {
                    NeoIconButton(
                        onClick = { sortMenuExpanded = true },
                        icon = Icons.AutoMirrored.Filled.Sort,
                        contentDescription = stringResource(R.string.taskListTooltipSort),
                    )
                    DropdownMenu(
                        expanded = sortMenuExpanded,
                        onDismissRequest = { sortMenuExpanded = false }
                    ) {
                        TaskSortOption.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(sortOptionLabel(option)) },
                                onClick = {
                                    sortOption = option
                                    sortMenuExpanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(AppTheme.spaceSmall))

                // Order Toggle (Asc/Desc)
                NeoIconButton(
                    onClick = { isAscending = !isAscending },
                    icon = if (isAscending) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                    contentDescription = if (isAscending) stringResource(R.string.taskListSortAscending)
                    else stringResource(R.string.taskListSortDescending),
                )
            }

            // Tag filter pills.
            TagFilterRow(
                tags = usedTags,
                selectedIds = activeTagIds,
                onSelectionChange = { selectedTagIds = it },
                modifier = Modifier.padding(vertical = AppTheme.spaceSmall),
            )

            // Scrollable task list.
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                val filteredRecurringCount = filteredRecurring.values.sumOf { it.size }
                if (tasks.isEmpty() && actionableRecurring.values.all { it.isEmpty() }) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            stringResource(R.string.taskListEmpty),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else if (processedTasks.isEmpty() && filteredRecurringCount == 0) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            stringResource(R.string.taskListFilterNoMatch),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    // Group tasks into overdue, due-today, upcoming, and completed buckets.
                    val buckets = remember(processedTasks) { TaskBuckets.of(processedTasks) }
                    val recurringOverdue = filteredRecurring[RecurringStatus.OVERDUE].orEmpty()
                    val recurringToday = filteredRecurring[RecurringStatus.DUE_TODAY].orEmpty()
                    val recurringUpcoming = filteredRecurring[RecurringStatus.UPCOMING].orEmpty()

                    val overdueTitle = "${stringResource(R.string.taskListSectionOverdue)} (${buckets.overdue.size + recurringOverdue.size})"
                    val todayTitle = "${stringResource(R.string.taskListSectionToday)} (${buckets.dueToday.size + recurringToday.size})"
                    val upcomingTitle = "${stringResource(R.string.taskListSectionUpcoming)} (${buckets.todo.size + recurringUpcoming.size})"
                    val doneTitle = "${stringResource(R.string.taskListSectionDone)} (${buckets.done.size})"

                    // Rows are lazy items rather than a Column inside one item, so an expanded
                    // section with hundreds of completed tasks only composes what's on screen.
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        if (buckets.overdue.isNotEmpty() || recurringOverdue.isNotEmpty()) {
                            taskSection(
                                key = "overdue",
                                title = overdueTitle,
                                color = ErrorColor,
                                expanded = isOverdueExpanded,
                                onExpandedChange = {
                                    settingsViewModel.setAccordionExpanded(AccordionKey.TASKS_OVERDUE, it)
                                },
                                tasks = buckets.overdue,
                                isOverdue = true,
                                actions = rowActions,
                                taskTags = taskTags,
                                recurringTags = recurringTags,
                                recurringTasks = recurringOverdue,
                                recurringStatus = RecurringStatus.OVERDUE,
                            )
                        }
                        if (buckets.dueToday.isNotEmpty() || recurringToday.isNotEmpty()) {
                            taskSection(
                                key = "today",
                                title = todayTitle,
                                color = WarningColor,
                                expanded = isDueTodayExpanded,
                                onExpandedChange = {
                                    settingsViewModel.setAccordionExpanded(AccordionKey.TASKS_DUE_TODAY, it)
                                },
                                tasks = buckets.dueToday,
                                actions = rowActions,
                                taskTags = taskTags,
                                recurringTags = recurringTags,
                                recurringTasks = recurringToday,
                                recurringStatus = RecurringStatus.DUE_TODAY,
                            )
                        }
                        if (buckets.todo.isNotEmpty() || recurringUpcoming.isNotEmpty()) {
                            taskSection(
                                key = "upcoming",
                                title = upcomingTitle,
                                color = TaskColor,
                                expanded = isUpcomingExpanded,
                                onExpandedChange = {
                                    settingsViewModel.setAccordionExpanded(AccordionKey.TASKS_UPCOMING, it)
                                },
                                tasks = buckets.todo,
                                actions = rowActions,
                                taskTags = taskTags,
                                recurringTags = recurringTags,
                                recurringTasks = recurringUpcoming,
                                recurringStatus = RecurringStatus.UPCOMING,
                            )
                        }
                        if (buckets.done.isNotEmpty()) {
                            taskSection(
                                key = "done",
                                title = doneTitle,
                                color = SuccessColor,
                                expanded = isDoneExpanded,
                                onExpandedChange = {
                                    settingsViewModel.setAccordionExpanded(AccordionKey.TASKS_DONE, it)
                                },
                                tasks = buckets.done,
                                actions = rowActions,
                                taskTags = taskTags,
                                recurringTags = recurringTags,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
@ReadOnlyComposable
private fun sortOptionLabel(option: TaskSortOption): String = when (option) {
    TaskSortOption.DUE_DATE -> stringResource(R.string.taskListSortDueDate)
    TaskSortOption.PRIORITY -> stringResource(R.string.taskListSortPriority)
    TaskSortOption.SIZE -> stringResource(R.string.taskListSortSize)
    TaskSortOption.ALPHABETICAL -> stringResource(R.string.taskListSortAlphabetical)
}

/** The task list's sections; completed tasks go to [done] whatever their due date. */
private data class TaskBuckets(
    val overdue: List<TaskWithSubtasks>,
    val dueToday: List<TaskWithSubtasks>,
    val todo: List<TaskWithSubtasks>,
    val done: List<TaskWithSubtasks>,
) {
    companion object {
        fun of(tasks: List<TaskWithSubtasks>): TaskBuckets {
            val zone = ZoneId.systemDefault()
            val now = Instant.now()
            val today = now.atZone(zone).toLocalDate()
            val overdue = mutableListOf<TaskWithSubtasks>()
            val dueToday = mutableListOf<TaskWithSubtasks>()
            val todo = mutableListOf<TaskWithSubtasks>()
            val done = mutableListOf<TaskWithSubtasks>()
            tasks.forEach { item ->
                val task = item.task
                when {
                    task.isCompleted -> done.add(item)
                    task.dueDate != null && task.dueDate.isBefore(now) -> overdue.add(item)
                    task.dueDate != null && task.dueDate.atZone(zone).toLocalDate() == today -> dueToday.add(item)
                    else -> todo.add(item)
                }
            }
            return TaskBuckets(overdue, dueToday, todo, done)
        }
    }
}

/** What tapping, checking or deleting a row does; the same in every section. */
private class TaskRowActions(
    val onToggleCompleted: (TaskEntity) -> Unit,
    val onOpenTask: (String?) -> Unit,
    val onDeleteTask: (TaskEntity) -> Unit,
    val onCompleteRecurring: (RecurringTaskEntity) -> Unit,
    val onOpenRecurring: (String) -> Unit,
)

/** One collapsible section: a header item, then a lazy item per row while expanded. */
private fun LazyListScope.taskSection(
    key: String,
    title: String,
    color: Color,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    tasks: List<TaskWithSubtasks>,
    actions: TaskRowActions,
    taskTags: Map<String, List<TagEntity>>,
    recurringTags: Map<String, List<TagEntity>>,
    isOverdue: Boolean = false,
    recurringTasks: List<RecurringTaskEntity> = emptyList(),
    recurringStatus: RecurringStatus = RecurringStatus.SCHEDULED,
) {
    item(key = "$key-header") {
        ExpansionSection(
            title = title,
            color = color,
            expanded = expanded,
            onExpandedChange = onExpandedChange,
            bottomSpacing = 0.dp,
        ) {}
    }
    if (expanded) {
        items(tasks, key = { it.task.id }) { taskWithSubtasks ->
            val taskEntity = taskWithSubtasks.task
            TaskListTile(
                task = taskEntity,
                isOverdue = isOverdue,
                subtasksCompleted = taskWithSubtasks.subtasks.count { it.isCompleted },
                subtasksTotal = taskWithSubtasks.subtasks.size,
                onToggleCompleted = { actions.onToggleCompleted(taskEntity) },
                onTap = { actions.onOpenTask(taskEntity.id) },
                onDelete = { actions.onDeleteTask(taskEntity) },
                tags = taskTags[taskEntity.id].orEmpty(),
                modifier = Modifier.animateItem(),
            )
        }
        items(recurringTasks, key = { "recurring-${it.id}" }) { recurringTask ->
            RecurringTaskListTile(
                task = recurringTask,
                status = recurringStatus,
                onComplete = { actions.onCompleteRecurring(recurringTask) },
                onTap = { actions.onOpenRecurring(recurringTask.id) },
                tags = recurringTags[recurringTask.id].orEmpty(),
                modifier = Modifier.animateItem(),
            )
        }
    }
    item(key = "$key-end") { Spacer(modifier = Modifier.height(SECTION_BOTTOM_SPACING)) }
}

private val SECTION_BOTTOM_SPACING = 12.dp
