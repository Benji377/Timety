package io.github.benji377.timety.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import io.github.benji377.timety.data.model.task.SubtaskEntity
import io.github.benji377.timety.data.model.task.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Transaction
    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    fun getAllTasks(): Flow<List<io.github.benji377.timety.data.model.task.TaskWithSubtasks>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    fun getTaskById(id: String): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTask(task: TaskEntity)

    @Update
    fun updateTask(task: TaskEntity)

    @Delete
    fun deleteTask(task: TaskEntity)

    @Query("DELETE FROM tasks")
    fun clearAll(): Int

    // Subtasks
    @Query("SELECT * FROM subtasks WHERE taskId = :taskId")
    fun getSubtasksForTask(taskId: String): Flow<List<SubtaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSubtask(subtask: SubtaskEntity)

    @Update
    fun updateSubtask(subtask: SubtaskEntity)

    @Delete
    fun deleteSubtask(subtask: SubtaskEntity)
}
