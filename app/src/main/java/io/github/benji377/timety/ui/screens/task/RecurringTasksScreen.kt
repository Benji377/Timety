package io.github.benji377.timety.ui.screens.task

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.benji377.timety.ui.components.common.BackNavigationIcon
import io.github.benji377.timety.R
import io.github.benji377.timety.data.model.task.RecurringTaskWithOccurrences
import io.github.benji377.timety.ui.components.common.NeoFab
import io.github.benji377.timety.ui.components.common.NeoTopBar
import io.github.benji377.timety.ui.components.task.recurrenceCadenceLabel
import io.github.benji377.timety.ui.components.task.recurringStatusColor
import io.github.benji377.timety.ui.components.task.rememberRecurringCompleter
import io.github.benji377.timety.ui.theme.AppTheme
import io.github.benji377.timety.ui.theme.SuccessColor
import io.github.benji377.timety.ui.theme.TaskColor
import io.github.benji377.timety.ui.utils.LocalDateFormatSettings
import io.github.benji377.timety.ui.utils.quantityString
import io.github.benji377.timety.ui.viewmodel.AppViewModelProvider
import io.github.benji377.timety.ui.viewmodel.RecurringTaskViewModel
import io.github.benji377.timety.ui.viewmodel.SettingsViewModel
import io.github.benji377.timety.util.datetime.AppDateFormatUtils
import io.github.benji377.timety.util.task.RecurrenceUtils
import io.github.benji377.timety.util.task.RecurringStatus
import java.time.Instant

/**
 * Lists all recurring tasks with their cadence, next due date, and completion count. Recurring
 * tasks live only here — never in the normal task list — and are created via this screen's FAB.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringTasksScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (String?) -> Unit,
    viewModel: RecurringTaskViewModel = viewModel(factory = AppViewModelProvider.Factory),
    settingsViewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val recurringTasks by viewModel.allRecurringTasks.collectAsState()
    val horizonDays by settingsViewModel.upcomingTasksHorizon.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val completeTask = rememberRecurringCompleter(viewModel, snackbarHostState)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            NeoTopBar(
                title = stringResource(R.string.recurringTasksTitle),
                navigationIcon = {
                    BackNavigationIcon(onClick = onNavigateBack)
                }
            )
        },
        floatingActionButton = {
            NeoFab(onClick = { onNavigateToDetail(null) }, containerColor = TaskColor)
        }
    ) { paddingValues ->
        if (recurringTasks.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(AppTheme.spaceLarge),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.recurringTasksEmpty),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(
                    top = AppTheme.spaceSmall,
                    // Keep the last card's complete button clear of the FAB.
                    bottom = AppTheme.space3XLarge,
                ),
            ) {
                items(recurringTasks, key = { it.task.id }) { item ->
                    RecurringTaskCard(
                        item = item,
                        status = RecurrenceUtils.statusOf(item.task, Instant.now(), horizonDays),
                        onClick = { onNavigateToDetail(item.task.id) },
                        onComplete = { completeTask(item.task) },
                    )
                }
            }
        }
    }
}

@Composable
private fun RecurringTaskCard(
    item: RecurringTaskWithOccurrences,
    status: RecurringStatus,
    onClick: () -> Unit,
    onComplete: () -> Unit,
) {
    val dateFmt = LocalDateFormatSettings.current
    val borderColor = recurringStatusColor(status)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppTheme.spaceSmall, vertical = AppTheme.spaceXSmall)
            .clickable(onClick = onClick),
        border = BorderStroke(AppTheme.listTileBorderWidth, borderColor),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = AppTheme.spaceMedium,
                    top = AppTheme.spaceMedium,
                    bottom = AppTheme.spaceMedium,
                    end = AppTheme.spaceSmall,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Repeat,
                contentDescription = null,
                tint = TaskColor,
            )
            Spacer(modifier = Modifier.width(AppTheme.spaceMedium))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.task.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = recurrenceCadenceLabel(item.task),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(
                        R.string.recurringTasksNextDue,
                        "${
                            AppDateFormatUtils.formatDate(
                                item.task.dueDate,
                                dateFmt.dateFormatCode
                            )
                        } " +
                                AppDateFormatUtils.formatTime(
                                    item.task.dueDate,
                                    dateFmt.use24HourFormat
                                )
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = quantityString(
                        R.plurals.recurringTasksCompletedCount,
                        item.occurrences.size,
                        0,
                        item.occurrences.size,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // Quick-complete only makes sense for actionable occurrences; far-future ones are
            // completed early through the detail page instead.
            if (status != RecurringStatus.SCHEDULED) {
                IconButton(onClick = onComplete) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = stringResource(R.string.recurringTaskCompleteNow),
                        tint = SuccessColor,
                    )
                }
            }
        }
    }
}
