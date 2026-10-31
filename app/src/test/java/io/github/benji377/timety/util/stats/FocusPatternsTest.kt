package io.github.benji377.timety.util.stats

import io.github.benji377.timety.data.model.focus.DistractionEntity
import io.github.benji377.timety.data.model.focus.FocusSessionEntity
import io.github.benji377.timety.data.model.user.DayRating
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class FocusPatternsTest {
    private val zone = ZoneOffset.UTC

    // A Saturday.
    private val today = LocalDate.of(2026, 10, 31)
    private val window = FocusPatterns.window(today)
    private val thursday = LocalDate.of(2026, 10, 29)

    private fun at(day: LocalDate, hour: Int, minute: Int = 0): Instant =
        day.atTime(hour, minute).toInstant(zone)

    private fun session(start: Instant, minutes: Int, id: String = start.toString()) = FocusSessionEntity(
        id = id,
        modeId = "m",
        startTime = start,
        endTime = start.plusSeconds(minutes * 60L),
        totalSecondsFocused = minutes * 60,
    )

    @Test
    fun window_coversExactlyTwelveWeeksEndingToday() {
        assertEquals(today.minusDays(83), window.start)
        assertEquals(today, window.endInclusive)
    }

    @Test
    fun dayPart_usesTheAppsTimeOfDayHours() {
        assertEquals(DayPart.NIGHT, DayPart.of(4))
        assertEquals(DayPart.MORNING, DayPart.of(5))
        assertEquals(DayPart.MORNING, DayPart.of(11))
        assertEquals(DayPart.AFTERNOON, DayPart.of(12))
        assertEquals(DayPart.AFTERNOON, DayPart.of(16))
        assertEquals(DayPart.EVENING, DayPart.of(17))
        assertEquals(DayPart.EVENING, DayPart.of(20))
        assertEquals(DayPart.NIGHT, DayPart.of(21))
    }

    @Test
    fun weekdaySlots_spreadsASessionAcrossItsSlots() {
        // Thursday 07:30 for 60 minutes: 30 in the 4-8 slot, 30 in the 8-12 slot.
        val slots = FocusPatterns.weekdaySlots(listOf(session(at(thursday, 7, 30), 60)), window, zone)
        val thursdaySlots = slots[DayOfWeek.THURSDAY.value - 1]
        assertEquals(6, thursdaySlots.size)
        assertEquals(30, thursdaySlots[1])
        assertEquals(30, thursdaySlots[2])
        assertEquals(60, slots.flatten().sum())
    }

    @Test
    fun weekdaySlots_followsASessionPastMidnight() {
        val wednesdayNight = at(thursday.minusDays(1), 23, 30)
        val slots = FocusPatterns.weekdaySlots(listOf(session(wednesdayNight, 60)), window, zone)
        assertEquals(30, slots[DayOfWeek.WEDNESDAY.value - 1][5])
        assertEquals(30, slots[DayOfWeek.THURSDAY.value - 1][0])
    }

    @Test
    fun weekdaySlots_ignoresSessionsOutsideTheWindow() {
        val old = session(at(window.start.minusDays(1), 9), 60)
        assertEquals(0, FocusPatterns.weekdaySlots(listOf(old), window, zone).flatten().sum())
    }

    @Test
    fun focusPeak_needsEnoughSessions() {
        val nineteen = List(19) { session(at(thursday.minusDays(it * 7L % 70), 9), 30, id = "s$it") }
        assertNull(FocusPatterns.focusPeak(nineteen, window, zone))
    }

    @Test
    fun focusPeak_findsTheBusiestWeekdayAndDayPart() {
        val sessions = List(19) { session(at(today.minusDays(it.toLong()), 15), 10, id = "s$it") } +
            session(at(thursday, 9), 300, id = "big")
        assertEquals(
            FocusPeak(DayOfWeek.THURSDAY, DayPart.MORNING),
            FocusPatterns.focusPeak(sessions, window, zone),
        )
    }

    @Test
    fun averageByWeekday_startsAtTheFirstFocusDay() {
        val averages = FocusPatterns.averageMinutesByWeekday(
            mapOf(thursday to 60, today to 30),
            window,
        )
        assertEquals(60, averages[DayOfWeek.THURSDAY])
        assertEquals(0, averages[DayOfWeek.FRIDAY])
        assertEquals(30, averages[DayOfWeek.SATURDAY])
        assertEquals(setOf(DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY), averages.keys)
    }

    @Test
    fun averageByWeekday_emptyWithoutFocus() {
        assertEquals(emptyMap<DayOfWeek, Int>(), FocusPatterns.averageMinutesByWeekday(emptyMap(), window))
    }

    @Test
    fun busiestTaskWeekday_needsEnoughCompletions() {
        assertNull(FocusPatterns.busiestTaskWeekday(List(19) { at(thursday, 9) }, window, zone))
        assertEquals(
            DayOfWeek.THURSDAY,
            FocusPatterns.busiestTaskWeekday(List(20) { at(thursday, 9) }, window, zone),
        )
    }

    @Test
    fun medianDistractionMinute_needsEnoughDistractions() {
        val start = at(thursday, 9)
        val base = session(start, 120)
        fun distraction(minutes: Long) =
            base to DistractionEntity(sessionId = base.id, time = start.plusSeconds(minutes * 60))

        assertNull(FocusPatterns.medianDistractionMinute(List(9) { distraction(10) }, window, zone))
        assertEquals(
            45,
            FocusPatterns.medianDistractionMinute(
                List(5) { distraction(10) } + List(6) { distraction(45) },
                window,
                zone,
            ),
        )
    }

    @Test
    fun focusByMood_needsEnoughRatedDays() {
        val thirteen = (0L until 13L).associate { today.minusDays(it) to DayRating.GREAT }
        assertNull(FocusPatterns.focusByMood(thirteen, emptyMap(), window))
    }

    @Test
    fun focusByMood_averagesEachMood_andDropsThinOnes() {
        val moods = (0L until 7L).associate { today.minusDays(it) to DayRating.GREAT } +
            (7L until 14L).associate { today.minusDays(it) to DayRating.OK } +
            (14L until 16L).associate { today.minusDays(it) to DayRating.BAD }
        val minutes = (0L until 7L).associate { today.minusDays(it) to 70 }
        assertEquals(
            mapOf(DayRating.GREAT to 70, DayRating.OK to 0),
            FocusPatterns.focusByMood(moods, minutes, window),
        )
    }

    @Test
    fun focusByMood_nullWithOnlyOneMoodLeft() {
        val moods = (0L until 14L).associate { today.minusDays(it) to DayRating.GREAT }
        assertNull(FocusPatterns.focusByMood(moods, emptyMap(), window))
    }

    @Test
    fun completionsByDayPart_countsInsideTheWindowOnly() {
        val counts = FocusPatterns.completionsByDayPart(
            listOf(at(today, 8), at(today, 9), at(today, 22), at(window.start.minusDays(1), 8)),
            window,
            zone,
        )
        assertEquals(mapOf(DayPart.MORNING to 2, DayPart.NIGHT to 1), counts)
    }
}
