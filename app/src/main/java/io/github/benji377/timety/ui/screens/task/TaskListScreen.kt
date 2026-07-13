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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.benji377.timety.R
import io.github.benji377.timety.data.model.task.RecurringTaskEntity
import io.github.benji377.timety.data.model.task.TaskSortOption
import io.github.benji377.timety.data.model.task.TaskWithSubtasks
import io.github.benji377.timety.ui.components.common.ExpansionSection
import io.github.benji377.timety.ui.components.common.TimetyFab
import io.github.benji377.timety.ui.components.common.TimetyTopBar
import io.github.benji377.timety.ui.components.task.RecurringTaskListTile
import io.github.benji377.timety.ui.components.task.TaskListTile
import io.github.benji377.timety.ui.components.task.rememberRecurringCompleter
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
import io.github.benji377.timety.ui.components.common.TimetyOutlinedTextField as OutlinedTextField


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
            TimetyTopBar(
                title = stringResource(R.string.taskListTitle),
                actions = {
                    IconButton(onClick = onNavigateToRecurring) {
                        Icon(
                            imageVector = Icons.Filled.Repeat,
                            contentDescription = stringResource(R.string.recurringTasksTitle),
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            TimetyFab(onClick = { onNavigateToTaskDetail(null) }, containerColor = TaskColor)
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
                    .padding(start = 8.dp, top = 8.dp, end = 8.dp),
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
                Spacer(modifier = Modifier.width(8.dp))

                // Sort Dropdown
                Box {
                    IconButton(onClick = { sortMenuExpanded = true }) {
                        Icon(
                            Icons.AutoMirrored.Filled.Sort,
                            contentDescription = stringResource(R.string.taskListTooltipSort)
                        )
                    }
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

                // Order Toggle (Asc/Desc)
                IconButton(onClick = { isAscending = !isAscending }) {
                    Icon(
                        if (isAscending) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                        contentDescription = if (isAscending) stringResource(R.string.taskListSortAscending)
                        else stringResource(R.string.taskListSortDescending)
                    )
                }
            }

            // Category filter pills.
            if (allCategories.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(vertical = 8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategoryFilter == null,
                            onClick = { selectedCategoryFilter = null },
                            label = { Text(stringResource(R.string.taskListFilterAll)) }
                        )
                    }
                    items(allCategories, key = { it }) { category ->
                        val isSelected = selectedCategoryFilter == category
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedCategoryFilter = if (isSelected) null else category
                            },
                            label = { Text(category) },
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
                    val zone = ZoneId.systemDefault()
                    val now = Instant.now()
                    val today = now.atZone(zone).toLocalDate()

                    val overdue = mutableListOf<TaskWithSubtasks>()
                    val dueToday = mutableListOf<TaskWithSubtasks>()
                    val todo = mutableListOf<TaskWithSubtasks>()
                    val done = mutableListOf<TaskWithSubtasks>()

                    processedTasks.forEach { item ->
                        val task = item.task
                        when {
                            task.isCompleted -> done.add(item)
                            task.dueDate != null && task.dueDate.isBefore(now) -> overdue.add(item)
                            task.dueDate != null && task.dueDate.atZone(zone)
                                .toLocalDate() == today -> dueToday.add(item)

                            else -> todo.add(item)
                        }
                    }

                    val recurringOverdue = filteredRecurring[RecurringStatus.OVERDUE].orEmpty()
                    val recurringToday = filteredRecurring[RecurringStatus.DUE_TODAY].orEmpty()
                    val recurringUpcoming = filteredRecurring[RecurringStatus.UPCOMING].orEmpty()

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        // ExpansionSection doesn't accept a list directly, so each section is guarded
                        // against being empty before it's rendered.
                        if (overdue.isNotEmpty() || recurringOverdue.isNotEmpty()) {
                            item {
                                ExpansionSection(
                                    title = "${stringResource(R.string.taskListSectionOverdue)} (${overdue.size + recurringOverdue.size})",
                                    color = ErrorColor,
                                    initiallyExpanded = true
                                ) {
                                    TaskSectionContent(
                                        overdue,
                                        isOverdue = true,
                                        viewModel = viewModel,
                                        onNavigateToTaskDetail = onNavigateToTaskDetail,
                                        recurringTasks = recurringOverdue,
                                        recurringStatus = RecurringStatus.OVERDUE,
                                        onCompleteRecurring = completeRecurring,
                                        onNavigateToRecurringDetail = onNavigateToRecurringDetail,
                                    )
                                }
                            }
                        }
                        if (dueToday.isNotEmpty() || recurringToday.isNotEmpty()) {
                            item {
                                ExpansionSection(
                                    title = "${stringResource(R.string.taskListSectionToday)} (${dueToday.size + recurringToday.size})",
                                    color = WarningColor,
                                    initiallyExpanded = true
                                ) {
                                    TaskSectionContent(
                                        dueToday,
                                        isOverdue = false,
                                        viewModel = viewModel,
                                        onNavigateToTaskDetail = onNavigateToTaskDetail,
                                        recurringTasks = recurringToday,
                                        recurringStatus = RecurringStatus.DUE_TODAY,
                                        onCompleteRecurring = completeRecurring,
                                        onNavigateToRecurringDetail = onNavigateToRecurringDetail,
                                    )
                                }
                            }
                        }
                        if (todo.isNotEmpty() || recurringUpcoming.isNotEmpty()) {
                            item {
                                ExpansionSection(
                                    title = "${stringResource(R.string.taskListSectionUpcoming)} (${todo.size + recurringUpcoming.size})",
                                    color = TaskColor,
                                    initiallyExpanded = true
                                ) {
                                    TaskSectionContent(
                                        todo,
                                        isOverdue = false,
                                        viewModel = viewModel,
                                        onNavigateToTaskDetail = onNavigateToTaskDetail,
                                        recurringTasks = recurringUpcoming,
                                        recurringStatus = RecurringStatus.UPCOMING,
                                        onCompleteRecurring = completeRecurring,
                                        onNavigateToRecurringDetail = onNavigateToRecurringDetail,
                                    )
                                }
                            }
                        }
                        if (done.isNotEmpty()) {
                            item {
                                ExpansionSection(
                                    title = "${stringResource(R.string.taskListSectionDone)} (${done.size})",
                                    color = SuccessColor,
                                    initiallyExpanded = false
                                ) {
                                    TaskSectionContent(
                                        done,
                                        isOverdue = false,
                                        viewModel = viewModel,
                                        onNavigateToTaskDetail = onNavigateToTaskDetail
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun sortOptionLabel(option: TaskSortOption): String = when (option) {
    TaskSortOption.DUE_DATE -> stringResource(R.string.taskListSortDueDate)
    TaskSortOption.PRIORITY -> stringResource(R.string.taskListSortPriority)
    TaskSortOption.SIZE -> stringResource(R.string.taskListSortSize)
    TaskSortOption.ALPHABETICAL -> stringResource(R.string.taskListSortAlphabetical)
    TaskSortOption.CATEGORY -> stringResource(R.string.taskListSortCategory)
}

@Composable
private fun TaskSectionContent(
    tasks: List<TaskWithSubtasks>,
    isOverdue: Boolean,
    viewModel: TaskViewModel,
    onNavigateToTaskDetail: (String?) -> Unit,
    recurringTasks: List<RecurringTaskEntity> = emptyList(),
    recurringStatus: RecurringStatus = RecurringStatus.SCHEDULED,
    onCompleteRecurring: (RecurringTaskEntity) -> Unit = {},
    onNavigateToRecurringDetail: (String) -> Unit = {},
) {
    Column {
        tasks.forEach { taskWithSubtasks ->
            val taskEntity = taskWithSubtasks.task
            val subtasksCompleted = taskWithSubtasks.subtasks.count { it.isCompleted }
            val subtasksTotal = taskWithSubtasks.subtasks.size

            TaskListTile(
                task = taskEntity,
                isOverdue = isOverdue,
                subtasksCompleted = subtasksCompleted,
                subtasksTotal = subtasksTotal,
                onToggleCompleted = { viewModel.toggleTaskCompletion(taskEntity) },
                onTap = { onNavigateToTaskDetail(taskEntity.id) },
                onDelete = { viewModel.deleteTask(taskEntity) }
            )
        }
        recurringTasks.forEach { recurringTask ->
            RecurringTaskListTile(
                task = recurringTask,
                status = recurringStatus,
                onComplete = { onCompleteRecurring(recurringTask) },
                onTap = { onNavigateToRecurringDetail(recurringTask.id) },
            )
        }
    }
}
