package io.github.benji377.timety.util.stats

import io.github.benji377.timety.data.model.focus.FocusSessionEntity
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

/** Days with a focus target so far in a period, and how many of them reached it. */
data class GoalDays(val met: Int, val total: Int)

/** Numbers behind the cards of the Time page. */
object FocusPeriodStats {
    /** The change from [previous] to [current] in percent, or null with nothing to compare to. */
    fun percentChange(current: Int, previous: Int): Int? =
        if (previous <= 0) null else ((current - previous) * 100.0 / previous).roundToInt()

    /** The minutes of [session] that fell in [range]. */
    fun minutesIn(session: FocusSessionEntity, range: ClosedRange<LocalDate>, zone: ZoneId): Int =
        FocusSessionDayStats.minutesByDay(session, zone).filterKeys { it in range }.values.sum()

    /** Mean length of the [sessions] that started in [range], or null when there are none. */
    fun averageSessionMinutes(
        sessions: List<FocusSessionEntity>,
        range: ClosedRange<LocalDate>,
        zone: ZoneId,
    ): Int? = sessions
        .filter { it.startTime.atZone(zone).toLocalDate() in range }
        .takeIf { it.isNotEmpty() }
        ?.map { it.totalSecondsFocused / 60.0 }
        ?.average()
        ?.roundToInt()

    /**
     * The days of [range] up to [today] that have a target according to [targetOn], and how many
     * reached it in [minutesByDay].
     */
    fun goalDays(
        minutesByDay: Map<LocalDate, Int>,
        targetOn: (LocalDate) -> Int?,
        range: ClosedRange<LocalDate>,
        today: LocalDate,
    ): GoalDays {
        var met = 0
        var total = 0
        var day = range.start
        while (!day.isAfter(minOf(range.endInclusive, today))) {
            targetOn(day)?.let { target ->
                total++
                if ((minutesByDay[day] ?: 0) >= target) met++
            }
            day = day.plusDays(1)
        }
        return GoalDays(met, total)
    }
}
