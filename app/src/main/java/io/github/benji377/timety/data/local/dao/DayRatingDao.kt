package io.github.benji377.timety.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.benji377.timety.data.model.user.DayRatingEntity
import kotlinx.coroutines.flow.Flow

/** Data access object for end-of-day ratings. */
@Dao
interface DayRatingDao {
    @Query("SELECT * FROM day_ratings ORDER BY dayKey ASC")
    fun getAll(): Flow<List<DayRatingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(rating: DayRatingEntity)

    @Query("DELETE FROM day_ratings")
    fun clearAll(): Int
}
