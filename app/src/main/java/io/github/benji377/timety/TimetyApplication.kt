package io.github.benji377.timety

import android.app.Application
import io.github.benji377.timety.di.AppContainer
import io.github.benji377.timety.di.DefaultAppContainer
import io.github.benji377.timety.services.NotificationService
import io.github.benji377.timety.services.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Application entry point that builds the DI container and resyncs reminder alarms on process start.
 */
class TimetyApplication : Application() {
    lateinit var container: AppContainer

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
        NotificationService(this).ensureChannels()

        applicationScope.launch {
            // Self-heal all reminder alarms on process start: a force-stop wipes them, and on
            // a fresh install nothing else arms the daily notifications until a reboot.
            try {
                ReminderScheduler.resyncAll(this@TimetyApplication)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
