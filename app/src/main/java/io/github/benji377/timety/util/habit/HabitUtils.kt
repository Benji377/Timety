package io.github.benji377.timety.util.habit

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.benji377.timety.R
import io.github.benji377.timety.data.model.habit.HabitEntity
import io.github.benji377.timety.data.model.habit.HabitFrequency
import io.github.benji377.timety.data.model.habit.HabitWithCompletions
import io.github.benji377.timety.util.datetime.AppDateUtils
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale


object HabitUtils {


    fun parseWeekdays(raw: String?): Set<Int> {
        if (raw.isNullOrBlank()) return emptySet()
        return raw.removePrefix("[").removeSuffix("]")
            .split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .toSet()
    }


    fun serializeWeekdays(days: Set<Int>): String =
        days.sorted().joinToString(separator = ",", prefix = "[", postfix = "]")


    fun isCompletedOn(hwc: HabitWithCompletions, date: LocalDate): Boolean =
        hwc.completions.any {
            it.completionDate.atZone(ZoneId.systemDefault()).toLocalDate() == date
        }


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


    fun isWeeklyGoalMet(hwc: HabitWithCompletions): Boolean {
        if (hwc.habit.frequency != HabitFrequency.WEEKLY_FLEXIBLE) return false
        val doneThisWeek = getCompletionsThisWeek(hwc, includeToday = false)
        return doneThisWeek >= (hwc.habit.targetDaysPerWeek ?: 1)
    }


    @Composable
    fun buildHabitSubtitle(habit: HabitEntity, completionsThisWeek: Int): String {
        return when (habit.frequency) {
            HabitFrequency.DAILY -> stringResource(R.string.habitFreqDaily)
            HabitFrequency.WEEKLY_EXACT -> {
                val locale = androidx.compose.ui.platform.LocalLocale.current.platformLocale
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
