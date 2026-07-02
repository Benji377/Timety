package io.github.benji377.timety.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import io.github.benji377.timety.data.local.dao.*
import io.github.benji377.timety.data.model.focus.*
import io.github.benji377.timety.data.model.habit.*
import io.github.benji377.timety.data.model.task.*
import io.github.benji377.timety.data.model.user.*

@Database(
    entities = [
        TaskEntity::class,
        SubtaskEntity::class,
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
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class TimetyDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun habitDao(): HabitDao
    abstract fun focusDao(): FocusDao
    abstract fun userDao(): UserDao
}
