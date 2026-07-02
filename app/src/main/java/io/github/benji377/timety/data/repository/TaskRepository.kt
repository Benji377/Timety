package io.github.benji377.timety.data.repository

import io.github.benji377.timety.data.local.dao.TaskDao
import io.github.benji377.timety.data.model.task.SubtaskEntity
import io.github.benji377.timety.data.model.task.TaskEntity
import kotlinx.coroutines.flow.Flow
class TaskRepository(
    private val taskDao: TaskDao
) {
    val allTasks: Flow<List<TaskEntity>> = taskDao.getAllTasks()

    suspend fun getTaskById(id: String): TaskEntity? = taskDao.getTaskById(id)

    suspend fun insertTask(task: TaskEntity) {
        taskDao.insertTask(task)
    }

    suspend fun updateTask(task: TaskEntity) {
        taskDao.updateTask(task)
    }

    suspend fun deleteTask(task: TaskEntity) {
        taskDao.deleteTask(task)
    }

    // Subtasks
    fun getSubtasksForTask(taskId: String): Flow<List<SubtaskEntity>> = taskDao.getSubtasksForTask(taskId)

    suspend fun insertSubtask(subtask: SubtaskEntity) {
        taskDao.insertSubtask(subtask)
    }

    suspend fun updateSubtask(subtask: SubtaskEntity) {
        taskDao.updateSubtask(subtask)
    }

    suspend fun deleteSubtask(subtask: SubtaskEntity) {
        taskDao.deleteSubtask(subtask)
    }
}
