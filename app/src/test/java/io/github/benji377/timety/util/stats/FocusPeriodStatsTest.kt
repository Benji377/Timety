package io.github.benji377.timety.util.stats

import io.github.benji377.timety.data.model.focus.FocusSessionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class FocusPeriodStatsTest {
    private val zone = ZoneOffset.UTC
    private val monday = LocalDate.of(2026, 10, 26)
    private val week = monday..monday.plusDays(6)

    private fun session(day: LocalDate, hour: Int, minutes: Int): FocusSessionEntity {
        val start = day.atTime(hour, 0).toInstant(zone)
        return FocusSessionEntity(
            id = "$day-$hour",
            modeId = "m",
            startTime = start,
            endTime = start.plusSeconds(minutes * 60L),
            totalSecondsFocused = minutes * 60,
        )
    }

    @Test
    fun percentChange_roundsAndHandlesNoBaseline() {
        assertEquals(9, FocusPeriodStats.percentChange(625, 573))
        assertEquals(-50, FocusPeriodStats.percentChange(50, 100))
        assertEquals(0, FocusPeriodStats.percentChange(100, 100))
        assertNull(FocusPeriodStats.percentChange(100, 0))
    }

    @Test
    fun minutesIn_countsOnlyTheRangesPartOfAMidnightSession() {
        val overnight = session(monday.minusDays(1), 23, 120)
        assertEquals(60, FocusPeriodStats.minutesIn(overnight, week, zone))
    }

    @Test
    fun averageSession_usesSessionsStartedInTheRange() {
        val sessions = listOf(
            session(monday, 9, 20),
            session(monday, 12, 41),
            session(monday.minusDays(2), 9, 500),
        )
        assertEquals(31, FocusPeriodStats.averageSessionMinutes(sessions, week, zone))
        assertNull(FocusPeriodStats.averageSessionMinutes(emptyList(), week, zone))
    }

    @Test
    fun goalDays_skipsFutureDaysAndDaysWithoutTarget() {
        val wednesday = monday.plusDays(2)
        val result = FocusPeriodStats.goalDays(
            minutesByDay = mapOf(monday to 90, monday.plusDays(1) to 89, wednesday to 200, monday.plusDays(3) to 500),
            targetOn = { day -> if (day == wednesday) null else 90 },
            range = week,
            today = monday.plusDays(3),
        )
        assertEquals(GoalDays(met = 2, total = 3), result)
    }
}
