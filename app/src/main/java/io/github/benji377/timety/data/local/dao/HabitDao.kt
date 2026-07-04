package io.github.benji377.timety.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import io.github.benji377.timety.data.model.habit.HabitCompletionEntity
import io.github.benji377.timety.data.model.habit.HabitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits ORDER BY createdAt DESC")
    fun getAllHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habit_completions")
    fun getAllCompletions(): Flow<List<HabitCompletionEntity>>

    @Query("SELECT * FROM habits WHERE id = :id")
    fun getHabitById(id: String): HabitEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertHabit(habit: HabitEntity)

    @Update
    fun updateHabit(habit: HabitEntity)

    @Delete
    fun deleteHabit(habit: HabitEntity)

    @Query("DELETE FROM habits")
    fun clearAll(): Int

    // Completions
    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId")
    fun getCompletionsForHabit(habitId: String): Flow<List<HabitCompletionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertCompletion(completion: HabitCompletionEntity)

    @Delete
    fun deleteCompletion(completion: HabitCompletionEntity)

    @Query("DELETE FROM habit_completions WHERE habitId = :habitId AND completionDate = :date")
    fun deleteCompletionByDate(habitId: String, date: Long): Int
}
