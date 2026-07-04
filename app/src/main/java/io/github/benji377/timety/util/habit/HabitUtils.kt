package io.github.benji377.timety.util.habit

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.benji377.timety.R
import io.github.benji377.timety.data.model.habit.HabitEntity
import io.github.benji377.timety.data.model.habit.HabitFrequency
import io.github.benji377.timety.data.model.habit.HabitWithCompletions
import io.github.benji377.timety.util.datetime.AppDateUtils
import io.github.benji377.timety.util.habit.HabitUtils.parseWeekdays
import io.github.benji377.timety.util.habit.HabitUtils.serializeWeekdays
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

/**
 * Utility functions for habit-related logic. Mirrors `utils/habit/habit_utils.dart`
 * plus the day-to-day query helpers that live on Flutter's `HabitProvider`
 * (`isCompletedOn`, `getCompletionsThisWeek`, `getHabitsForDay`'s per-habit predicate).
 *
 * Kotlin's [HabitEntity.targetWeekdays] stores the weekday set as a JSON-ish string
 * (`"[1,3,5]"`, 1=Monday..7=Sunday) instead of Flutter's `List<int>`; [parseWeekdays]/
 * [serializeWeekdays] are the shared codec so the list/detail screens agree on the format.
 */
object HabitUtils {

    /** Parses the "[1,3,5]"-style stored string into a set of ISO weekdays (1=Mon..7=Sun). */
    fun parseWeekdays(raw: String?): Set<Int> {
        if (raw.isNullOrBlank()) return emptySet()
        return raw.removePrefix("[").removeSuffix("]")
            .split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .toSet()
    }

    /** Serializes a set of ISO weekdays back into the "[1,3,5]"-style stored string. */
    fun serializeWeekdays(days: Set<Int>): String =
        days.sorted().joinToString(separator = ",", prefix = "[", postfix = "]")

    /** Checks whether [hwc] has a completion on [date]. Mirrors `HabitProvider.isCompletedOn`. */
    fun isCompletedOn(hwc: HabitWithCompletions, date: LocalDate): Boolean =
        hwc.completions.any {
            it.completionDate.atZone(ZoneId.systemDefault()).toLocalDate() == date
        }

    /**
     * Counts completions within the current real calendar week (Monday..Sunday), optionally
     * excluding today. Mirrors `HabitProvider.getCompletionsThisWeek` - always relative to
     * "now", independent of any stats-screen week navigation.
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

    /** Mirrors `HabitListScreen._isHabitDueToday`. */
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

    /** Mirrors `HabitListScreen._isWeeklyGoalMet`. */
    fun isWeeklyGoalMet(hwc: HabitWithCompletions): Boolean {
        if (hwc.habit.frequency != HabitFrequency.WEEKLY_FLEXIBLE) return false
        val doneThisWeek = getCompletionsThisWeek(hwc, includeToday = false)
        return doneThisWeek >= (hwc.habit.targetDaysPerWeek ?: 1)
    }

    /**
     * Builds the frequency-based subtitle for a habit tile. Mirrors `HabitUtils.buildHabitSubtitle`
     * (time-of-day suffix, if any, is appended by the caller - same split as Flutter).
     */
    @Composable
    fun buildHabitSubtitle(habit: HabitEntity, completionsThisWeek: Int): String {
        return when (habit.frequency) {
            HabitFrequency.DAILY -> stringResource(R.string.habitFreqDaily)
            HabitFrequency.WEEKLY_EXACT -> {
                val locale = Locale.getDefault()
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
     * Determines if a habit in a stack is locked based on previous habit completion.
     * Mirrors `HabitUtils.isHabitLocked`.
     */
    fun isHabitLocked(
        index: Int,
        isCurrentHabitDone: Boolean,
        isPreviousHabitDone: Boolean
    ): Boolean =
        index > 0 && !isCurrentHabitDone && !isPreviousHabitDone

    /** Checks if all habits in a stack are completed for a given date. Mirrors `HabitUtils.isStackFullyCompleted`. */
    fun isStackFullyCompleted(stackHabits: List<HabitWithCompletions>, date: LocalDate): Boolean {
        if (stackHabits.isEmpty()) return false
        return stackHabits.all { isCompletedOn(it, date) }
    }

    /** Gets the completion count for a stack on a specific date. Mirrors `HabitUtils.getStackCompletionCount`. */
    fun getStackCompletionCount(stackHabits: List<HabitWithCompletions>, date: LocalDate): Int =
        stackHabits.count { isCompletedOn(it, date) }
}
