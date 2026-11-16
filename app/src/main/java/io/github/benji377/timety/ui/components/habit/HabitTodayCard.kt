package io.github.benji377.timety.ui.components.habit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.benji377.timety.R
import io.github.benji377.timety.data.model.habit.HabitWithCompletions
import io.github.benji377.timety.ui.components.common.NeoButton
import io.github.benji377.timety.ui.components.common.NeoCard
import io.github.benji377.timety.ui.components.common.NeoIconButton
import io.github.benji377.timety.ui.components.common.NeoTextButton
import io.github.benji377.timety.ui.theme.AppTheme
import io.github.benji377.timety.ui.utils.LocalDateFormatSettings
import io.github.benji377.timety.ui.viewmodel.HabitViewModel
import io.github.benji377.timety.util.datetime.AppDateFormatUtils
import java.time.LocalDate
import java.time.ZoneId

/**
 * Today's check-ins of a habit done more than once a day, on its view page: progress, each
 * check-in with its time and a remove button, a check-in button, and a link to the full history.
 */
@Composable
fun HabitTodayCard(
    habitWithCompletions: HabitWithCompletions,
    color: Color,
    viewModel: HabitViewModel,
    onOpenHistory: () -> Unit,
) {
    val habit = habitWithCompletions.habit
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now()
    val todays = habitWithCompletions.completions
        .filter { it.completionDate.atZone(zone).toLocalDate() == today }
        .sortedBy { it.completionDate }
    val checkIn = rememberHabitCheckIn(viewModel)
    val remove = rememberHabitCheckInRemover(viewModel)
    val use24Hour = LocalDateFormatSettings.current.use24HourFormat

    NeoCard {
        Column(modifier = Modifier.fillMaxWidth().padding(AppTheme.spaceLarge)) {
            Text(stringResource(R.string.habitTodayTitle), fontWeight = AppTheme.fwBold)
            Spacer(Modifier.height(AppTheme.spaceSmall))
            HabitCheckInProgress(count = todays.size, target = habit.timesPerDay, color = color)
            todays.forEach { completion ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = AppTheme.spaceSmall),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = AppDateFormatUtils.formatTime(completion.completionDate, use24Hour),
                        modifier = Modifier.weight(1f),
                    )
                    NeoIconButton(
                        onClick = { remove(habit, completion, todays.size - 1) },
                        icon = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.commonLabelRemove),
                    )
                }
            }
            Spacer(Modifier.height(AppTheme.spaceMedium))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Stops at the target: a tap there would remove one, which the list above does.
                NeoButton(
                    onClick = { checkIn(habit) },
                    enabled = todays.size < habit.timesPerDay,
                    colors = ButtonDefaults.buttonColors(containerColor = color),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(AppTheme.spaceSmall))
                    Text(stringResource(R.string.habitTodayCheckIn))
                }
                NeoTextButton(onClick = onOpenHistory, text = stringResource(R.string.habitTodayHistory))
            }
        }
    }
}
