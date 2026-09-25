package io.github.benji377.timety.util.task

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class ReminderUtilsTest {

    private val now = Instant.parse("2026-10-01T08:00:00Z")
    private val due = Instant.parse("2026-10-03T14:30:00Z")

    private fun before(due: Instant, minutes: Long) = due.minus(minutes, ChronoUnit.MINUTES)

    @Test
    fun presetOf_matchesEveryPresetExactly() {
        ReminderPreset.entries.forEach { preset ->
            assertEquals(preset, ReminderUtils.presetOf(due, ReminderUtils.timeFor(due, preset)))
        }
    }

    @Test
    fun presetOf_returnsNullForOtherGapsAndForSecondsOff() {
        assertNull(ReminderUtils.presetOf(due, before(due, 45)))
        assertNull(ReminderUtils.presetOf(due, before(due, 30).minusSeconds(1)))
        assertNull(ReminderUtils.presetOf(due, due.plusSeconds(60)))
    }

    @Test
    fun toggle_addsThenRemovesAndKeepsSorted() {
        val custom = Instant.parse("2026-10-02T09:00:00Z")
        val added = ReminderUtils.toggle(listOf(custom), due, ReminderPreset.HOUR_1)
        assertEquals(listOf(custom, before(due, 60)).sorted(), added)
        assertEquals(listOf(custom), ReminderUtils.toggle(added, due, ReminderPreset.HOUR_1))
    }

    @Test
    fun toggle_onTimeMatchesReminderEqualToDue() {
        assertEquals(emptyList<Instant>(), ReminderUtils.toggle(listOf(due), due, ReminderPreset.ON_TIME))
    }

    @Test
    fun isPresetAvailable_blocksPastTimesButNotRemovingThem() {
        val soonDue = now.plusSeconds(20 * 60)
        assertFalse(ReminderUtils.isPresetAvailable(emptyList(), soonDue, ReminderPreset.MINUTES_30, now))
        assertTrue(ReminderUtils.isPresetAvailable(emptyList(), soonDue, ReminderPreset.ON_TIME, now))
        val stale = listOf(before(soonDue, 30))
        assertTrue(ReminderUtils.isPresetAvailable(stale, soonDue, ReminderPreset.MINUTES_30, now))
    }

    @Test
    fun isPresetAvailable_blocksNewOnesAtTheCap() {
        val full = (1..ReminderUtils.MAX_REMINDERS).map { now.plusSeconds(it * 60L) }
        assertFalse(ReminderUtils.isPresetAvailable(full, due, ReminderPreset.ON_TIME, now))
        val withPreset = full.drop(1) + due
        assertTrue(ReminderUtils.isPresetAvailable(withPreset, due, ReminderPreset.ON_TIME, now))
    }

    @Test
    fun customIssue_rejectsPastAndAfterDue() {
        assertEquals(CustomReminderIssue.IN_PAST, ReminderUtils.customIssue(now, due, now))
        assertEquals(CustomReminderIssue.IN_PAST, ReminderUtils.customIssue(now.minusSeconds(1), null, now))
        assertEquals(CustomReminderIssue.AFTER_DUE, ReminderUtils.customIssue(due.plusSeconds(1), due, now))
        assertNull(ReminderUtils.customIssue(due, due, now))
        assertNull(ReminderUtils.customIssue(now.plusSeconds(1), null, now))
    }

    @Test
    fun shift_movesPresetsAndKeepsCustoms() {
        val custom = Instant.parse("2026-10-02T09:00:00Z")
        val reminders = listOf(custom, before(due, 30))
        val newDue = due.plus(2, ChronoUnit.DAYS)
        val result = ReminderUtils.shiftWithDueDate(reminders, due, newDue, now)
        assertTrue(result.changed)
        assertEquals(listOf(custom, before(newDue, 30)), result.reminders)
    }

    @Test
    fun shift_isNoOpWithoutBothDatesOrWhenUnchanged() {
        val reminders = listOf(before(due, 30))
        assertEquals(ReminderShift(reminders, false), ReminderUtils.shiftWithDueDate(reminders, null, due, now))
        assertEquals(ReminderShift(reminders, false), ReminderUtils.shiftWithDueDate(reminders, due, null, now))
        assertEquals(ReminderShift(reminders, false), ReminderUtils.shiftWithDueDate(reminders, due, due, now))
    }

    @Test
    fun shift_dropsPresetsThatLandInThePast() {
        val reminders = listOf(before(due, 24 * 60), before(due, 30))
        val newDue = now.plus(2, ChronoUnit.HOURS)
        val result = ReminderUtils.shiftWithDueDate(reminders, due, newDue, now)
        assertTrue(result.changed)
        assertEquals(listOf(before(newDue, 30)), result.reminders)
    }

    @Test
    fun shift_dropsCustomsAfterTheNewDueDate() {
        val custom = Instant.parse("2026-10-03T10:00:00Z")
        val newDue = Instant.parse("2026-10-02T14:30:00Z")
        val result = ReminderUtils.shiftWithDueDate(listOf(custom), due, newDue, now)
        assertTrue(result.changed)
        assertEquals(emptyList<Instant>(), result.reminders)
    }

    @Test
    fun shift_dedupesWhenAShiftedPresetLandsOnACustom() {
        val newDue = due.plus(1, ChronoUnit.DAYS)
        val custom = before(newDue, 30)
        val result = ReminderUtils.shiftWithDueDate(listOf(custom, before(due, 30)), due, newDue, now)
        assertEquals(listOf(custom), result.reminders)
    }

    @Test
    fun shift_reportsUnchangedWhenNothingMoves() {
        val custom = Instant.parse("2026-10-02T09:00:00Z")
        val result = ReminderUtils.shiftWithDueDate(listOf(custom), due, due.plusSeconds(3600), now)
        assertFalse(result.changed)
        assertEquals(listOf(custom), result.reminders)
    }

    @Test
    fun shift_keepsAlreadyPastCustomsUntouched() {
        val past = now.minusSeconds(3600)
        val result = ReminderUtils.shiftWithDueDate(listOf(past), due, due.plusSeconds(3600), now)
        assertEquals(listOf(past), result.reminders)
    }
}
