package io.github.benji377.timety.data.repository

import io.github.benji377.timety.data.local.dao.TaskDao
import io.github.benji377.timety.data.model.task.SubtaskEntity
import io.github.benji377.timety.data.model.task.TaskEntity
import io.github.benji377.timety.data.model.task.TaskWithSubtasks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class TaskRepository(
    private val taskDao: TaskDao
) {
    val allTasks: Flow<List<TaskWithSubtasks>> =
        taskDao.getAllTasks()

    suspend fun getOpenTasksDueBefore(deadline: java.time.Instant): List<TaskEntity> =
        withContext(Dispatchers.IO) {
            taskDao.getOpenTasksDueBefore(deadline)
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

    suspend fun deleteSubtask(subtask: SubtaskEntity) = withContext(Dispatchers.IO) {
        taskDao.deleteSubtask(subtask)
    }
}
