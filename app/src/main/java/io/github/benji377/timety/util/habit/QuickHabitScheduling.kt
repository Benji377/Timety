package io.github.benji377.timety.util.habit

import java.time.Instant
import java.time.ZoneId

/**
 * Pure computation of when a quick habit should next fire. Kept separate from the Android
 * scheduling code so the chain logic is unit-testable.
 */
object QuickHabitScheduling {

    const val MINUTES_PER_DAY = 24 * 60

    /**
     * The next instant at or after [now] when a quick habit firing every [intervalMinutes] should
     * nag, honoring an optional active window and an optional set of allowed weekdays. Occurrences
     * are anchored to the window start each day; once the day's window is exhausted the chain
     * resumes at the next day's start.
     *
     * @param startMinuteOfDay window start in minutes from midnight, or null for all-day.
     * @param endMinuteOfDay window end in minutes from midnight, or null for all-day.
     * @param allowedWeekdays weekday numbers (Mon=1..Sun=7) the habit may fire on; empty = every day.
     */
    fun nextTrigger(
        now: Instant,
        intervalMinutes: Int,
        startMinuteOfDay: Int?,
        endMinuteOfDay: Int?,
        allowedWeekdays: Set<Int> = emptySet(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): Instant {
        val naive = nextTriggerIgnoringWeekdays(
            now, intervalMinutes, startMinuteOfDay, endMinuteOfDay, zone
        )
        if (allowedWeekdays.isEmpty()) return naive

        val naiveDate = naive.atZone(zone).toLocalDate()
        if (naiveDate.dayOfWeek.value in allowedWeekdays) return naive

        // The naive day is excluded, and every occurrence on a day shares that day's weekday, so
        // roll forward to the window start of the next allowed day.
        val startMin = (startMinuteOfDay ?: 0).toLong()
        var date = naiveDate.plusDays(1)
        repeat(7) {
            if (date.dayOfWeek.value in allowedWeekdays) {
                return date.atStartOfDay(zone).plusMinutes(startMin).toInstant()
            }
            date = date.plusDays(1)
        }
        return naive // Unreachable while allowedWeekdays is non-empty.
    }

    private fun nextTriggerIgnoringWeekdays(
        now: Instant,
        intervalMinutes: Int,
        startMinuteOfDay: Int?,
        endMinuteOfDay: Int?,
        zone: ZoneId,
    ): Instant {
        val interval = intervalMinutes.coerceAtLeast(1).toLong()
        // Treat a missing/degenerate window as all day.
        val startMin = (startMinuteOfDay ?: 0).toLong()
        val endMin = (endMinuteOfDay ?: MINUTES_PER_DAY).toLong()
        val allDay = startMinuteOfDay == null || endMinuteOfDay == null || endMin <= startMin

        val zonedNow = now.atZone(zone)
        val todayMidnight = zonedNow.toLocalDate().atStartOfDay(zone)
        val windowStart = todayMidnight.plusMinutes(startMin)
        val windowEnd = if (allDay) todayMidnight.plusMinutes(MINUTES_PER_DAY.toLong())
        else todayMidnight.plusMinutes(endMin)
        val nextDayStart = windowStart.plusDays(1)

        return when {
            // Before today's window opens: fire at the window start.
            now.isBefore(windowStart.toInstant()) -> windowStart.toInstant()
            // Today's window is over: fire at tomorrow's window start.
            !now.isBefore(windowEnd.toInstant()) -> nextDayStart.toInstant()
            else -> {
                // Inside the window: step to the first occurrence strictly after now.
                val minutesSinceStart =
                    java.time.Duration.between(windowStart, zonedNow).toMinutes()
                val steps = (minutesSinceStart / interval) + 1
                val candidate = windowStart.plusMinutes(steps * interval)
                // The window end is inclusive so an "08:00–20:00" habit still fires at 20:00.
                if (!candidate.toInstant().isAfter(windowEnd.toInstant())) candidate.toInstant()
                else nextDayStart.toInstant()
            }
        }
    }
}
