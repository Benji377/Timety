package io.github.benji377.timety.services

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * Snapshot of the running focus/rest phase. Mirrors the countdown-relevant subset of Flutter's
 * `FocusProvider` state (`_isRunning`, `_isPaused`, `_secondsRemainingInPhase`,
 * `_currentPhaseTotalSeconds`, `_activeMode!.name`, phase `type`).
 *
 * [isAwaitingContinue] mirrors `FocusProvider._awaitingPhaseContinue`: true once a phase counts
 * down to zero on its own (as opposed to an explicit stop), until the caller either starts the
 * next phase (via [FocusTimerManager.setMode] + [FocusTimerManager.startTimer]) or stops the
 * session (via [FocusTimerManager.stopTimer]). [FocusTimerService] uses it to keep the foreground
 * notification/service alive across the "ready to continue" gap instead of tearing down as soon
 * as the phase's countdown reaches zero - the actual advance-vs-stop decision still lives in
 * `FocusScreen`/`FocusViewModel` (phase count/index are UI-owned), this flag only prevents the
 * service from disappearing prematurely while that decision is pending.
 */
data class TimerState(
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val isAwaitingContinue: Boolean = false,
    val secondsRemaining: Int = 25 * 60,
    val totalPhaseSeconds: Int = 25 * 60,
    val modeName: String = "Pomodoro",
    val isRestPhase: Boolean = false,
    val isStopwatch: Boolean = false,
    val elapsedSeconds: Int = 0,
) {
    val progress: Float
        get() = if (isStopwatch) 0f else if (totalPhaseSeconds > 0) (secondsRemaining.toFloat() / totalPhaseSeconds).coerceIn(
            0f,
            1f
        ) else 1f

    val centerText: String
        get() {
            val displaySecs = if (isStopwatch) elapsedSeconds else secondsRemaining
            val mins = displaySecs / 60
            val secs = displaySecs % 60
            return String.format(java.util.Locale.ROOT, "%02d:%02d", mins, secs)
        }
}

/**
 * Singleton countdown engine backing [FocusTimerService]. Mirrors the anchor-based (absolute
 * `DateTime` difference, not tick-accumulation) drift-proof timing technique used by both
 * `FocusProvider._tick` and the unused `focus_timer_service.dart` reference.
 */
object FocusTimerManager {
    private val _timerState = MutableStateFlow(TimerState())
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    val phaseCompleteEvent =
        kotlinx.coroutines.flow.MutableSharedFlow<Pair<Boolean, Int>>(extraBufferCapacity = 1)

    /**
     * Emitted once per [stopTimer] call on an active session, so session bookkeeping (logging the
     * partial phase, resetting the UI's phase cursor) happens identically whether the stop came
     * from the in-app dialog or the notification's Stop action while the app is backgrounded.
     *
     * [elapsedFocusSeconds] is 0 when the stop happened at an awaiting-continue boundary: that
     * phase's full duration was already banked via [phaseCompleteEvent], counting it again here
     * would double it.
     */
    data class StopInfo(
        val elapsedFocusSeconds: Int,
        val wasRestPhase: Boolean,
        val discard: Boolean,
    )

    val stopEvent = kotlinx.coroutines.flow.MutableSharedFlow<StopInfo>(extraBufferCapacity = 1)

    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private var phaseBaseTime: Instant? = null
    private var currentPhaseTotalSeconds: Int = 0
    private var accumulatedElapsed: Int = 0

