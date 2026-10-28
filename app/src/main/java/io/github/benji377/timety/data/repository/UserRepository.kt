package io.github.benji377.timety.data.repository

import io.github.benji377.timety.data.local.dao.UserDao
import io.github.benji377.timety.data.model.user.UserProfileEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.time.Instant

/** Repository for the user profile, wrapping [UserDao] with IO dispatching. */
class UserRepository(
    private val userDao: UserDao
) {
    val userProfile: Flow<UserProfileEntity?> = userDao.getUserProfile()

    /** Creates a default user profile if one doesn't already exist. */
    suspend fun initializeIfNeeded() = withContext(Dispatchers.IO) {
        val current = userDao.getUserProfileSynchronous()
        if (current == null) {
            userDao.insertUserProfile(
                UserProfileEntity(
                    name = "Bobert",
                    accountCreated = Instant.now()
                )
            )
        }
    }

    suspend fun insertUserProfile(userProfile: UserProfileEntity) = withContext(Dispatchers.IO) {
        userDao.insertUserProfile(userProfile)
    }

    suspend fun updateUserProfile(userProfile: UserProfileEntity) = withContext(Dispatchers.IO) {
        userDao.updateUserProfile(userProfile)
    }
}
