package io.github.benji377.timety.testutil

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import io.github.benji377.timety.data.local.TimetyDatabase
import io.github.benji377.timety.data.repository.DayRatingRepository
import io.github.benji377.timety.data.repository.FocusRepository
import io.github.benji377.timety.data.repository.GoalRepository
import io.github.benji377.timety.data.repository.HabitRepository
import io.github.benji377.timety.data.repository.QuickHabitRepository
import io.github.benji377.timety.data.repository.RecurringTaskRepository
import io.github.benji377.timety.data.repository.SettingsRepository
import io.github.benji377.timety.data.repository.TaskRepository
import io.github.benji377.timety.data.repository.UserRepository
import io.github.benji377.timety.di.AppContainer
import io.github.benji377.timety.services.BackupService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import java.io.File
import java.util.UUID

/**
 * An [AppContainer] backed by an in-memory database and a throwaway settings store, so workflow
 * tests can drive the real repositories and view models without touching the on-device data.
 *
 * Mirrors `DefaultAppContainer`'s wiring exactly; if a repository gains a dependency there, it has
 * to gain the same one here or the tests stop reflecting production.
 */
class TestAppContainer(context: Context) : AppContainer {

    /** Exposed so tests can reach a DAO directly when a repository has no method for the setup they need. */
    val database: TimetyDatabase =
        Room.inMemoryDatabaseBuilder(context, TimetyDatabase::class.java).build()

    private val dataStoreScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Never name this file "settings": `Context.dataStore` is a process-wide delegate on that name
    // and TimetyApplication.onCreate has already opened it by the time any test runs, so a second
    // DataStore over the same file throws. A per-instance name also isolates tests from each other.
    private val dataStoreFile =
        File(context.cacheDir, "workflow_test_${UUID.randomUUID()}.preferences_pb")

    private val testDataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
        scope = dataStoreScope,
        produceFile = { dataStoreFile },
    )

    override val taskRepository = TaskRepository(database.taskDao())
    override val recurringTaskRepository = RecurringTaskRepository(database.recurringTaskDao())
    override val habitRepository = HabitRepository(database.habitDao())
    override val quickHabitRepository = QuickHabitRepository(database.quickHabitDao())
    override val goalRepository = GoalRepository(database.goalDao())
    override val focusRepository = FocusRepository(database.focusDao())
    override val userRepository = UserRepository(database.userDao())
    override val dayRatingRepository = DayRatingRepository(database.dayRatingDao())
    override val settingsRepository = SettingsRepository(testDataStore)
    override val backupService = BackupService(context, database, settingsRepository)

    /** Releases the database and settings file; call from `@After` in every test that builds one. */
    fun close() {
        database.close()
        dataStoreScope.cancel()
        dataStoreFile.delete()
    }
}
