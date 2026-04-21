package com.github.benji377.timety.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.github.benji377.timety.data.Category
import com.github.benji377.timety.data.MainRepository
import com.github.benji377.timety.data.Task
import com.github.benji377.timety.data.TaskPriority
import com.github.benji377.timety.data.TaskSize
import com.github.benji377.timety.data.TaskStatus
import com.github.benji377.timety.utils.ReminderManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class TasksViewModel(
    private val repository: MainRepository,
    private val context: Context? = null
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(System.currentTimeMillis())
    val selectedDate: StateFlow<Long> = _selectedDate.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val tasksByDate: StateFlow<List<Task>> = _selectedDate.flatMapLatest { date ->
        val cal = Calendar.getInstance()
        cal.timeInMillis = date
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        val end = cal.timeInMillis
        repository.getTasksInRange(start, end)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val upcomingTasks: StateFlow<List<Task>> = _selectedDate.flatMapLatest { date ->
        val cal = Calendar.getInstance()
        cal.timeInMillis = date
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        val endOfSelectedDay = cal.timeInMillis
        repository.getTasksInRange(endOfSelectedDay + 1, Long.MAX_VALUE)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allTasks: StateFlow<List<Task>> = repository.allTasks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _selectedCategoryId = MutableStateFlow<Int?>(null)
    val selectedCategoryId: StateFlow<Int?> = _selectedCategoryId.asStateFlow()

    val categories: StateFlow<List<Category>> = repository.allCategories.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.DUE_DATE)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    enum class SortOrder {
        PRIORITY, NAME, SIZE, DUE_DATE
    }

    val filteredTasks: StateFlow<List<Task>> = kotlinx.coroutines.flow.combine(
        allTasks, _selectedCategoryId, _searchQuery, _sortOrder
    ) { tasks, catId, query, sort ->
        var filtered = tasks
        if (catId != null) {
            filtered = filtered.filter { it.categoryId == catId }
        }
        if (query.isNotBlank()) {
            filtered = filtered.filter { it.title.contains(query, ignoreCase = true) }
        }

        when (sort) {
            SortOrder.PRIORITY -> filtered.sortedByDescending { it.priority }
            SortOrder.NAME -> filtered.sortedBy { it.title }
            SortOrder.SIZE -> filtered.sortedByDescending { it.size }
            SortOrder.DUE_DATE -> filtered.sortedBy { it.dueDate ?: Long.MAX_VALUE }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val overdueTasks: StateFlow<List<Task>> = filteredTasks.flatMapLatest { tasks ->
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        kotlinx.coroutines.flow.flowOf(
            tasks.filter { it.status != TaskStatus.DONE && it.dueDate != null && it.dueDate < todayStart }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val todayTasks: StateFlow<List<Task>> = filteredTasks.flatMapLatest { tasks ->
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val todayEnd = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
        kotlinx.coroutines.flow.flowOf(
            tasks.filter { it.status != TaskStatus.DONE && it.dueDate != null && it.dueDate in todayStart..todayEnd }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val upcomingTasksList: StateFlow<List<Task>> = filteredTasks.flatMapLatest { tasks ->
        val todayEnd = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
        kotlinx.coroutines.flow.flowOf(
            tasks.filter { it.status != TaskStatus.DONE && (it.dueDate == null || it.dueDate > todayEnd) }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val doneTasks: StateFlow<List<Task>> = filteredTasks.flatMapLatest { tasks ->
        kotlinx.coroutines.flow.flowOf(
            tasks.filter { it.status == TaskStatus.DONE }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getTotalFocusTimeForTask(taskId: Int?): Long {
        if (taskId == null) return 0L
        // This would need to be a proper query in real implementation
        // For now, users would need to check stats
        return 0L
    }

    fun selectDate(date: Long) {
        _selectedDate.value = date
    }

    fun addTask(
        title: String,
        description: String?,
        location: String? = null,
        dueDate: Long? = null,
        reminders: List<Long> = emptyList(),
        iconName: String = "Check",
        priority: TaskPriority = TaskPriority.MEDIUM,
        size: TaskSize = TaskSize.MEDIUM
    ) {
        viewModelScope.launch {
            val newTask = Task(
                title = title,
                description = description,
                iconName = iconName,
                location = location,
                dueDate = dueDate ?: _selectedDate.value,
                reminders = reminders,
                status = TaskStatus.TODO,
                priority = priority,
                size = size
            )
            repository.insertTask(newTask)

            // Schedule reminders if provided
            if (reminders.isNotEmpty() && context != null) {
                ReminderManager.scheduleMultipleReminders(context, newTask.id, title, reminders)
            }
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            repository.updateTask(task)
        }
    }

    fun selectCategory(categoryId: Int?) {
        _selectedCategoryId.value = categoryId
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
    }

    fun deleteTasks(tasks: List<Task>) {
        viewModelScope.launch {
            tasks.forEach { repository.deleteTask(it) }
        }
    }

    fun moveTasksToCategory(tasks: List<Task>, categoryId: Int) {
        viewModelScope.launch {
            tasks.forEach { repository.updateTask(it.copy(categoryId = categoryId)) }
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
                    repository.insertOrUpdateUser(
                        it.copy(
                            xp = it.xp + xpGained,
                            level = ((it.xp + xpGained) / 100) + 1
                        )
                    )
                }
            }
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }
}

class TasksViewModelFactory(
    private val repository: MainRepository,
    private val context: Context? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TasksViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TasksViewModel(repository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
