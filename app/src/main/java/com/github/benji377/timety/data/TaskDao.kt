package com.github.benji377.timety.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM task ORDER BY dueDate ASC")
    fun getAllTasks(): Flow<List<Task>>

    @Query("SELECT * FROM task WHERE id = :id")
    suspend fun getTaskById(id: Int): Task?

    @Query("SELECT * FROM task WHERE status = :status")
    fun getTasksByStatus(status: TaskStatus): Flow<List<Task>>

    @Query("SELECT * FROM task WHERE dueDate BETWEEN :start AND :end ORDER BY dueDate ASC")
    fun getTasksInRange(start: Long, end: Long): Flow<List<Task>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task)

    @Update
    suspend fun updateTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)

    @Query("UPDATE task SET status = :status WHERE id = :taskId")
    suspend fun updateTaskStatus(taskId: Int, status: TaskStatus)

    @Query("UPDATE task SET status = :newStatus WHERE status = :oldStatus AND dueDate < :now")
    suspend fun updateOverdueTasks(now: Long, oldStatus: TaskStatus, newStatus: TaskStatus)
}
