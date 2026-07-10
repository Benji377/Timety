package io.github.benji377.timety.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import io.github.benji377.timety.data.model.task.SubtaskEntity
import io.github.benji377.timety.data.model.task.TaskCategoryEntity
import io.github.benji377.timety.data.model.task.TaskEntity
import io.github.benji377.timety.data.model.task.TaskWithSubtasks
import kotlinx.coroutines.flow.Flow

/** Data access object for tasks, task categories, and subtasks. */
@Dao
interface TaskDao {
    @Transaction
    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    fun getAllTasks(): Flow<List<TaskWithSubtasks>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    fun getTaskById(id: String): TaskEntity?


    @Query(
        "SELECT * FROM tasks WHERE isCompleted = 0 AND dueDate IS NOT NULL AND dueDate < :deadline " +
                "ORDER BY dueDate ASC"
    )
    fun getOpenTasksDueBefore(deadline: java.time.Instant): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTask(task: TaskEntity)

    @Update
    fun updateTask(task: TaskEntity)

    @Delete
    fun deleteTask(task: TaskEntity)

    @Query("DELETE FROM tasks")
    fun clearAll(): Int

    // Categories
    @Query("SELECT * FROM task_categories ORDER BY name COLLATE NOCASE ASC")
    fun getAllCategories(): Flow<List<TaskCategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertCategory(category: TaskCategoryEntity)

    // Creation path: an existing category with the same name wins, keeping its color.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertCategoryIfAbsent(category: TaskCategoryEntity)

    @Delete
    fun deleteCategory(category: TaskCategoryEntity)

    @Query("DELETE FROM task_categories")
    fun clearAllCategories()

    @Query("SELECT DISTINCT category FROM tasks WHERE category != ''")
    fun getDistinctTaskCategoryNames(): List<String>

    @Query("UPDATE tasks SET category = :newName WHERE category = :oldName")
    fun renameTaskCategory(oldName: String, newName: String)

    @Transaction
    fun updateCategoryAndTasks(oldName: String, updated: TaskCategoryEntity) {
        insertCategory(updated)
        if (updated.name != oldName) renameTaskCategory(oldName, updated.name)
    }

    @Transaction
    fun deleteCategoryAndClearTasks(category: TaskCategoryEntity) {
        deleteCategory(category)
        renameTaskCategory(category.name, "")
    }

    // Subtasks
    @Query("SELECT * FROM subtasks WHERE taskId = :taskId")
    fun getSubtasksForTask(taskId: String): Flow<List<SubtaskEntity>>

    @Query("SELECT * FROM subtasks WHERE taskId = :taskId")
    fun getSubtasksForTaskOnce(taskId: String): List<SubtaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSubtask(subtask: SubtaskEntity)

    @Update
    fun updateSubtask(subtask: SubtaskEntity)

    @Delete
    fun deleteSubtask(subtask: SubtaskEntity)
}
