package io.github.benji377.timety.ui.screens.habit

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.benji377.timety.R
import io.github.benji377.timety.data.model.habit.HabitFrequency
import io.github.benji377.timety.data.model.habit.HabitWithCompletions
import io.github.benji377.timety.ui.components.common.ExpansionSection
import io.github.benji377.timety.ui.components.common.TimetyFab
import io.github.benji377.timety.ui.components.common.TimetyTopBar
import io.github.benji377.timety.ui.components.habit.GroupedHabitsSection
import io.github.benji377.timety.ui.components.habit.HabitBottomSheet
import io.github.benji377.timety.ui.components.habit.HabitListTile
import io.github.benji377.timety.ui.theme.AppTheme
import io.github.benji377.timety.ui.theme.HabitColor
import io.github.benji377.timety.ui.theme.InfoColor
import io.github.benji377.timety.ui.theme.SuccessColor
import io.github.benji377.timety.ui.theme.WarningColor
import io.github.benji377.timety.ui.viewmodel.AppViewModelProvider
import io.github.benji377.timety.ui.viewmodel.HabitViewModel
import io.github.benji377.timety.ui.viewmodel.SettingsViewModel
import io.github.benji377.timety.util.habit.HabitUtils
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import io.github.benji377.timety.ui.components.common.TimetyOutlinedTextField as OutlinedTextField


