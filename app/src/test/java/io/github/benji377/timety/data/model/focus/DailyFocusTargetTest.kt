package io.github.benji377.timety.data.model.focus

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class DailyFocusTargetTest {

    // 2026-10-19 is a Monday, 2026-10-24 a Saturday.
    private val monday = LocalDate.of(2026, 10, 19)
    private val saturday = LocalDate.of(2026, 10, 24)

    @Test
    fun defaultTargetsEveryDay() {
        val target = DailyFocusTarget()
        assertEquals(DailyFocusTarget.DEFAULT_MINUTES, target.minutesOn(monday))
        assertEquals(DailyFocusTarget.DEFAULT_MINUTES, target.minutesOn(saturday))
    }

    @Test
    fun zeroMinutesIsOffOnEveryDay() {
        val target = DailyFocusTarget(minutes = 0)
        assertNull(target.minutesOn(monday))
        assertNull(target.minutesOn(saturday))
    }

    @Test
    fun deselectedWeekdayHasNoTarget() {
        val target = DailyFocusTarget(minutes = 60, weekdays = (1..5).toSet())
        assertEquals(60, target.minutesOn(monday))
        assertNull(target.minutesOn(saturday))
    }
}
