package com.github.benji377.timety.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.github.benji377.timety.data.MainRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar

class CalendarViewModel(private val repository: MainRepository) : ViewModel() {

    private val _selectedDate = MutableStateFlow(System.currentTimeMillis())
    val selectedDate: StateFlow<Long> = _selectedDate.asStateFlow()

    private val _currentMonth = MutableStateFlow(Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis)
    val currentMonth: StateFlow<Long> = _currentMonth.asStateFlow()

    val allTasks = repository.allTasks.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )
    val allSessions = repository.allSessions.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val selectedDayTasks = combine(_selectedDate, allTasks) { date, tasks ->
        val cal = Calendar.getInstance().apply { timeInMillis = date }
        val startOfDay = cal.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val endOfDay = startOfDay + 24 * 60 * 60 * 1000L
        tasks.filter { it.dueDate != null && it.dueDate >= startOfDay && it.dueDate < endOfDay }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedDaySessions = combine(_selectedDate, allSessions) { date, sessions ->
        val cal = Calendar.getInstance().apply { timeInMillis = date }
        val startOfDay = cal.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val endOfDay = startOfDay + 24 * 60 * 60 * 1000L
        sessions.filter { it.startTime >= startOfDay && it.startTime < endOfDay }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedDayFocusTime = selectedDaySessions.map { sessions ->
        sessions.sumOf { it.duration }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    fun selectDate(date: Long) {
        _selectedDate.value = date
    }

    fun nextMonth() {
        val cal = Calendar.getInstance().apply {
            timeInMillis = _currentMonth.value
            add(Calendar.MONTH, 1)
        }
        _currentMonth.value = cal.timeInMillis
    }

    fun previousMonth() {
        val cal = Calendar.getInstance().apply {
            timeInMillis = _currentMonth.value
            add(Calendar.MONTH, -1)
        }
        _currentMonth.value = cal.timeInMillis
    }
}

class CalendarViewModelFactory(private val repository: MainRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CalendarViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CalendarViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
