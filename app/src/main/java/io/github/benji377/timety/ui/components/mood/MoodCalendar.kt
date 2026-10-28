package io.github.benji377.timety.ui.components.mood

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.benji377.timety.R
import io.github.benji377.timety.data.model.user.DayRating
import io.github.benji377.timety.ui.components.common.MonthNavigator
import io.github.benji377.timety.ui.components.common.NeoCard
import io.github.benji377.timety.ui.theme.AppTheme
import io.github.benji377.timety.ui.theme.UserColor
import io.github.benji377.timety.ui.utils.LocalDateFormatSettings
import io.github.benji377.timety.util.datetime.AppDateFormatUtils
import io.github.benji377.timety.util.datetime.CalendarUtils
import java.time.LocalDate

private val WeekdayHeaders = listOf(
    R.string.calendarHeaderMon,
    R.string.calendarHeaderTue,
    R.string.calendarHeaderWed,
    R.string.calendarHeaderThu,
    R.string.calendarHeaderFri,
    R.string.calendarHeaderSat,
    R.string.calendarHeaderSun,
)

/**
 * One month of moods, a face per rated day. Days up to [today] open [onDayClick]; later days
 * and later months can't be rated yet.
 */
@Composable
fun MoodCalendar(
    month: LocalDate,
    today: LocalDate,
    moods: Map<LocalDate, DayRating>,
    onMonthChange: (LocalDate) -> Unit,
    onDayClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val weeks = remember(month) { CalendarUtils.generateWeeks(month) }
    NeoCard(modifier = modifier) {
        Column(modifier = Modifier.padding(AppTheme.spaceSmall)) {
            MonthNavigator(
                focusedMonth = month,
                onPrevious = { onMonthChange(month.minusMonths(1)) },
                onNext = { onMonthChange(month.plusMonths(1)) },
                nextEnabled = month.withDayOfMonth(1).isBefore(today.withDayOfMonth(1)),
            )
            Spacer(Modifier.height(AppTheme.spaceMedium))
            Row(Modifier.fillMaxWidth()) {
                WeekdayHeaders.forEach { header ->
                    Text(
                        text = stringResource(header),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                    )
                }
            }
            Spacer(Modifier.height(AppTheme.spaceXSmall))
            weeks.forEach { week ->
                Row(Modifier.fillMaxWidth()) {
                    week.forEach { day ->
                        val cellModifier = Modifier.weight(1f)
                        // Neighbouring months' days stay blank so each face belongs to this month.
                        if (day.month != month.month) {
                            Spacer(cellModifier)
                        } else {
                            MoodDayCell(
                                day = day,
                                mood = moods[day],
                                isToday = day == today,
                                isFuture = day.isAfter(today),
                                onClick = { onDayClick(day) },
                                modifier = cellModifier,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MoodDayCell(
    day: LocalDate,
    mood: DayRating?,
    isToday: Boolean,
    isFuture: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dfs = LocalDateFormatSettings.current
    val description = "${AppDateFormatUtils.formatDate(day, dfs.dateFormatCode)}: " +
            stringResource(mood?.labelRes ?: R.string.moodDayNotRated)
    Column(
        modifier = modifier
            .padding(2.dp)
            .clip(AppTheme.brMedium)
            .border(
                width = AppTheme.borderHairline,
                color = if (isToday) UserColor else Color.Transparent,
                shape = AppTheme.brMedium,
            )
            .clickable(enabled = !isFuture, role = Role.Button, onClick = onClick)
            .clearAndSetSemantics { contentDescription = description }
            .padding(vertical = AppTheme.spaceXSmall),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = day.dayOfMonth.toString(),
            fontSize = 12.sp,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
            color = if (isFuture) {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
        Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
            when {
                mood != null -> Icon(
                    imageVector = mood.icon,
                    contentDescription = null,
                    tint = mood.color,
                    modifier = Modifier.size(28.dp),
                )
                // An empty ring marks a past day that can still be rated.
                !isFuture -> Box(
                    Modifier
                        .size(20.dp)
                        .border(AppTheme.borderHairline, MaterialTheme.colorScheme.outline, CircleShape)
                )
            }
        }
    }
}
