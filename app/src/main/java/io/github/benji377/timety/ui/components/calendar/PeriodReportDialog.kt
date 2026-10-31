package io.github.benji377.timety.ui.components.calendar

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
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Loop
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.benji377.timety.R
import io.github.benji377.timety.ui.components.common.NeoAlertDialog
import io.github.benji377.timety.ui.theme.AppTheme
import io.github.benji377.timety.ui.theme.HabitColor
import io.github.benji377.timety.ui.theme.SuccessColor
import io.github.benji377.timety.ui.theme.TaskColor
import io.github.benji377.timety.util.datetime.AppDateFormatUtils
import io.github.benji377.timety.util.stats.PeriodSummary
import io.github.benji377.timety.util.stats.ReportPeriod
import java.time.LocalDate
import kotlin.math.abs

/**
 * The calendar's weekly or monthly report: every number behind the grid, labelled, with the
 * change from the [comparedWith] days of the period before. The weekly one doubles as the legend
 * for a week chip's number and bars.
 */
@Composable
fun PeriodReportDialog(
    period: ReportPeriod,
    start: LocalDate,
    comparedWith: ClosedRange<LocalDate>,
    summary: PeriodSummary,
    previous: PeriodSummary,
    onDismiss: () -> Unit,
) {
    NeoAlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    stringResource(
                        when (period) {
                            ReportPeriod.WEEK -> R.string.calendarWeeklyReportTitle
                            ReportPeriod.MONTH -> R.string.calendarMonthlyReportTitle
                            ReportPeriod.YEAR -> R.string.calendarYearlyReportTitle
                        }
                    )
                )
                Text(
                    when (period) {
                        ReportPeriod.WEEK -> formatRange(start..period.endOf(start))
                        ReportPeriod.MONTH -> AppDateFormatUtils.formatMonthYear(start)
                        ReportPeriod.YEAR -> start.year.toString()
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column {
                ReportRow(
                    icon = Icons.Outlined.Event,
                    color = TaskColor,
                    label = stringResource(R.string.calendarReportTasksDue),
                    value = summary.tasksDue.toString(),
                )
                ReportRow(
                    icon = Icons.Outlined.CheckCircle,
                    color = TaskColor,
                    label = stringResource(R.string.calendarReportTasksCompleted),
                    value = summary.tasksCompleted.toString(),
                    change = countChange(summary.tasksCompleted - previous.tasksCompleted),
                )
                ReportRow(
                    icon = Icons.Outlined.Loop,
                    color = HabitColor,
                    label = stringResource(R.string.calendarReportHabitCheckIns),
                    value = summary.habitCheckIns.toString(),
                    change = countChange(summary.habitCheckIns - previous.habitCheckIns),
                )
                ReportRow(
                    icon = Icons.Outlined.Timer,
                    color = SuccessColor,
                    label = stringResource(R.string.calendarReportFocusSessions),
                    value = summary.focusSessions.toString(),
                    change = countChange(summary.focusSessions - previous.focusSessions),
                )
                ReportRow(
                    icon = Icons.Outlined.Schedule,
                    color = SuccessColor,
                    label = stringResource(R.string.calendarReportFocusTime),
                    value = AppDateFormatUtils.formatMinutesCompact(summary.focusMinutes),
                    change = durationChange(summary.focusMinutes - previous.focusMinutes),
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = AppTheme.spaceMedium))
                Text(
                    stringResource(R.string.calendarReportComparedHint, formatRange(comparedWith)),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.commonLabelClose)) }
        }
    )
}

@Composable
private fun ReportRow(
    icon: ImageVector,
    color: Color,
    label: String,
    value: String,
    change: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AppTheme.spaceXSmall),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(AppTheme.spaceMedium))
            Text(label, color = MaterialTheme.colorScheme.onSurface)
        }
        // The change sits beside the value rather than under it, so every row keeps one height.
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (change != null) {
                Text(change, style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.width(AppTheme.spaceSmall))
            }
            Text(value, fontWeight = FontWeight.Bold, color = color)
        }
    }
    Spacer(modifier = Modifier.height(AppTheme.spaceXSmall))
}

private fun formatRange(range: ClosedRange<LocalDate>): String =
    "${AppDateFormatUtils.formatShortDate(range.start)} - ${AppDateFormatUtils.formatShortDate(range.endInclusive)}"

// No change shows nothing rather than "+0", so the eye lands on the rows that moved.
private fun countChange(delta: Int): String? = when {
    delta > 0 -> "+$delta"
    delta < 0 -> "-${abs(delta)}"
    else -> null
}

private fun durationChange(deltaMinutes: Int): String? = when {
    deltaMinutes > 0 -> "+" + AppDateFormatUtils.formatMinutesCompact(deltaMinutes)
    deltaMinutes < 0 -> "-" + AppDateFormatUtils.formatMinutesCompact(abs(deltaMinutes))
    else -> null
}
