package io.github.benji377.timety.data.repository

import io.github.benji377.timety.data.local.dao.RecurringTaskDao
import io.github.benji377.timety.data.model.task.RecurringOccurrenceEntity
import io.github.benji377.timety.data.model.task.RecurringSubtaskEntity
import io.github.benji377.timety.data.model.task.RecurringTaskEntity
import io.github.benji377.timety.data.model.task.RecurringTaskWithOccurrences
import io.github.benji377.timety.data.model.task.withSortedSubtasks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import java.time.Instant

/** Repository for recurring tasks and their occurrences, wrapping [RecurringTaskDao] with IO dispatching. */
class RecurringTaskRepository(
    private val recurringTaskDao: RecurringTaskDao
) {
    val allRecurringTasks: Flow<List<RecurringTaskWithOccurrences>> =
        recurringTaskDao.getAllWithOccurrences().map { tasks -> tasks.map { it.withSortedSubtasks() } }

    // Serializes completing and unchecking across the app, the widget and focus auto-complete,
    // which all read the due date, then write it; this repository is a process-wide singleton.
    val completionMutex = Mutex()

    fun observeTask(id: String): Flow<RecurringTaskWithOccurrences?> =
        recurringTaskDao.observeWithOccurrences(id).map { it?.withSortedSubtasks() }

    /** Every recurring task without its occurrence log, for callers that only need the schedule. */
    suspend fun getAllTasks(): List<RecurringTaskEntity> = withContext(Dispatchers.IO) {
        recurringTaskDao.getAll()
    }

    suspend fun getTaskById(id: String): RecurringTaskEntity? = withContext(Dispatchers.IO) {
        recurringTaskDao.getById(id)
    }

    suspend fun insertTask(task: RecurringTaskEntity) = withContext(Dispatchers.IO) {
        recurringTaskDao.insert(task)
    }

    suspend fun updateTask(task: RecurringTaskEntity) = withContext(Dispatchers.IO) {
        recurringTaskDao.update(task)
    }

    suspend fun deleteTask(task: RecurringTaskEntity) = withContext(Dispatchers.IO) {
        recurringTaskDao.delete(task)
    }

    suspend fun insertOccurrence(occurrence: RecurringOccurrenceEntity): Long =
        withContext(Dispatchers.IO) {
            recurringTaskDao.insertOccurrence(occurrence)
        }

    suspend fun deleteOccurrence(occurrence: RecurringOccurrenceEntity) =
        withContext(Dispatchers.IO) {
            recurringTaskDao.deleteOccurrence(occurrence)
        }

    suspend fun deleteOccurrenceById(id: Long) = withContext(Dispatchers.IO) {
        recurringTaskDao.deleteOccurrenceById(id)
    }

    suspend fun getLatestAdvancingOccurrenceId(taskId: String): Long? = withContext(Dispatchers.IO) {
        recurringTaskDao.getLatestAdvancingOccurrenceId(taskId)
    }

    suspend fun getSubtasks(taskId: String): List<RecurringSubtaskEntity> = withContext(Dispatchers.IO) {
        recurringTaskDao.getSubtasks(taskId)
    }

    suspend fun insertSubtask(subtask: RecurringSubtaskEntity) = withContext(Dispatchers.IO) {
        recurringTaskDao.insertSubtask(subtask)
    }

    suspend fun updateSubtask(subtask: RecurringSubtaskEntity) = withContext(Dispatchers.IO) {
        recurringTaskDao.updateSubtask(subtask)
    }

    suspend fun updateSubtasks(subtasks: List<RecurringSubtaskEntity>) = withContext(Dispatchers.IO) {
        recurringTaskDao.updateSubtasks(subtasks)
    }

    suspend fun deleteSubtask(subtask: RecurringSubtaskEntity) = withContext(Dispatchers.IO) {
        recurringTaskDao.deleteSubtask(subtask)
    }

    suspend fun resetSubtasks(taskId: String) = withContext(Dispatchers.IO) {
        recurringTaskDao.resetSubtasks(taskId)
    }

    suspend fun setPinnedAt(id: String, pinnedAt: Instant?) = withContext(Dispatchers.IO) {
        recurringTaskDao.setPinnedAt(id, pinnedAt)
    }
}
