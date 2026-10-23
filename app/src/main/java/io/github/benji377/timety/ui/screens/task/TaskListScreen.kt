package io.github.benji377.timety.ui.screens.task

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.benji377.timety.R
import io.github.benji377.timety.data.model.task.RecurringTaskEntity
import io.github.benji377.timety.data.model.task.TaskEntity
import io.github.benji377.timety.data.model.task.TaskSortOption
import io.github.benji377.timety.data.model.task.TaskWithSubtasks
import io.github.benji377.timety.data.repository.AccordionKey
import io.github.benji377.timety.ui.components.common.ExpansionSection
import io.github.benji377.timety.ui.components.common.NeoFab
import io.github.benji377.timety.ui.components.common.NeoFilterChip
import io.github.benji377.timety.ui.components.common.NeoIconButton
import io.github.benji377.timety.ui.components.common.NeoTopBar
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
import io.github.benji377.timety.ui.utils.AppUtils
import io.github.benji377.timety.ui.viewmodel.AppViewModelProvider
import io.github.benji377.timety.ui.viewmodel.RecurringTaskViewModel
import io.github.benji377.timety.ui.viewmodel.SettingsViewModel
import io.github.benji377.timety.ui.viewmodel.TaskViewModel
import io.github.benji377.timety.util.task.RecurrenceUtils
import io.github.benji377.timety.util.task.RecurringStatus
import io.github.benji377.timety.util.task.TaskFilterEngine
import java.time.Instant
import java.time.ZoneId
import io.github.benji377.timety.ui.components.common.NeoOutlinedTextField as OutlinedTextField


