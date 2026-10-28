package io.github.benji377.timety.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import io.github.benji377.timety.data.model.task.RecurringOccurrenceEntity
import io.github.benji377.timety.data.model.task.RecurringTaskEntity
import io.github.benji377.timety.data.model.task.RecurringTaskWithOccurrences
import kotlinx.coroutines.flow.Flow

/** Data access object for recurring tasks and their logged occurrences. */
@Dao
interface RecurringTaskDao {
    @Transaction
    @Query("SELECT * FROM recurring_tasks ORDER BY dueDate ASC")
    fun getAllWithOccurrences(): Flow<List<RecurringTaskWithOccurrences>>

    @Query("SELECT * FROM recurring_tasks WHERE id = :id")
    fun getById(id: String): RecurringTaskEntity?

    @Query("SELECT * FROM recurring_tasks ORDER BY dueDate ASC")
    fun getAll(): List<RecurringTaskEntity>

    @Transaction
    @Query("SELECT * FROM recurring_tasks WHERE id = :id")
    fun observeWithOccurrences(id: String): Flow<RecurringTaskWithOccurrences?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(task: RecurringTaskEntity)

    @Update
    fun update(task: RecurringTaskEntity)

    @Delete
    fun delete(task: RecurringTaskEntity)

    @Query("DELETE FROM recurring_tasks")
    fun clearAll(): Int

    // Occurrences
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOccurrence(occurrence: RecurringOccurrenceEntity): Long

    @Delete
    fun deleteOccurrence(occurrence: RecurringOccurrenceEntity)

    @Query("DELETE FROM recurring_occurrences WHERE id = :id")
    fun deleteOccurrenceById(id: Long)

    /** The newest occurrence that advanced the task's due date, or null if none did. */
    @Query(
        "SELECT MAX(id) FROM recurring_occurrences " +
                "WHERE recurringTaskId = :taskId AND previousDueDate IS NOT NULL"
    )
    fun getLatestAdvancingOccurrenceId(taskId: String): Long?
}
