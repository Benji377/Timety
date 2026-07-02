package io.github.benji377.timety.services

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.Instant

data class TimerState(
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val secondsRemaining: Int = 25 * 60,
    val totalPhaseSeconds: Int = 25 * 60,
    val phaseName: String = "Focus Phase",
    val modeName: String = "Pomodoro",
    val isRestPhase: Boolean = false
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

object FocusTimerManager {
    private val _timerState = MutableStateFlow(TimerState())
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    private var phaseBaseTime: Instant? = null
    private var currentPhaseTotalSeconds: Int = 0

    fun startTimer() {
        if (_timerState.value.isRunning) return
        
        _timerState.update { it.copy(isRunning = true, isPaused = false) }
        
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
                secondsRemaining = it.totalPhaseSeconds
            ) 
        }
    }

    private fun tick() {
        val baseTime = phaseBaseTime ?: return
        val now = Instant.now()
        val elapsed = now.epochSecond - baseTime.epochSecond
        val remaining = (currentPhaseTotalSeconds - elapsed).toInt()

        if (remaining <= 0) {
            // Phase complete
            job?.cancel()
            _timerState.update { 
                it.copy(
                    isRunning = false,
                    isPaused = false,
                    secondsRemaining = 0
                ) 
            }
            // Logic to switch to next phase or complete session would go here, 
            // potentially communicating back to the ViewModel to save the session to the DB.
        } else {
            _timerState.update { it.copy(secondsRemaining = remaining) }
        }
    }
}
