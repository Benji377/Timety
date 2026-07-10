package io.github.benji377.timety.data.repository

import io.github.benji377.timety.data.local.dao.DayRatingDao
import io.github.benji377.timety.data.model.user.DayRatingEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/** Repository for end-of-day ratings, wrapping [DayRatingDao] with IO dispatching. */
class DayRatingRepository(
    private val dayRatingDao: DayRatingDao
) {
    val allRatings: Flow<List<DayRatingEntity>> = dayRatingDao.getAll()

    suspend fun upsert(rating: DayRatingEntity) = withContext(Dispatchers.IO) {
        dayRatingDao.upsert(rating)
    }
}
