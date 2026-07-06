package io.github.benji377.timety.services

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Instant


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


/** Coarse running/paused flags; changes only on phase transitions, never per tick. */
data class TimerFlags(val isRunning: Boolean = false, val isPaused: Boolean = false)


object FocusTimerManager {
    private val _timerState = MutableStateFlow(TimerState())
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    /**
     * Transition-level view of [timerState] for UI that only cares whether a session is
     * active. StateFlow conflates equal values, so collectors recompose on state changes
     * but not on every one-second tick.
     */
    val timerFlags: StateFlow<TimerFlags> = _timerState
        .map { TimerFlags(it.isRunning, it.isPaused) }
        .stateIn(
            CoroutineScope(Dispatchers.Default + SupervisorJob()),
            SharingStarted.Eagerly,
            TimerFlags()
        )

    val phaseCompleteEvent =
        kotlinx.coroutines.flow.MutableSharedFlow<Pair<Boolean, Int>>(extraBufferCapacity = 1)


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


    fun setMode(
        name: String,
        totalPhaseSeconds: Int,
        isRestPhase: Boolean,
        isStopwatch: Boolean = false
    ) {
        if (_timerState.value.isRunning) return
        val resuming = _timerState.value.isPaused
        // isAwaitingContinue is deliberately NOT cleared here (startTimer does that): when
        // continuing into the next phase, clearing it would emit a transient all-idle state
        // that FocusTimerService's observer treats as "session over" - it then kills itself
        // in a race with the incoming ACTION_START, losing the foreground notification and
        // the phase-end sound alarm for the rest of the session.
        _timerState.update {
            it.copy(
                modeName = name,
                totalPhaseSeconds = totalPhaseSeconds,
                secondsRemaining = if (resuming) it.secondsRemaining else totalPhaseSeconds,
                isRestPhase = isRestPhase,
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
