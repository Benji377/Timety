package io.github.benji377.timety.ui.components.habit

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.benji377.timety.R
import io.github.benji377.timety.data.model.habit.HabitCompletionEntity
import io.github.benji377.timety.data.model.habit.HabitEntity
import io.github.benji377.timety.data.repository.CheckInChange
import io.github.benji377.timety.ui.components.common.NeoSegmentedProgressBar
import io.github.benji377.timety.ui.theme.AppTheme
import io.github.benji377.timety.ui.theme.LocalSnackbarHostState
import io.github.benji377.timety.ui.theme.SuccessColor
import io.github.benji377.timety.ui.viewmodel.HabitViewModel
import kotlinx.coroutines.launch

/**
 * Today's check-in tap for a habit, shared by every in-app habit list. Habits done more than once
 * a day also get a snackbar with the new count and an Undo that reverts exactly that tap.
 */
@Composable
fun rememberHabitCheckIn(viewModel: HabitViewModel): (HabitEntity) -> Unit {
    val snackbarHostState = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()
    val undoLabel = stringResource(R.string.commonLabelUndo)
    val template = stringResource(R.string.habitCheckInSnackbar)
    return remember(viewModel, snackbarHostState, undoLabel, template) {
        { habit ->
            viewModel.toggleCompletionToday(habit.id) { change ->
                if (habit.timesPerDay <= 1) return@toggleCompletionToday
                scope.launch {
                    // Quick taps replace the snackbar instead of queueing one per tap.
                    snackbarHostState.currentSnackbarData?.dismiss()
                    val result = snackbarHostState.showSnackbar(
                        message = template.format(habit.name, change.countAfter, habit.timesPerDay),
                        actionLabel = undoLabel,
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) viewModel.undoCheckIn(change)
                }
            }
        }
    }
}

/**
 * Deletes one of a habit's check-ins today, with the same snackbar and Undo as a tap; for picking
 * out the wrong one once the tap's own Undo is gone.
 */
@Composable
fun rememberHabitCheckInRemover(viewModel: HabitViewModel): (HabitEntity, HabitCompletionEntity, Int) -> Unit {
    val snackbarHostState = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()
    val undoLabel = stringResource(R.string.commonLabelUndo)
    val template = stringResource(R.string.habitCheckInSnackbar)
    return remember(viewModel, snackbarHostState, undoLabel, template) {
        { habit, completion, countAfter ->
            viewModel.deleteCompletion(completion)
            scope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                val result = snackbarHostState.showSnackbar(
                    message = template.format(habit.name, countAfter, habit.timesPerDay),
                    actionLabel = undoLabel,
                    duration = SnackbarDuration.Short,
                )
                if (result == SnackbarResult.ActionPerformed) {
                    viewModel.undoCheckIn(CheckInChange.Removed(completion, countAfter))
                }
            }
        }
    }
}

/** The "2/3" row with one bar segment per check-in, for habits done more than once a day. */
@Composable
fun HabitCheckInProgress(count: Int, target: Int, color: Color, modifier: Modifier = Modifier) {
    val isMet = count >= target
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Filled.Repeat,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = "${count.coerceAtMost(target)}/$target",
            fontSize = AppTheme.fsLabel,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = AppTheme.fwMedium,
        )
        Spacer(Modifier.width(8.dp))
        NeoSegmentedProgressBar(
            filled = count,
            total = target,
            color = if (isMet) SuccessColor else color,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            modifier = Modifier
                .weight(1f)
                .height(4.dp),
        )
    }
}
