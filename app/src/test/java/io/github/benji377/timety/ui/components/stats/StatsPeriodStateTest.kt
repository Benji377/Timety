package io.github.benji377.timety.ui.components.stats

import io.github.benji377.timety.util.stats.ReportPeriod
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class StatsPeriodStateTest {
    private val today = LocalDate.of(2026, 10, 31)

    private fun state() = StatsPeriodState(ReportPeriod.WEEK, today, emptySet())

    @Test
    fun switchingPeriodsInALoop_neverMovesTheDates() {
        val state = state()
        repeat(5) {
            state.selectPeriod(ReportPeriod.MONTH)
            state.selectPeriod(ReportPeriod.YEAR)
            state.selectPeriod(ReportPeriod.WEEK)
        }
        assertEquals(LocalDate.of(2026, 10, 26), state.start)
        state.selectPeriod(ReportPeriod.YEAR)
        assertEquals(LocalDate.of(2026, 1, 1), state.start)
    }

    @Test
    fun aPastYear_staysPutWhenSwitching() {
        val state = state()
        state.selectPeriod(ReportPeriod.YEAR)
        state.showPrevious()
        repeat(5) {
            state.selectPeriod(ReportPeriod.WEEK)
            state.selectPeriod(ReportPeriod.MONTH)
            state.selectPeriod(ReportPeriod.YEAR)
        }
        assertEquals(LocalDate.of(2025, 1, 1), state.start)
    }

    @Test
    fun switchingKeepsThePastPeriodInView() {
        val state = state()
        state.showPrevious()
        state.selectPeriod(ReportPeriod.MONTH)
        assertEquals(LocalDate.of(2026, 10, 1), state.start)
        state.selectPeriod(ReportPeriod.WEEK)
        assertEquals(LocalDate.of(2026, 10, 19), state.start)
    }

    @Test
    fun steppingForward_neverGoesPastToday() {
        val state = StatsPeriodState(ReportPeriod.MONTH, LocalDate.of(2026, 9, 30), emptySet())
        state.showNext(LocalDate.of(2026, 10, 5))
        state.selectPeriod(ReportPeriod.WEEK)
        assertEquals(LocalDate.of(2026, 10, 5), state.start)
    }
}
