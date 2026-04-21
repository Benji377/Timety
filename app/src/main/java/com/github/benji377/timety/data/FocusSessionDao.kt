package com.github.benji377.timety.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusSessionDao {
    @Query("SELECT * FROM focus_session ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<FocusSession>>

    @Query("SELECT * FROM focus_session WHERE taskId = :taskId ORDER BY startTime DESC")
    fun getSessionsForTask(taskId: Int): Flow<List<FocusSession>>

    @Query("SELECT * FROM focus_session WHERE categoryId = :categoryId ORDER BY startTime DESC")
    fun getSessionsByCategory(categoryId: Int): Flow<List<FocusSession>>

    @Query("SELECT * FROM focus_session WHERE startTime >= :startOfDay AND startTime < :endOfDay")
    fun getSessionsForDay(startOfDay: Long, endOfDay: Long): Flow<List<FocusSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: FocusSession)

    @Delete
    suspend fun deleteSession(session: FocusSession)
}
