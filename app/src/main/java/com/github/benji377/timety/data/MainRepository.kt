package com.github.benji377.timety.data

import kotlinx.coroutines.flow.Flow

class MainRepository(private val database: AppDatabase) {
    // User
    val user: Flow<User?> = database.userDao().getUser()
    suspend fun insertOrUpdateUser(user: User) = database.userDao().insertOrUpdateUser(user)

    // Tasks
    val allTasks: Flow<List<Task>> = database.taskDao().getAllTasks()
    fun getTasksByStatus(status: TaskStatus): Flow<List<Task>> = database.taskDao().getTasksByStatus(status)
    fun getTasksInRange(start: Long, end: Long): Flow<List<Task>> = database.taskDao().getTasksInRange(start, end)
    suspend fun insertTask(task: Task) = database.taskDao().insertTask(task)
    suspend fun updateTask(task: Task) = database.taskDao().updateTask(task)
    suspend fun deleteTask(task: Task) = database.taskDao().deleteTask(task)
    suspend fun updateOverdueTasks(now: Long) = database.taskDao().updateOverdueTasks(now, TaskStatus.TODO, TaskStatus.OVERDUE)

    // Categories
    val allCategories: Flow<List<Category>> = database.categoryDao().getAllCategories()
    suspend fun insertCategory(category: Category) = database.categoryDao().insertCategory(category)
    suspend fun updateCategory(category: Category) = database.categoryDao().updateCategory(category)
    suspend fun deleteCategory(category: Category) = database.categoryDao().deleteCategory(category)

    // Focus Sessions
    val allSessions: Flow<List<FocusSession>> = database.focusSessionDao().getAllSessions()
    fun getSessionsForTask(taskId: Int) = database.focusSessionDao().getSessionsForTask(taskId)
    fun getSessionsByCategory(categoryId: Int) = database.focusSessionDao().getSessionsByCategory(categoryId)
    suspend fun insertSession(session: FocusSession) = database.focusSessionDao().insertSession(session)
    suspend fun deleteSession(session: FocusSession) = database.focusSessionDao().deleteSession(session)
}
