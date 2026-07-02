package io.github.benji377.timety.data.repository

import io.github.benji377.timety.data.local.dao.UserDao
import io.github.benji377.timety.data.model.user.UserProfileEntity
import kotlinx.coroutines.flow.Flow
class UserRepository(
    private val userDao: UserDao
) {
    val userProfile: Flow<UserProfileEntity?> = userDao.getUserProfile()

    suspend fun insertUserProfile(userProfile: UserProfileEntity) {
        userDao.insertUserProfile(userProfile)
    }

    suspend fun updateUserProfile(userProfile: UserProfileEntity) {
        userDao.updateUserProfile(userProfile)
    }
}
