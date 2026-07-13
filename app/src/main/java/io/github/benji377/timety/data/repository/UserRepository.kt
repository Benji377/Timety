package io.github.benji377.timety.data.repository

import io.github.benji377.timety.data.local.dao.UserDao
import io.github.benji377.timety.data.model.user.UserProfileEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.time.Instant

/** Repository for the user profile, wrapping [UserDao] with IO dispatching and XP bookkeeping. */
class UserRepository(
    private val userDao: UserDao
) {
    val userProfile: Flow<UserProfileEntity?> = userDao.getUserProfile()

    suspend fun getUserProfileSnapshot(): UserProfileEntity? = withContext(Dispatchers.IO) {
        userDao.getUserProfileSynchronous()
    }

    /** Creates a default user profile if one doesn't already exist. */
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

    /** Adds XP to the existing profile, creating a default profile first if none exists yet. */
    suspend fun addXp(amount: Int) = withContext(Dispatchers.IO) {
        val current = userDao.getUserProfileSynchronous()
        if (current != null) {
            // Clamped at zero: reverting a completion that never awarded XP in this install
            // (e.g. items restored as already-completed from a backup) must not push the total
            // negative, or the level math (sqrt of XP) breaks.
            userDao.updateUserProfile(
                current.copy(totalXp = (current.totalXp + amount).coerceAtLeast(0))
            )
        } else {
            userDao.insertUserProfile(
                UserProfileEntity(
                    name = "Bobert",
                    accountCreated = Instant.now(),
                    totalXp = amount.coerceAtLeast(0)
                )
            )
        }
    }
}