    /**
     * Loads a phase's configuration. Safe to call again for the *same* phase while paused (e.g.
     * resuming) - progress is only reset when not currently paused, so a fresh call (new phase,
     * or first start) starts at [totalPhaseSeconds] while a resume-from-pause call preserves
     * whatever [TimerState.secondsRemaining] currently holds. Mirrors `FocusProvider._setupPhase`
     * being re-run for every phase (fresh anchor) while `startSession`'s resume path leaves
     * `_secondsRemainingInPhase` untouched.
     */
    fun setMode(
        name: String,
        totalPhaseSeconds: Int,
        isRestPhase: Boolean,
        isStopwatch: Boolean = false
    ) {
        if (_timerState.value.isRunning) return
        val resuming = _timerState.value.isPaused
        _timerState.update {
            it.copy(
                modeName = name,
                totalPhaseSeconds = totalPhaseSeconds,
                secondsRemaining = if (resuming) it.secondsRemaining else totalPhaseSeconds,
                isRestPhase = isRestPhase,
                isAwaitingContinue = false,
                isStopwatch = isStopwatch,
                elapsedSeconds = if (resuming) it.elapsedSeconds else 0
            )
        }
        if (!resuming) {
            phaseBaseTime = null
            accumulatedElapsed = 0
        }
    }

    fun startTimer() {
        if (_timerState.value.isRunning) return
        // Must be read before the state update below clears the paused flag, otherwise the
        // resume branch is unreachable and the countdown silently swallows the pause duration.
        val resuming = _timerState.value.isPaused

        _timerState.update {
            it.copy(
                isRunning = true,
                isPaused = false,
                isAwaitingContinue = false
            )
        }

        if (phaseBaseTime == null) {
            phaseBaseTime = Instant.now()
            currentPhaseTotalSeconds = _timerState.value.totalPhaseSeconds
        } else if (resuming) {
            // Bank the pre-pause elapsed time and re-anchor, so the paused wall time is excluded.
            accumulatedElapsed = _timerState.value.elapsedSeconds
            phaseBaseTime = Instant.now()
        }

        job?.cancel()
        job = scope.launch {
            while (isActive) {
                tick()
                delay(1000)
            }
        }
    }

    fun pauseTimer() {
        job?.cancel()
        _timerState.update { it.copy(isRunning = false, isPaused = true) }
    }

    fun stopTimer(discard: Boolean = false) {
        job?.cancel()
        val state = _timerState.value
        val wasActive = state.isRunning || state.isPaused || state.isAwaitingContinue
        val elapsed = when {
            state.isAwaitingContinue -> 0 // already banked via phaseCompleteEvent
            state.isStopwatch -> state.elapsedSeconds
            else -> (state.totalPhaseSeconds - state.secondsRemaining).coerceAtLeast(0)
        }
        phaseBaseTime = null
        accumulatedElapsed = 0
        _timerState.update {
            it.copy(
                isRunning = false,
                isPaused = false,
                isAwaitingContinue = false,
                secondsRemaining = it.totalPhaseSeconds,
                elapsedSeconds = 0,
            )
        }
        if (wasActive) {
            stopEvent.tryEmit(StopInfo(elapsed, state.isRestPhase, discard))
        }
    }

    private fun tick() {
        val baseTime = phaseBaseTime ?: return
        val now = Instant.now()
        val currentChunkElapsed = (now.epochSecond - baseTime.epochSecond).toInt()
        val totalElapsed = accumulatedElapsed + currentChunkElapsed

        if (_timerState.value.isStopwatch) {
            _timerState.update { it.copy(elapsedSeconds = totalElapsed) }
        } else {
            val remaining = (currentPhaseTotalSeconds - totalElapsed).coerceAtLeast(0)
            if (remaining <= 0) {
                job?.cancel()
                val isRestPhase = _timerState.value.isRestPhase
                val duration = _timerState.value.totalPhaseSeconds

                _timerState.update {
                    it.copy(
                        isRunning = false,
                        isPaused = false,
                        isAwaitingContinue = true,
                        secondsRemaining = 0,
                    )
                }
                phaseCompleteEvent.tryEmit(Pair(isRestPhase, duration))
            } else {
                _timerState.update {
                    it.copy(
                        secondsRemaining = remaining,
                        elapsedSeconds = totalElapsed
                    )
                }
            }
        }
    }
}
