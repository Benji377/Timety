package io.github.benji377.timety

import android.app.Application
import io.github.benji377.timety.di.AppContainer
import io.github.benji377.timety.di.DefaultAppContainer
import io.github.benji377.timety.migration.FlutterMigration
import io.github.benji377.timety.services.NotificationService
import io.github.benji377.timety.services.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Application entry point that builds the DI container, runs the one-time legacy-data migration,
 * and resyncs reminder alarms on process start.
 */
class TimetyApplication : Application() {
    lateinit var container: AppContainer

    // MainActivity holds off composing the UI until this flips: ViewModels seed default
    // data (user profile, tags, system modes) into an empty database on first use, which
    // would defeat the migration's empty-database guard if it ran concurrently.
    private val _startupComplete = MutableStateFlow(false)
    val startupComplete: StateFlow<Boolean> = _startupComplete.asStateFlow()

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
        NotificationService(this).ensureChannels()

        applicationScope.launch {
            // One-shot legacy-data migration (docs/flutter-migration.md).
            // Must finish before the UI and the reminder resync touch the database.
            try {
                FlutterMigration.runIfNeeded(this@TimetyApplication, container)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            _startupComplete.value = true

            // Self-heal all reminder alarms on process start: a force-stop wipes them, and on
            // a fresh install nothing else arms the daily notifications until a reboot. Runs
            // after the migration so freshly imported task reminders get scheduled too.
            try {
                ReminderScheduler.resyncAll(this@TimetyApplication)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
