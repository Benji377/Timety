package io.github.benji377.timety.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import io.github.benji377.timety.data.local.dao.FocusDao
import io.github.benji377.timety.data.local.dao.HabitDao
import io.github.benji377.timety.data.local.dao.TaskDao
import io.github.benji377.timety.data.local.dao.UserDao
import io.github.benji377.timety.data.model.focus.DistractionEntity
import io.github.benji377.timety.data.model.focus.FocusModeEntity
import io.github.benji377.timety.data.model.focus.FocusSessionEntity
import io.github.benji377.timety.data.model.focus.FocusTagEntity
import io.github.benji377.timety.data.model.focus.SessionPhaseEntity
import io.github.benji377.timety.data.model.habit.HabitCompletionEntity
import io.github.benji377.timety.data.model.habit.HabitEntity
import io.github.benji377.timety.data.model.task.SubtaskEntity
import io.github.benji377.timety.data.model.task.TaskCategoryEntity
import io.github.benji377.timety.data.model.task.TaskEntity
import io.github.benji377.timety.data.model.user.UserProfileEntity

@Database(
    entities = [
        TaskEntity::class,
        SubtaskEntity::class,
        TaskCategoryEntity::class,
        FocusModeEntity::class,
        SessionPhaseEntity::class,
        FocusSessionEntity::class,
        DistractionEntity::class,
        FocusTagEntity::class,
        HabitEntity::class,
        HabitCompletionEntity::class,
        UserProfileEntity::class
    ],
    version = 1,
    exportSchema = true
)
/** Room database for Timety, exposing DAOs for tasks, habits, focus sessions, and the user profile. */
@TypeConverters(Converters::class)
abstract class TimetyDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun habitDao(): HabitDao
    abstract fun focusDao(): FocusDao
    abstract fun userDao(): UserDao
}
