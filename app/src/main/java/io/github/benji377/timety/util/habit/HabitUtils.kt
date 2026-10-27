package io.github.benji377.timety.util.habit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import io.github.benji377.timety.R
import io.github.benji377.timety.data.model.habit.HabitEntity
import io.github.benji377.timety.data.model.habit.HabitFrequency
import io.github.benji377.timety.data.model.habit.HabitWithCompletions
import io.github.benji377.timety.util.datetime.AppDateUtils
import io.github.benji377.timety.util.habit.QuickHabitScheduling.MINUTES_PER_DAY
import io.github.benji377.timety.util.habit.HabitUtils.parseWeekdays
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

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

    /** The most reminder times a single habit can have. */
    const val MAX_REMINDER_TIMES = 5

    /** The highest target of a weekly flexible habit: every day of the week. */
    const val MAX_TARGET_PER_WEEK = 7

    /** The highest target of a monthly flexible habit, reachable even in February. */
    const val MAX_TARGET_PER_MONTH = 28

    /** Parses a stored `"[480,1200]"` reminder list into sorted, distinct minutes from midnight. */
    fun parseReminderTimes(raw: String?): List<Int> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.removePrefix("[").removeSuffix("]")
            .split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it in 0 until MINUTES_PER_DAY }
            .distinct()
            .sorted()
            .take(MAX_REMINDER_TIMES)
    }

    /** Serializes reminder times into the format read by [parseReminderTimes]; null when empty. */
    fun serializeReminderTimes(times: Collection<Int>): String? =
        times.takeIf { it.isNotEmpty() }
            ?.sorted()
            ?.joinToString(separator = ",", prefix = "[", postfix = "]")

    // Compares instants against the day's bounds, computed once, rather than converting every
    // completion to a local date: these run per habit tile on every recomposition.
    fun isCompletedOn(hwc: HabitWithCompletions, date: LocalDate): Boolean {
        val zone = ZoneId.systemDefault()
        val start = date.atStartOfDay(zone).toInstant()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant()
        return hwc.completions.any { !it.completionDate.isBefore(start) && it.completionDate.isBefore(end) }
    }

    /**
     * Counts completions in the current period of [hwc]'s flexible target: the calendar month for
     * monthly habits, otherwise the Monday-based week. [includeToday] set to `false` excludes
     * today, which callers use when checking whether a flexible-frequency goal is still open.
     */
    fun getCompletionsThisPeriod(hwc: HabitWithCompletions, includeToday: Boolean = true): Int {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()
        val (firstDay, nextFirstDay) = if (hwc.habit.frequency == HabitFrequency.MONTHLY_FLEXIBLE) {
            today.withDayOfMonth(1).let { it to it.plusMonths(1) }
        } else {
            AppDateUtils.startOfWeekMonday(today).let { it to it.plusDays(7) }
        }
        val periodStart = firstDay.atStartOfDay(zone).toInstant()
        val periodEnd = nextFirstDay.atStartOfDay(zone).toInstant()
        val todayStart = today.atStartOfDay(zone).toInstant()
        val todayEnd = today.plusDays(1).atStartOfDay(zone).toInstant()
        return hwc.completions.count {
            val at = it.completionDate
            val isInPeriod = !at.isBefore(periodStart) && at.isBefore(periodEnd)
            val isToday = !at.isBefore(todayStart) && at.isBefore(todayEnd)
            isInPeriod && (includeToday || !isToday)
        }
    }

    /**
     * Whether [hwc] should appear in today's habit list: daily habits always are, exact-weekday
     * habits check today against their target days, and weekly flexible habits are due as long as
     * this week's target count hasn't already been reached (excluding today's own completion).
     * Monthly flexible habits have no due days, so they never are: they stay off Home, the widget,
     * reminders, and the habit notifications, and are logged from the Habits screen.
     */
    fun isHabitDueToday(hwc: HabitWithCompletions): Boolean {
        val habit = hwc.habit
        return when (habit.frequency) {
            HabitFrequency.DAILY -> true
            HabitFrequency.WEEKLY_EXACT -> parseWeekdays(habit.targetWeekdays).contains(LocalDate.now().dayOfWeek.value)
            HabitFrequency.WEEKLY_FLEXIBLE -> getCompletionsThisPeriod(
                hwc,
                includeToday = false
            ) < (habit.targetCount ?: 1)

            HabitFrequency.MONTHLY_FLEXIBLE -> false
        }
    }

    /**
     * Whether a reminder for [hwc] should fire today: the habit is on today's list and not yet
     * done today. A weekly flexible habit whose target is met drops off the list, so it stops
     * reminding without a separate check; a monthly one is never on it.
     */
    fun needsReminderToday(hwc: HabitWithCompletions): Boolean =
        isHabitDueToday(hwc) && !isCompletedOn(hwc, LocalDate.now())

    /**
     * When [hwc]'s next reminder should ring after [now], or null if it has none. A habit keeps a
     * single pending reminder: today's remaining times are skipped once it no longer needs one
     * today, later days ring on every day the habit can be due, and the receiver checks again when
     * one fires (a flexible habit's weekly target can be met in the meantime). Monthly flexible
     * habits never remind.
     */
    fun nextReminderAt(hwc: HabitWithCompletions, now: ZonedDateTime): ZonedDateTime? {
        if (hwc.habit.frequency == HabitFrequency.MONTHLY_FLEXIBLE) return null
        val times = parseReminderTimes(hwc.habit.reminderTimes)
        if (times.isEmpty()) return null
        val weekdays = parseWeekdays(hwc.habit.targetWeekdays)
        val today = now.toLocalDate()
        // Eight days, so a weekly habit whose only time today has passed finds next week's.
        for (offset in 0L..7L) {
            val day = today.plusDays(offset)
            val canBeDue = hwc.habit.frequency != HabitFrequency.WEEKLY_EXACT ||
                day.dayOfWeek.value in weekdays
            if (!canBeDue || (offset == 0L && !needsReminderToday(hwc))) continue
            // The earliest by instant, whatever the stored order or a DST gap does to the times.
            times.map { day.atTime(it / 60, it % 60).atZone(now.zone) }
                .filter { it.isAfter(now) }
                .minOrNull()
                ?.let { return it }
        }
        return null
    }

    /** Whether a flexible-frequency habit already met this week's or month's target before today. */
    fun isPeriodGoalMet(hwc: HabitWithCompletions): Boolean {
        if (!hwc.habit.frequency.isFlexible) return false
        val doneThisPeriod = getCompletionsThisPeriod(hwc, includeToday = false)
        return doneThisPeriod >= (hwc.habit.targetCount ?: 1)
    }

    /** Localized subtitle describing a habit's schedule, e.g. "Daily" or "3/5 this week". */
    @Composable
    @ReadOnlyComposable
    fun buildHabitSubtitle(habit: HabitEntity, completionsThisPeriod: Int): String {
        return when (habit.frequency) {
            HabitFrequency.DAILY -> stringResource(R.string.habitFreqDaily)
            HabitFrequency.WEEKLY_EXACT -> {
                val locale = LocalLocale.current.platformLocale
                val days = parseWeekdays(habit.targetWeekdays).sorted()
                    .joinToString(", ") { AppDateUtils.weekdayToStringShort(locale, it) }
                stringResource(R.string.habitFreqWeekly, days)
            }

            HabitFrequency.WEEKLY_FLEXIBLE -> {
                val target = habit.targetCount ?: 0
                stringResource(R.string.habitFreqFlexible, completionsThisPeriod, target)
            }

            HabitFrequency.MONTHLY_FLEXIBLE -> {
                val target = habit.targetCount ?: 0
                stringResource(R.string.habitFreqFlexibleMonth, completionsThisPeriod, target)
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
