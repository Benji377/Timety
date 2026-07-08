package io.github.benji377.timety.ui.screens.habit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.benji377.timety.R
import io.github.benji377.timety.data.model.habit.HabitCompletionEntity
import io.github.benji377.timety.data.model.habit.HabitWithCompletions
import io.github.benji377.timety.ui.components.common.WeekNavigator
import io.github.benji377.timety.ui.components.stats.StatCard
import io.github.benji377.timety.ui.components.stats.StatCardStyle
import io.github.benji377.timety.ui.theme.AppTheme
import io.github.benji377.timety.ui.theme.HabitColor
import io.github.benji377.timety.ui.theme.SuccessColor
import io.github.benji377.timety.ui.theme.TaskColor
import io.github.benji377.timety.ui.theme.UserColor
import io.github.benji377.timety.ui.theme.WarningColor
import io.github.benji377.timety.ui.utils.quantityString
import io.github.benji377.timety.ui.viewmodel.AppViewModelProvider
import io.github.benji377.timety.ui.viewmodel.HabitViewModel
import io.github.benji377.timety.util.datetime.AppDateUtils
import io.github.benji377.timety.util.stats.StatsUtils
import io.github.benji377.timety.util.stats.StreakCalculator
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt


/**
 * Habit statistics: weekly summary cards, a daily completion chart, a time-of-day breakdown, and
 * per-habit streaks. Streaks are computed all-time; the other sections follow the selected week.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitStatsScreen(
    viewModel: HabitViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val habits by viewModel.habitsWithCompletions.collectAsState()
    var focusedDate by remember { mutableStateOf(LocalDate.now()) }

    Scaffold { paddingValues ->
        if (habits.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.habitStatsLabelEmpty))
            }
        } else {
            // The summary cards and the time-of-day breakdown follow the week selector, so
            // shifting the week visibly updates the stats. Streaks stay all-time: a
            // per-week streak is meaningless.
            val startOfWeek = AppDateUtils.startOfWeekMonday(focusedDate)
            val endOfWeek = startOfWeek.plusDays(6)
            val zone = ZoneId.systemDefault()
            val weekCompletions = habits.flatMap { hwc ->
                hwc.completions.filter { c ->
                    AppDateUtils.isWithinInclusive(
                        c.completionDate.atZone(zone).toLocalDate(),
                        startOfWeek,
                        endOfWeek
                    )
                }
            }
            val allTimeBestStreak = habits.maxOfOrNull { hwc ->
                StreakCalculator.calculateBestStreak(completionDates(hwc))
            } ?: 0

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = AppTheme.spaceLarge),
                contentPadding = PaddingValues(vertical = AppTheme.spaceLarge),
            ) {
                item {
                    WeekNavigator(
                        focusedDate = focusedDate,
                        onShiftWeek = { days -> focusedDate = focusedDate.plusDays(days.toLong()) },
                    )
                    Spacer(modifier = Modifier.height(AppTheme.spaceLarge))
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(
                            AppTheme.spaceMedium,
                            Alignment.CenterHorizontally
                        ),
                    ) {
                        StatCard(
                            title = stringResource(R.string.habitStatsLabelTotal),
                            value = weekCompletions.size.toString(),
                            icon = Icons.Filled.Timeline,
                            color = HabitColor,
                            style = StatCardStyle.COMPACT_VERTICAL,
                        )
                        StatCard(
                            title = stringResource(R.string.habitStatsLabelActive),
                            value = habits.size.toString(),
                            icon = Icons.Filled.AllInclusive,
                            color = SuccessColor,
                            style = StatCardStyle.COMPACT_VERTICAL,
                        )
                        StatCard(
                            title = stringResource(R.string.habitStatsLabelBestStreak),
                            value = allTimeBestStreak.toString(),
                            icon = Icons.Filled.MilitaryTech,
                            color = WarningColor,
                            style = StatCardStyle.COMPACT_VERTICAL,
                        )
                    }
                    Spacer(modifier = Modifier.height(AppTheme.space3XLarge))
                }

                item {
                    Text(
                        text = stringResource(R.string.habitStatsLabelVelocity),
                        fontSize = AppTheme.fsHeadingSmall,
                        fontWeight = AppTheme.fwBold,
                    )
                    Text(
                        text = stringResource(R.string.habitStatsLabelCompletionsDaily),
                        fontSize = AppTheme.fsBodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(AppTheme.spaceLarge))
                    HabitVelocityChart(habits = habits, focusedDate = focusedDate)
                    Spacer(modifier = Modifier.height(AppTheme.space3XLarge))
                }

                item {
                    TimeOfDayBreakdownCard(weekCompletions)
                    Spacer(modifier = Modifier.height(AppTheme.space3XLarge))
                }

                item {
                    Text(
                        text = stringResource(R.string.habitStatsLabelCurrentStreak),
                        fontSize = AppTheme.fsHeadingSmall,
                        fontWeight = AppTheme.fwBold,
                    )
                    Spacer(modifier = Modifier.height(AppTheme.spaceMedium))
                }

                items(habits, key = { it.habit.id }) { hwc ->
                    val dates = completionDates(hwc)
                    val currentStreak = StreakCalculator.calculateCurrentStreak(dates)
                    val bestStreak = StreakCalculator.calculateBestStreak(dates)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = AppTheme.spaceSmall),
                        shape = AppTheme.brNeo,
                        border = BorderStroke(AppTheme.neoBorderWidth, HabitColor),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(AppTheme.spaceLarge),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Filled.Stars, contentDescription = null, tint = HabitColor)
                            Spacer(modifier = Modifier.width(AppTheme.spaceLarge))
                            Text(
                                text = hwc.habit.name,
                                fontWeight = AppTheme.fwBold,
                                modifier = Modifier.weight(1f),
                            )
                            Column(horizontalAlignment = Alignment.End) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = currentStreak.toString(),
                                        fontSize = 16.sp,
                                        fontWeight = AppTheme.fwExtraBold,
                                        color = WarningColor,
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        Icons.Filled.Whatshot,
                                        contentDescription = null,
                                        tint = WarningColor,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                                Text(
                                    text = stringResource(R.string.habitStatsLabelBest, bestStreak),
                                    fontSize = AppTheme.fsCaption,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(AppTheme.space3XLarge)) }
            }
        }
    }
}

private fun completionDates(hwc: HabitWithCompletions): List<LocalDate> =
    hwc.completions.map { it.completionDate.atZone(ZoneId.systemDefault()).toLocalDate() }

@Composable
private fun HabitVelocityChart(habits: List<HabitWithCompletions>, focusedDate: LocalDate) {
    val startOfWeek = AppDateUtils.startOfWeekMonday(focusedDate)
    val endOfWeek = startOfWeek.plusDays(6)
    val today = LocalDate.now()
    val isCurrentRealWeek = AppDateUtils.isWithinInclusive(today, startOfWeek, endOfWeek)
    val todayIndex = today.dayOfWeek.value - 1

    val dailyCounts = IntArray(7)
    val zone = ZoneId.systemDefault()
    habits.forEach { hwc ->
        hwc.completions.forEach { c ->
            val date = c.completionDate.atZone(zone).toLocalDate()
            if (!date.isBefore(startOfWeek) && !date.isAfter(endOfWeek)) {
                dailyCounts[date.dayOfWeek.value - 1]++
            }
        }
    }

    val maxY = StatsUtils.maxValue(dailyCounts.toList(), minimum = 5.0)
    val weekdays = listOf(
        stringResource(R.string.commonWeekdayMon),
        stringResource(R.string.commonWeekdayTue),
        stringResource(R.string.commonWeekdayWed),
        stringResource(R.string.commonWeekdayThu),
        stringResource(R.string.commonWeekdayFri),
        stringResource(R.string.commonWeekdaySat),
        stringResource(R.string.commonWeekdaySun),
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.Bottom,
    ) {
        for (i in 0 until 7) {
            val isToday = isCurrentRealWeek && i == todayIndex
            val fraction = (dailyCounts[i] / (maxY + 1)).toFloat().coerceIn(0f, 1f)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(0.5f)
                        .background(
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f),
                            RoundedCornerShape(4.dp)
                        ),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(fraction)
                            .background(
                                color = if (isToday) HabitColor else HabitColor.copy(alpha = AppTheme.OPACITY_MEDIUM),
                                shape = RoundedCornerShape(4.dp),
                            ),
                    )
                }
                Spacer(modifier = Modifier.height(AppTheme.spaceSmall))
                Text(
                    text = weekdays[i],
                    fontSize = AppTheme.fsBodySmall,
                    fontWeight = if (isToday) AppTheme.fwBold else AppTheme.fwNormal,
                    color = if (isToday) HabitColor else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private data class TimeOfDayBucket(
    val label: String,
    val subtitle: String,
    val count: Int,
    val color: Color,
    val icon: ImageVector,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TimeOfDayBreakdownCard(completions: List<HabitCompletionEntity>) {
    val zone = ZoneId.systemDefault()
    var morning = 0
    var afternoon = 0
    var evening = 0
    var night = 0
    completions.forEach { c ->
        val hour = c.completionDate.atZone(zone).hour
        when (hour) {
            in 5..11 -> morning++
            in 12..16 -> afternoon++
            in 17..20 -> evening++
            else -> night++
        }
    }
    val total = morning + afternoon + evening + night

    val buckets = listOf(
        TimeOfDayBucket(
            label = stringResource(R.string.commonDaytimeMorning),
            subtitle = stringResource(R.string.commonDaytimeMorningRange),
            count = morning,
            color = WarningColor,
            icon = Icons.Outlined.WbSunny,
        ),
        TimeOfDayBucket(
            label = stringResource(R.string.commonDaytimeAfternoon),
            subtitle = stringResource(R.string.commonDaytimeAfternoonRange),
            count = afternoon,
            color = TaskColor,
            icon = Icons.Filled.WbSunny,
        ),
        TimeOfDayBucket(
            label = stringResource(R.string.commonDaytimeEvening),
            subtitle = stringResource(R.string.commonDaytimeEveningRange),
            count = evening,
            color = HabitColor,
            icon = Icons.Filled.NightlightRound,
        ),
        TimeOfDayBucket(
            label = stringResource(R.string.commonDaytimeNight),
            subtitle = stringResource(R.string.commonDaytimeNightRange),
            count = night,
            color = UserColor,
            icon = Icons.Outlined.NightsStay,
        ),
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.habitStatsLabelCompletionsTime),
                    fontSize = AppTheme.fsHeadingSmall,
                    fontWeight = AppTheme.fwBold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.habitStatsLabelCompletionsDistribution),
                    fontSize = AppTheme.fsBodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.width(AppTheme.spaceLarge))
            Text(
                text = quantityString(
                    R.plurals.nHabitsCount,
                    total,
                    zeroRes = R.string.nHabitsCountZero,
                    total
                ),
                fontSize = 15.sp,
                fontWeight = AppTheme.fwExtraBold,
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceContainerHighest,
                    RoundedCornerShape(999.dp)
                ),
        ) {
            if (total == 0) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f)),
                )
            } else {
                buckets.filter { it.count > 0 }.forEach { bucket ->
                    Box(
                        modifier = Modifier
                            .weight(bucket.count.toFloat())
                            .fillMaxHeight()
                            .background(bucket.color),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            buckets.forEach { bucket ->
                val percent =
                    if (total == 0) 0 else ((bucket.count.toFloat() / total) * 100).roundToInt()
                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.47f)
                        .background(bucket.color.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
                        .border(1.dp, bucket.color.copy(alpha = 0.22f), RoundedCornerShape(18.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(bucket.color.copy(alpha = 0.14f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            bucket.icon,
                            contentDescription = null,
                            tint = bucket.color,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = bucket.label, fontSize = 14.sp, fontWeight = AppTheme.fwBold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = bucket.subtitle,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = bucket.count.toString(),
                            fontSize = 16.sp,
                            fontWeight = AppTheme.fwExtraBold
                        )
                        Text(
                            text = "$percent%",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
