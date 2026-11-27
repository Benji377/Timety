package io.github.benji377.timety.ui.components.task

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import io.github.benji377.timety.R
import io.github.benji377.timety.data.model.task.OccurrenceStatus
import io.github.benji377.timety.data.model.task.RecurringTaskEntity
import io.github.benji377.timety.ui.utils.LocalDateFormatSettings
import io.github.benji377.timety.ui.viewmodel.RecurringTaskViewModel
import io.github.benji377.timety.util.datetime.AppDateFormatUtils
import kotlinx.coroutines.launch

/**
 * A completion action for recurring tasks shared by every surface that offers one: it logs the
 * occurrence through [viewModel] as [status] (a completion, or a skip) and shows a snackbar on
 * [snackbarHostState] with the new due date and an Undo action that reverts it.
 */
@Composable
fun rememberRecurringCompleter(
    viewModel: RecurringTaskViewModel,
    snackbarHostState: SnackbarHostState,
    status: OccurrenceStatus = OccurrenceStatus.COMPLETED,
): (RecurringTaskEntity) -> Unit {
    val scope = rememberCoroutineScope()
    val dateFmt = LocalDateFormatSettings.current
    val undoLabel = stringResource(R.string.commonLabelUndo)
    // Resolved at composition (not Context.getString in the callback) so the template
    // tracks configuration changes; the date is formatted into it when the snackbar shows.
    val completedTemplate = stringResource(
        when (status) {
            OccurrenceStatus.COMPLETED -> R.string.recurringTaskCompletedSnackbar
            OccurrenceStatus.SKIPPED -> R.string.recurringTaskSkippedSnackbar
        }
    )
    return remember(viewModel, snackbarHostState, status, dateFmt, undoLabel, completedTemplate) {
        { task ->
            viewModel.completeOccurrence(task, status) { undo ->
                scope.launch {
                    val nextDue = AppDateFormatUtils.formatDate(
                        undo.advancedTask.dueDate, dateFmt.dateFormatCode
                    )
                    val result = snackbarHostState.showSnackbar(
                        message = completedTemplate.format(nextDue),
                        actionLabel = undoLabel,
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.undoCompleteOccurrence(undo)
                    }
                }
            }
        }
    }
}
