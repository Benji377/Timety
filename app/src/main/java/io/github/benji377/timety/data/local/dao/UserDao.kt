package io.github.benji377.timety.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import io.github.benji377.timety.data.model.user.UserProfileEntity
import kotlinx.coroutines.flow.Flow

/** Data access object for the singleton user profile. */
@Dao
interface UserDao {
    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    fun getUserProfile(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    fun getUserProfileSynchronous(): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertUserProfile(userProfile: UserProfileEntity)

    @Update
    fun updateUserProfile(userProfile: UserProfileEntity)
}