/**
 * Lists the user's habits grouped into due-today, upcoming, and done sections for the current day.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitListScreen(
    viewModel: HabitViewModel = viewModel(factory = AppViewModelProvider.Factory),
    settingsViewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory),
    onNavigateToHabitDetail: (String?) -> Unit,
    onNavigateToQuickHabits: () -> Unit = {},
    onNavigateToGoals: () -> Unit = {},
) {
    val habitsWithCompletions by viewModel.habitsWithCompletions.collectAsState()
    val use24HourFormat by settingsViewModel.use24HourFormat.collectAsState()
    var historySheetFor by remember { mutableStateOf<HabitWithCompletions?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var reorderMode by remember { mutableStateOf(false) }

    // Habits have no description field (unlike tasks), so search matches name and notes.
    val filteredHabits = remember(habitsWithCompletions, searchQuery) {
        if (searchQuery.isBlank()) habitsWithCompletions
        else habitsWithCompletions.filter { hwc ->
            hwc.habit.name.lowercase().contains(searchQuery.lowercase()) ||
                hwc.habit.notes?.lowercase()?.contains(searchQuery.lowercase()) == true
        }
    }

    Scaffold(
        topBar = {
            TimetyTopBar(
                title = stringResource(R.string.habitsListTitle),
                actions = {
                    IconButton(onClick = onNavigateToGoals) {
                        Icon(
                            imageVector = Icons.Outlined.Flag,
                            contentDescription = stringResource(R.string.goalsTitle),
                        )
                    }
                    IconButton(onClick = onNavigateToQuickHabits) {
                        Icon(
                            imageVector = Icons.Outlined.Alarm,
                            contentDescription = stringResource(R.string.quickHabitsTitle),
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            TimetyFab(
                onClick = { onNavigateToHabitDetail(null) },
                containerColor = HabitColor
            )
        }
    ) { paddingValues ->
        if (habitsWithCompletions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    stringResource(R.string.habitScreenEmpty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, top = 8.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it; if (it.isNotEmpty()) reorderMode = false },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text(stringResource(R.string.habitListSearchHint)) },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        singleLine = true,
                        shape = AppTheme.brNeo
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            reorderMode = !reorderMode
                            if (reorderMode) searchQuery = ""
                        }
                    ) {
                        Icon(
                            Icons.Filled.DragHandle,
                            contentDescription = stringResource(R.string.habitListReorderToggleTooltip),
                            tint = if (reorderMode) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                if (filteredHabits.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            stringResource(R.string.habitListFilterNoMatch),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    val today = LocalDate.now()
                    val dueToday = mutableListOf<HabitWithCompletions>()
                    val upcoming = mutableListOf<HabitWithCompletions>()
                    val done = mutableListOf<HabitWithCompletions>()

                    filteredHabits.forEach { hwc ->
                        when {
                            HabitUtils.isCompletedOn(
                                hwc,
                                today
                            ) || HabitUtils.isWeeklyGoalMet(hwc) -> done.add(hwc)

                            HabitUtils.isHabitDueToday(hwc) -> dueToday.add(hwc)
                            else -> upcoming.add(hwc)
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                if (dueToday.isNotEmpty()) {
                    item {
                        ExpansionSection(
                            title = "${stringResource(R.string.commonTimeDueToday)} (${dueToday.size})",
                            color = WarningColor,
                            initiallyExpanded = true,
                        ) {
                            GroupedHabitsSection(
                                habits = dueToday,
                                allHabitsForStacks = habitsWithCompletions,
                                targetDate = today,
                                isReorderMode = reorderMode,
                                onStandaloneReordered = { newOrder ->
                                    viewModel.commitStandaloneReorder(newOrder.map { it.habit })
                                },
                                onStackReordered = { stackName, newOrder ->
                                    viewModel.commitStackReorder(stackName, newOrder.map { it.habit })
                                },
                            ) { habit, isDone, isStacked, isLocked, isReorderMode ->
                                HabitTileWrapper(
                                    hwc = habit,
                                    isDone = isDone,
                                    isStacked = isStacked,
                                    isLocked = isLocked,
                                    isReorderMode = isReorderMode,
                                    viewModel = viewModel,
                                    use24HourFormat = use24HourFormat,
                                    onNavigateToHabitDetail = onNavigateToHabitDetail,
                                    onOpenHistory = { historySheetFor = habit },
                                )
                            }
                        }
                    }
                }
                if (upcoming.isNotEmpty()) {
                    item {
                        ExpansionSection(
                            title = "${stringResource(R.string.commonTimeUpcoming)} (${upcoming.size})",
                            color = InfoColor,
                            initiallyExpanded = false,
                        ) {
                            GroupedHabitsSection(
                                habits = upcoming,
                                allHabitsForStacks = habitsWithCompletions,
                                targetDate = today,
                                isReorderMode = reorderMode,
                                onStandaloneReordered = { newOrder ->
                                    viewModel.commitStandaloneReorder(newOrder.map { it.habit })
                                },
                                onStackReordered = { stackName, newOrder ->
                                    viewModel.commitStackReorder(stackName, newOrder.map { it.habit })
                                },
                            ) { habit, isDone, isStacked, isLocked, isReorderMode ->
                                HabitTileWrapper(
                                    hwc = habit,
                                    isDone = isDone,
                                    isStacked = isStacked,
                                    isLocked = isLocked,
                                    isReorderMode = isReorderMode,
                                    viewModel = viewModel,
                                    use24HourFormat = use24HourFormat,
                                    onNavigateToHabitDetail = onNavigateToHabitDetail,
                                    onOpenHistory = { historySheetFor = habit },
                                )
                            }
                        }
                    }
                }
                if (done.isNotEmpty()) {
                    item {
                        ExpansionSection(
                            title = "${stringResource(R.string.commonTimeDone)} (${done.size})",
                            color = SuccessColor,
                            initiallyExpanded = false,
                        ) {
                            GroupedHabitsSection(
                                habits = done,
                                allHabitsForStacks = habitsWithCompletions,
                                targetDate = today,
                                isReorderMode = reorderMode,
                                onStandaloneReordered = { newOrder ->
                                    viewModel.commitStandaloneReorder(newOrder.map { it.habit })
                                },
                                onStackReordered = { stackName, newOrder ->
                                    viewModel.commitStackReorder(stackName, newOrder.map { it.habit })
                                },
                            ) { habit, isDone, isStacked, isLocked, isReorderMode ->
                                HabitTileWrapper(
                                    hwc = habit,
                                    isDone = isDone,
                                    isStacked = isStacked,
                                    isLocked = isLocked,
                                    isReorderMode = isReorderMode,
                                    viewModel = viewModel,
                                    use24HourFormat = use24HourFormat,
                                    onNavigateToHabitDetail = onNavigateToHabitDetail,
                                    onOpenHistory = { historySheetFor = habit },
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

    val sheetHabit = historySheetFor?.let { hwc ->
        habitsWithCompletions.find { it.habit.id == hwc.habit.id } ?: hwc
    }
    if (sheetHabit != null) {
        HabitBottomSheet(
            habitWithCompletions = sheetHabit,
            onDismissRequest = { historySheetFor = null },
            onDateSelected = { instant ->
                viewModel.markCompletionOnDate(sheetHabit.habit.id, instant)
            },
            onDateDeselected = { date ->
                viewModel.unmarkCompletionOnDate(sheetHabit.habit.id, date)
            },
        )
    }
}

@Composable
private fun HabitTileWrapper(
    hwc: HabitWithCompletions,
    isDone: Boolean,
    isStacked: Boolean,
    isLocked: Boolean,
    isReorderMode: Boolean,
    viewModel: HabitViewModel,
    use24HourFormat: Boolean,
    onNavigateToHabitDetail: (String?) -> Unit,
    onOpenHistory: () -> Unit,
) {
    val habit = hwc.habit
    val completionsThisWeek = HabitUtils.getCompletionsThisWeek(hwc, includeToday = true)

    var subtitleText = HabitUtils.buildHabitSubtitle(habit, completionsThisWeek)
    if (habit.targetTimeMinutes != null) {
        val time = LocalTime.of(habit.targetTimeMinutes / 60, habit.targetTimeMinutes % 60)
        val pattern = if (use24HourFormat) "HH:mm" else "hh:mm a"
        val locale = LocalLocale.current.platformLocale
        val formatted = time.atDate(LocalDate.now()).atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern(pattern, locale))
        subtitleText += " | $formatted"
    }

    val progressValue = if (habit.frequency == HabitFrequency.WEEKLY_FLEXIBLE && !isDone) {
        completionsThisWeek.toFloat() / (habit.targetDaysPerWeek ?: 1).toFloat()
    } else null

    HabitListTile(
        habit = habit,
        isCompleted = isDone,
        isStacked = isStacked,
        isLocked = isLocked,
        isReorderMode = isReorderMode,
        subtitleText = subtitleText,
        progressValue = progressValue,
        onToggleCompleted = { viewModel.toggleCompletionToday(habit.id) },
        onMarkPastCompletion = onOpenHistory,
        onTap = { onNavigateToHabitDetail(habit.id) },
        onDelete = { viewModel.deleteHabit(habit) },
    )
}
