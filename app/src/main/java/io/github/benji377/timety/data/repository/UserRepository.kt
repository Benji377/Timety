package io.github.benji377.timety.data.repository

import io.github.benji377.timety.data.local.dao.UserDao
import io.github.benji377.timety.data.model.user.UserProfileEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.time.Instant

class UserRepository(
    private val userDao: UserDao
) {
    val userProfile: Flow<UserProfileEntity?> = userDao.getUserProfile()

    suspend fun getUserProfileSnapshot(): UserProfileEntity? = withContext(Dispatchers.IO) {
        userDao.getUserProfileSynchronous()
    }

    suspend fun initializeIfNeeded() = withContext(Dispatchers.IO) {
        val current = userDao.getUserProfileSynchronous()
        if (current == null) {
            userDao.insertUserProfile(
                UserProfileEntity(
                    name = "Bobert",
                    accountCreated = Instant.now(),
                    totalXp = 0
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

    suspend fun addXp(amount: Int) = withContext(Dispatchers.IO) {
        val current = userDao.getUserProfileSynchronous()
        if (current != null) {
            userDao.updateUserProfile(current.copy(totalXp = current.totalXp + amount))
        } else {
            userDao.insertUserProfile(
                UserProfileEntity(
                    name = "Bobert",
                    accountCreated = Instant.now(),
                    totalXp = amount
                )
            )
        }
    }
}
