package io.github.benji377.timety.di

import android.content.Context
import androidx.room.Room
import io.github.benji377.timety.data.local.TimetyDatabase
import io.github.benji377.timety.data.repository.FocusRepository
import io.github.benji377.timety.data.repository.HabitRepository
import io.github.benji377.timety.data.repository.TaskRepository
import io.github.benji377.timety.data.repository.UserRepository

interface AppContainer {
    val taskRepository: TaskRepository
    val habitRepository: HabitRepository
    val focusRepository: FocusRepository
    val userRepository: UserRepository
}

class DefaultAppContainer(private val context: Context) : AppContainer {
    private val database: TimetyDatabase by lazy {
        Room.databaseBuilder(
            context,
            TimetyDatabase::class.java,
            "timety_database"
        ).build()
    }

    override val taskRepository: TaskRepository by lazy {
        TaskRepository(database.taskDao())
    }

    override val habitRepository: HabitRepository by lazy {
        HabitRepository(database.habitDao())
    }

    override val focusRepository: FocusRepository by lazy {
        FocusRepository(database.focusDao())
    }

    override val userRepository: UserRepository by lazy {
        UserRepository(database.userDao())
    }
}
