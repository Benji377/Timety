package io.github.benji377.timety.ui.components.user

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.benji377.timety.R
import io.github.benji377.timety.ui.theme.FocusColor
import io.github.benji377.timety.ui.theme.HabitColor
import io.github.benji377.timety.ui.theme.TaskColor
import io.github.benji377.timety.ui.theme.WarningColor
import io.github.benji377.timety.ui.utils.quantityString
import io.github.benji377.timety.util.datetime.AppDateUtils
import java.time.LocalDate
import java.util.Locale
import java.time.format.TextStyle as JavaTextStyle


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UserStreakTimelineCard(
    activityDates: List<LocalDate>,
    taskDates: List<LocalDate>,
    focusDates: List<LocalDate>,
    habitDates: List<LocalDate>,
    currentStreak: Int,
    highestStreak: Int,
    modifier: Modifier = Modifier,
) {
    val today = remember { LocalDate.now() }
    val currentStreakKeys = remember(activityDates) { buildCurrentStreakDayKeys(activityDates) }
    val taskKeys = remember(taskDates) { taskDates.map { AppDateUtils.dayKey(it) }.toSet() }
    val focusKeys = remember(focusDates) { focusDates.map { AppDateUtils.dayKey(it) }.toSet() }
    val habitKeys = remember(habitDates) { habitDates.map { AppDateUtils.dayKey(it) }.toSet() }

    val days = remember(today, currentStreakKeys, taskKeys, focusKeys, habitKeys) {
        (0..6).map { index ->
            val day = today.minusDays((6 - index).toLong())
            val key = AppDateUtils.dayKey(day)
            StreakDayInfo(
                date = day,
                isToday = AppDateUtils.isSameDay(day, today),
                inCurrentStreak = currentStreakKeys.contains(key),
                hasTask = taskKeys.contains(key),
                hasFocus = focusKeys.contains(key),
                hasHabit = habitKeys.contains(key),
            )
        }
    }

    val statusText = streakStatusText(activityDates, currentStreak)

    val listState = rememberLazyListState()
    LaunchedEffect(days.size) {
        if (days.isNotEmpty()) listState.scrollToItem(days.size - 1)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
                            MaterialTheme.colorScheme.surface,
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
                    )
                )
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.LocalFireDepartment,
                    contentDescription = null,
                    tint = WarningColor,
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.streakTimelineTitle),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.streakTimelineSubtitle),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(R.string.streakTimelineCurrent),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = quantityString(
                            R.plurals.streakTimelineDays,
                            currentStreak,
                            formatArgs = arrayOf(currentStreak)
                        ),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = statusText,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))

            LazyRow(
                state = listState,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp),
            ) {
                itemsIndexed(days) { _, info ->
                    DayTile(info, modifier = Modifier.width(56.dp))
                }
            }

            Spacer(Modifier.height(14.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                TimelineLegendDot(TaskColor, stringResource(R.string.globalLabelTask))
                TimelineLegendDot(HabitColor, stringResource(R.string.globalLabelHabit))
                TimelineLegendDot(FocusColor, stringResource(R.string.focusTitle))
                TimelineLegendDot(WarningColor, stringResource(R.string.streakLegendStreakDay))
            }
        }
    }
}

private data class StreakDayInfo(
    val date: LocalDate,
    val isToday: Boolean,
    val inCurrentStreak: Boolean,
    val hasTask: Boolean,
    val hasFocus: Boolean,
    val hasHabit: Boolean,
)


private fun buildCurrentStreakDayKeys(dates: List<LocalDate>): Set<String> {
    val dayKeys = dates.map { AppDateUtils.dayKey(it) }.toSet()
    if (dayKeys.isEmpty()) return emptySet()

    var checkDate = LocalDate.now()
    if (!dayKeys.contains(AppDateUtils.dayKey(checkDate))) {
        val yesterday = checkDate.minusDays(1)
        if (!dayKeys.contains(AppDateUtils.dayKey(yesterday))) return emptySet()
        checkDate = yesterday
    }

    val streakKeys = mutableSetOf<String>()
    while (dayKeys.contains(AppDateUtils.dayKey(checkDate))) {
        streakKeys.add(AppDateUtils.dayKey(checkDate))
        checkDate = checkDate.minusDays(1)
    }
    return streakKeys
}


@Composable
private fun streakStatusText(activityDates: List<LocalDate>, currentStreak: Int): String {
    if (activityDates.isEmpty()) return stringResource(R.string.streakStatusNone)

    val today = LocalDate.now()
    val todayKey = AppDateUtils.dayKey(today)
    val yesterdayKey = AppDateUtils.dayKey(today.minusDays(1))
    val dayKeys = activityDates.map { AppDateUtils.dayKey(it) }.toSet()

    return when {
        dayKeys.contains(todayKey) && currentStreak > 0 -> stringResource(R.string.streakStatusActive)
        dayKeys.contains(yesterdayKey) && currentStreak > 0 -> stringResource(R.string.streakStatusFrozen)
        dayKeys.contains(todayKey) -> stringResource(R.string.streakStatusBuilding)
        else -> stringResource(R.string.streakStatusStart)
    }
}

@Composable
private fun DayTile(info: StreakDayInfo, modifier: Modifier = Modifier) {
    val surfaceHighest = MaterialTheme.colorScheme.surfaceVariant
    val backgroundColor = when {
        info.isToday -> TaskColor.copy(alpha = 0.12f)
        info.inCurrentStreak -> WarningColor.copy(alpha = 0.14f)
        info.hasTask || info.hasHabit || info.hasFocus -> surfaceHighest.copy(alpha = 0.7f)
        else -> surfaceHighest.copy(alpha = 0.35f)
    }
    val borderColor = when {
        info.isToday -> TaskColor
        info.inCurrentStreak -> WarningColor
        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .border(if (info.isToday) 1.8.dp else 1.dp, borderColor, RoundedCornerShape(16.dp))
            .padding(horizontal = 6.dp, vertical = 7.dp)
            .height(84.dp - 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = if (info.isToday) stringResource(R.string.streakDayToday)
            else info.date.dayOfWeek.getDisplayName(JavaTextStyle.SHORT, androidx.compose.ui.text.intl.Locale.current.platformLocale),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = if (info.isToday) TaskColor else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "${info.date.dayOfMonth}",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            DayDot(info.hasTask, TaskColor)
            Spacer(Modifier.width(2.dp))
            DayDot(info.hasHabit, HabitColor)
            Spacer(Modifier.width(2.dp))
            DayDot(info.hasFocus, FocusColor)
        }
    }
}

@Composable
private fun DayDot(active: Boolean, color: Color) {
    Box(
        modifier = Modifier
            .size(7.dp)
            .clip(CircleShape)
            .background(if (active) color else color.copy(alpha = 0.12f))
    )
}

@Composable
private fun TimelineLegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
