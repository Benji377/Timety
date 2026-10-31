package io.github.benji377.timety.data.repository

import io.github.benji377.timety.data.local.dao.TaskDao
import io.github.benji377.timety.data.model.task.SubtaskEntity
import io.github.benji377.timety.data.model.task.TaskEntity
import io.github.benji377.timety.data.model.task.TaskWithSubtasks
import io.github.benji377.timety.data.model.task.withSortedSubtasks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** Repository for tasks and subtasks, wrapping [TaskDao] with IO dispatching. */
class TaskRepository(
    private val taskDao: TaskDao
) {
    val allTasks: Flow<List<TaskWithSubtasks>> =
        taskDao.getAllTasks().map { tasks -> tasks.map { it.withSortedSubtasks() } }

    fun observeTask(id: String): Flow<TaskWithSubtasks?> =
        taskDao.observeTaskWithSubtasks(id).map { it?.withSortedSubtasks() }

    suspend fun getOpenTasksDueBefore(deadline: java.time.Instant): List<TaskEntity> =
        withContext(Dispatchers.IO) {
            taskDao.getOpenTasksDueBefore(deadline)
        }

    suspend fun getTaskById(id: String): TaskEntity? = withContext(Dispatchers.IO) {
        taskDao.getTaskById(id)
    }

    suspend fun insertTask(task: TaskEntity) = withContext(Dispatchers.IO) {
        taskDao.insertTask(task)
    }

    suspend fun updateTask(task: TaskEntity) = withContext(Dispatchers.IO) {
        taskDao.updateTask(task)
    }

    suspend fun deleteTask(task: TaskEntity) = withContext(Dispatchers.IO) {
        taskDao.deleteTask(task)
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        taskDao.clearAll()
    }

    suspend fun insertSubtask(subtask: SubtaskEntity) = withContext(Dispatchers.IO) {
        taskDao.insertSubtask(subtask)
    }

    suspend fun updateSubtask(subtask: SubtaskEntity) = withContext(Dispatchers.IO) {
        taskDao.updateSubtask(subtask)
    }

    suspend fun updateSubtasks(subtasks: List<SubtaskEntity>) = withContext(Dispatchers.IO) {
        taskDao.updateSubtasks(subtasks)
    }

    suspend fun deleteSubtask(subtask: SubtaskEntity) = withContext(Dispatchers.IO) {
        taskDao.deleteSubtask(subtask)
    }
}
