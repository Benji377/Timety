package io.github.benji377.timety.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import io.github.benji377.timety.data.model.tag.GoalTagEntity
import io.github.benji377.timety.data.model.tag.HabitTagEntity
import io.github.benji377.timety.data.model.tag.ItemTag
import io.github.benji377.timety.data.model.tag.RecurringTaskTagEntity
import io.github.benji377.timety.data.model.tag.SessionTagEntity
import io.github.benji377.timety.data.model.tag.TagEntity
import io.github.benji377.timety.data.model.tag.TaggedItemType
import io.github.benji377.timety.data.model.tag.TaskTagEntity
import kotlinx.coroutines.flow.Flow

/** Data access object for tags and their attachments to tasks, habits, goals, and sessions. */
@Dao
interface TagDao {
    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun getAllTags(): Flow<List<TagEntity>>

    // The name column compares without case.
    @Query("SELECT * FROM tags WHERE name = :name LIMIT 1")
    fun getTagByName(name: String): TagEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertTag(tag: TagEntity)

    // Never INSERT OR REPLACE: it deletes the row first, which would cascade to every attachment.
    @Update(onConflict = OnConflictStrategy.IGNORE)
    fun updateTag(tag: TagEntity)

    @Delete
    fun deleteTag(tag: TagEntity)

    @Query("DELETE FROM tags")
    fun clearAllTags()

    // An existing tag with the same name wins, keeping its color.
    @Transaction
    fun getOrCreateTag(tag: TagEntity): TagEntity =
        getTagByName(tag.name) ?: tag.also { insertTag(it) }

    @Query(
        "SELECT task_tags.taskId AS itemId, tags.* FROM task_tags " +
                "JOIN tags ON tags.id = task_tags.tagId ORDER BY tags.name ASC"
    )
    fun getTaskTags(): Flow<List<ItemTag>>

    @Query(
        "SELECT recurring_task_tags.recurringTaskId AS itemId, tags.* FROM recurring_task_tags " +
                "JOIN tags ON tags.id = recurring_task_tags.tagId ORDER BY tags.name ASC"
    )
    fun getRecurringTaskTags(): Flow<List<ItemTag>>

    @Query(
        "SELECT habit_tags.habitId AS itemId, tags.* FROM habit_tags " +
                "JOIN tags ON tags.id = habit_tags.tagId ORDER BY tags.name ASC"
    )
    fun getHabitTags(): Flow<List<ItemTag>>

    @Query(
        "SELECT goal_tags.goalId AS itemId, tags.* FROM goal_tags " +
                "JOIN tags ON tags.id = goal_tags.tagId ORDER BY tags.name ASC"
    )
    fun getGoalTags(): Flow<List<ItemTag>>

    @Query(
        "SELECT session_tags.sessionId AS itemId, tags.* FROM session_tags " +
                "JOIN tags ON tags.id = session_tags.tagId ORDER BY tags.name ASC"
    )
    fun getSessionTags(): Flow<List<ItemTag>>

    fun getItemTags(type: TaggedItemType): Flow<List<ItemTag>> = when (type) {
        TaggedItemType.TASK -> getTaskTags()
        TaggedItemType.RECURRING_TASK -> getRecurringTaskTags()
        TaggedItemType.HABIT -> getHabitTags()
        TaggedItemType.GOAL -> getGoalTags()
        TaggedItemType.SESSION -> getSessionTags()
    }

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertTaskTags(tags: List<TaskTagEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertRecurringTaskTags(tags: List<RecurringTaskTagEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertHabitTags(tags: List<HabitTagEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertGoalTags(tags: List<GoalTagEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertSessionTags(tags: List<SessionTagEntity>)

    @Query("DELETE FROM task_tags WHERE taskId = :id")
    fun clearTaskTags(id: String)

    @Query("DELETE FROM recurring_task_tags WHERE recurringTaskId = :id")
    fun clearRecurringTaskTags(id: String)

    @Query("DELETE FROM habit_tags WHERE habitId = :id")
    fun clearHabitTags(id: String)

    @Query("DELETE FROM goal_tags WHERE goalId = :id")
    fun clearGoalTags(id: String)

    @Query("DELETE FROM session_tags WHERE sessionId = :id")
    fun clearSessionTags(id: String)

    /** Replaces the tags on one item. */
    @Transaction
    fun setItemTags(type: TaggedItemType, itemId: String, tagIds: Collection<String>) {
        when (type) {
            TaggedItemType.TASK -> {
                clearTaskTags(itemId)
                insertTaskTags(tagIds.map { TaskTagEntity(itemId, it) })
            }

            TaggedItemType.RECURRING_TASK -> {
                clearRecurringTaskTags(itemId)
                insertRecurringTaskTags(tagIds.map { RecurringTaskTagEntity(itemId, it) })
            }

            TaggedItemType.HABIT -> {
                clearHabitTags(itemId)
                insertHabitTags(tagIds.map { HabitTagEntity(itemId, it) })
            }

            TaggedItemType.GOAL -> {
                clearGoalTags(itemId)
                insertGoalTags(tagIds.map { GoalTagEntity(itemId, it) })
            }

            TaggedItemType.SESSION -> {
                clearSessionTags(itemId)
                insertSessionTags(tagIds.map { SessionTagEntity(itemId, it) })
            }
        }
    }
}
