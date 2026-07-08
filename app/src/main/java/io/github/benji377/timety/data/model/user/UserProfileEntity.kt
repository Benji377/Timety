package io.github.benji377.timety.data.model.user

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

/** The single local user profile, tracking XP and unlocked achievements. */
@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey
    val id: Int = 1, // Singleton profile.
    val name: String,
    val profileImagePath: String? = null,
    val accountCreated: Instant,
    val unlockedAchievements: List<String> = emptyList(), // Requires a Room TypeConverter.
    val totalXp: Int = 0
)
