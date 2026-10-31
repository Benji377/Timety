package io.github.benji377.timety.util.stats

import io.github.benji377.timety.data.model.focus.FocusSessionEntity
import io.github.benji377.timety.data.model.focus.FocusTargetType
import io.github.benji377.timety.data.model.tag.TagEntity
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset

class FocusHistoryQueryTest {
    private val zone = ZoneOffset.UTC
    private val oct29 = LocalDate.of(2026, 10, 29)
    private val oct30 = oct29.plusDays(1)
    private val oct31 = oct29.plusDays(2)
    private val nov1 = oct29.plusDays(3)

    private fun at(day: LocalDate, hour: Int): Instant = day.atTime(hour, 0).toInstant(zone)

    private fun session(
        id: String,
        start: Instant,
        minutes: Int,
        modeId: String = "pomodoro",
        targetType: FocusTargetType = FocusTargetType.TAG,
    ) = FocusSessionEntity(
        id = id,
        modeId = modeId,
        startTime = start,
        endTime = start.plusSeconds(minutes * 60L),
        totalSecondsFocused = minutes * 60,
        targetType = targetType,
    )

    private fun tag(id: String) = TagEntity(id = id, name = id, colorValue = 0)

    private fun query(
        sessions: List<FocusSessionEntity>,
        filter: FocusHistoryFilter,
        tags: Map<String, List<TagEntity>> = emptyMap(),
    ) = FocusHistoryQuery.filter(sessions, filter, tags, zone) { it.id }.map { it.id }

    private val morning = session("morning", at(oct29, 9), 60)
    private val evening = session("evening", at(oct29, 20), 30, modeId = "flexible")
    private val next = session("next", at(oct30, 9), 45, targetType = FocusTargetType.TASK)

    // 22:00 to 02:00 for 240 minutes: 120 on each side of midnight.
    private val overnight = session("overnight", at(oct30, 22), 240)

    @Test
    fun filter_emptyFilterKeepsEverything() {
        assertEquals(
            listOf("morning", "evening", "next"),
            query(listOf(morning, evening, next), FocusHistoryFilter()),
        )
    }

    @Test
    fun filter_byDates_includesAMidnightSessionOnEitherDay() {
        val onLaterDay = query(listOf(next, overnight), FocusHistoryFilter(dates = oct31..oct31))
        assertEquals(listOf("overnight"), onLaterDay)
        val onEarlierDay = query(listOf(next, overnight), FocusHistoryFilter(dates = oct30..oct30))
        assertEquals(listOf("next", "overnight"), onEarlierDay)
    }

    @Test
    fun filter_byTags_requiresEverySelectedTag() {
        val tags = mapOf(
            "morning" to listOf(tag("work"), tag("meeting")),
            "evening" to listOf(tag("work")),
        )
        assertEquals(
            listOf("morning"),
            query(listOf(morning, evening, next), FocusHistoryFilter(tagIds = setOf("work", "meeting")), tags),
        )
    }

    @Test
    fun filter_byTargetAndMode() {
        val all = listOf(morning, evening, next)
        assertEquals(listOf("next"), query(all, FocusHistoryFilter(targetType = FocusTargetType.TASK)))
        assertEquals(listOf("evening"), query(all, FocusHistoryFilter(modeId = "flexible")))
    }

    @Test
    fun filter_searchIgnoresCaseAndSurroundingSpaces() {
        assertEquals(
            listOf("evening"),
            query(listOf(morning, evening, next), FocusHistoryFilter(search = "  EVEN ")),
        )
    }

    @Test
    fun groupByDay_newest_ordersDaysAndSessionsDescending_withTotals() {
        val months = FocusHistoryQuery.groupByDay(
            listOf(morning, evening, next),
            null,
            FocusHistorySort.NEWEST,
            zone,
        )
        assertEquals(1, months.size)
        assertEquals(YearMonth.of(2026, 10), months[0].month)
        assertEquals(135, months[0].minutes)
        assertEquals(listOf(oct30, oct29), months[0].days.map { it.day })
        assertEquals(90, months[0].days[1].minutes)
        assertEquals(listOf("evening", "morning"), months[0].days[1].entries.map { it.session.id })
    }

    @Test
    fun groupByDay_oldest_ordersAscending() {
        val days = FocusHistoryQuery.groupByDay(
            listOf(evening, next, morning),
            null,
            FocusHistorySort.OLDEST,
            zone,
        ).single().days
        assertEquals(listOf(oct29, oct30), days.map { it.day })
        assertEquals(listOf("morning", "evening"), days[0].entries.map { it.session.id })
    }

    @Test
    fun groupByDay_listsAMidnightSessionUnderBothDaysWithThatDaysMinutes() {
        val days = FocusHistoryQuery.groupByDay(listOf(overnight), null, FocusHistorySort.NEWEST, zone)
            .single().days
        assertEquals(listOf(oct31, oct30), days.map { it.day })
        assertEquals(listOf(120, 120), days.map { it.minutes })
        assertEquals(listOf("overnight", "overnight"), days.map { it.entries.single().session.id })
    }

    @Test
    fun groupByDay_dateFilterKeepsOnlyTheDaysInside() {
        val days = FocusHistoryQuery.groupByDay(
            listOf(overnight),
            oct31..oct31,
            FocusHistorySort.NEWEST,
            zone,
        ).single().days
        assertEquals(listOf(oct31), days.map { it.day })
        assertEquals(120, days.single().minutes)
    }

    @Test
    fun groupByDay_splitsMonths_evenForOneSession() {
        val monthEnd = session("monthEnd", at(oct31, 22), 240)
        val months = FocusHistoryQuery.groupByDay(listOf(monthEnd), null, FocusHistorySort.NEWEST, zone)
        assertEquals(listOf(YearMonth.of(2026, 11), YearMonth.of(2026, 10)), months.map { it.month })
        assertEquals(nov1, months[0].days.single().day)
    }

    @Test
    fun groupByDay_keepsASessionUnderAMinuteOnItsStartDay() {
        val short = FocusSessionEntity(
            id = "short",
            modeId = "m",
            startTime = at(oct29, 9),
            totalSecondsFocused = 20,
        )
        val day = FocusHistoryQuery.groupByDay(listOf(short), null, FocusHistorySort.NEWEST, zone)
            .single().days.single()
        assertEquals(oct29, day.day)
        assertEquals(0, day.minutes)
    }

    @Test
    fun longestFirst_sortsByLength_thenNewest() {
        val tie = session("tie", at(oct30, 7), 60)
        assertEquals(
            listOf("tie", "morning", "next", "evening"),
            FocusHistoryQuery.longestFirst(listOf(morning, evening, next, tie)).map { it.id },
        )
    }

    @Test
    fun summary_countsASessionOnce_andOnlyMinutesInsideTheDates() {
        assertEquals(
            FocusHistorySummary(sessions = 2, minutes = 285),
            FocusHistoryQuery.summary(listOf(next, overnight), null, zone),
        )
        assertEquals(
            FocusHistorySummary(sessions = 1, minutes = 120),
            FocusHistoryQuery.summary(listOf(overnight), oct31..oct31, zone),
        )
    }

    @Test
    fun daysWithSessions_includesEveryDayTouched() {
        assertEquals(
            setOf(oct29, oct30, oct31),
            FocusHistoryQuery.daysWithSessions(listOf(morning, overnight), zone),
        )
    }
}
