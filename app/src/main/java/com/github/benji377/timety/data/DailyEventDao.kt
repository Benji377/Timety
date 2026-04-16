package com.github.benji377.timety.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyEventDao {
    @Query("SELECT * FROM daily_event WHERE timestamp >= :startOfDay AND timestamp < :endOfDay ORDER BY timestamp ASC")
    fun getEventsForDay(startOfDay: Long, endOfDay: Long): Flow<List<DailyEvent>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: DailyEvent)

    @Delete
    suspend fun deleteEvent(event: DailyEvent)
}
