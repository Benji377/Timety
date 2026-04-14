package com.github.benji377.timety.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.github.benji377.timety.data.Category
import com.github.benji377.timety.data.FocusRating
import com.github.benji377.timety.data.FocusSession
import com.github.benji377.timety.data.MainRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class FocusViewModel(private val repository: MainRepository) : ViewModel() {

    val categories: StateFlow<List<Category>> = repository.allCategories.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _timerMillis = MutableStateFlow(0L)
    val timerMillis: StateFlow<Long> = _timerMillis.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _showReviewModal = MutableStateFlow(false)
    val showReviewModal: StateFlow<Boolean> = _showReviewModal.asStateFlow()

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
        val duration = if (_isStopwatchMode.value) _timerMillis.value else (endTime - currentSessionStartTime)
        
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
                    val xpGained = (duration / 60000).toInt()
                    val totalXp = it.xp + xpGained
                    val levelUpXp = 100
                    val newLevel = it.level + (totalXp / levelUpXp)
                    val remainingXp = totalXp % levelUpXp
                    repository.insertOrUpdateUser(it.copy(xp = remainingXp, level = newLevel))
                }
            }
        }
        _showReviewModal.value = false
        if (!_isStopwatchMode.value) _timerMillis.value = 0
    }

    fun dismissReview() {
        _showReviewModal.value = false
        if (!_isStopwatchMode.value) _timerMillis.value = 0
    }
}

class FocusViewModelFactory(private val repository: MainRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FocusViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FocusViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
