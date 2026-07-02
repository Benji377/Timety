package io.github.benji377.timety

import android.app.Application
import io.github.benji377.timety.di.AppContainer
import io.github.benji377.timety.di.DefaultAppContainer

class TimetyApplication : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}
