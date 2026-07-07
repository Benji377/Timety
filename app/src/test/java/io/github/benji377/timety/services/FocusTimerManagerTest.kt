package io.github.benji377.timety.services

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test


class FocusTimerManagerTest {

    @Before
    fun resetSingleton() {
        // FocusTimerManager is a singleton; make sure no state leaks between tests.
        FocusTimerManager.stopTimer(discard = true)
    }

    // --- TimerState (pure) ---

    @Test
    fun countdownProgressIsRemainingOverTotal() {
        val state = TimerState(secondsRemaining = 150, totalPhaseSeconds = 300)
        assertEquals(0.5f, state.progress)
    }

    @Test
    fun progressIsFullWhenTotalIsZero() {
        assertEquals(1f, TimerState(secondsRemaining = 0, totalPhaseSeconds = 0).progress)
    }

    @Test
    fun stopwatchProgressIsAlwaysZero() {
        val state = TimerState(isStopwatch = true, elapsedSeconds = 90, totalPhaseSeconds = 0)
        assertEquals(0f, state.progress)
    }

    @Test
    fun centerTextShowsRemainingForCountdown() {
        val state = TimerState(secondsRemaining = 25 * 60, totalPhaseSeconds = 25 * 60)
        assertEquals("25:00", state.centerText)
    }

    @Test
    fun centerTextShowsElapsedForStopwatch() {
        val state = TimerState(isStopwatch = true, elapsedSeconds = 61, secondsRemaining = 0)
        assertEquals("01:01", state.centerText)
    }

    @Test
    fun centerTextZeroPadsMinutesAndSeconds() {
        val state = TimerState(secondsRemaining = 9, totalPhaseSeconds = 60)
        assertEquals("00:09", state.centerText)
    }

    // --- setMode ---

    @Test
    fun setModeInitializesCountdownState() {
        FocusTimerManager.setMode("Pomodoro Classic", 25 * 60, isRestPhase = false)
        val state = FocusTimerManager.timerState.value
        assertEquals("Pomodoro Classic", state.modeName)
        assertEquals(25 * 60, state.totalPhaseSeconds)
        assertEquals(25 * 60, state.secondsRemaining)
        assertFalse(state.isRestPhase)
        assertFalse(state.isStopwatch)
        assertFalse(state.isRunning)
    }

    @Test
    fun setModeInitializesStopwatchState() {
        FocusTimerManager.setMode("Stopwatch", 0, isRestPhase = false, isStopwatch = true)
        val state = FocusTimerManager.timerState.value
        assertTrue(state.isStopwatch)
        assertEquals(0, state.elapsedSeconds)
        assertEquals("00:00", state.centerText)
    }

    @Test
    fun setModeIsIgnoredWhileRunning() {
        FocusTimerManager.setMode("First", 300, isRestPhase = false)
        FocusTimerManager.startTimer()
        FocusTimerManager.setMode("Second", 60, isRestPhase = true)
        val state = FocusTimerManager.timerState.value
        assertEquals("First", state.modeName)
        assertEquals(300, state.totalPhaseSeconds)
        assertFalse(state.isRestPhase)
    }

    // --- start / pause / stop ---

    @Test
    fun startTimerSetsRunning() {
        FocusTimerManager.setMode("Focus", 300, isRestPhase = false)
        FocusTimerManager.startTimer()
        val state = FocusTimerManager.timerState.value
        assertTrue(state.isRunning)
        assertFalse(state.isPaused)
    }

    @Test
    fun pausePreservesRemainingTime() {
        FocusTimerManager.setMode("Focus", 300, isRestPhase = false)
        FocusTimerManager.startTimer()
        Thread.sleep(1200)
        FocusTimerManager.pauseTimer()
        val paused = FocusTimerManager.timerState.value
        assertTrue(paused.isPaused)
        assertFalse(paused.isRunning)

        // Remaining time must not change while paused.
        Thread.sleep(1500)
        assertEquals(paused.secondsRemaining, FocusTimerManager.timerState.value.secondsRemaining)
    }

