package com.github.benji377.timety

import android.app.Application
import com.github.benji377.timety.data.AppDatabase
import com.github.benji377.timety.data.MainRepository

class TimetyApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { MainRepository(database) }
}
