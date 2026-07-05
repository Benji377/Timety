package io.github.benji377.timety.ui.screens.task

import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.benji377.timety.R
import io.github.benji377.timety.data.model.task.TaskSortOption
import io.github.benji377.timety.data.model.task.TaskWithSubtasks
import io.github.benji377.timety.ui.components.common.ExpansionSection
import io.github.benji377.timety.ui.components.task.TaskListTile
import io.github.benji377.timety.ui.theme.AppTheme
import io.github.benji377.timety.ui.theme.ErrorColor
import io.github.benji377.timety.ui.theme.SuccessColor
import io.github.benji377.timety.ui.theme.TaskColor
import io.github.benji377.timety.ui.theme.WarningColor
import io.github.benji377.timety.ui.viewmodel.AppViewModelProvider
import io.github.benji377.timety.ui.viewmodel.TaskViewModel
import io.github.benji377.timety.util.task.TaskFilterEngine
import java.time.Instant
import java.time.ZoneId
import io.github.benji377.timety.ui.components.common.TimetyOutlinedTextField as OutlinedTextField


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    viewModel: TaskViewModel = viewModel(factory = AppViewModelProvider.Factory),
    onNavigateToTaskDetail: (String?) -> Unit
) {
    val tasks by viewModel.allTasks.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf<String?>(null) }
    var sortOption by remember { mutableStateOf(TaskSortOption.DUE_DATE) }
    var isAscending by remember { mutableStateOf(true) }
    var sortMenuExpanded by remember { mutableStateOf(false) }

    // Extract unique categories from tasks (trimmed, non-empty, sorted).
    val allCategories = remember(tasks) {
        tasks.map { it.task.category.trim() }.filter { it.isNotEmpty() }.distinct().sorted()
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
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.taskListTitle),
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToTaskDetail(null) },
                modifier = Modifier.border(
                    AppTheme.neoBorderWidth,
                    MaterialTheme.colorScheme.outline,
                    AppTheme.brNeo
                ),
                shape = AppTheme.brNeo,
                elevation = androidx.compose.material3.FloatingActionButtonDefaults.elevation(
                    0.dp,
                    0.dp,
                    0.dp,
                    0.dp
                ),
                containerColor = TaskColor,
                contentColor = androidx.compose.ui.graphics.Color.White
            ) {
                Icon(Icons.Filled.Add, stringResource(R.string.commonLabelAdd))
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // --- SEARCH & SORT HEADER ---
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

            // --- CATEGORY PILLS ---
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
                            label = { Text(category) }
                        )
                    }
                }
            }

            // --- TASKS SCROLLABLE LIST ---
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (tasks.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            stringResource(R.string.taskListEmpty),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else if (processedTasks.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            stringResource(R.string.taskListFilterNoMatch),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    // Grouping Logic (mirrors Flutter: completed -> overdue -> due today -> todo)
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

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        // Mirrors Flutter's ExpansionSection, which renders nothing when its
                        // task list is empty; the shared Kotlin ExpansionSection doesn't take a
                        // list so callers guard emptiness themselves.
                        if (overdue.isNotEmpty()) {
                            item {
                                ExpansionSection(
                                    title = "${stringResource(R.string.taskListSectionOverdue)} (${overdue.size})",
                                    color = ErrorColor,
                                    initiallyExpanded = true
                                ) {
                                    TaskSectionContent(
                                        overdue,
                                        isOverdue = true,
                                        viewModel = viewModel,
                                        onNavigateToTaskDetail = onNavigateToTaskDetail
                                    )
                                }
                            }
                        }
                        if (dueToday.isNotEmpty()) {
                            item {
                                ExpansionSection(
                                    title = "${stringResource(R.string.taskListSectionToday)} (${dueToday.size})",
                                    color = WarningColor,
                                    initiallyExpanded = true
                                ) {
                                    TaskSectionContent(
                                        dueToday,
                                        isOverdue = false,
                                        viewModel = viewModel,
                                        onNavigateToTaskDetail = onNavigateToTaskDetail
                                    )
                                }
                            }
                        }
                        if (todo.isNotEmpty()) {
                            item {
                                ExpansionSection(
                                    title = "${stringResource(R.string.taskListSectionUpcoming)} (${todo.size})",
                                    color = TaskColor,
                                    initiallyExpanded = true
                                ) {
                                    TaskSectionContent(
                                        todo,
                                        isOverdue = false,
                                        viewModel = viewModel,
                                        onNavigateToTaskDetail = onNavigateToTaskDetail
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
    onNavigateToTaskDetail: (String?) -> Unit
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
    }
}
