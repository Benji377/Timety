package io.github.benji377.timety.services

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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
) {
    val progress: Float
        get() = if (totalPhaseSeconds > 0) (secondsRemaining.toFloat() / totalPhaseSeconds).coerceIn(0f, 1f) else 1f

    val centerText: String
        get() {
            val mins = secondsRemaining / 60
            val secs = secondsRemaining % 60
            return String.format("%02d:%02d", mins, secs)
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

    val phaseCompleteEvent = kotlinx.coroutines.flow.MutableSharedFlow<Pair<Boolean, Int>>(extraBufferCapacity = 1)

    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private var phaseBaseTime: Instant? = null
    private var currentPhaseTotalSeconds: Int = 0

    /**
     * Loads a phase's configuration. Safe to call again for the *same* phase while paused (e.g.
     * resuming) - progress is only reset when not currently paused, so a fresh call (new phase,
     * or first start) starts at [totalPhaseSeconds] while a resume-from-pause call preserves
     * whatever [TimerState.secondsRemaining] currently holds. Mirrors `FocusProvider._setupPhase`
     * being re-run for every phase (fresh anchor) while `startSession`'s resume path leaves
     * `_secondsRemainingInPhase` untouched.
     */
    fun setMode(name: String, totalPhaseSeconds: Int, isRestPhase: Boolean) {
        if (_timerState.value.isRunning) return
        val resuming = _timerState.value.isPaused
        _timerState.update {
            it.copy(
                modeName = name,
                totalPhaseSeconds = totalPhaseSeconds,
                secondsRemaining = if (resuming) it.secondsRemaining else totalPhaseSeconds,
                isRestPhase = isRestPhase,
                isAwaitingContinue = false,
            )
        }
        if (!resuming) {
            // Fresh phase (first start, or advancing after a natural completion): drop the old
            // anchor so `startTimer` re-derives it from `totalPhaseSeconds` instead of reusing a
            // stale one left over from whichever phase ran before this one.
            phaseBaseTime = null
        }
    }

    fun startTimer() {
        if (_timerState.value.isRunning) return

        _timerState.update { it.copy(isRunning = true, isPaused = false, isAwaitingContinue = false) }

        if (phaseBaseTime == null) {
            phaseBaseTime = Instant.now()
            currentPhaseTotalSeconds = _timerState.value.totalPhaseSeconds
        } else if (_timerState.value.isPaused) {
            // Adjust base time after resume
            val elapsedSoFar = currentPhaseTotalSeconds - _timerState.value.secondsRemaining
            phaseBaseTime = Instant.now().minusSeconds(elapsedSoFar.toLong())
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

    fun stopTimer() {
        job?.cancel()
        phaseBaseTime = null
        _timerState.update {
            it.copy(
                isRunning = false,
                isPaused = false,
                isAwaitingContinue = false,
                secondsRemaining = it.totalPhaseSeconds,
            )
        }
    }

    private fun tick() {
        val baseTime = phaseBaseTime ?: return
        val now = Instant.now()
        val elapsed = now.epochSecond - baseTime.epochSecond
        val remaining = (currentPhaseTotalSeconds - elapsed).toInt()

        if (remaining <= 0) {
            // Phase complete - freeze at zero and wait for the caller to either advance to the
            // next phase (setMode + startTimer) or stop the session (stopTimer).
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
            _timerState.update { it.copy(secondsRemaining = remaining) }
        }
    }
}
