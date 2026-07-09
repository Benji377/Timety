package io.github.benji377.timety.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import io.github.benji377.timety.data.model.habit.QuickHabitEntity
import kotlinx.coroutines.flow.Flow

/** Data access object for quick habits (interval reminders). */
@Dao
interface QuickHabitDao {
    @Query("SELECT * FROM quick_habits ORDER BY createdAt DESC")
    fun getAll(): Flow<List<QuickHabitEntity>>

    @Query("SELECT * FROM quick_habits")
    fun getAllSynchronous(): List<QuickHabitEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(quickHabit: QuickHabitEntity)

    @Update
    fun update(quickHabit: QuickHabitEntity)

    @Delete
    fun delete(quickHabit: QuickHabitEntity)

    @Query("DELETE FROM quick_habits")
    fun clearAll(): Int
}
