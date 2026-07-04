package io.github.benji377.timety.widget

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
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
import io.github.benji377.timety.MainActivity
import io.github.benji377.timety.R
import io.github.benji377.timety.TimetyApplication
import io.github.benji377.timety.data.model.habit.HabitFrequency
import io.github.benji377.timety.util.datetime.AppDateUtils
import io.github.benji377.timety.util.habit.HabitUtils
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.util.Locale

class HabitWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appContainer = (context.applicationContext as TimetyApplication).container
        val habitRepository = appContainer.habitRepository

        val habitsFlow = habitRepository.allHabits
        val completionsFlow = habitRepository.allCompletions

        val habitsWithCompletions = combine(habitsFlow, completionsFlow) { habits, completions ->
            habits.map { habit ->
                io.github.benji377.timety.data.model.habit.HabitWithCompletions(
                    habit = habit,
                    completions = completions.filter { it.habitId == habit.id }
                )
            }
        }.first()

        val today = LocalDate.now()
        val todayHabits = habitsWithCompletions.filter { HabitUtils.isHabitDueToday(it) }

        val completionStatus =
            todayHabits.associate { it.habit.id to HabitUtils.isCompletedOn(it, today) }

        val grouped =
            mutableMapOf<String, MutableList<io.github.benji377.timety.data.model.habit.HabitWithCompletions>>()
        val standalone =
            mutableListOf<io.github.benji377.timety.data.model.habit.HabitWithCompletions>()

        for (hwc in todayHabits) {
            val stackName = hwc.habit.stackName?.trim()
            if (!stackName.isNullOrEmpty()) {
                grouped.getOrPut(stackName) { mutableListOf() }.add(hwc)
            } else {
                standalone.add(hwc)
            }
        }

        provideContent {
            val localContext = LocalContext.current
            GlanceTheme {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .background(GlanceTheme.colors.surface)
                        .clickable(
                            actionStartActivity(
                                Intent(
                                    localContext,
                                    MainActivity::class.java
                                )
                            )
                        ),
                    horizontalAlignment = Alignment.Start,
                    verticalAlignment = Alignment.Top
                ) {
                    // Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = GlanceModifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        Text(
                            text = localContext.getString(R.string.widgetHabitsToday),
                            style = TextStyle(
                                color = androidx.glance.unit.ColorProvider(Color(0xFF7C3AED)),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            modifier = GlanceModifier.defaultWeight()
                        )
                        Spacer(modifier = GlanceModifier.width(8.dp))
                        Text(
                            text = "${todayHabits.size}",
                            style = TextStyle(
                                color = androidx.glance.unit.ColorProvider(Color(0xFF7C3AED)),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        )
                    }

                    if (todayHabits.isEmpty()) {
                        Text(
                            text = localContext.getString(R.string.habitScreenEmpty),
                            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant)
                        )
                    } else {
                        // Stacked Habits
                        for ((stackName, stackHabitsList) in grouped) {
                            val stackHabits = stackHabitsList.sortedWith(Comparator { a, b ->
                                val aDone = completionStatus[a.habit.id] ?: false
                                val bDone = completionStatus[b.habit.id] ?: false
                                if (aDone != bDone) if (aDone) 1 else -1
                                else (a.habit.stackOrder ?: 99).compareTo(b.habit.stackOrder ?: 99)
                            })

                            val completedInStack =
                                stackHabits.count { completionStatus[it.habit.id] == true }
                            val totalInStack = stackHabits.size
                            val allDone = completedInStack == totalInStack

                            Column(
                                modifier = GlanceModifier.fillMaxWidth().padding(bottom = 8.dp)
                            ) {
                                // Stack Header
                                Row(
                                    modifier = GlanceModifier.fillMaxWidth()
                                        .background(ImageProvider(R.drawable.widget_habit_stack_header_bg))
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stackName.uppercase(Locale.getDefault()),
                                        maxLines = 1,
                                        style = TextStyle(
                                            color = androidx.glance.unit.ColorProvider(
                                                Color(
                                                    0xFF7C3AED
                                                )
                                            ),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        ),
                                        modifier = GlanceModifier.defaultWeight()
                                    )
                                    Spacer(modifier = GlanceModifier.width(8.dp))
                                    Text(
                                        text = "$completedInStack / $totalInStack",
                                        style = TextStyle(
                                            color = androidx.glance.unit.ColorProvider(
                                                if (allDone) Color(
                                                    0xFF16A34A
                                                ) else Color(0xFF7C3AED)
                                            ),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        )
                                    )
                                }

                                // Stack Items
                                stackHabits.forEachIndexed { i, hwc ->
                                    val isDone = completionStatus[hwc.habit.id] ?: false
                                    val prevDone =
                                        if (i == 0) true else completionStatus[stackHabits[i - 1].habit.id]
                                            ?: false
                                    val isLocked = HabitUtils.isHabitLocked(i, isDone, prevDone)
                                    val completionsThisWeek = HabitUtils.getCompletionsThisWeek(hwc)

                                    val subtitle = when (hwc.habit.frequency) {
                                        HabitFrequency.DAILY -> localContext.getString(R.string.habitFreqDaily)
                                        HabitFrequency.WEEKLY_EXACT -> {
                                            val days =
                                                HabitUtils.parseWeekdays(hwc.habit.targetWeekdays)
                                                    .sorted()
                                                    .joinToString(", ") {
                                                        AppDateUtils.weekdayToStringShort(
                                                            Locale.getDefault(),
                                                            it
                                                        )
                                                    }
                                            localContext.getString(R.string.habitFreqWeekly, days)
                                        }

                                        HabitFrequency.WEEKLY_FLEXIBLE -> {
                                            val target = hwc.habit.targetDaysPerWeek ?: 0
                                            localContext.getString(
                                                R.string.habitFreqFlexible,
                                                completionsThisWeek,
                                                target
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = GlanceModifier.fillMaxWidth()
                                            .background(ImageProvider(R.drawable.widget_habit_stack_item_bg))
                                            .padding(horizontal = 8.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = hwc.habit.name,
                                            maxLines = 1,
                                            style = TextStyle(
                                                color = if (isLocked) GlanceTheme.colors.onSurfaceVariant else GlanceTheme.colors.onSurface,
                                                fontWeight = FontWeight.Bold,
                                                textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None
                                            ),
                                            modifier = GlanceModifier.defaultWeight()
                                        )
                                        Spacer(modifier = GlanceModifier.width(4.dp))
                                        Text(
                                            text = subtitle,
                                            style = TextStyle(
                                                color = GlanceTheme.colors.onSurfaceVariant,
                                                fontSize = 12.sp
                                            )
                                        )
                                    }
                                }

                                // Stack Footer
                                Box(
                                    modifier = GlanceModifier.fillMaxWidth().height(2.dp)
                                        .background(ImageProvider(R.drawable.widget_habit_stack_footer_bg))
                                ) {}
                            }
                        }

                        // Standalone Habits
                        standalone.forEach { hwc ->
                            val isDone = completionStatus[hwc.habit.id] ?: false
                            val completionsThisWeek = HabitUtils.getCompletionsThisWeek(hwc)

                            val subtitle = when (hwc.habit.frequency) {
                                HabitFrequency.DAILY -> localContext.getString(R.string.habitFreqDaily)
                                HabitFrequency.WEEKLY_EXACT -> {
                                    val days =
                                        HabitUtils.parseWeekdays(hwc.habit.targetWeekdays).sorted()
                                            .joinToString(", ") {
                                                AppDateUtils.weekdayToStringShort(
                                                    Locale.getDefault(),
                                                    it
                                                )
                                            }
                                    localContext.getString(R.string.habitFreqWeekly, days)
                                }

                                HabitFrequency.WEEKLY_FLEXIBLE -> {
                                    val target = hwc.habit.targetDaysPerWeek ?: 0
                                    localContext.getString(
                                        R.string.habitFreqFlexible,
                                        completionsThisWeek,
                                        target
                                    )
                                }
                            }

                            Box(modifier = GlanceModifier.fillMaxWidth().padding(bottom = 8.dp)) {
                                Row(
                                    modifier = GlanceModifier.fillMaxWidth()
                                        .background(ImageProvider(if (isDone) R.drawable.widget_habit_standalone_done_bg else R.drawable.widget_habit_standalone_pending_bg))
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = hwc.habit.name,
                                        maxLines = 1,
                                        style = TextStyle(
                                            color = GlanceTheme.colors.onSurface,
                                            fontWeight = FontWeight.Bold,
                                            textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None
                                        ),
                                        modifier = GlanceModifier.defaultWeight()
                                    )
                                    Spacer(modifier = GlanceModifier.width(4.dp))
                                    Text(
                                        text = subtitle,
                                        style = TextStyle(
                                            color = GlanceTheme.colors.onSurfaceVariant,
                                            fontSize = 12.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
