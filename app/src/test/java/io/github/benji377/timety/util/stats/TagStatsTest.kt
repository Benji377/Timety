package io.github.benji377.timety.util.stats

import io.github.benji377.timety.data.model.focus.FocusSessionEntity
import io.github.benji377.timety.data.model.tag.TagEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class TagStatsTest {
    private val zone = ZoneOffset.UTC
    private val monday = LocalDate.of(2026, 10, 19)
    private val sunday = monday.plusDays(6)
    private val work = TagEntity("work", "work", 1)
    private val meeting = TagEntity("meeting", "meeting", 2)

    private fun at(day: LocalDate, hour: Int): Instant = day.atTime(hour, 0).toInstant(zone)

    private fun session(id: String, start: Instant, minutes: Int) = FocusSessionEntity(
        id = id,
        modeId = "mode",
        startTime = start,
        endTime = start.plusSeconds(minutes * 60L),
        totalSecondsFocused = minutes * 60,
    )

    private fun minutes(sessions: List<FocusSessionEntity>, tags: Map<String, List<TagEntity>>) =
        TagStats.minutesByTag(sessions, tags, monday, sunday, zone)

    @Test
    fun severalTags_countUnderEach_largestFirst() {
        val result = minutes(
            listOf(
                session("a", at(monday, 9), 30),
                session("b", at(monday, 11), 20),
            ),
            mapOf("a" to listOf(work, meeting), "b" to listOf(work)),
        )

        assertEquals(listOf(TagTotal(work, 50), TagTotal(meeting, 30)), result.totals)
        assertEquals(0, result.untagged)
    }

    @Test
    fun sessionsWithoutTags_countAsUntagged() {
        val result = minutes(listOf(session("a", at(monday, 9), 15)), emptyMap())

        assertEquals(emptyList<TagTotal>(), result.totals)
        assertEquals(15, result.untagged)
        assertEquals(15, result.maxValue)
    }

    @Test
    fun onlyTimeInsideTheWeekCounts() {
        val acrossMonday = session("a", at(monday.minusDays(1), 23), 120)

        val result = minutes(
            listOf(acrossMonday, session("b", at(sunday.plusDays(1), 9), 60)),
            mapOf("a" to listOf(work), "b" to listOf(meeting)),
        )

        assertEquals(listOf(TagTotal(work, 60)), result.totals)
    }

    @Test
    fun noTimeInTheWeek_isEmpty() {
        val result = minutes(listOf(session("a", at(sunday.plusDays(1), 9), 60)), emptyMap())

        assertTrue(result.isEmpty)
    }

    @Test
    fun countsByTag_countsItemsPerTagAndUntagged() {
        val result = TagStats.countsByTag(
            listOf("a", "b", "c"),
            mapOf("a" to listOf(work, meeting), "b" to listOf(work)),
        )

        assertEquals(listOf(TagTotal(work, 2), TagTotal(meeting, 1)), result.totals)
        assertEquals(1, result.untagged)
    }
}
