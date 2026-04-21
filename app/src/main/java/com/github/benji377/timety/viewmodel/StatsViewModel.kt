package com.github.benji377.timety.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.github.benji377.timety.data.Category
import com.github.benji377.timety.data.FocusSession
import com.github.benji377.timety.data.MainRepository
import com.github.benji377.timety.data.User
import com.github.benji377.timety.utils.InsightsGenerator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar

class StatsViewModel(private val repository: MainRepository) : ViewModel() {

    val user: StateFlow<User?> = repository.user.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val allSessions: StateFlow<List<FocusSession>> = repository.allSessions.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allCategories: StateFlow<List<Category>> = repository.allCategories.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val todayFocusTime: StateFlow<Long> = allSessions.map { sessions ->
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        sessions.filter { it.startTime >= today }.sumOf { it.duration }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0L
    )

    val categoryDistribution: StateFlow<Map<Int, Long>> = allSessions.map { sessions ->
        sessions.groupBy { it.categoryId }
            .mapValues { (_, sessions) -> sessions.sumOf { it.duration } }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )

    val insights: StateFlow<List<String>> = allSessions.map { sessions ->
        InsightsGenerator.generateInsights(sessions)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = listOf("Start focusing to unlock insights!")
    )

    val weeklyFocusData: StateFlow<Map<String, Long>> = allSessions.map { sessions ->
        val calendar = Calendar.getInstance()
        val focusByDay = mutableMapOf<String, Long>()

        // Initialize last 7 days
        repeat(7) { i ->
            val dayMills = calendar.timeInMillis - (i * 24 * 60 * 60 * 1000L)
            val cal = Calendar.getInstance().apply {
                timeInMillis = dayMills
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val dayName = when (cal.get(Calendar.DAY_OF_WEEK)) {
                1 -> "Sun"
                2 -> "Mon"
                3 -> "Tue"
                4 -> "Wed"
                5 -> "Thu"
                6 -> "Fri"
                else -> "Sat"
            }
            focusByDay[dayName] = 0L
        }

        // Aggregate sessions by day
        sessions.forEach { session ->
            val cal = Calendar.getInstance().apply { timeInMillis = session.startTime }
            val dayName = when (cal.get(Calendar.DAY_OF_WEEK)) {
                1 -> "Sun"
                2 -> "Mon"
                3 -> "Tue"
                4 -> "Wed"
                5 -> "Thu"
                6 -> "Fri"
                else -> "Sat"
            }
            focusByDay[dayName] = (focusByDay[dayName] ?: 0L) + session.duration
        }

        focusByDay
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )

    fun getEventsForDay(date: Long): kotlinx.coroutines.flow.Flow<List<com.github.benji377.timety.data.DailyEvent>> {
        val calendar = Calendar.getInstance().apply { timeInMillis = date }
        val startOfDay = calendar.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val endOfDay = startOfDay + 24 * 60 * 60 * 1000L
        return repository.getEventsForDay(startOfDay, endOfDay)
    }
}

class StatsViewModelFactory(private val repository: MainRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StatsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StatsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
