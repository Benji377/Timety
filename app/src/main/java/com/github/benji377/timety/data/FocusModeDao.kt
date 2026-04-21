package com.github.benji377.timety.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusModeDao {
    @Query("SELECT * FROM focus_mode")
    fun getAllFocusModes(): Flow<List<FocusMode>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFocusMode(focusMode: FocusMode)

    @Update
    suspend fun updateFocusMode(focusMode: FocusMode)

    @Delete
    suspend fun deleteFocusMode(focusMode: FocusMode)
}
