package io.github.benji377.timety.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import io.github.benji377.timety.data.model.habit.HabitCompletionEntity
import io.github.benji377.timety.data.model.habit.HabitEntity
import io.github.benji377.timety.data.model.habit.HabitStackEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/** Data access object for habits and their completion records. */
@Dao
interface HabitDao {
    @Query("SELECT * FROM habits ORDER BY sortOrder ASC, createdAt DESC")
    fun getAllHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habit_completions")
    fun getAllCompletions(): Flow<List<HabitCompletionEntity>>

    @Query("SELECT * FROM habits WHERE id = :id")
    fun getHabitById(id: String): HabitEntity?

    @Query("SELECT * FROM habit_completions WHERE completionDate >= :cutoff")
    fun getCompletionsSince(cutoff: java.time.Instant): List<HabitCompletionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertHabit(habit: HabitEntity)

    @Update
    fun updateHabit(habit: HabitEntity)

    @Update
    fun updateHabits(habits: List<HabitEntity>)

    @Delete
    fun deleteHabit(habit: HabitEntity)

    @Query("DELETE FROM habits")
    fun clearAll(): Int

    // Completions
    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId")
    fun getCompletionsForHabit(habitId: String): Flow<List<HabitCompletionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertCompletion(completion: HabitCompletionEntity): Long

    @Delete
    fun deleteCompletion(completion: HabitCompletionEntity)

    /** Sets or clears just the pin, so pinning never writes back a stale copy of the habit. */
    @Query("UPDATE habits SET pinnedAt = :pinnedAt WHERE id = :id")
    fun setPinnedAt(id: String, pinnedAt: Instant?)

    // Stacks
    @Query("SELECT * FROM habit_stacks")
    fun getAllStacks(): Flow<List<HabitStackEntity>>

    @Query("SELECT * FROM habit_stacks")
    fun getStacks(): List<HabitStackEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertStack(stack: HabitStackEntity)

    @Query("DELETE FROM habit_stacks WHERE name = :name")
    fun deleteStack(name: String)

    @Query("UPDATE habits SET stackName = :newName WHERE stackName = :oldName")
    fun moveStackMembers(oldName: String, newName: String)

    // A stack without a row is in order, so a new stack gets that row here.
    @Query(
        "INSERT OR IGNORE INTO habit_stacks (name, inOrder) " +
                "SELECT DISTINCT stackName, 1 FROM habits WHERE stackName IS NOT NULL"
    )
    fun insertMissingStacks()

    @Query("DELETE FROM habit_stacks WHERE name NOT IN (SELECT stackName FROM habits WHERE stackName IS NOT NULL)")
    fun deleteEmptyStacks()

    /** Gives every stack a row and drops rows whose stack has no habits left. */
    @Transaction
    fun syncStacks() {
        insertMissingStacks()
        deleteEmptyStacks()
    }

    /** Renames [oldName] to [newName] on the row and every member habit, and sets [inOrder]. */
    @Transaction
    fun renameStack(oldName: String, newName: String, inOrder: Boolean) {
        upsertStack(HabitStackEntity(newName, inOrder))
        if (newName != oldName) {
            moveStackMembers(oldName, newName)
            deleteStack(oldName)
        }
    }
}
