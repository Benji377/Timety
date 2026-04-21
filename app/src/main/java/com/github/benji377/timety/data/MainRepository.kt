package com.github.benji377.timety.data

import kotlinx.coroutines.flow.Flow

class MainRepository(private val database: AppDatabase) {
    // User
    val user: Flow<User?> = database.userDao().getUser()
    suspend fun insertOrUpdateUser(user: User) = database.userDao().insertOrUpdateUser(user)

    // Tasks
    val allTasks: Flow<List<Task>> = database.taskDao().getAllTasks()
    fun getTasksByStatus(status: TaskStatus): Flow<List<Task>> =
        database.taskDao().getTasksByStatus(status)

    fun getTasksInRange(start: Long, end: Long): Flow<List<Task>> =
        database.taskDao().getTasksInRange(start, end)

    suspend fun insertTask(task: Task) = database.taskDao().insertTask(task)
    suspend fun getTaskById(taskId: Int): Task? = database.taskDao().getTaskById(taskId)
    suspend fun updateTaskStatus(taskId: Int, status: TaskStatus) =
        database.taskDao().updateTaskStatus(taskId, status)

    suspend fun updateTask(task: Task) = database.taskDao().updateTask(task)
    suspend fun deleteTask(task: Task) = database.taskDao().deleteTask(task)
    suspend fun updateOverdueTasks(now: Long) =
        database.taskDao().updateOverdueTasks(now, TaskStatus.TODO, TaskStatus.OVERDUE)

    // Categories
    val allCategories: Flow<List<Category>> = database.categoryDao().getAllCategories()
    suspend fun insertCategory(category: Category) = database.categoryDao().insertCategory(category)
    suspend fun updateCategory(category: Category) = database.categoryDao().updateCategory(category)
    suspend fun deleteCategory(category: Category) = database.categoryDao().deleteCategory(category)

    // Focus Sessions
    val allSessions: Flow<List<FocusSession>> = database.focusSessionDao().getAllSessions()
    fun getSessionsForTask(taskId: Int) = database.focusSessionDao().getSessionsForTask(taskId)
    fun getSessionsByCategory(categoryId: Int) =
        database.focusSessionDao().getSessionsByCategory(categoryId)

    fun getSessionsForDay(startOfDay: Long, endOfDay: Long) =
        database.focusSessionDao().getSessionsForDay(startOfDay, endOfDay)

    suspend fun insertSession(session: FocusSession) =
        database.focusSessionDao().insertSession(session)

    suspend fun deleteSession(session: FocusSession) =
        database.focusSessionDao().deleteSession(session)

    // Focus Modes
    val allFocusModes: Flow<List<FocusMode>> = database.focusModeDao().getAllFocusModes()
    suspend fun insertFocusMode(focusMode: FocusMode) =
        database.focusModeDao().insertFocusMode(focusMode)

    suspend fun updateFocusMode(focusMode: FocusMode) =
        database.focusModeDao().updateFocusMode(focusMode)

    suspend fun deleteFocusMode(focusMode: FocusMode) =
        database.focusModeDao().deleteFocusMode(focusMode)

    // Daily Events
    fun getEventsForDay(startOfDay: Long, endOfDay: Long) =
        database.dailyEventDao().getEventsForDay(startOfDay, endOfDay)

    suspend fun insertEvent(event: DailyEvent) = database.dailyEventDao().insertEvent(event)
    suspend fun deleteEvent(event: DailyEvent) = database.dailyEventDao().deleteEvent(event)
}
