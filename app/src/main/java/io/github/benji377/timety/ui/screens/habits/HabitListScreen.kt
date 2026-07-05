package io.github.benji377.timety.ui.screens.habits

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import io.github.benji377.timety.data.model.habit.HabitFrequency
import io.github.benji377.timety.data.model.habit.HabitWithCompletions
import io.github.benji377.timety.ui.components.common.ExpansionSection
import io.github.benji377.timety.ui.components.habit.GroupedHabitsSection
import io.github.benji377.timety.ui.components.habit.HabitBottomSheet
import io.github.benji377.timety.ui.components.habit.HabitListTile
import io.github.benji377.timety.ui.theme.InfoColor
import io.github.benji377.timety.ui.theme.SuccessColor
import io.github.benji377.timety.ui.theme.WarningColor
import io.github.benji377.timety.ui.viewmodel.AppViewModelProvider
import io.github.benji377.timety.ui.viewmodel.HabitViewModel
import io.github.benji377.timety.util.habit.HabitUtils
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitListScreen(
    viewModel: HabitViewModel = viewModel(factory = AppViewModelProvider.Factory),
    settingsViewModel: io.github.benji377.timety.ui.viewmodel.SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory),
    onNavigateToHabitDetail: (String?) -> Unit
) {
    val habitsWithCompletions by viewModel.habitsWithCompletions.collectAsState()
    val use24HourFormat by settingsViewModel.use24HourFormat.collectAsState()
    var historySheetFor by remember { mutableStateOf<HabitWithCompletions?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                title = { Text(stringResource(R.string.habitsListTitle)) })
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToHabitDetail(null) },
                modifier = Modifier.border(
                    io.github.benji377.timety.ui.theme.AppTheme.neoBorderWidth,
                    MaterialTheme.colorScheme.outline,
                    io.github.benji377.timety.ui.theme.AppTheme.brNeo
                ),
                shape = io.github.benji377.timety.ui.theme.AppTheme.brNeo,
                elevation = androidx.compose.material3.FloatingActionButtonDefaults.elevation(
                    0.dp,
                    0.dp,
                    0.dp,
                    0.dp
                ),
                containerColor = io.github.benji377.timety.ui.theme.HabitColor,
                contentColor = androidx.compose.ui.graphics.Color.White
            ) {
                Icon(Icons.Filled.Add, stringResource(R.string.commonLabelAdd))
            }
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
            val today = LocalDate.now()
            val dueToday = mutableListOf<HabitWithCompletions>()
            val upcoming = mutableListOf<HabitWithCompletions>()
            val done = mutableListOf<HabitWithCompletions>()

            habitsWithCompletions.forEach { hwc ->
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
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
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
                            ) { habit, isDone, isStacked, isLocked ->
                                HabitTileWrapper(
                                    hwc = habit,
                                    isDone = isDone,
                                    isStacked = isStacked,
                                    isLocked = isLocked,
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
                            ) { habit, isDone, isStacked, isLocked ->
                                HabitTileWrapper(
                                    hwc = habit,
                                    isDone = isDone,
                                    isStacked = isStacked,
                                    isLocked = isLocked,
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
                            ) { habit, isDone, isStacked, isLocked ->
                                HabitTileWrapper(
                                    hwc = habit,
                                    isDone = isDone,
                                    isStacked = isStacked,
                                    isLocked = isLocked,
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

    val sheetHabit = historySheetFor
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
        val formatted = time.atDate(LocalDate.now()).atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))
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
        subtitleText = subtitleText,
        progressValue = progressValue,
        onToggleCompleted = { viewModel.toggleCompletionToday(habit.id) },
        onMarkPastCompletion = onOpenHistory,
        onTap = { onNavigateToHabitDetail(habit.id) },
        onDelete = { viewModel.deleteHabit(habit) },
    )
}
