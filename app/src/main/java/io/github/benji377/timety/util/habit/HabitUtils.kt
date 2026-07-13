package io.github.benji377.timety.util.habit

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import io.github.benji377.timety.R
import io.github.benji377.timety.data.model.habit.HabitEntity
import io.github.benji377.timety.data.model.habit.HabitFrequency
import io.github.benji377.timety.data.model.habit.HabitWithCompletions
import io.github.benji377.timety.util.datetime.AppDateUtils
import io.github.benji377.timety.util.habit.HabitUtils.parseWeekdays
import java.time.LocalDate
import java.time.ZoneId


/** Habit scheduling, completion, and stack-locking rules shared by the habit list and widgets. */
object HabitUtils {


    /** Parses a `"[1,3,5]"`-style stored weekday set back into ISO weekday numbers (1 = Monday). */
    fun parseWeekdays(raw: String?): Set<Int> {
        if (raw.isNullOrBlank()) return emptySet()
        return raw.removePrefix("[").removeSuffix("]")
            .split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .toSet()
    }


    /** Serializes weekday numbers into the `"[1,3,5]"` format read by [parseWeekdays]. */
    fun serializeWeekdays(days: Set<Int>): String =
        days.sorted().joinToString(separator = ",", prefix = "[", postfix = "]")


    fun isCompletedOn(hwc: HabitWithCompletions, date: LocalDate): Boolean =
        hwc.completions.any {
            it.completionDate.atZone(ZoneId.systemDefault()).toLocalDate() == date
        }


    /**
     * Counts completions in the current Monday-based week. [includeToday] set to `false` excludes
     * today, which callers use when checking whether a flexible-frequency goal is still open.
     */
    fun getCompletionsThisWeek(hwc: HabitWithCompletions, includeToday: Boolean = true): Int {
        val today = LocalDate.now()
        val startOfWeek = AppDateUtils.startOfWeekMonday(today)
        val startOfNextWeek = startOfWeek.plusDays(7)
        return hwc.completions.count {
            val date = it.completionDate.atZone(ZoneId.systemDefault()).toLocalDate()
            val isInWeek = !date.isBefore(startOfWeek) && date.isBefore(startOfNextWeek)
            isInWeek && (includeToday || date != today)
        }
    }


    /**
     * Whether [hwc] should appear in today's habit list: daily habits always are, exact-weekday
     * habits check today against their target days, and flexible habits are due as long as this
     * week's target count hasn't already been reached (excluding today's own completion).
     */
    fun isHabitDueToday(hwc: HabitWithCompletions): Boolean {
        val habit = hwc.habit
        return when (habit.frequency) {
            HabitFrequency.DAILY -> true
            HabitFrequency.WEEKLY_EXACT -> parseWeekdays(habit.targetWeekdays).contains(LocalDate.now().dayOfWeek.value)
            HabitFrequency.WEEKLY_FLEXIBLE -> getCompletionsThisWeek(
                hwc,
                includeToday = false
            ) < (habit.targetDaysPerWeek ?: 1)
        }
    }


    /** Whether a flexible-frequency habit already met its weekly target before today. */
    fun isWeeklyGoalMet(hwc: HabitWithCompletions): Boolean {
        if (hwc.habit.frequency != HabitFrequency.WEEKLY_FLEXIBLE) return false
        val doneThisWeek = getCompletionsThisWeek(hwc, includeToday = false)
        return doneThisWeek >= (hwc.habit.targetDaysPerWeek ?: 1)
    }


    /** Localized subtitle describing a habit's schedule, e.g. "Daily" or "3/5 this week". */
    @Composable
    fun buildHabitSubtitle(habit: HabitEntity, completionsThisWeek: Int): String {
        return when (habit.frequency) {
            HabitFrequency.DAILY -> stringResource(R.string.habitFreqDaily)
            HabitFrequency.WEEKLY_EXACT -> {
                val locale = LocalLocale.current.platformLocale
                val days = parseWeekdays(habit.targetWeekdays).sorted()
                    .joinToString(", ") { AppDateUtils.weekdayToStringShort(locale, it) }
                stringResource(R.string.habitFreqWeekly, days)
            }

            HabitFrequency.WEEKLY_FLEXIBLE -> {
                val target = habit.targetDaysPerWeek ?: 0
                stringResource(R.string.habitFreqFlexible, completionsThisWeek, target)
            }
        }
    }


    /**
     * A stacked habit is locked until the one before it in the stack is done: the first habit in
     * a stack ([index] 0) is never locked, and any already-completed habit is never locked either.
     */
    fun isHabitLocked(
        index: Int,
        isCurrentHabitDone: Boolean,
        isPreviousHabitDone: Boolean
    ): Boolean =
        index > 0 && !isCurrentHabitDone && !isPreviousHabitDone


    fun isStackFullyCompleted(stackHabits: List<HabitWithCompletions>, date: LocalDate): Boolean {
        if (stackHabits.isEmpty()) return false
        return stackHabits.all { isCompletedOn(it, date) }
    }


    fun getStackCompletionCount(stackHabits: List<HabitWithCompletions>, date: LocalDate): Int =
        stackHabits.count { isCompletedOn(it, date) }
}
