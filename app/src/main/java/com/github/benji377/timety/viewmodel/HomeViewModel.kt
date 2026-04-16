package com.github.benji377.timety.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.github.benji377.timety.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class HomeViewModel(private val repository: MainRepository) : ViewModel() {

    val user: StateFlow<User?> = repository.user.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val greeting: StateFlow<String> = user.map { user ->
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val timeGreeting = when (hour) {
            in 0..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..20 -> "Good evening"
            else -> "Good night"
        }
        "$timeGreeting, ${user?.name ?: "Hero"}"
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "Welcome"
    )

    val todayTasks: StateFlow<List<Task>> = repository.allTasks.map { tasks ->
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val todayEnd = todayStart + 24 * 60 * 60 * 1000L - 1
        tasks.filter { it.status != TaskStatus.DONE && it.dueDate != null && it.dueDate in todayStart..todayEnd }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val todayFocusTime: StateFlow<Long> = repository.allSessions.map { sessions ->
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

    init {
        // Update overdue tasks periodically
        viewModelScope.launch {
            while (true) {
                repository.updateOverdueTasks(System.currentTimeMillis())
                delay(60000) // Check every minute
            }
        }
    }

    fun toggleTaskStatus(task: Task) {
        viewModelScope.launch {
            val isCompleting = task.status != TaskStatus.DONE
            val newStatus = if (isCompleting) TaskStatus.DONE else TaskStatus.TODO
            repository.updateTask(task.copy(status = newStatus))

            if (isCompleting) {
                val user = repository.user.first()
                user?.let {
                    val baseXp = 50
                    val priorityMult = when (task.priority) {
                        TaskPriority.URGENT -> 2.0
                        TaskPriority.HIGH -> 1.5
                        else -> 1.0
                    }
                    val sizeMult = when (task.size) {
                        TaskSize.XLARGE -> 2.0
                        TaskSize.LARGE -> 1.5
                        TaskSize.MEDIUM -> 1.0
                        TaskSize.SMALL -> 0.75
                        TaskSize.TINY -> 0.5
                    }
                    val streakMult = (1.0 + (it.currentStreak * 0.05)).coerceAtMost(1.5)
                    
                    val xpGained = (baseXp * priorityMult * sizeMult * streakMult).toInt()
                    repository.insertOrUpdateUser(it.copy(
                        xp = it.xp + xpGained,
                        level = ((it.xp + xpGained) / 100) + 1
                    ))
                }
            }
        }
    }

    fun updateXp(additionalXp: Int) {
        viewModelScope.launch {
            user.value?.let { currentUser ->
                val newXp = currentUser.xp + additionalXp
                // Simple level up logic: 100 XP per level
                val newLevel = (newXp / 100) + 1
                repository.insertOrUpdateUser(currentUser.copy(xp = newXp, level = newLevel))
            }
        }
    }
}

class HomeViewModelFactory(private val repository: MainRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
