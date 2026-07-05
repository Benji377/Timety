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

class TimetyApplication : Application() {
    lateinit var container: AppContainer

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
        NotificationService(this).ensureChannels()

        // Self-heal all reminder alarms on process start: a force-stop wipes them, and on
        // a fresh install nothing else arms the daily notifications until a reboot.
        applicationScope.launch {
            try {
                ReminderScheduler.resyncAll(this@TimetyApplication)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
