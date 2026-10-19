package io.github.benji377.timety.util.stats

import io.github.benji377.timety.data.model.focus.FocusSessionEntity
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId

/**
 * Distributes a session's focused minutes across the calendar days its time span actually touches.
 *
 * A session that crosses midnight (e.g. 10pm-2am) contributes to *both* days, split in proportion
 * to how much of its wall-clock span fell on each - so the per-day stats read the way the time was
 * really spent rather than piling the whole session onto its start day. The session's *identity*
 * still belongs to its start day everywhere else (the session list, the calendar, the clock); this
 * split is only for "how many minutes did I focus on day X" aggregations.
 */
object FocusSessionDayStats {

    /** Minutes of [session] attributable to each calendar day it overlaps, in [zone]. */
    fun minutesByDay(session: FocusSessionEntity, zone: ZoneId): Map<LocalDate, Int> {
        val totalMinutes = session.totalSecondsFocused / 60
        if (totalMinutes <= 0) return emptyMap()

        val start = session.startTime
        val startDay = start.atZone(zone).toLocalDate()
        // An open session has no stored end; treat it as contiguous from its start for the length
        // it logged, so the split rests on the time recorded and never on the wall clock at render.
        val end = session.endTime ?: start.plusSeconds(session.totalSecondsFocused.toLong())
        val endDay = end.atZone(zone).toLocalDate()

        if (startDay == endDay) return mapOf(startDay to totalMinutes)

        val wallSeconds = Duration.between(start, end).seconds
        if (wallSeconds <= 0) return mapOf(startDay to totalMinutes)

        val result = linkedMapOf<LocalDate, Int>()
        var allocated = 0
        var day = startDay
        while (!day.isAfter(endDay)) {
            val dayStart = day.atStartOfDay(zone).toInstant()
            if (!dayStart.isBefore(end)) break // a span ending exactly at midnight owns nothing here
            val segStart = maxOf(start, dayStart)
            val segEnd = minOf(end, day.plusDays(1).atStartOfDay(zone).toInstant())
            val overlap = Duration.between(segStart, segEnd).seconds
            if (overlap > 0) {
                val minutes = (totalMinutes.toLong() * overlap / wallSeconds).toInt()
                if (minutes > 0) {
                    result[day] = minutes
                    allocated += minutes
                }
            }
            day = day.plusDays(1)
        }
        // Sub-minute rounding leftovers stay with the owning start day, so the per-day parts always
        // sum back to the session's whole-minute total.
        val remainder = totalMinutes - allocated
        if (remainder != 0) result[startDay] = (result[startDay] ?: 0) + remainder
        return result
    }

    /** Sums [minutesByDay] over many sessions into a single day-keyed map. */
    fun minutesByDay(sessions: List<FocusSessionEntity>, zone: ZoneId): Map<LocalDate, Int> {
        val acc = linkedMapOf<LocalDate, Int>()
        sessions.forEach { session ->
            minutesByDay(session, zone).forEach { (day, minutes) ->
                acc[day] = (acc[day] ?: 0) + minutes
            }
        }
        return acc
    }
}
