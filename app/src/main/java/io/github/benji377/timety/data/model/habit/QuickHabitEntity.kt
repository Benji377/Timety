package io.github.benji377.timety.data.model.habit

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * A recurring interval reminder ("drink water every 2 h"). Unlike [HabitEntity] this is a pure
 * nag: it has no completions, streaks, or XP — just a name, a repeat interval, and an optional
 * active time-of-day window during which it fires.
 */
@Entity(tableName = "quick_habits")
data class QuickHabitEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val intervalMinutes: Int,          // Fire every N minutes.
    val startMinuteOfDay: Int? = null, // 480 = 08:00; null start and end together = all day.
    val endMinuteOfDay: Int? = null,   // 1200 = 20:00.
    val targetWeekdays: String? = null, // "[1,3,5]" (Mon=1..Sun=7); null or empty = every day.
    val isEnabled: Boolean = true,     // Pause without deleting.
    val createdAt: Instant,
)
