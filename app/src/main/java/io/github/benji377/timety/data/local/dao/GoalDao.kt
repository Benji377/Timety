package io.github.benji377.timety.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import io.github.benji377.timety.data.model.goal.GoalEntity
import io.github.benji377.timety.data.model.goal.GoalEntryEntity
import io.github.benji377.timety.data.model.goal.GoalWithEntries
import kotlinx.coroutines.flow.Flow

/** Data access object for goals and their progress entries. */
@Dao
interface GoalDao {
    @Transaction
    @Query("SELECT * FROM goals ORDER BY createdAt DESC")
    fun getAllGoalsWithEntries(): Flow<List<GoalWithEntries>>

    @Transaction
    @Query("SELECT * FROM goals WHERE id = :id")
    fun getGoalWithEntriesById(id: String): GoalWithEntries?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertGoal(goal: GoalEntity)

    @Update
    fun updateGoal(goal: GoalEntity)

    @Delete
    fun deleteGoal(goal: GoalEntity)

    @Query("DELETE FROM goals")
    fun clearAll(): Int

    // Entries
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertEntry(entry: GoalEntryEntity)

    @Delete
    fun deleteEntry(entry: GoalEntryEntity)
}
