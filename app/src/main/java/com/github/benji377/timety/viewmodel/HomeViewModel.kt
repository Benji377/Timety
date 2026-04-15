package com.github.benji377.timety.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.github.benji377.timety.data.MainRepository
import com.github.benji377.timety.data.Task
import com.github.benji377.timety.data.TaskStatus
import com.github.benji377.timety.data.User
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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

    val todayTasks = repository.getTasksByStatus(TaskStatus.TODO).stateIn(
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
            val newStatus = if (task.status == TaskStatus.DONE) TaskStatus.TODO else TaskStatus.DONE
            repository.updateTask(task.copy(status = newStatus))
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
