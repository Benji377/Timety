package io.github.benji377.timety.util.habit

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Pure computation of when a quick habit should next fire. Kept separate from the Android
 * scheduling code so the chain logic is unit-testable.
 */
object QuickHabitScheduling {

    const val MINUTES_PER_DAY = 24 * 60

    /**
     * The next instant strictly after [now] when a quick habit firing every [intervalMinutes]
     * should nag, honoring an optional active window and an optional set of allowed weekdays.
     * Occurrences are anchored to the window start each day; once a day's window is exhausted
     * the chain resumes at the next allowed day's start.
     *
     * Window edges are wall-clock times, so they hold on daylight-saving days too. An end before
     * the start is a window across midnight ("22:00 to 06:00"); the weekday is the one it opens on.
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
        val interval = intervalMinutes.coerceAtLeast(1).toLong()
        val window = window(startMinuteOfDay, endMinuteOfDay)
        val today = now.atZone(zone).toLocalDate()
        // Yesterday's window can still be open after midnight when it crosses it.
        for (offset in -1L..DAYS_TO_SEARCH) {
            val day = today.plusDays(offset)
            if (allowedWeekdays.isNotEmpty() && day.dayOfWeek.value !in allowedWeekdays) continue
            nextInWindow(now, day, window, interval, zone)?.let { return it }
        }
        // Unreachable: every day has a window and the weekday set covers at least one day a week.
        return now.plus(Duration.ofMinutes(interval))
    }

    /**
     * The window's length in minutes: a full day for all-day, wrapping past midnight when the
     * end is before the start. The editor keeps the interval within it.
     */
    fun windowLengthMinutes(startMinuteOfDay: Int?, endMinuteOfDay: Int?): Int {
        val (start, length) = window(startMinuteOfDay, endMinuteOfDay)
        return if (start == null) MINUTES_PER_DAY else length
    }

    /** The start time (null for all-day) and the length in minutes. A zero-length window is all day. */
    private fun window(startMinuteOfDay: Int?, endMinuteOfDay: Int?): Pair<LocalTime?, Int> {
        if (startMinuteOfDay == null || endMinuteOfDay == null || startMinuteOfDay == endMinuteOfDay) {
            return null to MINUTES_PER_DAY
        }
        val length = Math.floorMod(endMinuteOfDay - startMinuteOfDay, MINUTES_PER_DAY)
        return LocalTime.MIDNIGHT.plusMinutes(startMinuteOfDay.toLong()) to length
    }

    /** The first occurrence after [now] in the window that opens on [day], or null if it's over. */
    private fun nextInWindow(
        now: Instant,
        day: LocalDate,
        window: Pair<LocalTime?, Int>,
        interval: Long,
        zone: ZoneId,
    ): Instant? {
        val (start, length) = window
        val open = day.atTime(start ?: LocalTime.MIDNIGHT)
        // All day runs to the next midnight; the end is inclusive so "08:00 to 20:00" fires at 20:00.
        val close = open.plusMinutes(length.toLong())
        val nowLocal = LocalDateTime.ofInstant(now, zone)
        var step = if (nowLocal.isBefore(open)) 0L
        else Duration.between(open, nowLocal).toMinutes() / interval
        while (true) {
            val candidate = open.plusMinutes(step * interval)
            if (candidate.isAfter(close)) return null
            // A time skipped by a clock change resolves forward; one repeated takes its first instant.
            val instant = candidate.atZone(zone).toInstant()
            if (instant.isAfter(now)) return instant
            step++
        }
    }

    private const val DAYS_TO_SEARCH = 8L
}
