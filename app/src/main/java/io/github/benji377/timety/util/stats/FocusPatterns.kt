package io.github.benji377.timety.util.stats

import io.github.benji377.timety.data.model.focus.DistractionEntity
import io.github.benji377.timety.data.model.focus.FocusSessionEntity
import io.github.benji377.timety.data.model.user.DayRating
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** A part of the day, using the same hours as the app's time-of-day labels. */
enum class DayPart {
    MORNING,
    AFTERNOON,
    EVENING,
    NIGHT;

    companion object {
        /** The part of the day containing [hour] (0 to 23). */
        fun of(hour: Int): DayPart = when (hour) {
            in 5..11 -> MORNING
            in 12..16 -> AFTERNOON
            in 17..20 -> EVENING
            else -> NIGHT
        }
    }
}

/** The weekday and part of the day holding the most focus minutes. */
data class FocusPeak(val weekday: DayOfWeek, val dayPart: DayPart)

/**
 * The recurring patterns on the Patterns page, always over the last [WINDOW_WEEKS] weeks. Results
 * that would rest on too little data come back null, so the page never overstates a few days.
 */
object FocusPatterns {
    const val WINDOW_WEEKS = 12L
    const val HOURS_PER_SLOT = 4

    private const val DAYS_PER_WEEK = 7L
    private const val HOURS_PER_DAY = 24
    private const val SECONDS_PER_MINUTE = 60L
    private const val MIN_SESSIONS = 20
    private const val MIN_COMPLETED_TASKS = 20
    private const val MIN_DISTRACTIONS = 10
    private const val MIN_RATED_DAYS = 14
    private const val MIN_DAYS_PER_MOOD = 3
    private const val MIN_MOODS_TO_COMPARE = 2

    /** The days the patterns cover, ending on [today]. */
    fun window(today: LocalDate): ClosedRange<LocalDate> =
        today.minusDays(WINDOW_WEEKS * DAYS_PER_WEEK - 1)..today

    /**
     * Focus minutes per weekday and [HOURS_PER_SLOT]-hour slot of the day, indexed
     * `[weekday.value - 1][slot]`. A session fills the clock from its start for as long as it focused.
     */
    fun weekdaySlots(
        sessions: List<FocusSessionEntity>,
        window: ClosedRange<LocalDate>,
        zone: ZoneId,
    ): List<List<Int>> = hourGrid(sessions, window, zone).map { hours ->
        hours.toList().chunked(HOURS_PER_SLOT) { it.sum() }
    }

    /** When focus peaks, or null below [MIN_SESSIONS] sessions in [window]. */
    fun focusPeak(
        sessions: List<FocusSessionEntity>,
        window: ClosedRange<LocalDate>,
        zone: ZoneId,
    ): FocusPeak? {
        val inWindow = sessions.count { it.startTime.atZone(zone).toLocalDate() in window }
        if (inWindow < MIN_SESSIONS) return null

        val grid = hourGrid(sessions, window, zone)
        return DayOfWeek.entries.flatMap { weekday ->
            DayPart.entries.map { part ->
                val minutes = (0 until HOURS_PER_DAY)
                    .filter { DayPart.of(it) == part }
                    .sumOf { grid[weekday.value - 1][it] }
                FocusPeak(weekday, part) to minutes
            }
        }.maxByOrNull { it.second }?.takeIf { it.second > 0 }?.first
    }

    /**
     * Average focus minutes per weekday, starting from the first day with focus so a new user's
     * averages aren't diluted by weeks before they started. Empty without any focus in [window].
     */
    fun averageMinutesByWeekday(
        minutesByDay: Map<LocalDate, Int>,
        window: ClosedRange<LocalDate>,
    ): Map<DayOfWeek, Int> {
        val inWindow = minutesByDay.filterKeys { it in window }
        val firstDay = inWindow.keys.minOrNull() ?: return emptyMap()
        val days = generateSequence(firstDay) { it.plusDays(1) }
            .takeWhile { !it.isAfter(window.endInclusive) }
            .toList()
        return days.groupBy { it.dayOfWeek }.mapValues { (_, weekdays) ->
            weekdays.sumOf { inWindow[it] ?: 0 } / weekdays.size
        }
    }