/**
 * Shows all tasks grouped into overdue, due-today, upcoming, and completed sections, with search,
 * category filtering, and sorting controls.
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
) {
    val tasks by viewModel.allTasks.collectAsState()
    val recurringItems by recurringViewModel.allRecurringTasks.collectAsState()
    val horizonDays by settingsViewModel.upcomingTasksHorizon.collectAsState()
    val completeRecurring =
        rememberRecurringCompleter(recurringViewModel, LocalSnackbarHostState.current)
    val toggleTaskCompletion = rememberTaskCompletionToggle(viewModel)

    val isOverdueExpanded by settingsViewModel.accordionExpanded(AccordionKey.TASKS_OVERDUE)
        .collectAsState()
    val isDueTodayExpanded by settingsViewModel.accordionExpanded(AccordionKey.TASKS_DUE_TODAY)
        .collectAsState()
    val isUpcomingExpanded by settingsViewModel.accordionExpanded(AccordionKey.TASKS_UPCOMING)
        .collectAsState()
    val isDoneExpanded by settingsViewModel.accordionExpanded(AccordionKey.TASKS_DONE)
        .collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf<String?>(null) }
    var sortOption by remember { mutableStateOf(TaskSortOption.DUE_DATE) }
    var isAscending by remember { mutableStateOf(true) }
    var sortMenuExpanded by remember { mutableStateOf(false) }

    // Filter pills come from the tasks themselves (not the task_categories table) so
    // only categories that actually match something are offered; untrimmed so each
    // pill matches TaskFilterEngine's exact category comparison.
    val allCategories = remember(tasks, recurringItems) {
        (tasks.map { it.task.category } + recurringItems.map { it.task.category })
            .filter { it.isNotBlank() }.distinct().sorted()
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
    val filteredRecurring = remember(actionableRecurring, searchQuery, selectedCategoryFilter) {
        actionableRecurring.mapValues { (_, grouped) ->
            grouped.filter { task ->
                (selectedCategoryFilter.isNullOrEmpty() || task.category == selectedCategoryFilter) &&
                        (searchQuery.isEmpty() ||
                                task.title.lowercase().contains(searchQuery.lowercase()) ||
                                task.description.lowercase().contains(searchQuery.lowercase()))
            }
        }
    }
    val categoryEntities by viewModel.allCategories.collectAsState()
    val categoryColors = remember(categoryEntities) {
        categoryEntities.associate { it.name to it.colorValue }
    }

    val processedTasks =
        remember(tasks, searchQuery, selectedCategoryFilter, sortOption, isAscending) {
            val engine = TaskFilterEngine(
                searchQuery = searchQuery,
                categoryFilter = selectedCategoryFilter,
                sortOption = sortOption,
                isAscending = isAscending
            )
            engine.process(tasks) { it.task }
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
                OutlinedTextField(
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

            // Category filter pills.
            if (allCategories.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(vertical = AppTheme.spaceSmall),
                    contentPadding = PaddingValues(horizontal = AppTheme.spaceSmall),
                    horizontalArrangement = Arrangement.spacedBy(AppTheme.spaceSmall)
                ) {
                    item {
                        NeoFilterChip(
                            selected = selectedCategoryFilter == null,
                            onClick = { selectedCategoryFilter = null },
                            label = stringResource(R.string.taskListFilterAll)
                        )
                    }
                    items(allCategories, key = { it }) { category ->
                        val isSelected = selectedCategoryFilter == category
                        NeoFilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedCategoryFilter = if (isSelected) null else category
                            },
                            label = category,
                            leadingIcon = {
                                categoryColors[category]?.let { AppUtils.CategoryDot(it) }
                            }
                        )
                    }
                }
            }

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
                                viewModel = viewModel,
                                onToggleCompleted = toggleTaskCompletion,
                                onNavigateToTaskDetail = onNavigateToTaskDetail,
                                recurringTasks = recurringOverdue,
                                recurringStatus = RecurringStatus.OVERDUE,
                                onCompleteRecurring = completeRecurring,
                                onNavigateToRecurringDetail = onNavigateToRecurringDetail,
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
                                isOverdue = false,
                                viewModel = viewModel,
                                onToggleCompleted = toggleTaskCompletion,
                                onNavigateToTaskDetail = onNavigateToTaskDetail,
                                recurringTasks = recurringToday,
                                recurringStatus = RecurringStatus.DUE_TODAY,
                                onCompleteRecurring = completeRecurring,
                                onNavigateToRecurringDetail = onNavigateToRecurringDetail,
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
                                isOverdue = false,
                                viewModel = viewModel,
                                onToggleCompleted = toggleTaskCompletion,
                                onNavigateToTaskDetail = onNavigateToTaskDetail,
                                recurringTasks = recurringUpcoming,
                                recurringStatus = RecurringStatus.UPCOMING,
                                onCompleteRecurring = completeRecurring,
                                onNavigateToRecurringDetail = onNavigateToRecurringDetail,
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
                                isOverdue = false,
                                viewModel = viewModel,
                                onToggleCompleted = toggleTaskCompletion,
                                onNavigateToTaskDetail = onNavigateToTaskDetail,
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
    TaskSortOption.CATEGORY -> stringResource(R.string.taskListSortCategory)
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

/** One collapsible section: a header item, then a lazy item per row while expanded. */
private fun LazyListScope.taskSection(
    key: String,
    title: String,
    color: Color,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    tasks: List<TaskWithSubtasks>,
    isOverdue: Boolean,
    viewModel: TaskViewModel,
    onToggleCompleted: (TaskEntity) -> Unit,
    onNavigateToTaskDetail: (String?) -> Unit,
    recurringTasks: List<RecurringTaskEntity> = emptyList(),
    recurringStatus: RecurringStatus = RecurringStatus.SCHEDULED,
    onCompleteRecurring: (RecurringTaskEntity) -> Unit = {},
    onNavigateToRecurringDetail: (String) -> Unit = {},
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
                onToggleCompleted = { onToggleCompleted(taskEntity) },
                onTap = { onNavigateToTaskDetail(taskEntity.id) },
                onDelete = { viewModel.deleteTask(taskEntity) },
                modifier = Modifier.animateItem(),
            )
        }
        items(recurringTasks, key = { "recurring-${it.id}" }) { recurringTask ->
            RecurringTaskListTile(
                task = recurringTask,
                status = recurringStatus,
                onComplete = { onCompleteRecurring(recurringTask) },
                onTap = { onNavigateToRecurringDetail(recurringTask.id) },
                modifier = Modifier.animateItem(),
            )
        }
    }
    item(key = "$key-end") { Spacer(modifier = Modifier.height(SECTION_BOTTOM_SPACING)) }
}

private val SECTION_BOTTOM_SPACING = 12.dp