    @Test
    fun stopResetsStateAndEmitsElapsedFocusSeconds() = runBlocking {
        FocusTimerManager.setMode("Focus", 300, isRestPhase = false)
        FocusTimerManager.startTimer()
        Thread.sleep(2200)

        val stopInfo = async(Dispatchers.Default) { FocusTimerManager.stopEvent.first() }
        FocusTimerManager.stopEvent.subscriptionCount.first { it > 0 }
        FocusTimerManager.stopTimer()

        val info = withTimeout(5000) { stopInfo.await() }
        assertTrue("expected 1..4 elapsed, got ${info.elapsedFocusSeconds}", info.elapsedFocusSeconds in 1..4)
        assertFalse(info.wasRestPhase)
        assertFalse(info.discard)

        val state = FocusTimerManager.timerState.value
        assertFalse(state.isRunning)
        assertFalse(state.isPaused)
        assertEquals(300, state.secondsRemaining)
    }

    @Test
    fun stopWhileIdleEmitsNothing() = runBlocking {
        // stopTimer on an inactive engine must not produce a bookkeeping event (that would
        // log ghost sessions).
        val received = async(Dispatchers.Default) { FocusTimerManager.stopEvent.first() }
        FocusTimerManager.stopEvent.subscriptionCount.first { it > 0 }
        FocusTimerManager.stopTimer()
        Thread.sleep(300)
        assertTrue(received.isActive)
        received.cancel()
    }

    @Test
    fun stopwatchCountsUpAndReportsElapsedOnStop() = runBlocking {
        FocusTimerManager.setMode("Stopwatch", 0, isRestPhase = false, isStopwatch = true)
        FocusTimerManager.startTimer()
        Thread.sleep(2200)

        val running = FocusTimerManager.timerState.value
        assertTrue(running.isRunning)
        assertFalse("stopwatch must not await a next phase", running.isAwaitingContinue)
        assertTrue("expected elapsed >= 1, got ${running.elapsedSeconds}", running.elapsedSeconds >= 1)

        val stopInfo = async(Dispatchers.Default) { FocusTimerManager.stopEvent.first() }
        FocusTimerManager.stopEvent.subscriptionCount.first { it > 0 }
        FocusTimerManager.stopTimer()
        val info = withTimeout(5000) { stopInfo.await() }
        assertTrue("expected 1..4 elapsed, got ${info.elapsedFocusSeconds}", info.elapsedFocusSeconds in 1..4)
    }

    @Test
    fun countdownCompletionEntersAwaitingContinueAndBanksPhase() = runBlocking {
        val phaseComplete = async(Dispatchers.Default) { FocusTimerManager.phaseCompleteEvent.first() }
        FocusTimerManager.phaseCompleteEvent.subscriptionCount.first { it > 0 }

        FocusTimerManager.setMode("Short", 1, isRestPhase = false)
        FocusTimerManager.startTimer()

        val (wasRest, duration) = withTimeout(5000) { phaseComplete.await() }
        assertFalse(wasRest)
        assertEquals(1, duration)

        val state = FocusTimerManager.timerState.value
        assertTrue(state.isAwaitingContinue)
        assertFalse(state.isRunning)
        assertEquals(0, state.secondsRemaining)

        // The phase was already banked via phaseCompleteEvent; stopping now must report 0
        // so the session is not double-counted.
        val stopInfo = async(Dispatchers.Default) { FocusTimerManager.stopEvent.first() }
        FocusTimerManager.stopEvent.subscriptionCount.first { it > 0 }
        FocusTimerManager.stopTimer()
        assertEquals(0, withTimeout(5000) { stopInfo.await() }.elapsedFocusSeconds)
    }

    @Test
    fun discardStopIsFlagged() = runBlocking {
        FocusTimerManager.setMode("Focus", 300, isRestPhase = false)
        FocusTimerManager.startTimer()

        val stopInfo = async(Dispatchers.Default) { FocusTimerManager.stopEvent.first() }
        FocusTimerManager.stopEvent.subscriptionCount.first { it > 0 }
        FocusTimerManager.stopTimer(discard = true)
        assertTrue(withTimeout(5000) { stopInfo.await() }.discard)
    }
}
