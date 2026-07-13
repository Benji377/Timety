package io.github.benji377.timety.util.stats

import io.github.benji377.timety.data.model.focus.FocusSessionEntity
import io.github.benji377.timety.util.datetime.AppDateUtils
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

/** One calendar day's focus minutes; [inRange] is false for future-dated grid padding. */
data class HeatmapDay(
    val date: LocalDate,
    val minutes: Int,
    val inRange: Boolean,
)

/** A Monday-start week of exactly 7 [HeatmapDay]s, oldest to newest. */
data class HeatmapWeek(val days: List<HeatmapDay>)

/**
 * A contribution grid of [WEEKS_SHOWN] complete Monday-start weeks ending with the current week,
 * like GitHub's graph. Anchored on whole weeks rather than a fixed trailing day count so the
 * *oldest* (leftmost) column is always a full week - the only column that can ever be partial is
 * the newest (rightmost) one, padded with not-yet-happened days when today isn't a Sunday.
 * [nonZeroQuartiles] are 3 ascending minute thresholds splitting active days into 4 intensity
 * bands (a 5th, level 0, is implicit for zero-minute days) - derived from the data itself so a
 * light user's handful of sessions still spans the color range instead of collapsing to one shade.
 */
data class HeatmapGrid(
    val weeks: List<HeatmapWeek>,
    val nonZeroQuartiles: List<Int>,
) {
    /** Intensity band for [minutes]: 0 (none) through 4 (top quartile of active days). */
    fun intensityLevel(minutes: Int): Int {
        if (minutes <= 0 || nonZeroQuartiles.isEmpty()) return 0
        for ((index, threshold) in nonZeroQuartiles.withIndex()) {
            if (minutes <= threshold) return index + 1
        }
        return nonZeroQuartiles.size + 1
    }
}

/** Buckets focus sessions into a [HeatmapGrid]. Pure and unit-testable. */
object FocusHeatmapBucketer {

    private const val WEEKS_SHOWN = 53

    fun grid(
        sessions: List<FocusSessionEntity>,
        today: LocalDate = LocalDate.now(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): HeatmapGrid {
        val minutesByDay = mutableMapOf<LocalDate, Int>()
        sessions.forEach { session ->
            val day = session.startTime.atZone(zone).toLocalDate()
            minutesByDay[day] = (minutesByDay[day] ?: 0) + session.totalSecondsFocused / 60
        }

        val currentWeekStart = AppDateUtils.startOfWeekMonday(today)
        val gridStart = currentWeekStart.minusWeeks((WEEKS_SHOWN - 1).toLong())
        val gridEnd = currentWeekStart.plusDays(6)

        val weeks = mutableListOf<HeatmapWeek>()
        var cursor = gridStart
        while (!cursor.isAfter(gridEnd)) {
            val days = (0..6L).map { offset ->
                val date = cursor.plusDays(offset)
                val inRange = !date.isAfter(today)
                HeatmapDay(date, if (inRange) minutesByDay[date] ?: 0 else 0, inRange)
            }
            weeks.add(HeatmapWeek(days))
            cursor = cursor.plusWeeks(1)
        }

        val activeMinutes = weeks.asSequence()
            .flatMap { it.days.asSequence() }
            .filter { it.inRange && it.minutes > 0 }
            .map { it.minutes }
            .sorted()
            .toList()

        return HeatmapGrid(weeks, quartileThresholds(activeMinutes))
    }

    private fun quartileThresholds(sortedActiveMinutes: List<Int>): List<Int> {
        if (sortedActiveMinutes.isEmpty()) return emptyList()
        fun percentile(p: Double): Int {
            val index = (p * (sortedActiveMinutes.size - 1)).roundToInt()
                .coerceIn(0, sortedActiveMinutes.size - 1)
            return sortedActiveMinutes[index]
        }
        return listOf(percentile(0.25), percentile(0.5), percentile(0.75))
    }
}
