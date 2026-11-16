package io.github.benji377.timety.util.task

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

class ReminderUtilsTest {
    private val zone: ZoneId = ZoneOffset.UTC
    private val now = Instant.parse("2026-10-01T08:00:00Z")
    private val dueAt = Instant.parse("2026-10-03T14:30:00Z")
    private val due = timed(dueAt)
    private val dateOnly = Due.of(LocalDate.of(2026, 10, 3), null, zone)
    private val dateOnlyAnchor = Instant.parse("2026-10-03T09:00:00Z")

    private fun timed(at: Instant) = Due(at, hasTime = true)

    private fun before(at: Instant, minutes: Long) = at.minus(minutes, ChronoUnit.MINUTES)

    @Test
    fun presetOf_matchesEveryPresetExactly() {
        ReminderPreset.entries.forEach { preset ->
            assertEquals(preset, ReminderUtils.presetOf(due, ReminderUtils.timeFor(due, preset, zone), zone))
        }
    }

    @Test
    fun presetOf_returnsNullForOtherGapsAndForSecondsOff() {
        assertNull(ReminderUtils.presetOf(due, before(dueAt, 45), zone))
        assertNull(ReminderUtils.presetOf(due, before(dueAt, 30).minusSeconds(1), zone))
        assertNull(ReminderUtils.presetOf(due, dueAt.plusSeconds(60), zone))
    }

    @Test
    fun toggle_addsThenRemovesAndKeepsSorted() {
        val custom = Instant.parse("2026-10-02T09:00:00Z")
        val added = ReminderUtils.toggle(listOf(custom), due, ReminderPreset.HOUR_1, zone)
        assertEquals(listOf(custom, before(dueAt, 60)).sorted(), added)
        assertEquals(listOf(custom), ReminderUtils.toggle(added, due, ReminderPreset.HOUR_1, zone))
    }

    @Test
    fun toggle_onTimeMatchesReminderEqualToDue() {
        assertEquals(emptyList<Instant>(), ReminderUtils.toggle(listOf(dueAt), due, ReminderPreset.ON_TIME, zone))
    }

    @Test
    fun isPresetAvailable_blocksPastTimesButNotRemovingThem() {
        val soonDue = timed(now.plusSeconds(20 * 60))
        assertFalse(ReminderUtils.isPresetAvailable(emptyList(), soonDue, ReminderPreset.MINUTES_30, now, zone))
        assertTrue(ReminderUtils.isPresetAvailable(emptyList(), soonDue, ReminderPreset.ON_TIME, now, zone))
        val stale = listOf(before(soonDue.at, 30))
        assertTrue(ReminderUtils.isPresetAvailable(stale, soonDue, ReminderPreset.MINUTES_30, now, zone))
    }

    @Test
    fun isPresetAvailable_blocksNewOnesAtTheCap() {
        val full = (1..ReminderUtils.MAX_REMINDERS).map { now.plusSeconds(it * 60L) }
        assertFalse(ReminderUtils.isPresetAvailable(full, due, ReminderPreset.ON_TIME, now, zone))
        val withPreset = full.drop(1) + dueAt
        assertTrue(ReminderUtils.isPresetAvailable(withPreset, due, ReminderPreset.ON_TIME, now, zone))
    }

    @Test
    fun withoutPresets_dropsPresetsAndKeepsCustoms() {
        val custom = Instant.parse("2026-10-02T09:00:00Z")
        val reminders = listOf(custom, before(dueAt, 30), before(dueAt, 60))
        assertEquals(listOf(custom), ReminderUtils.withoutPresets(reminders, due, zone))
    }

    @Test
    fun customIssue_rejectsPastAndAfterDue() {
        assertEquals(CustomReminderIssue.IN_PAST, ReminderUtils.customIssue(now, due, now, zone))
        assertEquals(CustomReminderIssue.IN_PAST, ReminderUtils.customIssue(now.minusSeconds(1), null, now, zone))
        assertEquals(CustomReminderIssue.AFTER_DUE, ReminderUtils.customIssue(dueAt.plusSeconds(1), due, now, zone))
        assertNull(ReminderUtils.customIssue(dueAt, due, now, zone))
        assertNull(ReminderUtils.customIssue(now.plusSeconds(1), null, now, zone))
    }

    @Test
    fun shift_movesPresetsAndKeepsCustoms() {
        val custom = Instant.parse("2026-10-02T09:00:00Z")
        val reminders = listOf(custom, before(dueAt, 30))
        val newDue = timed(dueAt.plus(2, ChronoUnit.DAYS))
        val result = ReminderUtils.shiftWithDueDate(reminders, due, newDue, now, zone)
        assertTrue(result.changed)
        assertEquals(listOf(custom, before(newDue.at, 30)), result.reminders)
    }

