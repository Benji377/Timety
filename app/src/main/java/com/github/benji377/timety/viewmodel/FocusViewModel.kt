package com.github.benji377.timety.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.github.benji377.timety.data.Category
import com.github.benji377.timety.data.FocusMode
import com.github.benji377.timety.data.FocusRating
import com.github.benji377.timety.data.FocusSession
import com.github.benji377.timety.data.FocusStep
import com.github.benji377.timety.data.FocusStepType
import com.github.benji377.timety.data.MainRepository
import com.github.benji377.timety.utils.NotificationHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class FocusViewModel(
    private val repository: MainRepository,
    private val context: Context? = null
) : ViewModel() {

    val categories: StateFlow<List<Category>> = repository.allCategories.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allFocusModes: StateFlow<List<FocusMode>> = repository.allFocusModes.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _currentFocusMode = MutableStateFlow<FocusMode?>(null)
    val currentFocusMode: StateFlow<FocusMode?> = _currentFocusMode.asStateFlow()

    private val _currentStepIndex = MutableStateFlow(0)
    val currentStepIndex: StateFlow<Int> = _currentStepIndex.asStateFlow()

    private val _timerMillis = MutableStateFlow(0L)
    val timerMillis: StateFlow<Long> = _timerMillis.asStateFlow()

    private val _targetMillis = MutableStateFlow(0L)
    val targetMillis: StateFlow<Long> = _targetMillis.asStateFlow()

    fun selectFocusMode(mode: FocusMode) {
        if (!_isRunning.value) {
            _currentFocusMode.value = mode
            _currentStepIndex.value = 0
            updateStepTimer()
        }
    }

    private fun updateStepTimer() {
        val mode = _currentFocusMode.value ?: return
        val step = mode.steps.getOrNull(_currentStepIndex.value) ?: return

        when (step.type) {
            FocusStepType.FOCUS, FocusStepType.REST -> {
                _timerMillis.value = step.durationMins * 60 * 1000L
                _targetMillis.value = _timerMillis.value
            }

            FocusStepType.STOPWATCH -> {
                _timerMillis.value = 0
                _targetMillis.value = 120 * 60 * 1000L // Default max for UI circle
            }

            FocusStepType.FLEXIBLE -> {
                _timerMillis.value = 0
                _targetMillis.value = 0
            }

            else -> {}
        }
    }

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _showReviewModal = MutableStateFlow(false)
    val showReviewModal: StateFlow<Boolean> = _showReviewModal.asStateFlow()

    private val _recentSessions = MutableStateFlow<List<FocusSession>>(emptyList())
    val recentSessions: StateFlow<List<FocusSession>> = _recentSessions.asStateFlow()

    private val _isStopwatchMode = MutableStateFlow(false)
    val isStopwatchMode: StateFlow<Boolean> = _isStopwatchMode.asStateFlow()

    private var currentSessionStartTime: Long = 0
    private var currentSessionCategoryId: Int = 0
    private var currentSessionTaskId: Int? = null

    private var timerJob: Job? = null
    private var startTime: Long = 0

    fun setMode(isStopwatch: Boolean) {
        if (!_isRunning.value) {
            _isStopwatchMode.value = isStopwatch
            _timerMillis.value = 0
        }
    }

    fun setTimerDuration(minutes: Int) {
        if (!_isRunning.value && !_isStopwatchMode.value) {
            _timerMillis.value = minutes * 60 * 1000L
        }
    }

    private val _totalFocusTimeToday = MutableStateFlow(0L)
    val totalFocusTimeToday: StateFlow<Long> = _totalFocusTimeToday.asStateFlow()

    private val _dailyTargetMins = MutableStateFlow(120L)
    val dailyTargetMins: StateFlow<Long> = _dailyTargetMins.asStateFlow()

    init {
        viewModelScope.launch {
            repository.allSessions.collect { sessions ->
                val today = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                _totalFocusTimeToday.value =
                    sessions.filter { it.startTime >= today }.sumOf { it.duration }
                _recentSessions.value = sessions.take(10) // Get last 10 sessions for Time Machine
            }
        }
        viewModelScope.launch {
            repository.user.collect { user ->
                user?.let {
                    _dailyTargetMins.value = it.dailyFocusTarget / (60 * 1000L)
                }
            }
        }
    }

    fun startTimer(categoryId: Int, taskId: Int? = null) {
        if (_isRunning.value) return

        startTime = System.currentTimeMillis()
        currentSessionStartTime = startTime
        currentSessionCategoryId = categoryId
        currentSessionTaskId = taskId
        _isRunning.value = true

        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            if (_isStopwatchMode.value) {
                while (true) {
                    delay(1000)
                    _timerMillis.value += 1000
                }
            } else {
                while (_timerMillis.value > 0) {
                    delay(1000)
                    _timerMillis.value -= 1000
                }
                _isRunning.value = false
                _showReviewModal.value = true
            }
        }
    }

    fun pauseTimer() {
        timerJob?.cancel()
        _isRunning.value = false
    }

    fun stopTimerManual() {
        timerJob?.cancel()
        _isRunning.value = false
        _showReviewModal.value = true
    }

    fun saveSession(rating: FocusRating, note: String? = null) {
        val endTime = System.currentTimeMillis()
        val duration =
            if (_isStopwatchMode.value) _timerMillis.value else (endTime - currentSessionStartTime)

        if (duration > 1000) {
            viewModelScope.launch {
                repository.insertSession(
                    FocusSession(
                        categoryId = currentSessionCategoryId,
                        taskId = currentSessionTaskId,
                        startTime = currentSessionStartTime,
                        endTime = endTime,
                        duration = duration,
                        rating = rating,
                        note = note
                    )
                )
                val user = repository.user.first()
                user?.let {
                    // Streak logic: check if user completed a focus today
                    val today = Calendar.getInstance().apply {
                        timeInMillis = System.currentTimeMillis()
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis

                    val lastActiveCal = Calendar.getInstance().apply {
                        timeInMillis = it.lastActiveDate
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val lastActiveDateNormalized = lastActiveCal.timeInMillis

                    val diffDays =
                        ((today - lastActiveDateNormalized) / (24 * 60 * 60 * 1000L)).toInt()

                    val (newStreak, newHighestStreak) = when {
                        diffDays <= 0 -> {
                            // Already focused today or in future (?), streak unchanged
                            it.currentStreak to it.highestStreak
                        }

                        diffDays == 1 -> {
                            // Focused yesterday, increment streak
                            val newCurrent = it.currentStreak + 1
                            newCurrent to maxOf(newCurrent, it.highestStreak)
                        }

                        diffDays <= 2 -> {
                            // Within 2-day frozen period (Day 1 and Day 2 of inactivity), keep streak
                            it.currentStreak to it.highestStreak
                        }

                        else -> {
                            // Streak broken on Day 3 of inactivity, reset to 1
                            1 to it.highestStreak
                        }
                    }

                    val streakMult = (1.0 + (newStreak * 0.05)).coerceAtMost(1.5)
                    val xpGained = ((duration / 60000) * streakMult).toInt()
                    val totalXp = it.xp + xpGained
                    val newLevel = (totalXp / 100) + 1

                    repository.insertOrUpdateUser(
                        it.copy(
                            xp = totalXp,
                            level = newLevel,
                            currentStreak = newStreak,
                            highestStreak = newHighestStreak,
                            lastActiveDate = today
                        )
                    )

                    // Send notification when session completes
                    context?.let {
                        NotificationHelper.showTimerCompleteNotification(
                            context,
                            "Focus session complete! +$xpGained XP"
                        )
                    }
                }
            }
        }
        _showReviewModal.value = false
        if (!_isStopwatchMode.value) _timerMillis.value = 0
    }

    fun addFocusMode(title: String, steps: List<FocusStep>) {
        viewModelScope.launch {
            repository.insertFocusMode(
                FocusMode(
                    title = title,
                    isCustom = true,
                    steps = steps
                )
            )
        }
    }

    fun deleteFocusMode(focusMode: FocusMode) {
        if (focusMode.isCustom) {
            viewModelScope.launch {
                repository.deleteFocusMode(focusMode)
            }
        }
    }

    fun deleteSession(session: FocusSession) {
        viewModelScope.launch {
            repository.deleteSession(session)
        }
    }

    fun logDailyEvent(type: String, description: String? = null) {
        viewModelScope.launch {
            repository.insertEvent(
                com.github.benji377.timety.data.DailyEvent(
                    timestamp = System.currentTimeMillis(),
                    type = type,
                    description = description
                )
            )
        }
    }

    fun deleteFocusMode(modeId: Int) {
        viewModelScope.launch {
            val mode = allFocusModes.value.find { it.id == modeId }
            if (mode != null && mode.isCustom) {
                repository.deleteFocusMode(mode)
            }
        }
    }

    fun startTimeMachineSession(
        startTimeMillis: Long,
        durationMins: Int,
        categoryId: Int,
        rating: FocusRating,
        note: String? = null
    ) {
        val endTime = startTimeMillis + (durationMins * 60 * 1000L)
        val now = System.currentTimeMillis()

        if (endTime <= now) {
            // Retroactive logging
            viewModelScope.launch {
                repository.insertSession(
                    FocusSession(
                        categoryId = categoryId,
                        startTime = startTimeMillis,
                        endTime = endTime,
                        duration = durationMins * 60 * 1000L,
                        rating = rating,
                        note = note
                    )
                )
                // Award XP and handle streaks based on this historical session
                awardXPAndHandleStreak(durationMins * 60 * 1000L)
            }
        } else {
            // Start the timer with remaining time
            val remainingMillis = endTime - now
            currentSessionStartTime = startTimeMillis
            currentSessionCategoryId = categoryId
            _timerMillis.value = remainingMillis
            _targetMillis.value = durationMins * 60 * 1000L
            _isRunning.value = true

            timerJob?.cancel()
            timerJob = viewModelScope.launch {
                while (_timerMillis.value > 0) {
                    delay(1000)
                    _timerMillis.value -= 1000
                }
                _isRunning.value = false
                _showReviewModal.value = true
            }
        }
    }

    private suspend fun awardXPAndHandleStreak(duration: Long) {
        val user = repository.user.first()
        user?.let {
            val today = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val lastActiveCal = Calendar.getInstance().apply {
                timeInMillis = it.lastActiveDate
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val lastActiveDateNormalized = lastActiveCal.timeInMillis

            val diffDays = ((today - lastActiveDateNormalized) / (24 * 60 * 60 * 1000L)).toInt()

            val (newStreak, newHighestStreak) = when {
                diffDays <= 0 -> it.currentStreak to it.highestStreak
                diffDays == 1 -> (it.currentStreak + 1) to maxOf(
                    it.currentStreak + 1,
                    it.highestStreak
                )

                diffDays <= 2 -> it.currentStreak to it.highestStreak
                else -> 1 to it.highestStreak
            }

            val streakMult = (1.0 + (newStreak * 0.05)).coerceAtMost(1.5)
            val xpGained = ((duration / 60000) * streakMult).toInt()
            val totalXp = it.xp + xpGained
            val newLevel = (totalXp / 100) + 1

            repository.insertOrUpdateUser(
                it.copy(
                    xp = totalXp,
                    level = newLevel,
                    currentStreak = newStreak,
                    highestStreak = newHighestStreak,
                    lastActiveDate = today
                )
            )
        }
    }

    fun dismissReview() {
        _showReviewModal.value = false
        if (!_isStopwatchMode.value) {
            updateStepTimer()
        }
    }
}

class FocusViewModelFactory(
    private val repository: MainRepository,
    private val context: Context? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FocusViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FocusViewModel(repository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
