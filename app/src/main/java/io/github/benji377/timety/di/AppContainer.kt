package io.github.benji377.timety.di

import android.content.Context
import androidx.room.Room
import io.github.benji377.timety.data.local.ALL_MIGRATIONS
import io.github.benji377.timety.data.local.TimetyDatabase
import io.github.benji377.timety.data.repository.FocusRepository
import io.github.benji377.timety.data.repository.HabitRepository
import io.github.benji377.timety.data.repository.QuickHabitRepository
import io.github.benji377.timety.data.repository.SettingsRepository
import io.github.benji377.timety.data.repository.TaskRepository
import io.github.benji377.timety.data.repository.UserRepository
import io.github.benji377.timety.data.repository.dataStore
import io.github.benji377.timety.services.BackupService

/**
 * Provides access to the app's repositories and services, constructed once and held by the
 * [Application][io.github.benji377.timety.TimetyApplication].
 */
interface AppContainer {
    val taskRepository: TaskRepository
    val habitRepository: HabitRepository
    val quickHabitRepository: QuickHabitRepository
    val focusRepository: FocusRepository
    val userRepository: UserRepository
    val settingsRepository: SettingsRepository
    val backupService: BackupService
}

/** Default [AppContainer] that lazily builds the Room database and its dependent repositories. */
class DefaultAppContainer(private val context: Context) : AppContainer {
    private val database: TimetyDatabase by lazy {
        Room.databaseBuilder(
            context,
            TimetyDatabase::class.java,
            "timety_database"
        ).addMigrations(*ALL_MIGRATIONS).build()
    }

    override val taskRepository: TaskRepository by lazy {
        TaskRepository(database.taskDao())
    }

    override val habitRepository: HabitRepository by lazy {
        HabitRepository(database.habitDao())
    }

    override val quickHabitRepository: QuickHabitRepository by lazy {
        QuickHabitRepository(database.quickHabitDao())
    }

    override val focusRepository: FocusRepository by lazy {
        FocusRepository(database.focusDao())
    }

    override val userRepository: UserRepository by lazy {
        UserRepository(database.userDao())
    }

    override val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(context.dataStore)
    }

    override val backupService: BackupService by lazy {
        BackupService(context, database, settingsRepository)
    }
}
