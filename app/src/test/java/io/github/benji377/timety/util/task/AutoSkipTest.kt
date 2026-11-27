package io.github.benji377.timety.util.task

import io.github.benji377.timety.data.model.task.RecurrenceUnit
import io.github.benji377.timety.data.model.task.RecurringTaskEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Auto-skip (#265): a late completion also passes over the next round when it is closer than the
 * setting, capped at a quarter of the gap between rounds so short intervals keep their rounds.
 */
class AutoSkipTest {
    private val zone = ZoneId.of("UTC")
    private val oneDay = Duration.ofHours(24)

    // 2026-11-21 is a Saturday.
    private fun at(d: Int, h: Int, mo: Int = 11): Instant = LocalDateTime.of(2026, mo, d, h, 0).atZone(zone).toInstant()

    private fun task(due: Instant, unit: RecurrenceUnit, interval: Int = 1, daysOfWeek: String? = null, hasDueTime: Boolean = true) =
        RecurringTaskEntity(
            id = "t1",
            title = "Test",
            dueDate = due,
            hasDueTime = hasDueTime,
            unit = unit,
            interval = interval,
            daysOfWeek = daysOfWeek,
            createdAt = due,
        )

    private fun next(task: RecurringTaskEntity, now: Instant, window: Duration = oneDay) =
        RecurrenceUtils.nextDueDate(task, now, zone, autoSkip = window)

    @Test
    fun saturdayAt2_completedNextSaturdayAt1_passesOverThatAfternoon() {
        val weekly = task(at(14, 14), RecurrenceUnit.WEEK)

        assertEquals(at(28, 14), next(weekly, at(21, 13)))
    }

    @Test
    fun turnedOff_keepsTheNearRound() {
        val weekly = task(at(14, 14), RecurrenceUnit.WEEK)

        assertEquals(at(21, 14), next(weekly, at(21, 13), Duration.ZERO))
    }

    @Test
    fun aRoundFurtherAwayThanTheSetting_isKept() {
        val weekly = task(at(14, 14), RecurrenceUnit.WEEK)

        assertEquals(at(21, 14), next(weekly, at(21, 13), Duration.ofMinutes(30)))
    }

    @Test
    fun anOnTimeCompletion_isNeverAffected() {
        val weekly = task(at(21, 14), RecurrenceUnit.WEEK)

        assertEquals(at(28, 14), next(weekly, at(21, 13)))
    }

    @Test
    fun daily_completedAnHourBeforeTheNextRound_passesOverIt() {
        val daily = task(at(20, 8), RecurrenceUnit.DAY)

        assertEquals(at(22, 8), next(daily, at(21, 7)))
    }

    @Test
    fun daily_completedLateTheSameEvening_keepsTomorrowMorning() {
        val daily = task(at(20, 8), RecurrenceUnit.DAY)

        assertEquals(at(21, 8), next(daily, at(20, 22)))
    }

    @Test
    fun everyTwoDays_isCappedAtTwelveHours() {
        val everyOther = task(at(19, 8), RecurrenceUnit.DAY, interval = 2)

        assertEquals("8h before the next round", at(23, 8), next(everyOther, at(21, 0)))
        assertEquals("14h before the next round", at(21, 8), next(everyOther, at(20, 18)))
    }

    @Test
    fun monthly_completedJustBeforeTheNextRound_passesOverIt() {
        val monthly = task(at(21, 9, mo = 10), RecurrenceUnit.MONTH)

        assertEquals(at(21, 9, mo = 12), next(monthly, at(21, 8)))
    }

    @Test
    fun twoWeekdays_aDayApart_keepTheirOwnRounds() {
        // Monday and Tuesday at 20:00; Monday's done an hour late.
        val mondayTuesday = task(at(16, 20), RecurrenceUnit.WEEK, daysOfWeek = "[1,2]")

        assertEquals(at(17, 20), next(mondayTuesday, at(16, 21)))
    }

    @Test
    fun dateOnly_yesterdaysDoneToday_passesOverToday() {
        val start = LocalDate.of(2026, 11, 20).atStartOfDay(zone).toInstant()
        val daily = task(start, RecurrenceUnit.DAY, hasDueTime = false)

        assertEquals(LocalDate.of(2026, 11, 22).atStartOfDay(zone).toInstant(), next(daily, at(21, 15)))
    }

    @Test
    fun possibleNextDueDates_holdsThePlainAndThePassedOverRound() {
        val weekly = task(at(14, 14), RecurrenceUnit.WEEK)

        val possible = RecurrenceUtils.possibleNextDueDates(weekly, at(21, 13), zone)

        assertTrue(at(21, 14) in possible)
        assertTrue(at(28, 14) in possible)
    }
}
