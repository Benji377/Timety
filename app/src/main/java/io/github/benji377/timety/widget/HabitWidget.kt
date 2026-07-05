package io.github.benji377.timety.widget

import android.content.Context
import android.content.Intent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.lazy.itemsIndexed
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import io.github.benji377.timety.MainActivity
import io.github.benji377.timety.R
import io.github.benji377.timety.TimetyApplication
import io.github.benji377.timety.data.model.habit.HabitFrequency
import io.github.benji377.timety.data.model.habit.HabitWithCompletions
import io.github.benji377.timety.ui.theme.HabitColor
import io.github.benji377.timety.ui.theme.SuccessColor
import io.github.benji377.timety.util.datetime.AppDateUtils
import io.github.benji377.timety.util.habit.HabitUtils
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.util.Locale

/** One renderable line of the habit widget; flattened so the whole widget can scroll as one list. */
private sealed interface HabitWidgetRow {
    data class StackHeader(val name: String, val completed: Int, val total: Int) : HabitWidgetRow
    data class Habit(
        val name: String,
        val subtitle: String,
        val isDone: Boolean,
        val isLocked: Boolean,
        val isStacked: Boolean,
    ) : HabitWidgetRow

    data object StackFooter : HabitWidgetRow
}

/**
 * Home-screen widget listing the habits due today, stacks first (in stack order, locked steps
 * dimmed), then standalone habits. Styled after the app's neobrutalist cards; scrollable when
 * the list outgrows the widget. Tapping anywhere opens the app.
 */
class HabitWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appContainer = (context.applicationContext as TimetyApplication).container
        val habitRepository = appContainer.habitRepository

        val today = LocalDate.now()
        // Due-today/completed-today checks only look at the current Monday-based week, so a
        // week-bounded query is enough (padded a day for zone-edge completions).
        val weekCutoff = AppDateUtils.startOfWeekMonday(today)
            .minusDays(1)
            .atStartOfDay(java.time.ZoneId.systemDefault())
            .toInstant()
        val habits = habitRepository.allHabits.first()
        val completionsByHabit = habitRepository.getCompletionsSince(weekCutoff)
            .groupBy { it.habitId }
        val habitsWithCompletions = habits.map { habit ->
            HabitWithCompletions(habit = habit, completions = completionsByHabit[habit.id].orEmpty())
        }

        val todayHabits = habitsWithCompletions.filter { HabitUtils.isHabitDueToday(it) }
        val completionStatus =
            todayHabits.associate { it.habit.id to HabitUtils.isCompletedOn(it, today) }

        val grouped = todayHabits
            .filter { !it.habit.stackName?.trim().isNullOrEmpty() }
            .groupBy { it.habit.stackName!!.trim() }
        val standalone = todayHabits.filter { it.habit.stackName?.trim().isNullOrEmpty() }

        val rows = buildList {
            for ((stackName, stackHabitsList) in grouped) {
                val stackHabits = stackHabitsList.sortedWith(
                    compareBy<HabitWithCompletions> { completionStatus[it.habit.id] == true }
                        .thenBy { it.habit.stackOrder ?: 99 }
                )
                val completedInStack = stackHabits.count { completionStatus[it.habit.id] == true }
                add(HabitWidgetRow.StackHeader(stackName, completedInStack, stackHabits.size))
                stackHabits.forEachIndexed { i, hwc ->
                    val isDone = completionStatus[hwc.habit.id] ?: false
                    val prevDone =
                        if (i == 0) true else completionStatus[stackHabits[i - 1].habit.id] ?: false
                    add(
                        HabitWidgetRow.Habit(
                            name = hwc.habit.name,
                            subtitle = frequencySubtitle(context, hwc),
                            isDone = isDone,
                            isLocked = HabitUtils.isHabitLocked(i, isDone, prevDone),
                            isStacked = true,
                        )
                    )
                }
                add(HabitWidgetRow.StackFooter)
            }
            standalone.forEach { hwc ->
                add(
                    HabitWidgetRow.Habit(
                        name = hwc.habit.name,
                        subtitle = frequencySubtitle(context, hwc),
                        isDone = completionStatus[hwc.habit.id] ?: false,
                        isLocked = false,
                        isStacked = false,
                    )
                )
            }
        }

        provideContent {
            GlanceTheme {
                val openApp = actionStartActivity(Intent(context, MainActivity::class.java))
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(ImageProvider(R.drawable.widget_background))
                        .padding(12.dp)
                        .clickable(openApp),
                ) {
                    // Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = GlanceModifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        Text(
                            text = context.getString(R.string.widgetHabitsToday),
                            style = TextStyle(
                                color = ColorProvider(HabitColor),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            modifier = GlanceModifier.defaultWeight()
                        )
                        Spacer(modifier = GlanceModifier.width(8.dp))
                        Text(
                            text = "${todayHabits.size}",
                            style = TextStyle(
                                color = ColorProvider(HabitColor),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        )
                    }

                    if (rows.isEmpty()) {
                        Box(
                            modifier = GlanceModifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = context.getString(R.string.habitScreenEmpty),
                                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant)
                            )
                        }
                    } else {
                        LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                            itemsIndexed(rows) { _, row -> RenderRow(row, openApp) }
                        }
                    }
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun RenderRow(row: HabitWidgetRow, openApp: androidx.glance.action.Action) {
        when (row) {
            is HabitWidgetRow.StackHeader -> Row(
                modifier = GlanceModifier.fillMaxWidth()
                    .background(ImageProvider(R.drawable.widget_habit_stack_header_bg))
                    .padding(8.dp)
                    .clickable(openApp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = row.name.uppercase(Locale.getDefault()),
                    maxLines = 1,
                    style = TextStyle(
                        color = ColorProvider(HabitColor),
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    ),
                    modifier = GlanceModifier.defaultWeight()
                )
                Spacer(modifier = GlanceModifier.width(8.dp))
                Text(
                    text = "${row.completed} / ${row.total}",
                    style = TextStyle(
                        color = ColorProvider(if (row.completed == row.total) SuccessColor else HabitColor),
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                )
            }

            is HabitWidgetRow.Habit -> if (row.isStacked) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth()
                        .background(ImageProvider(R.drawable.widget_habit_stack_item_bg))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .clickable(openApp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HabitRowContent(row)
                }
            } else {
                Column(modifier = GlanceModifier.fillMaxWidth()) {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth()
                            .background(
                                ImageProvider(
                                    if (row.isDone) R.drawable.widget_habit_standalone_done_bg
                                    else R.drawable.widget_habit_standalone_pending_bg
                                )
                            )
                            .padding(8.dp)
                            .clickable(openApp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HabitRowContent(row)
                    }
                    Spacer(modifier = GlanceModifier.height(6.dp))
                }
            }

            HabitWidgetRow.StackFooter -> Column(modifier = GlanceModifier.fillMaxWidth()) {
                Box(
                    modifier = GlanceModifier.fillMaxWidth().height(2.dp)
                        .background(ImageProvider(R.drawable.widget_habit_stack_footer_bg))
                ) {}
                Spacer(modifier = GlanceModifier.height(8.dp))
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun androidx.glance.layout.RowScope.HabitRowContent(row: HabitWidgetRow.Habit) {
        Text(
            text = row.name,
            maxLines = 1,
            style = TextStyle(
                color = if (row.isLocked) GlanceTheme.colors.onSurfaceVariant else GlanceTheme.colors.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                textDecoration = if (row.isDone) TextDecoration.LineThrough else TextDecoration.None
            ),
            modifier = GlanceModifier.defaultWeight()
        )
        Spacer(modifier = GlanceModifier.width(4.dp))
        Text(
            text = row.subtitle,
            maxLines = 1,
            style = TextStyle(
                color = GlanceTheme.colors.onSurfaceVariant,
                fontSize = 12.sp
            )
        )
    }

    /** "Daily" / "Weekly: Mon, Wed" / "3 of 5 this week" line under each habit name. */
    private fun frequencySubtitle(context: Context, hwc: HabitWithCompletions): String =
        when (hwc.habit.frequency) {
            HabitFrequency.DAILY -> context.getString(R.string.habitFreqDaily)
            HabitFrequency.WEEKLY_EXACT -> {
                val days = HabitUtils.parseWeekdays(hwc.habit.targetWeekdays)
                    .sorted()
                    .joinToString(", ") {
                        AppDateUtils.weekdayToStringShort(Locale.getDefault(), it)
                    }
                context.getString(R.string.habitFreqWeekly, days)
            }

            HabitFrequency.WEEKLY_FLEXIBLE -> context.getString(
                R.string.habitFreqFlexible,
                HabitUtils.getCompletionsThisWeek(hwc),
                hwc.habit.targetDaysPerWeek ?: 0
            )
        }
}