    /** The weekday with the most [completions], or null below [MIN_COMPLETED_TASKS] in [window]. */
    fun busiestTaskWeekday(
        completions: List<Instant>,
        window: ClosedRange<LocalDate>,
        zone: ZoneId,
    ): DayOfWeek? {
        val days = completions.map { it.atZone(zone).toLocalDate() }.filter { it in window }
        if (days.size < MIN_COMPLETED_TASKS) return null
        return days.groupingBy { it.dayOfWeek }.eachCount().maxByOrNull { it.value }?.key
    }

    /**
     * The median minutes into a session at which a distraction was logged, or null below
     * [MIN_DISTRACTIONS] in [window].
     */
    fun medianDistractionMinute(
        distractions: List<Pair<FocusSessionEntity, DistractionEntity>>,
        window: ClosedRange<LocalDate>,
        zone: ZoneId,
    ): Int? {
        val minutes = distractions
            .filter { (_, distraction) -> distraction.time.atZone(zone).toLocalDate() in window }
            .map { (session, distraction) ->
                Duration.between(session.startTime, distraction.time).toMinutes().toInt().coerceAtLeast(0)
            }
            .sorted()
        if (minutes.size < MIN_DISTRACTIONS) return null
        return minutes[minutes.size / 2]
    }

    /**
     * Average focus minutes on the days rated each mood. Moods with fewer than [MIN_DAYS_PER_MOOD]
     * days are left out. Null below [MIN_RATED_DAYS] rated days or with nothing left to compare.
     */
    fun focusByMood(
        moodsByDay: Map<LocalDate, DayRating>,
        minutesByDay: Map<LocalDate, Int>,
        window: ClosedRange<LocalDate>,
    ): Map<DayRating, Int>? {
        val rated = moodsByDay.filterKeys { it in window }
        if (rated.size < MIN_RATED_DAYS) return null
        val averages = rated.entries.groupBy({ it.value }, { it.key })
            .filterValues { it.size >= MIN_DAYS_PER_MOOD }
            .mapValues { (_, days) -> days.sumOf { minutesByDay[it] ?: 0 } / days.size }
        return averages.takeIf { it.size >= MIN_MOODS_TO_COMPARE }
    }

    /** How many [completions] fall in each part of the day within [window]. */
    fun completionsByDayPart(
        completions: List<Instant>,
        window: ClosedRange<LocalDate>,
        zone: ZoneId,
    ): Map<DayPart, Int> = completions
        .map { it.atZone(zone) }
        .filter { it.toLocalDate() in window }
        .groupingBy { DayPart.of(it.hour) }
        .eachCount()

    private fun hourGrid(
        sessions: List<FocusSessionEntity>,
        window: ClosedRange<LocalDate>,
        zone: ZoneId,
    ): List<IntArray> {
        val grid = List(DayOfWeek.entries.size) { IntArray(HOURS_PER_DAY) }
        sessions.forEach { session ->
            var cursor = session.startTime.atZone(zone)
            var secondsLeft = session.totalSecondsFocused.toLong()
            while (secondsLeft > 0) {
                val nextHour = cursor.plusHours(1).withMinute(0).withSecond(0).withNano(0)
                val seconds = minOf(secondsLeft, Duration.between(cursor, nextHour).seconds)
                if (cursor.toLocalDate() in window) {
                    grid[cursor.dayOfWeek.value - 1][cursor.hour] += (seconds / SECONDS_PER_MINUTE).toInt()
                }
                cursor = nextHour
                secondsLeft -= seconds
            }
        }
        return grid
    }
}