    @Test
    fun shift_isNoOpWithoutBothDatesOrWhenUnchanged() {
        val reminders = listOf(before(dueAt, 30))
        assertEquals(ReminderShift(reminders, false), ReminderUtils.shiftWithDueDate(reminders, null, due, now, zone))
        assertEquals(ReminderShift(reminders, false), ReminderUtils.shiftWithDueDate(reminders, due, null, now, zone))
        assertEquals(ReminderShift(reminders, false), ReminderUtils.shiftWithDueDate(reminders, due, due, now, zone))
    }

    @Test
    fun shift_dropsPresetsThatLandInThePast() {
        val reminders = listOf(before(dueAt, 24 * 60), before(dueAt, 30))
        val newDue = timed(now.plus(2, ChronoUnit.HOURS))
        val result = ReminderUtils.shiftWithDueDate(reminders, due, newDue, now, zone)
        assertTrue(result.changed)
        assertEquals(listOf(before(newDue.at, 30)), result.reminders)
    }

    @Test
    fun shift_dropsCustomsAfterTheNewDueDate() {
        val custom = Instant.parse("2026-10-03T10:00:00Z")
        val newDue = timed(Instant.parse("2026-10-02T14:30:00Z"))
        val result = ReminderUtils.shiftWithDueDate(listOf(custom), due, newDue, now, zone)
        assertTrue(result.changed)
        assertEquals(emptyList<Instant>(), result.reminders)
    }

    @Test
    fun shift_dedupesWhenAShiftedPresetLandsOnACustom() {
        val newDue = timed(dueAt.plus(1, ChronoUnit.DAYS))
        val custom = before(newDue.at, 30)
        val result = ReminderUtils.shiftWithDueDate(listOf(custom, before(dueAt, 30)), due, newDue, now, zone)
        assertEquals(listOf(custom), result.reminders)
    }

    @Test
    fun shift_reportsUnchangedWhenNothingMoves() {
        val custom = Instant.parse("2026-10-02T09:00:00Z")
        val result = ReminderUtils.shiftWithDueDate(listOf(custom), due, timed(dueAt.plusSeconds(3600)), now, zone)
        assertFalse(result.changed)
        assertEquals(listOf(custom), result.reminders)
    }

    @Test
    fun shift_keepsAlreadyPastCustomsUntouched() {
        val past = now.minusSeconds(3600)
        val result = ReminderUtils.shiftWithDueDate(listOf(past), due, timed(dueAt.plusSeconds(3600)), now, zone)
        assertEquals(listOf(past), result.reminders)
    }

    @Test
    fun dateOnly_offersWholeDayPresetsAnchoredAtNine() {
        assertEquals(
            listOf(ReminderPreset.ON_TIME, ReminderPreset.DAY_1, ReminderPreset.WEEK_1),
            dateOnly.presets,
        )
        assertEquals(dateOnlyAnchor, ReminderUtils.timeFor(dateOnly, ReminderPreset.ON_TIME, zone))
        assertEquals(before(dateOnlyAnchor, 24 * 60), ReminderUtils.timeFor(dateOnly, ReminderPreset.DAY_1, zone))
        assertFalse(ReminderUtils.isPresetAvailable(emptyList(), dateOnly, ReminderPreset.HOUR_1, now, zone))
    }

    @Test
    fun dateOnly_minuteGapsAreCustomNotPresets() {
        assertNull(ReminderUtils.presetOf(dateOnly, before(dateOnlyAnchor, 30), zone))
    }

    @Test
    fun dateOnly_allowsCustomRemindersUntilTheDayEnds() {
        assertNull(ReminderUtils.customIssue(Instant.parse("2026-10-03T23:00:00Z"), dateOnly, now, zone))
        assertEquals(
            CustomReminderIssue.AFTER_DUE,
            ReminderUtils.customIssue(Instant.parse("2026-10-04T00:01:00Z"), dateOnly, now, zone),
        )
    }

    @Test
    fun shift_toDateOnlyKeepsDayPresetsAndDropsMinutePresets() {
        val reminders = listOf(before(dueAt, 24 * 60), before(dueAt, 30))
        val result = ReminderUtils.shiftWithDueDate(reminders, due, dateOnly, now, zone)
        assertEquals(listOf(before(dateOnlyAnchor, 24 * 60)), result.reminders)
    }

    @Test
    fun dateOnly_isOverdueOnlyOnceTheDayEnds() {
        assertFalse(dateOnly.isOverdue(Instant.parse("2026-10-03T23:59:00Z"), zone))
        assertTrue(dateOnly.isOverdue(Instant.parse("2026-10-04T00:00:01Z"), zone))
        assertTrue(due.isOverdue(dueAt.plusSeconds(1), zone))
    }
}
