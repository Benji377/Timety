package io.github.benji377.timety.data.repository

import io.github.benji377.timety.data.local.dao.RecurringTaskDao
import io.github.benji377.timety.data.model.task.RecurringOccurrenceEntity
import io.github.benji377.timety.data.model.task.RecurringTaskEntity
import io.github.benji377.timety.data.model.task.RecurringTaskWithOccurrences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/** Repository for recurring tasks and their occurrences, wrapping [RecurringTaskDao] with IO dispatching. */
class RecurringTaskRepository(
    private val recurringTaskDao: RecurringTaskDao
) {
    val allRecurringTasks: Flow<List<RecurringTaskWithOccurrences>> =
        recurringTaskDao.getAllWithOccurrences()

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
}
