package io.github.benji377.timety.services.backup

import io.github.benji377.timety.data.model.user.DayRating
import io.github.benji377.timety.data.model.user.DayRatingEntity
import io.github.benji377.timety.data.model.user.UserProfileEntity
import io.github.benji377.timety.util.ProfileImageStore
import org.json.JSONObject
import java.time.Instant

// Backup JSON for the user profile and day ratings.

private const val DEFAULT_NAME = "Bobert"

internal fun UserProfileEntity.toBackupJson(): JSONObject = JSONObject().apply {
    put("name", name)
    put("profileImagePath", orNull(profileImagePath))
    put("accountCreated", accountCreated.toString())
}

/** A fresh default profile when the backup has none. */
internal fun profileFromBackup(json: JSONObject?): UserProfileEntity {
    if (json == null) return UserProfileEntity(name = DEFAULT_NAME, accountCreated = Instant.now())
    return UserProfileEntity(
        name = json.stringOrNull("name") ?: DEFAULT_NAME,
        // Backups store only the path, not the image bytes, so a restored path is stale unless
        // the file happens to exist on this device (same-device restore).
        profileImagePath = ProfileImageStore.validOrNull(json.stringOrNull("profileImagePath")),
        accountCreated = json.instantOrNull("accountCreated") ?: Instant.now(),
    )
}

internal fun DayRatingEntity.toBackupJson(): JSONObject = JSONObject().apply {
    put("dayKey", dayKey)
    put("rating", rating)
    put("createdAt", createdAt.toString())
}

/** Null for a rating without a day or outside the known scale. */
internal fun dayRatingFromBackup(json: JSONObject): DayRatingEntity? {
    val dayKey = json.stringOrNull("dayKey") ?: return null
    val rating = DayRating.fromValue(json.optInt("rating")) ?: return null
    return DayRatingEntity(
        dayKey = dayKey,
        rating = rating.value,
        createdAt = json.instantOrNull("createdAt") ?: Instant.now(),
    )
}
